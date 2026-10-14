package lodge.view;

import lodge.model.AuthService;
import lodge.model.Database;
import lodge.model.User;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * VIEW: admin-only user manager.
 * Lists users, activates/deactivates them, resets passwords and changes the officiating code.
 */
final class AdminDialog extends JDialog {

    private final Database db;
    private final AuthService auth;
    private final User currentAdmin;
    private final UserTableModel model;
    private final JTable table;

    AdminDialog(Frame owner, Database db, AuthService auth, User currentAdmin) {
        super(owner, "Manage users", true);
        this.db = db;
        this.auth = auth;
        this.currentAdmin = currentAdmin;
        this.model = new UserTableModel();
        this.table = new JTable(model);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Logo.windowIcon());

        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(640, 320));

        RoundButton toggle = new RoundButton("Activate / Deactivate", RoundButton.Kind.SECONDARY);
        RoundButton reset = new RoundButton("Reset password", RoundButton.Kind.SECONDARY);
        RoundButton changeCode = new RoundButton("Change officiating code", RoundButton.Kind.SECONDARY);
        RoundButton close = new RoundButton("Close", RoundButton.Kind.PRIMARY);

        toggle.addActionListener(e -> toggleActive());
        reset.addActionListener(e -> resetPassword());
        changeCode.addActionListener(e -> changeOfficiatingCode());
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(toggle);
        buttons.add(reset);
        buttons.add(changeCode);
        buttons.add(close);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        reload();
        pack();
        setLocationRelativeTo(owner);
    }

    private void reload() {
        try {
            model.setUsers(db.listUsers());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not read users:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private User selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user first.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return model.get(row);
    }

    private void toggleActive() {
        User u = selected();
        if (u == null) return;
        if (u.id() == currentAdmin.id()) {
            JOptionPane.showMessageDialog(this, "You cannot deactivate your own account.",
                    "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            auth.setUserActive(u.id(), !u.active());
            reload();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not update user:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetPassword() {
        User u = selected();
        if (u == null) return;
        JPasswordField pw = new JPasswordField(16);
        JPasswordField again = new JPasswordField(16);
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("New password:"));
        p.add(pw);
        p.add(new JLabel("Repeat:"));
        p.add(again);
        int r = JOptionPane.showConfirmDialog(this, p, "Reset password for " + u.username(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        char[] a = pw.getPassword();
        char[] b = again.getPassword();
        try {
            if (!Arrays.equals(a, b)) {
                JOptionPane.showMessageDialog(this, "The passwords do not match.",
                        "Mismatch", JOptionPane.WARNING_MESSAGE);
                return;
            }
            auth.resetPassword(u.id(), a);
            JOptionPane.showMessageDialog(this, "Password updated.",
                    "Done", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Bad password", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not save:\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(a, '\0');
            Arrays.fill(b, '\0');
        }
    }

    private void changeOfficiatingCode() {
        JPasswordField cur = new JPasswordField(16);
        JPasswordField next = new JPasswordField(16);
        JPasswordField again = new JPasswordField(16);
        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        p.add(new JLabel("Current code:"));
        p.add(cur);
        p.add(new JLabel("New code (min 6):"));
        p.add(next);
        p.add(new JLabel("Repeat new code:"));
        p.add(again);
        int r = JOptionPane.showConfirmDialog(this, p, "Change officiating code",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        char[] c0 = cur.getPassword();
        char[] c1 = next.getPassword();
        char[] c2 = again.getPassword();
        try {
            if (!auth.officiatingCodeMatches(c0)) {
                JOptionPane.showMessageDialog(this, "The current code is wrong.",
                        "Wrong code", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (c1.length < 6) {
                JOptionPane.showMessageDialog(this, "The new code must be at least 6 characters.",
                        "Too short", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Arrays.equals(c1, c2)) {
                JOptionPane.showMessageDialog(this, "The two new codes do not match.",
                        "Mismatch", JOptionPane.WARNING_MESSAGE);
                return;
            }
            auth.setOfficiatingCode(c1);
            JOptionPane.showMessageDialog(this, "Officiating code updated.",
                    "Done", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not save:\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(c0, '\0');
            Arrays.fill(c1, '\0');
            Arrays.fill(c2, '\0');
        }
    }

    /** Table model for the user list. */
    private static final class UserTableModel extends AbstractTableModel {
        private static final String[] COLS = {"ID", "Username", "Display name", "Role", "Active"};
        private List<User> users = new ArrayList<>();

        void setUsers(List<User> list) {
            users = list;
            fireTableDataChanged();
        }

        User get(int row) {
            return users.get(row);
        }

        @Override public int getRowCount() { return users.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            User u = users.get(r);
            return switch (c) {
                case 0 -> u.id();
                case 1 -> u.username();
                case 2 -> u.displayName();
                case 3 -> u.role().name();
                default -> u.active() ? "Yes" : "No";
            };
        }
    }
}