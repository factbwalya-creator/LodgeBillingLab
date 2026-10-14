package lodge.view;

import lodge.model.AuthService;
import lodge.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * VIEW: modal login dialog.
 * Offers "Create account" (opens SignUpDialog) and returns the logged-in User.
 */
public class LoginDialog extends JDialog {

    private static final int MAX_ATTEMPTS = 3;

    private final AuthService auth;
    private final JTextField userField = new JTextField(16);
    private final JPasswordField passField = new JPasswordField(16);
    private final JLabel message = new JLabel(" ");
    private int attempts = 0;
    private User loggedInUser = null;

    public LoginDialog(Frame owner, AuthService auth) {
        super(owner, AppInfo.NAME + " - Login", true);
        this.auth = auth;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Logo.windowIcon());

        JPanel root = new JPanel(new BorderLayout(10, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(22, 28, 18, 28));

        // ---------- header ----------
        JLabel title = new JLabel(AppInfo.NAME);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.DARK_BLUE);

        JLabel sub = new JLabel("Please sign in to continue");
        sub.setForeground(Theme.DARK_BLUE);

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);

        JPanel head = new JPanel(new BorderLayout(14, 0));
        head.setOpaque(false);
        Icon logo = Logo.icon(120, 56);
        if (logo != null) {
            head.add(new JLabel(logo), BorderLayout.WEST);
        }
        head.add(titles, BorderLayout.CENTER);
        root.add(head, BorderLayout.NORTH);

        // ---------- form ----------
        Theme.styleField(userField);
        Theme.styleField(passField);
        userField.setPreferredSize(new Dimension(220, 36));
        passField.setPreferredSize(new Dimension(220, 36));

        final char defaultEcho = passField.getEchoChar();
        JCheckBox showPass = new JCheckBox("Show password");
        showPass.setOpaque(false);
        showPass.setFocusPainted(false);
        showPass.addActionListener(e ->
                passField.setEchoChar(showPass.isSelected() ? (char) 0 : defaultEcho));

        JLabel userLbl = new JLabel("Username:");
        userLbl.setLabelFor(userField);
        userLbl.setForeground(Theme.DARK_BLUE);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setLabelFor(passField);
        passLbl.setForeground(Theme.DARK_BLUE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        form.add(userLbl, c);
        c.gridx = 1; c.weightx = 1;
        form.add(userField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        form.add(passLbl, c);
        c.gridx = 1; c.weightx = 1;
        form.add(passField, c);

        c.gridx = 1; c.gridy = 2;
        form.add(showPass, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        message.setForeground(Theme.RED);
        form.add(message, c);

        root.add(form, BorderLayout.CENTER);

        // ---------- buttons ----------
        RoundButton login = new RoundButton("Login", RoundButton.Kind.PRIMARY);
        RoundButton create = new RoundButton("Create account", RoundButton.Kind.SECONDARY);
        RoundButton cancel = new RoundButton("Cancel", RoundButton.Kind.SECONDARY);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(create);
        buttons.add(cancel);
        buttons.add(login);
        root.add(buttons, BorderLayout.SOUTH);

        login.addActionListener(e -> attemptLogin());
        create.addActionListener(e -> openSignUp());
        cancel.addActionListener(e -> dispose());

        getRootPane().setDefaultButton(login);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);

        SwingUtilities.invokeLater(userField::requestFocusInWindow);
    }

    // =================================================================
    //  Login
    // =================================================================

    private void attemptLogin() {
        String user = userField.getText().trim();
        char[] pass = passField.getPassword();
        try {
            if (user.isEmpty() || pass.length == 0) {
                message.setForeground(Theme.RED);
                message.setText("Enter your username and password");
                return;
            }

            Optional<User> ok;
            try {
                ok = auth.authenticate(user, pass);
            } catch (SQLException e) {
                message.setForeground(Theme.RED);
                message.setText("Database error: " + e.getMessage());
                return;
            }

            if (ok.isPresent()) {
                loggedInUser = ok.get();
                dispose();
                return;
            }
        } finally {
            Arrays.fill(pass, '\0');
        }

        attempts++;
        passField.setText("");
        if (attempts >= MAX_ATTEMPTS) {
            JOptionPane.showMessageDialog(this,
                    "Too many failed attempts. The app will close.",
                    "Access denied", JOptionPane.ERROR_MESSAGE);
            dispose();
        } else {
            int left = MAX_ATTEMPTS - attempts;
            message.setForeground(Theme.RED);
            message.setText("Wrong username or password ("
                    + (left == 1 ? "1 try" : left + " tries") + " left)");
            passField.requestFocusInWindow();
        }
    }

    // =================================================================
    //  Create account
    // =================================================================

    private void openSignUp() {
        User created = new SignUpDialog(this, auth).showSignUp();
        if (created != null) {
            userField.setText(created.username());
            passField.requestFocusInWindow();
            message.setForeground(Theme.GREEN);
            message.setText("Account created — sign in now.");
        }
    }

    // =================================================================
    //  Public API
    // =================================================================

    /** Shows the dialog and returns the logged-in user, or null if login failed/cancelled. */
    public User showLogin() {
        setVisible(true);
        return loggedInUser;
    }
}