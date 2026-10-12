package lodge.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** VIEW: shows a receipt (logo on top) and lets the user print it or save it as a text file. */
final class ReceiptDialog extends JDialog {

    private final JTextArea area;
    private final String billId;

    ReceiptDialog(Frame owner, String billId, String text) {
        super(owner, "Receipt " + billId, true);
        this.billId = billId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Logo.windowIcon());

        area = new JTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setEditable(false);
        area.setForeground(Theme.TEXT);
        area.setMargin(new Insets(12, 14, 12, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(new Theme.RoundedBorder(Theme.BORDER, 8));
        scroll.setPreferredSize(new Dimension(440, Logo.isAvailable() ? 460 : 340));

        // The logo sits above the receipt text like a letterhead.
        Icon logo = Logo.icon(260, 80);
        if (logo != null) {
            JLabel logoLabel = new JLabel(logo, SwingConstants.CENTER);
            logoLabel.setOpaque(true);
            logoLabel.setBackground(Color.WHITE);
            logoLabel.setBorder(BorderFactory.createEmptyBorder(14, 12, 4, 12));
            scroll.setColumnHeaderView(logoLabel);
        }

        RoundButton print = new RoundButton("Print", RoundButton.Kind.SECONDARY);
        RoundButton save = new RoundButton("Save as text file", RoundButton.Kind.SECONDARY);
        RoundButton close = new RoundButton("Close", RoundButton.Kind.PRIMARY);
        print.addActionListener(e -> print());
        save.addActionListener(e -> save());
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(print);
        buttons.add(save);
        buttons.add(close);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Prints the receipt with the logo at the top. Choosing "Save as PDF" as the printer makes a PDF. */
    private void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Receipt " + billId);
        job.setPrintable(new ReceiptPrintable(area.getText()));
        if (!job.printDialog()) return;
        try {
            job.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Could not print the receipt:\n" + ex.getMessage(),
                    "Print problem", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void save() {
        Path target = FileDialogs.chooseSave(this, "receipt-" + billId + ".txt", "Text file (*.txt)", "txt");
        if (target == null) return;
        try {
            Files.writeString(target, area.getText(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Receipt saved to " + target.getFileName(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the receipt:\n" + ex.getMessage(),
                    "Save problem", JOptionPane.ERROR_MESSAGE);
        }
    }
}