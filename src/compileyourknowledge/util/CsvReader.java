package compileyourknowledge.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A small RFC 4180 CSV parser. Fields may be wrapped in double quotes, and a quoted field may
 * contain commas, line breaks and doubled quotes ("") for a literal quote. This is what lets a
 * question's code sample sit in one cell across several lines.
 */
public final class CsvReader {

    private CsvReader() {
    }

    /** Reads every record. Blank lines are skipped and a leading byte-order mark is ignored. */
    public static List<String[]> parse(Reader reader) throws IOException {
        Reader in = reader.markSupported() ? reader : new java.io.BufferedReader(reader);
        List<String[]> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;   // true once the current record has any content at all
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
        if (fieldStarted || field.length() > 0) {
            fields.add(field.toString());
            records.add(fields.toArray(new String[0]));
        }
        return records;
    }
}
