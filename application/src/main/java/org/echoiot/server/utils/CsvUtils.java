package org.echoiot.server.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.CharSequenceReader;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CsvUtils {

    @NotNull
    public static List<List<String>> parseCsv(String content, @NotNull Character delimiter) throws Exception {
        @NotNull CSVFormat csvFormat = delimiter.equals(',') ? CSVFormat.DEFAULT : CSVFormat.DEFAULT.withDelimiter(delimiter);

        List<CSVRecord> records;
        try (@NotNull CharSequenceReader reader = new CharSequenceReader(content)) {
            records = csvFormat.parse(reader).getRecords();
        }

        return records.stream()
                .map(record -> Stream.iterate(0, i -> i < record.size(), i -> i + 1)
                        .map(record::get)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

}
