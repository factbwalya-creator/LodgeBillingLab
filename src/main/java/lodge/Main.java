package lodge;

import lodge.controller.BillController;
import lodge.model.AuthService;
import lodge.model.BillRepository;
import lodge.model.Database;
import lodge.model.User;
import lodge.view.BillTableModel;
import lodge.view.LoginDialog;
import lodge.view.MainWindow;
import lodge.view.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

/** Entry point. Opens the database, then wires a view to a controller for each login. */
public final class Main {

    private static AuthService auth;
    private static BillRepository repository;
    private static BillTableModel tableModel;

    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        Theme.install();

        Database db;
        try {
            db = new Database(Database.defaultFile());
            auth = new AuthService(db);
            repository = new BillRepository(db);
            tableModel = new BillTableModel(repository);
            repository.load();
        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not open the database:\n" + e.getMessage()
                            + "\n\nFile: " + Database.defaultFile(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        if (!ensureOfficiatingCode()) {
            System.exit(1);
            return;
        }

        startWithLogin();
    }

    /** First run only: asks the user to choose the officiating code. */
    private static boolean ensureOfficiatingCode() {
        try {
            if (auth.hasOfficiatingCode()) return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not read the officiating code: " + e.getMessage(),
                    "Fatal error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        while (true) {
            JPasswordField code = new JPasswordField(18);
            JPasswordField again = new JPasswordField(18);
            JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
            p.add(new JLabel("Choose an officiating code (min 6):"));
            p.add(code);
            p.add(new JLabel("Repeat it:"));
            p.add(again);

            int choice = JOptionPane.showConfirmDialog(null, p,
                    "First run — set the officiating code",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return false;

            char[] c1 = code.getPassword();
            char[] c2 = again.getPassword();
            try {
                if (c1.length < 6) {
                    JOptionPane.showMessageDialog(null,
                            "The officiating code must be at least 6 characters.",
                            "Officiating code", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                if (!Arrays.equals(c1, c2)) {
                    JOptionPane.showMessageDialog(null,
                            "The two codes do not match. Try again.",
                            "Officiating code", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                auth.setOfficiatingCode(c1);
                return true;
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not save the officiating code: " + e.getMessage(),
                        "Fatal error", JOptionPane.ERROR_MESSAGE);
                return false;
            } finally {
                Arrays.fill(c1, '\0');
                Arrays.fill(c2, '\0');
            }
        }
    }

    /** Shows the login, then opens the main window. Also used by File > Log out. */
    public static void startWithLogin() {
        User user = new LoginDialog(null, auth).showLogin();
        if (user == null) {
            System.exit(0);
        }
        MainWindow window = new MainWindow(user, tableModel);
        new BillController(user, repository, window, Main::startWithLogin);
        window.setVisible(true);
    }
}