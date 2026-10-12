package lodge.view;

import lodge.model.BillRepository;
import lodge.model.Bill;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;

/**
 * VIEW adapter: shows the repository's bills in a JTable. It holds NO copy of the data,
 * so the table and the saved list can never get out of step.
 */
public final class BillTableModel extends AbstractTableModel {

    static final String[] COLUMNS = {"Bill", "Guest", "Room", "Nights", "Total", "Paid", "Balance"};
    static final int BALANCE_COLUMN = 6;

    private final BillRepository repository;

    public BillTableModel(BillRepository repository) {
        this.repository = repository;
        repository.addChangeListener(this::fireTableDataChanged);
    }

    @Override
    public int getRowCount() {
        return repository.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 2 -> lodge.model.RoomType.class;
            case 3 -> Integer.class;
            case 4, 5, 6 -> BigDecimal.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        Bill b = repository.get(row);
        return switch (column) {
            case 0 -> b.id();
            case 1 -> b.guest();
            case 2 -> b.room();
            case 3 -> b.nights();
            case 4 -> b.total();
            case 5 -> b.paid();
            default -> b.balance();
        };
    }
}
