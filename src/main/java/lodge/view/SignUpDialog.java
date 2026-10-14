package lodge.view;

import lodge.model.AuthService;
import lodge.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;

public final class SignUpDialog extends JDialog {

    private final AuthService auth;
    private final JTextField usernameField = new JTextField(18);
    private final JTextField displayField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JPasswordField codeField = new JPasswordField(18);
    private final JComboBox<User.Role> roleBox = new JComboBox<>(User.Role.values());
    private final JLabel message = new JLabel(" ");
    private User created = null;

    public SignUpDialog(LoginDialog owner, AuthService auth) {
        super(owner, AppInfo.NAME + " - Create account", true);
        this.auth = auth;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Logo.windowIcon());

        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        root.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        Theme.styleField(usernameField);
        Theme.styleField(displayField);
        Theme.styleField(passwordField);
        Theme.styleField(confirmField);
        Theme.styleField(codeField);
        Theme.styleCombo(roleBox);

        int row = 0;
        row = addRow(root, c, row, "Username:", usernameField);
        row = addRow(root, c, row, "Display name:", displayField);
        row = addRow(root, c, row, "Role:", roleBox);
        row = addRow(root, c, row, "Password (min 6):", passwordField);
        row = addRow(root, c, row, "Confirm password:", confirmField);
        row = addRow(root, c, row, "Officiating code:", codeField);

        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        message.setForeground(Theme.RED);
        root.add(message, c);

        RoundButton create = new RoundButton("Create account", RoundButton.Kind.PRIMARY);
        RoundButton cancel = new RoundButton("Cancel", RoundButton.Kind.SECONDARY);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(create);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        root.add(buttons, c);

        create.addActionListener(e -> attemptCreate());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(create);

        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private int addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setForeground(Theme.DARK_BLUE);
        l.setLabelFor(field);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(l, c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
        return row + 1;
    }

    private void attemptCreate() {
        String username = usernameField.getText().trim();
        String display = displayField.getText().trim();
        char[] pw = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        char[] code = codeField.getPassword();
        User.Role role = (User.Role) roleBox.getSelectedItem();

        message.setForeground(Theme.RED);
        try {
            if (username.isEmpty() || display.isEmpty() || pw.length == 0) {
                message.setText("All fields are required.");
                return;
            }
            if (!Arrays.equals(pw, confirm)) {
                message.setText("Passwords do not match.");
                return;
            }
            if (pw.length < 6) {
                message.setText("Password must be at least 6 characters.");
                return;
            }
            if (!auth.officiatingCodeMatches(code)) {
                message.setText("The officiating code is wrong.");
                return;
            }
            created = auth.createUser(username, display, pw, role);
            JOptionPane.showMessageDialog(this,
                    "Account created: " + created.username() + " (" + created.role() + ").\n"
                            + "You can now sign in.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException ex) {
            message.setText(ex.getMessage());
        } catch (SQLException ex) {
            message.setText("Database error: " + ex.getMessage());
        } finally {
            Arrays.fill(pw, '\0');
            Arrays.fill(confirm, '\0');
            Arrays.fill(code, '\0');
        }
    }

    public User showSignUp() {
        setVisible(true);
        return created;
    }
}