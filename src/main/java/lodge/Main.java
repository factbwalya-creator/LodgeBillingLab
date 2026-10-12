package lodge;

import lodge.controller.BillController;
import lodge.model.AuthService;
import lodge.model.BillRepository;
import lodge.view.BillTableModel;
import lodge.view.LoginDialog;
import lodge.view.MainWindow;
import lodge.view.Theme;

import javax.swing.*;
import java.io.IOException;

/** Entry point. Builds the model, then wires a view to a controller for each login. */
public final class Main {

    private static AuthService auth;
    private static BillRepository repository;
    private static BillTableModel tableModel;

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        Theme.install();
        auth = new AuthService();
        repository = new BillRepository(BillRepository.defaultFile());
        tableModel = new BillTableModel(repository);
        loadSavedBills();
        startWithLogin();
    }

    private static void loadSavedBills() {
        try {
            int skipped = repository.load();
            if (skipped > 0) {
                JOptionPane.showMessageDialog(null,
                        skipped + " saved line(s) could not be read and were skipped.\n"
                                + "A backup of the original file was kept next to it:\n" + repository.file(),
                        "Some saved data was skipped", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "The saved bills could not be read:\n" + e.getMessage()
                            + "\n\nThe app will start with an empty list. The original file was backed up.",
                    "Could not load saved bills", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Shows the login, then opens the main window. Also used by File > Log out. */
    public static void startWithLogin() {
        String user = new LoginDialog(null, auth).showLogin();
        if (user == null) {
            System.exit(0);
        }
        MainWindow window = new MainWindow(user, tableModel);
        new BillController(user, repository, window, Main::startWithLogin);
        window.setVisible(true);
    }
}
