package lodge.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** VIEW: small vector icons drawn in code, so no image files are needed. */
final class Icons {

    private Icons() {
    }

    private interface Painter {
        void paint(Graphics2D g, double s);
    }

    private record PaintedIcon(int size, Color color, Painter painter) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);
            painter.paint(g2, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    static Icon person(Color c) {
        return new PaintedIcon(18, c, (g, s) -> {
            g.fill(new Ellipse2D.Double(s * 0.30, s * 0.06, s * 0.40, s * 0.40));
            g.fill(new Arc2D.Double(s * 0.10, s * 0.54, s * 0.80, s * 0.84, 0, 180, Arc2D.PIE));
        });
    }

    static Icon receipt(Color c) {
        return new PaintedIcon(18, c, (g, s) -> {
            g.fill(new RoundRectangle2D.Double(s * 0.17, s * 0.04, s * 0.66, s * 0.92, 4, 4));
            g.setColor(Color.WHITE);
            for (int i = 0; i < 3; i++) {
                g.fill(new Rectangle2D.Double(s * 0.30, s * 0.28 + i * s * 0.20, s * 0.40, s * 0.07));
            }
        });
    }

    static Icon trash(Color c) {
        return new PaintedIcon(18, c, (g, s) -> {
            g.fill(new Rectangle2D.Double(s * 0.38, s * 0.06, s * 0.24, s * 0.10));
            g.fill(new Rectangle2D.Double(s * 0.12, s * 0.18, s * 0.76, s * 0.10));
            g.fill(new RoundRectangle2D.Double(s * 0.20, s * 0.32, s * 0.60, s * 0.62, 4, 4));
            g.setColor(Color.WHITE);
            for (int i = 0; i < 3; i++) {
                g.fill(new Rectangle2D.Double(s * 0.32 + i * s * 0.16, s * 0.42, s * 0.06, s * 0.42));
            }
        });
    }

    /** Window icon: a blue rounded square with a white L. */
    static Image appIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.BLUE);
        g.fillRoundRect(0, 0, 64, 64, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("L", (64 - fm.stringWidth("L")) / 2, (64 - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        return img;
    }
}
