package lodge.view;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** VIEW: colours, fonts and small styling helpers shared by every screen. */
public final class Theme {
    public static final Color BLUE = new Color(0x2457A6);
    public static final Color DARK_BLUE = new Color(0x1B4A8F);
    public static final Color ACCENT = new Color(0x3B82F6);
    public static final Color LIGHT_PANEL = new Color(0xE8F0FB);
    public static final Color BORDER = new Color(0xC9D7EA);
    public static final Color GREEN = new Color(0x0B7A3B);
    public static final Color RED = new Color(0xB00020);
    public static final Color TEXT = new Color(0x1C2B4A);
    public static final Color MUTED = new Color(0x6B7A90);
    public static final Color SELECTION = new Color(0xD6E4F8);

    private static final int BASE_SIZE = 14;

    private Theme() {
    }

    /** Call once at start-up, before any window is created. */
    public static void install() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep the default look
        }
        String family = pickFamily();
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource f) {
                UIManager.put(key, new FontUIResource(family, f.getStyle(), BASE_SIZE));
            }
        }
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("CheckBox.background", Color.WHITE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("OptionPane.messageForeground", TEXT);
    }

    private static String pickFamily() {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Noto Sans", "Open Sans", "Ubuntu"}) {
            if (available.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }

    // ---------- fields ----------

    public static void styleField(JComponent c) {
        c.setBackground(Color.WHITE);
        c.setForeground(TEXT);
        c.setBorder(fieldBorder(false));
    }

    public static void setInvalid(JComponent c, boolean invalid) {
        c.setBorder(fieldBorder(invalid));
    }

    /** Gives a drop-down the same rounded, flat look as the text fields. */
    public static void styleCombo(JComboBox<?> box) {
        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override
                    public void paint(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Color.WHITE);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(DARK_BLUE);
                        int cx = getWidth() / 2 - 2;
                        int cy = getHeight() / 2;
                        g2.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
                        g2.dispose();
                    }
                };
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setContentAreaFilled(false);
                b.setFocusable(false);
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(Color.WHITE);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        box.setBorder(new RoundedBorder(BORDER, 8));
        box.setBackground(Color.WHITE);
        box.setForeground(TEXT);
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                setForeground(TEXT);
                setBackground(selected ? SELECTION : Color.WHITE);
                return this;
            }
        });
    }

    private static Border fieldBorder(boolean invalid) {
        return BorderFactory.createCompoundBorder(
                new RoundedBorder(invalid ? RED : BORDER, 8),
                BorderFactory.createEmptyBorder(7, 10, 7, 10));
    }

    /** A thin rounded outline like the fields in the design. */
    static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int arc;

        RoundedBorder(Color color, int arc) {
            this.color = color;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, arc, arc));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }
}
