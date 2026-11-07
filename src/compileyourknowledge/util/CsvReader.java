package compileyourknowledge.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * a small csv reader. a field can be wrapped in double quotes, and a quoted field can hold commas,
 * line breaks and "" for a real quote mark. this lets a question's code sample sit in one cell
 * across several lines.
 */

public final class CsvReader {

    private CsvReader() {
    }

    // reads every row. blank lines are skipped and a byte-order mark at the start is ignored 
    public static List<String[]> parse(Reader reader) throws IOException {
        Reader in = reader.markSupported() ? reader : new BufferedReader(reader);
        List<String[]> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false; // true once the current row has any content at all
        boolean first = true;

        int c;
        while ((c = in.read()) != -1) {
            if (first) {
                first = false;
                if (c == '\uFEFF') {
                    continue;
                }
            }
            if (inQuotes) {
                if (c == '"') {
                    // a doubled quote means a real quote mark. a single one ends the quoted part
                    in.mark(1);
                    int next = in.read();
                    if (next == '"') {
                        field.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) {
                            in.reset();
                        }
                    }
                } else {
                    field.append((char) c);
                }
                continue;
            }
            switch (c) {
                case '"':
                    inQuotes = true;
                    fieldStarted = true;
                    break;
                case ',':
                    fields.add(field.toString());
                    field.setLength(0);
                    fieldStarted = true;
                    break;
                case '\r':
                    break; // handled together with '\n'
                case '\n':
                    if (fieldStarted || field.length() > 0) {
                        fields.add(field.toString());
                        records.add(fields.toArray(new String[0]));
                    }
                    fields = new ArrayList<>();
                    field.setLength(0);
                    fieldStarted = false;
                    break;
                default:
                    field.append((char) c);
                    fieldStarted = true;
            }
        }
        // the last row may not end with a line break
        if (fieldStarted || field.length() > 0) {
            fields.add(field.toString());
            records.add(fields.toArray(new String[0]));
        }
        return records;
    }
}
