/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.io.sstable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.metadata.IMetadataSerializer;
import org.apache.cassandra.io.sstable.metadata.LegacyMetadataSerializer;
import org.apache.cassandra.io.sstable.metadata.MetadataSerializer;
import org.apache.cassandra.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static org.apache.cassandra.io.sstable.Component.separator;

/**
 * A SSTable is described by the keyspace and column family it contains data
 * for, a generation (where higher generations contain more recent data) and
 * an alphabetic version string.
 *
 * A descriptor can be marked as temporary, which influences generated filenames.
 */
public class Descriptor
{
    @NotNull
    public static String TMP_EXT = ".tmp";

    /** canonicalized path to the directory where SSTable resides */
    @NotNull
    public final File directory;
    /** version has the following format: <code>[a-z]+</code> */
    @NotNull
    public final Version version;
    @NotNull
    public final String ksname;
    @NotNull
    public final String cfname;
    public final int generation;
    @NotNull
    public final SSTableFormat.Type formatType;
    /** digest component - might be {@code null} for old, legacy sstables */
    public final Component digestComponent;
    private final int hashCode;

    /**
     * A descriptor that assumes CURRENT_VERSION.
     */
    @VisibleForTesting
    public Descriptor(@NotNull File directory, @NotNull String ksname, @NotNull String cfname, int generation)
    {
        this(SSTableFormat.Type.current().info.getLatestVersion(), directory, ksname, cfname, generation, SSTableFormat.Type.current(), null);
    }

    /**
     * Constructor for sstable writers only.
     */
    public Descriptor(@NotNull File directory, @NotNull String ksname, @NotNull String cfname, int generation, @NotNull SSTableFormat.Type formatType)
    {
        this(formatType.info.getLatestVersion(), directory, ksname, cfname, generation, formatType, Component.digestFor(formatType.info.getLatestVersion().uncompressedChecksumType()));
    }

    @VisibleForTesting
    public Descriptor(String version, @NotNull File directory, @NotNull String ksname, @NotNull String cfname, int generation, @NotNull SSTableFormat.Type formatType)
    {
        this(formatType.info.getVersion(version), directory, ksname, cfname, generation, formatType, Component.digestFor(formatType.info.getLatestVersion().uncompressedChecksumType()));
    }

