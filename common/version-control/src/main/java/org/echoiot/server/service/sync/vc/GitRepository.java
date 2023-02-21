package org.echoiot.server.service.sync.vc;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.SortOrder;
import org.echoiot.server.common.data.sync.vc.BranchInfo;
import org.echoiot.server.common.data.sync.vc.RepositoryAuthMethod;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GitRepository {

    private final Git git;
    private final AuthHandler authHandler;
    @Getter
    private final RepositorySettings settings;

    @Getter
    private final String directory;

    private ObjectId headId;

    private GitRepository(Git git, RepositorySettings settings, AuthHandler authHandler, String directory) {
        this.git = git;
        this.settings = settings;
        this.authHandler = authHandler;
        this.directory = directory;
    }

    @NotNull
    public static GitRepository clone(@NotNull RepositorySettings settings, @NotNull File directory) throws GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(settings.getRepositoryUri())
                .setDirectory(directory)
                .setNoCheckout(true);
        @NotNull AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        authHandler.configureCommand(cloneCommand);
        Git git = cloneCommand.call();
        return new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
    }

    @NotNull
    public static GitRepository open(@NotNull File directory, @NotNull RepositorySettings settings) throws IOException {
        @NotNull Git git = Git.open(directory);
        @NotNull AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        return new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
    }

    public static void test(@NotNull RepositorySettings settings, @NotNull File directory) throws Exception {
        @NotNull AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        if (settings.isReadOnly()) {
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository().setRemote(settings.getRepositoryUri());
            authHandler.configureCommand(lsRemoteCommand);
            lsRemoteCommand.call();
        } else {
            Files.createDirectories(directory.toPath());
            try {
                Git git = Git.init().setDirectory(directory).call();
                @NotNull GitRepository repository = new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
                repository.execute(repository.git.remoteAdd()
                        .setName("origin")
                        .setUri(new URIish(settings.getRepositoryUri())));
                repository.push("", UUID.randomUUID().toString()); // trying to delete non-existing branch on remote repo
            } finally {
                try {
                    FileUtils.forceDelete(directory);
                } catch (Exception ignored) {}
            }
        }
    }

    public void fetch() throws GitAPIException {
        FetchResult result = execute(git.fetch()
                .setRemoveDeletedRefs(true));
        Ref head = result.getAdvertisedRef(Constants.HEAD);
        if (head != null) {
            this.headId = head.getObjectId();
        }
    }

    public void deleteLocalBranchIfExists(String branch) throws GitAPIException {
        execute(git.branchDelete()
                .setBranchNames(branch)
                .setForce(true));
    }

    public void resetAndClean() throws GitAPIException {
        execute(git.reset()
                .setMode(ResetCommand.ResetType.HARD));
        execute(git.clean()
                .setForce(true)
                .setCleanDirectories(true));
    }

    public void merge(String branch) throws IOException, GitAPIException {
        @NotNull ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        execute(git.merge()
                .include(branchId));
    }

    @NotNull
    public List<BranchInfo> listRemoteBranches() throws GitAPIException {
        return execute(git.branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)).stream()
                .filter(ref -> !ref.getName().equals(Constants.HEAD))
                .map(this::toBranchInfo)
                .distinct().collect(Collectors.toList());
    }

    public PageData<Commit> listCommits(String branch, @NotNull PageLink pageLink) throws IOException, GitAPIException {
        return listCommits(branch, null, pageLink);
    }

    @NotNull
    public PageData<Commit> listCommits(String branch, String path, @NotNull PageLink pageLink) throws IOException, GitAPIException {
        @NotNull ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            return new PageData<>();
        }
        LogCommand command = git.log()
                .add(branchId);

        command.setRevFilter(new CommitFilter(pageLink.getTextSearch(), settings.isShowMergeCommits()));
        if (StringUtils.isNotEmpty(path)) {
            command.addPath(path);
        }

        Iterable<RevCommit> commits = execute(command);
        return iterableToPageData(commits, this::toCommit, pageLink, revCommitComparatorFunction);
    }

    public List<String> listFilesAtCommit(String commitId) throws IOException {
        return listFilesAtCommit(commitId, null);
    }

    @NotNull
    public List<String> listFilesAtCommit(String commitId, @NotNull String path) throws IOException {
        @NotNull List<String> files = new ArrayList<>();
        RevCommit revCommit = resolveCommit(commitId);
        try (@NotNull TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
            treeWalk.reset(revCommit.getTree().getId());
            if (StringUtils.isNotEmpty(path)) {
                treeWalk.setFilter(PathFilter.create(path));
            }
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                files.add(treeWalk.getPathString());
            }
        }
        return files;
    }


    @NotNull
    public String getFileContentAtCommit(@NotNull String file, String commitId) throws IOException {
        RevCommit revCommit = resolveCommit(commitId);
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), file, revCommit.getTree())) {
            if (treeWalk == null) {
                throw new IllegalArgumentException("File not found");
            }
            ObjectId blobId = treeWalk.getObjectId(0);
            try (ObjectReader objectReader = git.getRepository().newObjectReader()) {
                ObjectLoader objectLoader = objectReader.open(blobId);
                @NotNull byte[] bytes = objectLoader.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }


    public void createAndCheckoutOrphanBranch(String name) throws GitAPIException {
        execute(git.checkout()
                .setOrphan(true)
                .setForced(true)
                .setName(name));
    }

    public void add(String filesPattern) throws GitAPIException {
        execute(git.add().setUpdate(true).addFilepattern(filesPattern));
        execute(git.add().addFilepattern(filesPattern));
    }

    @NotNull
    public Status status() throws GitAPIException {
        org.eclipse.jgit.api.Status status = execute(git.status());
        @NotNull Set<String> modified = new HashSet<>();
        modified.addAll(status.getModified());
        modified.addAll(status.getChanged());
        return new Status(status.getAdded(), modified, status.getRemoved());
    }

    @NotNull
    public Commit commit(String message, String authorName, String authorEmail) throws GitAPIException {
        RevCommit revCommit = execute(git.commit()
                .setAuthor(authorName, authorEmail)
                .setMessage(message));
        return toCommit(revCommit);
    }


    public void push(String localBranch, String remoteBranch) throws GitAPIException {
        execute(git.push()
                .setRefSpecs(new RefSpec(localBranch + ":" + remoteBranch)));
    }

    public String getContentsDiff(@NotNull String content1, @NotNull String content2) throws IOException {
        @NotNull RawText rawContent1 = new RawText(content1.getBytes());
        @NotNull RawText rawContent2 = new RawText(content2.getBytes());

        @NotNull ByteArrayOutputStream out = new ByteArrayOutputStream();
        @NotNull DiffFormatter diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(git.getRepository());

        @NotNull EditList edits = new EditList();
        edits.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, rawContent1, rawContent2));
        diffFormatter.format(edits, rawContent1, rawContent2);
        return out.toString();
    }

    @NotNull
    public List<Diff> getDiffList(String commit1, String commit2, @NotNull String path) throws IOException {
        ObjectReader reader = git.getRepository().newObjectReader();

        @NotNull CanonicalTreeParser tree1Iter = new CanonicalTreeParser();
        ObjectId tree1 = resolveCommit(commit1).getTree();
        tree1Iter.reset(reader, tree1);

        @NotNull CanonicalTreeParser tree2Iter = new CanonicalTreeParser();
        ObjectId tree2 = resolveCommit(commit2).getTree();
        tree2Iter.reset(reader, tree2);

        @NotNull ByteArrayOutputStream out = new ByteArrayOutputStream();
        @NotNull DiffFormatter diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(git.getRepository());
        if (StringUtils.isNotEmpty(path)) {
            diffFormatter.setPathFilter(PathFilter.create(path));
        }

        return diffFormatter.scan(tree1, tree2).stream()
                .map(diffEntry -> {
                    @NotNull Diff diff = new Diff();
                    try {
                        out.reset();
                        diffFormatter.format(diffEntry);
                        diff.setDiffStringValue(out.toString());
                        diff.setFilePath(diffEntry.getChangeType() != DiffEntry.ChangeType.DELETE ? diffEntry.getNewPath() : diffEntry.getOldPath());
                        diff.setChangeType(diffEntry.getChangeType());
                        try {
                            diff.setFileContentAtCommit1(getFileContentAtCommit(diff.getFilePath(), commit1));
                        } catch (IllegalArgumentException ignored) {
                        }
                        try {
                            diff.setFileContentAtCommit2(getFileContentAtCommit(diff.getFilePath(), commit2));
                        } catch (IllegalArgumentException ignored) {
                        }
                        return diff;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @NotNull
    private BranchInfo toBranchInfo(@NotNull Ref ref) {
        String name = org.eclipse.jgit.lib.Repository.shortenRefName(ref.getName());
        @Nullable String branchName = StringUtils.removeStart(name, "origin/");
        boolean isDefault = this.headId != null && this.headId.equals(ref.getObjectId());
        return new BranchInfo(branchName, isDefault);
    }

    @NotNull
    private Commit toCommit(@NotNull RevCommit revCommit) {
        return new Commit(revCommit.getCommitTime() * 1000L, revCommit.getName(),
                          revCommit.getFullMessage(), revCommit.getAuthorIdent().getName(), revCommit.getAuthorIdent().getEmailAddress());
    }

    private RevCommit resolveCommit(String id) throws IOException {
        return git.getRepository().parseCommit(resolve(id));
    }

    @NotNull
    private ObjectId resolve(String rev) throws IOException {
        ObjectId result = git.getRepository().resolve(rev);
        if (result == null) {
            throw new IllegalArgumentException("Failed to parse git revision string: \"" + rev + "\"");
        }
        return result;
    }

    private <C extends GitCommand<T>, T> T execute(C command) throws GitAPIException {
        if (command instanceof TransportCommand) {
            authHandler.configureCommand((TransportCommand) command);
        }
        return command.call();
    }

    private static final Function<PageLink, Comparator<RevCommit>> revCommitComparatorFunction = pageLink -> {
        SortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder != null
                && sortOrder.getProperty().equals("timestamp")
                && SortOrder.Direction.ASC.equals(sortOrder.getDirection())) {
            return Comparator.comparingInt(RevCommit::getCommitTime);
        }
        return null;
    };

    @NotNull
    private static <T, R> PageData<R> iterableToPageData(Iterable<T> iterable,
                                                         Function<? super T, ? extends R> mapper,
                                                         @NotNull PageLink pageLink,
                                                         @Nullable Function<PageLink, Comparator<T>> comparatorFunction) {
        iterable = Streams.stream(iterable).collect(Collectors.toList());
        int totalElements = Iterables.size(iterable);
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        int limit = startIndex + pageLink.getPageSize();
        if (comparatorFunction != null) {
            Comparator<T> comparator = comparatorFunction.apply(pageLink);
            if (comparator != null) {
                iterable = Ordering.from(comparator).immutableSortedCopy(iterable);
            }
        }
        iterable = Iterables.limit(iterable, limit);
        if (startIndex < totalElements) {
            iterable = Iterables.skip(iterable, startIndex);
        } else {
            iterable = Collections.emptyList();
        }
        @NotNull List<R> data = Streams.stream(iterable).map(mapper)
                                       .collect(Collectors.toList());
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + data.size();
        return new PageData<>(data, totalPages, totalElements, hasNext);
    }

    @RequiredArgsConstructor
    private static class AuthHandler {
        @NotNull
        private final CredentialsProvider credentialsProvider;
        @NotNull
        private final SshdSessionFactory sshSessionFactory;

        @NotNull
        protected static AuthHandler createFor(@NotNull RepositorySettings settings, File directory) {
            @Nullable CredentialsProvider credentialsProvider = null;
            @Nullable SshdSessionFactory sshSessionFactory = null;
            if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
                credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
            } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
                sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
            }
            return new AuthHandler(credentialsProvider, sshSessionFactory);
        }

        protected void configureCommand(@NotNull TransportCommand command) {
            if (credentialsProvider != null) {
                command.setCredentialsProvider(credentialsProvider);
            }
            if (sshSessionFactory != null) {
                command.setTransportConfigCallback(transport -> {
                    if (transport instanceof SshTransport) {
                        @NotNull SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    }
                });
            }
        }

        @NotNull
        private static CredentialsProvider newCredentialsProvider(String username, @Nullable String password) {
            return new UsernamePasswordCredentialsProvider(username, password == null ? "" : password);
        }

        @Nullable
        private static SshdSessionFactory newSshdSessionFactory(@NotNull String privateKey, String password, File directory) {
            @Nullable SshdSessionFactory sshSessionFactory = null;
            if (StringUtils.isNotBlank(privateKey)) {
                @NotNull Iterable<KeyPair> keyPairs = loadKeyPairs(privateKey, password);
                sshSessionFactory = new SshdSessionFactoryBuilder()
                        .setPreferredAuthentications("publickey")
                        .setDefaultKeysProvider(file -> keyPairs)
                        .setHomeDirectory(directory)
                        .setSshDirectory(directory)
                        .setServerKeyDatabase((file, file2) -> new ServerKeyDatabase() {
                            @NotNull
                            @Override
                            public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress, Configuration config) {
                                return Collections.emptyList();
                            }

                            @Override
                            public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey, Configuration config, CredentialsProvider provider) {
                                return true;
                            }
                        })
                        .build(new JGitKeyCache());
            }
            return sshSessionFactory;
        }

        @NotNull
        private static Iterable<KeyPair> loadKeyPairs(@NotNull String privateKeyContent, String password) {
            @Nullable Iterable<KeyPair> keyPairs = null;
            try {
                keyPairs = SecurityUtils.loadKeyPairIdentities(null,
                        null, new ByteArrayInputStream(privateKeyContent.getBytes()), (session, resourceKey, retryIndex) -> password);
            } catch (Exception e) {}
            if (keyPairs == null) {
                throw new IllegalArgumentException("Failed to load ssh private key");
            }
            return keyPairs;
        }
    }

    private static class CommitFilter extends RevFilter {

        @NotNull
        private final String textSearch;
        private final boolean showMergeCommits;

        CommitFilter(@NotNull String textSearch, boolean showMergeCommits) {
            this.textSearch = textSearch.toLowerCase();
            this.showMergeCommits = showMergeCommits;
        }

        @Override
        public boolean include(RevWalk walker, @NotNull RevCommit c) {
            return (showMergeCommits || c.getParentCount() < 2) && (StringUtils.isEmpty(textSearch)
                    || c.getFullMessage().toLowerCase().contains(textSearch));
        }

        @NotNull
        @Override
        public RevFilter clone() {
            return this;
        }

        @Override
        public boolean requiresCommitBody() {
            return false;
        }

    }

    @Data
    public static class Commit {
        private final long timestamp;
        @NotNull
        private final String id;
        @NotNull
        private final String message;
        @NotNull
        private final String authorName;
        @NotNull
        private final String authorEmail;
    }

    @Data
    public static class Status {
        @NotNull
        private final Set<String> added;
        @NotNull
        private final Set<String> modified;
        @NotNull
        private final Set<String> removed;
    }

    @Data
    public static class Diff {
        private String filePath;
        private DiffEntry.ChangeType changeType;
        private String fileContentAtCommit1;
        private String fileContentAtCommit2;
        private String diffStringValue;
    }

}
