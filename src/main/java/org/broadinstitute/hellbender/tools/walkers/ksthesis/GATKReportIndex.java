package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.report.GATKReportTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Speeds up searching for rows.
 *
 * TODO: KSTHESIS: Contrib/move back to GATKReport?
 */
public interface GATKReportIndex {
    /**
     * Returns the row index with these column values. If the row doesn't exist, the rowID is set as the row mapping,
     * and the values are filled in for each of the column values.
     *
     * @param rowID        The row mapping, passed to GATKReportTable.addRowID if necessary.
     * @param columnValues The column values for the row being searched.
     * @return The existing or a new row index.
     */
    int findRowByData(final Object rowID, final Object[] columnValues);

    /**
     * Indexes a table with a single row or stratified rows.
     *
     * @param table          The table to index.
     * @param rowIndexColumn The last column to index. Pass a negative value tables using a single row.
     */
    static GATKReportIndex newInstance(GATKReportTable table, int rowIndexColumn) {
        if (rowIndexColumn < 0) {
            return new SingleRowGATKReportIndex(table);
        } else {
            return new MultiRowGATKReportIndex(table, rowIndexColumn);
        }
    }
}

/**
 * Speeds up searching for rows in a table that will have only one row.
 */
@SuppressWarnings("WeakerAccess")
class SingleRowGATKReportIndex implements GATKReportIndex {
    private final GATKReportTable table;
    private boolean rowCreated;

    private static final int SINGLE_ROW_INDEX = 0;

    /**
     * Indexes a table with a single row.
     *
     * @param table The table to index.
     */
    public SingleRowGATKReportIndex(GATKReportTable table) {
        this.table = table;
        final int numRows = table.getNumRows();
        if (numRows > 1) {
            throw new IllegalArgumentException(
                    String.format("Only expected a single row in %s, instead found %d rows",
                            table.getTableName(), numRows));
        } else {
            rowCreated = numRows == 1;
        }
    }

    @Override
    public int findRowByData(Object rowID, Object[] columnValues) {
        if (columnValues.length != 0)
            throw new IllegalArgumentException(
                    String.format("Key must be empty for indexing in %s, instead got length %d",
                            table.getTableName(), columnValues.length));
        if (!rowCreated) {
            table.addRowID(rowID, false);
            rowCreated = true;
        }
        return SINGLE_ROW_INDEX;
    }
}

/**
 * Speeds up searching for rows by using nested lookup tables.
 */
@SuppressWarnings("WeakerAccess")
class MultiRowGATKReportIndex implements GATKReportIndex {
    private final GATKReportTable table;
    private final int rowIndexColumn;
    private final GATKReportIndexNode root;

    /**
     * Indexes a table with stratified rows.
     *
     * @param table          The table to index.
     * @param rowIndexColumn The last column to index.
     */
    public MultiRowGATKReportIndex(GATKReportTable table, int rowIndexColumn) {
        this.table = table;
        this.rowIndexColumn = rowIndexColumn;
        this.root = new GATKReportIndexNode(table);
        initRowIndexes();
    }

    private void initRowIndexes() {
        Object keyPart;
        final int numRows = table.getNumRows();
        for (int row = 0; row < numRows; row++) {
            GATKReportIndexNode node = root;
            for (int column = 0; column < rowIndexColumn; column++) {
                keyPart = table.get(row, column);
                node = node.getOrCreateNode(keyPart);
            }
            keyPart = table.get(row, rowIndexColumn);
            node.initRowIndex(keyPart, row);
        }
    }

    @Override
    public int findRowByData(final Object rowID, final Object[] columnValues) {
        if (columnValues.length != (rowIndexColumn + 1))
            throw new IllegalArgumentException(
                    String.format("Key must be length %d for indexing in %s, instead got %d",
                            rowIndexColumn + 1, table.getTableName(), columnValues.length));
        GATKReportIndexNode node = root;
        Object keyPart;
        for (int column = 0; column < rowIndexColumn; column++) {
            keyPart = columnValues[column];
            node = node.getOrCreateNode(keyPart);
        }

        keyPart = columnValues[rowIndexColumn];
        return node.findRowIndex(rowID, columnValues, keyPart);
    }

    /**
     * Nested maps, eventually leading to a leaf with the row index.
     */
    private static class GATKReportIndexNode {
        private final GATKReportTable table;
        private final Map<Object, Object> map = new HashMap<>();

        GATKReportIndexNode(final GATKReportTable table) {
            this.table = table;
        }

        public GATKReportIndexNode getOrCreateNode(final Object keyPart) {
            return (GATKReportIndexNode) map.computeIfAbsent(keyPart, unused -> new GATKReportIndexNode(table));
        }

        public void initRowIndex(final Object keyPart, final int rowIndex) {
            map.put(keyPart, rowIndex);
        }

        public int findRowIndex(final Object rowID, final Object[] columnValues, final Object keyPart) {
            return (int) map.computeIfAbsent(keyPart, unused -> {
                int rowIndex = table.addRowID(rowID, false);
                for (int columnIndex = 0; columnIndex < columnValues.length; columnIndex++) {
                    table.set(rowIndex, columnIndex, columnValues[columnIndex]);
                }
                return rowIndex;
            });
        }
    }
}