    public Descriptor(@NotNull Version version, @NotNull File directory, @NotNull String ksname, @NotNull String cfname, int generation, @NotNull SSTableFormat.Type formatType, Component digestComponent)
    {
        assert version != null && directory != null && ksname != null && cfname != null && formatType.info.getLatestVersion().getClass().equals(version.getClass());
        this.version = version;
        try
        {
            this.directory = directory.getCanonicalFile();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
        this.ksname = ksname;
        this.cfname = cfname;
        this.generation = generation;
        this.formatType = formatType;
        this.digestComponent = digestComponent;

        hashCode = Objects.hashCode(version, this.directory, generation, ksname, cfname, formatType);
    }

    @NotNull
    public Descriptor withGeneration(int newGeneration)
    {
        return new Descriptor(version, directory, ksname, cfname, newGeneration, formatType, digestComponent);
    }

    @NotNull
    public Descriptor withFormatType(@NotNull SSTableFormat.Type newType)
    {
        return new Descriptor(newType.info.getLatestVersion(), directory, ksname, cfname, generation, newType, digestComponent);
    }

    @NotNull
    public Descriptor withDigestComponent(Component newDigestComponent)
    {
        return new Descriptor(version, directory, ksname, cfname, generation, formatType, newDigestComponent);
    }

    @NotNull
    public String tmpFilenameFor(@NotNull Component component)
    {
        return filenameFor(component) + TMP_EXT;
    }

    @NotNull
    public String filenameFor(@NotNull Component component)
    {
        return baseFilename() + separator + component.name();
    }

    @NotNull
    public String baseFilename()
    {
        @NotNull StringBuilder buff = new StringBuilder();
        buff.append(directory).append(File.separatorChar);
        appendFileName(buff);
        return buff.toString();
    }

    private void appendFileName(@NotNull StringBuilder buff)
    {
        if (!version.hasNewFileName())
        {
            buff.append(ksname).append(separator);
            buff.append(cfname).append(separator);
        }
        buff.append(version).append(separator);
        buff.append(generation);
        if (formatType != SSTableFormat.Type.LEGACY)
            buff.append(separator).append(formatType.name);
    }

    @NotNull
    public String relativeFilenameFor(@NotNull Component component)
    {
        @NotNull final StringBuilder buff = new StringBuilder();
        appendFileName(buff);
        buff.append(separator).append(component.name());
        return buff.toString();
    }

    public SSTableFormat getFormat()
    {
        return formatType.info;
    }

    /** Return any temporary files found in the directory */
    @NotNull
    public List<File> getTemporaryFiles()
    {
        @NotNull List<File> ret = new ArrayList<>();
        @Nullable File[] tmpFiles = directory.listFiles((dir, name) ->
                name.endsWith(Descriptor.TMP_EXT));

        Collections.addAll(ret, tmpFiles);

        return ret;
    }

    /**
     *  Files obsoleted by CASSANDRA-7066 : temporary files and compactions_in_progress. We support
     *  versions 2.1 (ka) and 2.2 (la).
     *  Temporary files have tmp- or tmplink- at the beginning for 2.2 sstables or after ks-cf- for 2.1 sstables
     */

    private final static String LEGACY_COMP_IN_PROG_REGEX_STR = "^compactions_in_progress(\\-[\\d,a-f]{32})?$";
    private final static Pattern LEGACY_COMP_IN_PROG_REGEX = Pattern.compile(LEGACY_COMP_IN_PROG_REGEX_STR);
    private final static String LEGACY_TMP_REGEX_STR = "^((.*)\\-(.*)\\-)?tmp(link)?\\-((?:l|k).)\\-(\\d)*\\-(.*)$";
    private final static Pattern LEGACY_TMP_REGEX = Pattern.compile(LEGACY_TMP_REGEX_STR);

    public static boolean isLegacyFile(@NotNull File file)
    {
        if (file.isDirectory())
            return file.getParentFile() != null &&
                    file.getParentFile().getName().equalsIgnoreCase("system") &&
                    LEGACY_COMP_IN_PROG_REGEX.matcher(file.getName()).matches();
        else
            return LEGACY_TMP_REGEX.matcher(file.getName()).matches();
    }

    public static boolean isValidFile(@NotNull String fileName)
    {
        return fileName.endsWith(".db") && !LEGACY_TMP_REGEX.matcher(fileName).matches();
    }

    /**
     * @see #fromFilename(File directory, String name)
     * @param filename The SSTable filename
     * @return Descriptor of the SSTable initialized from filename
     */
    public static Descriptor fromFilename(@NotNull String filename)
    {
        return fromFilename(filename, false);
    }

    public static Descriptor fromFilename(@NotNull String filename, @NotNull SSTableFormat.Type formatType)
    {
        return fromFilename(filename).withFormatType(formatType);
    }

    public static Descriptor fromFilename(@NotNull String filename, boolean skipComponent)
    {
        @NotNull File file = new File(filename).getAbsoluteFile();
        return fromFilename(file.getParentFile(), file.getName(), skipComponent).left;
    }

    @NotNull
    public static Pair<Descriptor, String> fromFilename(File directory, @NotNull String name)
    {
        return fromFilename(directory, name, false);
    }

    /**
     * Filename of the form is vary by version:
     *
     * <ul>
     *     <li>&lt;ksname&gt;-&lt;cfname&gt;-(tmp-)?&lt;version&gt;-&lt;gen&gt;-&lt;component&gt; for cassandra 2.0 and before</li>
     *     <li>(&lt;tmp marker&gt;-)?&lt;version&gt;-&lt;gen&gt;-&lt;component&gt; for cassandra 3.0 and later</li>
     * </ul>
     *
     * If this is for SSTable of secondary index, directory should ends with index name for 2.1+.
     *
     * @param directory The directory of the SSTable files
     * @param name The name of the SSTable file
     * @param skipComponent true if the name param should not be parsed for a component tag
     *
     * @return A Descriptor for the SSTable, and the Component remainder.
     */
    @NotNull
    @SuppressWarnings("deprecation")
    public static Pair<Descriptor, String> fromFilename(@Nullable File directory, @NotNull String name, boolean skipComponent)
    {
        @NotNull File parentDirectory = directory != null ? directory : new File(".");

        // tokenize the filename
        @NotNull StringTokenizer st = new StringTokenizer(name, String.valueOf(separator));
        String nexttok;

        // read tokens backwards to determine version
        @NotNull Deque<String> tokenStack = new ArrayDeque<>();
        while (st.hasMoreTokens())
        {
            tokenStack.push(st.nextToken());
        }

        // component suffix
        @Nullable String component = skipComponent ? null : tokenStack.pop();

        nexttok = tokenStack.pop();
        // generation OR format type
        @NotNull SSTableFormat.Type fmt = SSTableFormat.Type.LEGACY;
        if (!CharMatcher.digit().matchesAllOf(nexttok))
        {
            fmt = SSTableFormat.Type.validate(nexttok);
            nexttok = tokenStack.pop();
        }

        // generation
        int generation = Integer.parseInt(nexttok);

        // version
        nexttok = tokenStack.pop();

        if (!Version.validate(nexttok))
            throw new UnsupportedOperationException("SSTable " + name + " is too old to open.  Upgrade to 2.0 first, and run upgradesstables");

        Version version = fmt.info.getVersion(nexttok);

        // ks/cf names
        String ksname, cfname;
        if (version.hasNewFileName())
        {
            // for 2.1+ read ks and cf names from directory
            File cfDirectory = parentDirectory;
            // check if this is secondary index
            @NotNull String indexName = "";
            if (cfDirectory.getName().startsWith(Directories.SECONDARY_INDEX_NAME_SEPARATOR))
            {
                indexName = cfDirectory.getName();
                cfDirectory = cfDirectory.getParentFile();
            }
            if (cfDirectory.getName().equals(Directories.BACKUPS_SUBDIR))
            {
                cfDirectory = cfDirectory.getParentFile();
            }
            else if (cfDirectory.getParentFile().getName().equals(Directories.SNAPSHOT_SUBDIR))
            {
                cfDirectory = cfDirectory.getParentFile().getParentFile();
            }
            cfname = cfDirectory.getName().split("-")[0] + indexName;
            ksname = cfDirectory.getParentFile().getName();
        }
        else
        {
            cfname = tokenStack.pop();
            ksname = tokenStack.pop();
        }
        assert tokenStack.isEmpty() : "Invalid file name " + name + " in " + directory;

        return Pair.create(new Descriptor(version, parentDirectory, ksname, cfname, generation, fmt,
                        // _assume_ version from version
                        Component.digestFor(version.uncompressedChecksumType())),
                component);
    }

    @NotNull
    @SuppressWarnings("deprecation")
    public IMetadataSerializer getMetadataSerializer()
    {
        if (version.hasNewStatsFile())
            return new MetadataSerializer();
        else
            return new LegacyMetadataSerializer();
    }

    /**
     * @return true if the current Cassandra version can read the given sstable version
     */
    public boolean isCompatible()
    {
        return version.isCompatible();
    }

    @Override
    public String toString()
    {
        return baseFilename();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof Descriptor))
            return false;
        @NotNull Descriptor that = (Descriptor)o;
        return that.directory.equals(this.directory)
                && that.generation == this.generation
                && that.ksname.equals(this.ksname)
                && that.cfname.equals(this.cfname)
                && that.formatType == this.formatType;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }
}
