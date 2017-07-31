import MemoryTable.Record;

import java.io.PrintStream;

import static java.lang.String.format;

/**
 * Created by Szymon on 20/07/2017.
 */
public class SimilarityMatrix {
    Double[][] wmatrix;
    Double colAvrg = 0.0;
    Double rowAvrg = 0.0;
    Double sourceLength;
    Double targetLength;
    String[] lab1Words;
    String[] lab2Words;

    public SimilarityMatrix(String label1, String label2) {
        lab1Words = label1.split(" ");
        lab2Words = label2.split(" ");
        sourceLength = Double.valueOf(lab1Words.length);
        targetLength = Double.valueOf(lab2Words.length);
        wmatrix = new Double[lab1Words.length + 1][lab2Words.length + 1];
        for (int x = 0; x < lab1Words.length; x++)
            for (int y = 0; y < lab2Words.length; y++) {
                Record retrieve = new Record();
                retrieve.put("source", lab1Words[x]);
                retrieve.put("target", lab2Words[y]);

                Record rec = Matchmaker.bestWordScorePrediction(lab1Words[x], lab2Words[y]);

                wmatrix[x][y] = Double.valueOf(String.valueOf(rec.get("score")));

                if (wmatrix[lab1Words.length][y] == null || wmatrix[lab1Words.length][y] < wmatrix[x][y])
                    wmatrix[lab1Words.length][y] = wmatrix[x][y];
                if (wmatrix[x][lab2Words.length] == null || wmatrix[x][lab2Words.length] < wmatrix[x][y])
                    wmatrix[x][lab2Words.length] = wmatrix[x][y];
            }
        for (int x = 0; x < lab1Words.length; x++) colAvrg = colAvrg + wmatrix[x][lab2Words.length];
        colAvrg = colAvrg / lab1Words.length;
        for (int y = 0; y < lab2Words.length; y++) rowAvrg = rowAvrg + wmatrix[lab1Words.length][y];
        rowAvrg = rowAvrg / lab2Words.length;
    }

    public void print() {
        final PrettyPrinter printer = new PrettyPrinter(System.out);

        String[][] matrix = new String[lab1Words.length+2][lab2Words.length+2];

        for (int i=0; i<lab1Words.length; i++) matrix[i+1][0] = lab1Words[i];
        for (int i=0; i<lab2Words.length; i++) matrix[0][i+1] = lab2Words[i];
        for (int i=0; i<=lab1Words.length; i++)
            for (int j=0; j<=lab2Words.length; j++)
                matrix[i+1][j+1]=String.valueOf(wmatrix[i][j]);

        printer.print(matrix);


    }

    public final class PrettyPrinter {

        private static final char BORDER_KNOT = '+';
        private static final char HORIZONTAL_BORDER = '-';
        private static final char VERTICAL_BORDER = '|';

        private static final String DEFAULT_AS_NULL = "(NULL)";

        private final PrintStream out;
        private final String asNull;

        public PrettyPrinter(PrintStream out) {
            this(out, DEFAULT_AS_NULL);
        }

        public PrettyPrinter(PrintStream out, String asNull) {
            if ( out == null ) {
                throw new IllegalArgumentException("No print stream provided");
            }
            if ( asNull == null ) {
                throw new IllegalArgumentException("No NULL-value placeholder provided");
            }
            this.out = out;
            this.asNull = asNull;
        }

        public void print(String[][] table) {
            if ( table == null ) {
                throw new IllegalArgumentException("No tabular data provided");
            }
            if ( table.length == 0 ) {
                return;
            }
            final int[] widths = new int[getMaxColumns(table)];
            adjustColumnWidths(table, widths);
            printPreparedTable(table, widths, getHorizontalBorder(widths));
        }

        private void printPreparedTable(String[][] table, int widths[], String horizontalBorder) {
            final int lineLength = horizontalBorder.length();
            System.out.println(horizontalBorder);
            for ( final String[] row : table ) {
                if ( row != null ) {
                    System.out.println(getRow(row, widths, lineLength));
                    System.out.println(horizontalBorder);
                }
            }
        }

        private String getRow(String[] row, int[] widths, int lineLength) {
            final StringBuilder builder = new StringBuilder(lineLength).append(VERTICAL_BORDER);
            final int maxWidths = widths.length;
            for ( int i = 0; i < maxWidths; i++ ) {
                builder.append(padRight(getCellValue(safeGet(row, i, null)), widths[i])).append(VERTICAL_BORDER);
            }
            return builder.toString();
        }

        private String getHorizontalBorder(int[] widths) {
            final StringBuilder builder = new StringBuilder(256);
            builder.append(BORDER_KNOT);
            for ( final int w : widths ) {
                for ( int i = 0; i < w; i++ ) {
                    builder.append(HORIZONTAL_BORDER);
                }
                builder.append(BORDER_KNOT);
            }
            return builder.toString();
        }

        private int getMaxColumns(String[][] rows) {
            int max = 0;
            for ( final String[] row : rows ) {
                if ( row != null && row.length > max ) {
                    max = row.length;
                }
            }
            return max;
        }

        private void adjustColumnWidths(String[][] rows, int[] widths) {
            for ( final String[] row : rows ) {
                if ( row != null ) {
                    for ( int c = 0; c < widths.length; c++ ) {
                        final String cv = getCellValue(safeGet(row, c, asNull));
                        final int l = cv.length();
                        if ( widths[c] < l ) {
                            widths[c] = l;
                        }
                    }
                }
            }
        }

        private String padRight(String s, int n) {
            return format("%1$-" + n + "s", s);
        }

        private String safeGet(String[] array, int index, String defaultValue) {
            return index < array.length ? array[index] : defaultValue;
        }

        private String getCellValue(Object value) {
            return value == null ? asNull : value.toString();
        }

    }
}
