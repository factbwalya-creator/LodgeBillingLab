package lodge.view;

import lodge.model.BillingCalculator;
import lodge.model.RoomType;
import lodge.model.User;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * VIEW: the main screen. It only draws things and reports what the user did.
 * It never decides what a click means: it tells its {@link Listener} (the controller).
 */
public class MainWindow extends JFrame {

    /** What the view reports to the controller. */
    public interface Listener {
        default void onFormChanged() { }
        default void onSave() { }
        default void onNewGuest() { }
        default void onViewReceipt() { }
        default void onDelete() { }
        default void onExport() { }
        default void onLogout() { }
        default void onExit() { }
        default void onAdmin() { }
    }

    private static final int FIELD_HEIGHT = 36;

    private final User user;
    private final BillTableModel tableModel;
    private Listener listener = new Listener() { };

    private final JTextField guestField = new JTextField();
    private final JComboBox<RoomType> roomBox = new JComboBox<>(RoomType.values());
    private final JTextField nightsField = new JTextField();
    private final JTextField extrasField = new JTextField();
    private final JTextField paidField = new JTextField();
    private final JTextArea notesArea = new JTextArea(3, 20);

    private final JLabel roomChargeLbl = new JLabel();
    private final JLabel discountLbl = new JLabel();
    private final JLabel extrasLbl = new JLabel();
    private final JLabel totalLbl = new JLabel();
    private final JLabel paidLbl = new JLabel();
    private final JLabel balanceLbl = new JLabel();

    private final JLabel statusLbl = new JLabel(" ");
    private final JLabel footerLbl = new JLabel(" ");
    private final Timer statusTimer = new Timer(8000, e -> statusLbl.setText(" "));
    private final HintField searchField = new HintField("Search guest or bill number");
    private final JTable table;
    private final TableRowSorter<BillTableModel> sorter;
    private RoundButton saveButton;
    private JScrollPane bodyScroll;

