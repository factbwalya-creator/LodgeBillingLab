package lodge.view;

import javax.swing.*;
import java.awt.*;

/** VIEW: a flat rounded button that looks the same on every operating system. */
final class RoundButton extends JButton {

    enum Kind { PRIMARY, SECONDARY, FLAT }

    private static final int ARC = 10;
    private final Kind kind;

    RoundButton(String text, Kind kind) {
        this(text, null, kind);
    }

    RoundButton(String text, Icon icon, Kind kind) {
        super(text, icon);
        this.kind = kind;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setIconTextGap(8);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(kind == Kind.PRIMARY ? Color.WHITE : Theme.DARK_BLUE);
        setBorder(BorderFactory.createEmptyBorder(
                kind == Kind.FLAT ? 6 : 8, kind == Kind.FLAT ? 12 : 20,
                kind == Kind.FLAT ? 6 : 8, kind == Kind.FLAT ? 12 : 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        Color fill = fillColor(getModel());
        if (fill != null) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        }
        if (kind == Kind.SECONDARY) {
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        }
        if (hasFocus() && isFocusable()) {
            g2.setColor(new Color(0x3B82F6));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, ARC, ARC);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    private Color fillColor(ButtonModel m) {
        if (!isEnabled()) {
            return switch (kind) {
                case PRIMARY -> new Color(0xA9C5F2);
                case SECONDARY -> Color.WHITE;
                case FLAT -> null;
            };
        }
        boolean down = m.isArmed() && m.isPressed();
        return switch (kind) {
            case PRIMARY -> down ? Theme.BLUE : m.isRollover() ? new Color(0x2F6FDB) : Theme.ACCENT;
            case SECONDARY -> down ? Theme.BORDER : m.isRollover() ? Theme.LIGHT_PANEL : new Color(0xF6F9FE);
            case FLAT -> down ? Theme.BORDER : m.isRollover() ? Theme.LIGHT_PANEL : null;
        };
    }
}