    public MainWindow(User user, BillTableModel tableModel) {
        super(AppInfo.NAME);
        this.user = user;
        this.tableModel = tableModel;
        this.table = new BillTable(tableModel);
        this.sorter = new TableRowSorter<>(tableModel);
        statusTimer.setRepeats(false);

        setIconImage(Logo.windowIcon());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                listener.onExit();
            }
        });
        setJMenuBar(buildMenuBar());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(saveButton);

        wireLiveUpdates();
        clearForm();
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1100, usable.width), Math.min(860, usable.height));
        setMinimumSize(new Dimension(Math.min(900, usable.width), Math.min(520, usable.height)));
        setLocationRelativeTo(null);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void dispose() {
        statusTimer.stop();
        tableModel.removeTableModelListener(table);
        super.dispose();
    }

    // =====================================================================
    //  Building the screen
    // =====================================================================

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BLUE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(true);
        bar.setBackground(Theme.BLUE);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 12));

        JComponent badge = logoBadge();
        if (badge != null) {
            bar.add(badge);
            bar.add(Box.createHorizontalStrut(12));
        }
        JLabel title = new JLabel(AppInfo.NAME);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(Color.WHITE);
        bar.add(title);
        bar.add(Box.createHorizontalGlue());

        JMenu file = menu("File", KeyEvent.VK_F);
        file.add(item("New guest", ctrl(KeyEvent.VK_N), () -> listener.onNewGuest()));
        file.add(item("Save bill", ctrl(KeyEvent.VK_S), () -> listener.onSave()));
        file.add(item("View receipt", ctrl(KeyEvent.VK_R), () -> listener.onViewReceipt()));
        file.add(item("Delete selected bill", ctrl(KeyEvent.VK_D), () -> listener.onDelete()));
        file.addSeparator();
        file.add(item("Export bills to CSV...", ctrl(KeyEvent.VK_E), () -> listener.onExport()));
        file.addSeparator();
        file.add(item("Log out", ctrl(KeyEvent.VK_L), () -> listener.onLogout()));
        file.add(item("Exit", ctrl(KeyEvent.VK_Q), () -> listener.onExit()));

        if (user.isAdmin()) {
            JMenu admin = menu("Admin", KeyEvent.VK_A);
            admin.add(item("Manage users...", ctrl(KeyEvent.VK_M), () -> listener.onAdmin()));
            bar.add(admin);
        }

        JMenu help = menu("Help", KeyEvent.VK_H);
        help.add(item("About", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), this::showAbout));

        bar.add(file);
        bar.add(help);
        return bar;
    }

    private static JComponent logoBadge() {
        Icon logo = Logo.icon(150, 34);
        if (logo == null) return null;
        RoundedPanel badge = new RoundedPanel(Color.WHITE, 12);
        badge.setLayout(new BorderLayout());
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        badge.add(new JLabel(logo));
        badge.setMaximumSize(badge.getPreferredSize());
        return badge;
    }

    private static KeyStroke ctrl(int key) {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    private static JMenu menu(String text, int mnemonic) {
        JMenu m = new JMenu(text);
        m.setMnemonic(mnemonic);
        m.setForeground(Color.WHITE);
        m.setFont(m.getFont().deriveFont(Font.PLAIN, 15f));
        m.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return m;
    }

    private static JMenuItem item(String text, KeyStroke accelerator, Runnable action) {
        JMenuItem mi = new JMenuItem(text);
        mi.setAccelerator(accelerator);
        mi.addActionListener(e -> action.run());
        return mi;
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                AppInfo.NAME + "\nLogged in as: " + user.displayName()
                        + " (" + user.username() + ", " + user.role() + ")"
                        + "\n\nShortcuts: Ctrl+N new guest, Ctrl+S save, Ctrl+R receipt,\n"
                        + "Ctrl+D delete, Ctrl+E export, Ctrl+L log out, Ctrl+Q exit",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private JComponent buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBackground(Color.WHITE);
        tb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 16)));

        tb.add(toolButton("New guest", Icons.person(Theme.DARK_BLUE),
                "Clear the form for a new guest (Ctrl+N)", () -> listener.onNewGuest()));
        tb.add(Box.createHorizontalStrut(14));
        tb.add(toolButton("View receipt", Icons.receipt(Theme.DARK_BLUE),
                "Show the receipt for the selected bill (Ctrl+R)", () -> listener.onViewReceipt()));
        tb.add(Box.createHorizontalStrut(14));
        tb.add(toolButton("Delete selected bill", Icons.trash(Theme.DARK_BLUE),
                "Delete the selected bill (Ctrl+D or Delete)", () -> listener.onDelete()));
        tb.add(Box.createHorizontalGlue());
        JLabel who = new JLabel("User: " + user.displayName() + " (" + user.role() + ")");
        who.setForeground(Theme.DARK_BLUE);
        tb.add(who);
        return tb;
    }

    private static RoundButton toolButton(String text, Icon icon, String tip, Runnable action) {
        RoundButton b = new RoundButton(text, icon, RoundButton.Kind.FLAT);
        b.setToolTipText(tip);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private JComponent buildBody() {
        JPanel body = new ScrollBody(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));

        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setOpaque(false);
        top.add(buildFormPanel(), BorderLayout.CENTER);
        top.add(buildSidePanel(), BorderLayout.EAST);
        body.add(top, BorderLayout.NORTH);
        body.add(buildSavedBillsPanel(), BorderLayout.CENTER);

        bodyScroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setBorder(null);
        bodyScroll.getViewport().setBackground(Color.WHITE);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(18);
        return bodyScroll;
    }

    private JComponent buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 2, 5, 12);
        c.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel(AppInfo.NAME);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(Theme.DARK_BLUE);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        c.insets = new Insets(5, 2, 16, 12);
        p.add(title, c);
        c.insets = new Insets(5, 2, 5, 12);

        for (JTextField f : new JTextField[]{guestField, nightsField, extrasField, paidField}) {
            Theme.styleField(f);
            f.setPreferredSize(new Dimension(100, FIELD_HEIGHT));
        }
        guestField.setToolTipText("The guest's full name (up to 60 characters)");
        nightsField.setToolTipText("Whole number of nights, 1 to 365");
        extrasField.setToolTipText("Extra charges in kwacha, e.g. 200 or 150.50");
        paidField.setToolTipText("Amount the guest has paid so far, in kwacha");
        Theme.styleCombo(roomBox);
        roomBox.setPreferredSize(new Dimension(100, FIELD_HEIGHT));

        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        addRow(p, c, 2, "Guest name:", guestField);
        addRow(p, c, 3, "Room type:", roomBox);
        addRow(p, c, 4, "Nights:", nightsField);
        addRow(p, c, 5, "Extras in K:", extrasField);
        addRow(p, c, 6, "Amount paid in K:", paidField);

        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setForeground(Theme.TEXT);
        notesArea.setMargin(new Insets(6, 8, 6, 8));
        notesArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        notesArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(new Theme.RoundedBorder(Theme.BORDER, 8));
        notesScroll.setPreferredSize(new Dimension(100, 76));
        addRow(p, c, 7, "Notes:", notesScroll);

        saveButton = new RoundButton("Save bill", RoundButton.Kind.PRIMARY);
        saveButton.addActionListener(e -> listener.onSave());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btns.setOpaque(false);
        btns.add(saveButton);
        c.gridx = 1; c.gridy = 8; c.weightx = 1;
        c.insets = new Insets(8, 0, 0, 12);
        p.add(btns, c);
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.DARK_BLUE);
        label.setLabelFor(field);
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        c.anchor = row == 7 ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
        p.add(label, c);
        c.gridx = 1; c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        p.add(field, c);
    }

    private JComponent buildSidePanel() {
        RoundedPanel side = new RoundedPanel(Theme.LIGHT_PANEL, 18) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(340, super.getPreferredSize().height);
            }
        };
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        side.add(left(heading("Practice room prices")));
        side.add(Box.createVerticalStrut(8));
        for (RoomType r : RoomType.values()) {
            side.add(left(new JLabel(r + ": " + BillingCalculator.money(r.pricePerNight()) + " per night")));
            side.add(Box.createVerticalStrut(3));
        }
        side.add(Box.createVerticalStrut(6));
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        side.add(left(sep));
        side.add(Box.createVerticalStrut(8));
        side.add(left(new JLabel("<html><body style='width:270px'>" + BillingCalculator.DISCOUNT_MIN_NIGHTS
                + " nights or more: " + BillingCalculator.discountLabel() + " off the room charge.<br><br>"
                + "Extras get no discount. No tax in this lab.</body></html>")));

        side.add(Box.createVerticalStrut(10));
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(Theme.BORDER);
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        side.add(left(sep2));
        side.add(Box.createVerticalStrut(8));
        side.add(left(heading("Live bill")));
        side.add(Box.createVerticalStrut(6));
        totalLbl.setFont(totalLbl.getFont().deriveFont(Font.BOLD));
        balanceLbl.setFont(balanceLbl.getFont().deriveFont(Font.BOLD, 15f));
        for (JLabel lbl : new JLabel[]{roomChargeLbl, discountLbl, extrasLbl, totalLbl, paidLbl, balanceLbl}) {
            side.add(left(lbl));
            side.add(Box.createVerticalStrut(3));
        }
        side.add(Box.createVerticalGlue());
        side.add(Box.createVerticalStrut(8));
        JLabel note = new JLabel("<html><i>Bills are saved automatically on this computer.</i></html>");
        note.setForeground(Theme.MUTED);
        side.add(left(note));
        return side;
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        l.setForeground(Theme.DARK_BLUE);
        return l;
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private JComponent buildSavedBillsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        JLabel savedTitle = new JLabel("Saved bills");
        savedTitle.setFont(savedTitle.getFont().deriveFont(Font.BOLD, 18f));
        savedTitle.setForeground(Theme.DARK_BLUE);
        statusLbl.setForeground(Theme.GREEN);
        statusLbl.setFont(statusLbl.getFont().deriveFont(Font.BOLD));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        titleRow.setOpaque(false);
        titleRow.add(savedTitle);
        titleRow.add(statusLbl);

        Theme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(250, 32));
        searchField.addActionListener(e -> { });
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleRow, BorderLayout.WEST);
        header.add(searchField, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        configureTable();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new Theme.RoundedBorder(Theme.BORDER, 8));
        sp.getViewport().setBackground(Color.WHITE);
        sp.setPreferredSize(new Dimension(100, 260));
        p.add(sp, BorderLayout.CENTER);

        footerLbl.setForeground(Theme.DARK_BLUE);
        footerLbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));
        p.add(footerLbl, BorderLayout.SOUTH);
        return p;
    }

    private void configureTable() {
        table.setRowSorter(sorter);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(Theme.SELECTION);
        table.setSelectionForeground(Theme.TEXT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        TableColumnModel cm = table.getColumnModel();
        int[] widths = {80, 240, 120, 80, 140, 140, 140};
        for (int i = 0; i < widths.length; i++) {
            cm.getColumn(i).setPreferredWidth(widths[i]);
            cm.getColumn(i).setCellRenderer(new CellRenderer(i == BillTableModel.BALANCE_COLUMN));
        }

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 36));
        TableCellRenderer base = header.getDefaultRenderer();
        header.setDefaultRenderer((t, value, sel, focus, row, col) -> {
            Component comp = base.getTableCellRendererComponent(t, value, sel, focus, row, col);
            if (comp instanceof JLabel l) {
                l.setOpaque(true);
                l.setBackground(Theme.LIGHT_PANEL);
                l.setForeground(Theme.DARK_BLUE);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDER),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            }
            return comp;
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    listener.onViewReceipt();
                }
            }
        });
        bindTableKey(KeyEvent.VK_DELETE, "deleteBill", () -> listener.onDelete());
        bindTableKey(KeyEvent.VK_ENTER, "viewReceipt", () -> listener.onViewReceipt());
    }

    private void bindTableKey(int key, String name, Runnable action) {
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
        table.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void wireLiveUpdates() {
        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { listener.onFormChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { listener.onFormChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { listener.onFormChanged(); }
        };
        nightsField.getDocument().addDocumentListener(dl);
        extrasField.getDocument().addDocumentListener(dl);
        paidField.getDocument().addDocumentListener(dl);
        roomBox.addActionListener(e -> listener.onFormChanged());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applySearch(); }
            @Override public void removeUpdate(DocumentEvent e) { applySearch(); }
            @Override public void changedUpdate(DocumentEvent e) { applySearch(); }
        });
    }

    private void applySearch() {
        String t = searchField.getText().trim();
        sorter.setRowFilter(t.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(t), 0, 1, 2));
    }

    // =====================================================================
    //  What the controller reads and sets
    // =====================================================================

    public String guestName() { return guestField.getText(); }
    public RoomType roomType() { return (RoomType) roomBox.getSelectedItem(); }
    public String nightsText() { return nightsField.getText(); }
    public String extrasText() { return extrasField.getText(); }
    public String paidText() { return paidField.getText(); }
    public String notesText() { return notesArea.getText(); }

    public int selectedRow() {
        int v = table.getSelectedRow();
        return v < 0 ? -1 : table.convertRowIndexToModel(v);
    }

    public void revealRow(int modelRow) {
        int v = table.convertRowIndexToView(modelRow);
        if (v >= 0) {
            table.setRowSelectionInterval(v, v);
            table.scrollRectToVisible(table.getCellRect(v, 0, true));
        }
    }

    public void showSummary(BillingCalculator.Result r) {
        roomChargeLbl.setText("Room charge: " + BillingCalculator.money(r.roomCharge()));
        discountLbl.setText("Discount (" + BillingCalculator.discountLabel() + "): -"
                + BillingCalculator.money(r.discount()));
        extrasLbl.setText("Extras: " + BillingCalculator.money(r.extras()));
        totalLbl.setText("Total: " + BillingCalculator.money(r.total()));
        paidLbl.setText("Paid: " + BillingCalculator.money(r.paid()));
        if (r.balance().signum() < 0) {
            balanceLbl.setText("Change due: " + BillingCalculator.money(r.balance().negate()));
            balanceLbl.setForeground(Theme.BLUE);
        } else {
            balanceLbl.setText("Balance: " + BillingCalculator.money(r.balance()));
            balanceLbl.setForeground(r.balance().signum() == 0 ? Theme.GREEN : Theme.RED);
        }
    }

    public void showInvalidSummary() {
        roomChargeLbl.setText("Room charge: -");
        discountLbl.setText("Discount: -");
        extrasLbl.setText("Extras: -");
        totalLbl.setText("Total: -");
        paidLbl.setText("Paid: -");
        balanceLbl.setText("Check your numbers");
        balanceLbl.setForeground(Theme.RED);
    }

    public void markInvalid(boolean nights, boolean extras, boolean paid) {
        Theme.setInvalid(nightsField, nights);
        Theme.setInvalid(extrasField, extras);
        Theme.setInvalid(paidField, paid);
    }

    public void setStatus(String message) {
        statusLbl.setText(message);
        statusTimer.restart();
    }

    public void setFooter(String text) {
        footerLbl.setText(text);
    }

    public void clearForm() {
        guestField.setText("");
        roomBox.setSelectedIndex(0);
        nightsField.setText("");
        extrasField.setText("");
        paidField.setText("");
        notesArea.setText("");
        markInvalid(false, false, false);
        guestField.requestFocusInWindow();
        if (bodyScroll != null) {
            bodyScroll.getVerticalScrollBar().setValue(0);
        }
    }

    public void focusGuest() { guestField.requestFocusInWindow(); }
    public void focusNights() { nightsField.requestFocusInWindow(); }

    public void showError(String message) {
        showError("Check input", message);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirm(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public Path chooseSaveFile(String suggestedName, String description, String extension) {
        return FileDialogs.chooseSave(this, suggestedName, description, extension);
    }

    public void showReceipt(String billId, String text) {
        new ReceiptDialog(this, billId, text).setVisible(true);
    }

    public void showAdminDialog(lodge.model.Database db, lodge.model.AuthService auth) {
        new AdminDialog(this, db, auth, user).setVisible(true);
    }

    // =====================================================================
    //  Small helper classes
    // =====================================================================

    private static final class ScrollBody extends JPanel implements Scrollable {
        ScrollBody(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(18, visible.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && vp.getHeight() >= getPreferredSize().height;
        }
    }

    private final class BillTable extends JTable {
        BillTable(BillTableModel model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0) {
                String msg = tableModel.getRowCount() == 0
                        ? "No saved bills yet" : "No bills match your search";
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Theme.MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, 30);
                g2.dispose();
            }
        }
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private final boolean colourBalance;

        CellRenderer(boolean colourBalance) {
            this.colourBalance = colourBalance;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            String text = value instanceof BigDecimal bd ? BillingCalculator.money(bd) : String.valueOf(value);
            super.getTableCellRendererComponent(t, text, selected, false, row, col);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            Color fg = Theme.TEXT;
            if (colourBalance && value instanceof BigDecimal bd) {
                fg = bd.signum() > 0 ? Theme.RED : bd.signum() == 0 ? Theme.GREEN : Theme.BLUE;
            }
            setForeground(fg);
            return this;
        }
    }

    private static final class HintField extends JTextField {
        private final String hint;

        HintField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Theme.MUTED);
                Insets in = getInsets();
                g2.drawString(hint, in.left, (getHeight() - g2.getFontMetrics().getHeight()) / 2
                        + g2.getFontMetrics().getAscent());
                g2.dispose();
            }
        }
    }
}