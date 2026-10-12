package lodge.view;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;

/** VIEW: draws a receipt on paper: logo centred at the top, then the receipt text. */
final class ReceiptPrintable implements Printable {

    private static final int FONT_SIZE = 11;
    private static final int LOGO_MAX_WIDTH = 220;
    private static final int LOGO_MAX_HEIGHT = 80;

    private final String text;

    ReceiptPrintable(String text) {
        this.text = text;
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(pf.getImageableX(), pf.getImageableY());
            int w = (int) pf.getImageableWidth();
            int h = (int) pf.getImageableHeight();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, FONT_SIZE));
            FontMetrics fm = g2.getFontMetrics();
            int lineH = fm.getHeight();
            List<String> lines = wrap(fm, w);

            // A large copy of the logo is used so it stays sharp on paper.
            BufferedImage logo = Logo.scaled(900, 360);
            int drawW = 0;
            int drawH = 0;
            if (logo != null) {
                double s = Math.min((double) Math.min(w, LOGO_MAX_WIDTH) / logo.getWidth(),
                        (double) LOGO_MAX_HEIGHT / logo.getHeight());
                drawW = Math.max(1, (int) Math.round(logo.getWidth() * s));
                drawH = Math.max(1, (int) Math.round(logo.getHeight() * s));
            }
            int headerH = logo == null ? 0 : drawH + 16;

            int firstCap = Math.max(1, (h - headerH) / lineH);
            int otherCap = Math.max(1, h / lineH);
            int start = pageIndex == 0 ? 0 : firstCap + (pageIndex - 1) * otherCap;
            if (pageIndex > 0 && start >= lines.size()) {
                return NO_SUCH_PAGE;
            }
            int end = Math.min(lines.size(), pageIndex == 0 ? firstCap : start + otherCap);

            int y = 0;
            if (pageIndex == 0 && logo != null) {
                g2.drawImage(logo, (w - drawW) / 2, 0, drawW, drawH, null);
                y = headerH;
            }
            int block = 0;
            for (String line : lines) {
                block = Math.max(block, fm.stringWidth(line));
            }
            int x0 = Math.max(0, (w - block) / 2);
            for (int i = start; i < end; i++) {
                g2.drawString(lines.get(i), x0, y + fm.getAscent());
                y += lineH;
            }
            return PAGE_EXISTS;
        } finally {
            g2.dispose();
        }
    }

    /** Breaks lines that are wider than the page (long notes) at spaces. */
    private List<String> wrap(FontMetrics fm, int width) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("\\R", -1)) {
            String line = raw.stripTrailing();
            if (fm.stringWidth(line) <= width) {
                out.add(line);
                continue;
            }
            StringBuilder cur = new StringBuilder();
            for (String word : line.split(" ")) {
                String trial = cur.length() == 0 ? word : cur + " " + word;
                if (fm.stringWidth(trial) <= width) {
                    cur = new StringBuilder(trial);
                    continue;
                }
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur = new StringBuilder();
                }
                String rest = word;
                while (fm.stringWidth(rest) > width && rest.length() > 1) {
                    int n = rest.length();
                    while (n > 1 && fm.stringWidth(rest.substring(0, n)) > width) n--;
                    out.add(rest.substring(0, n));
                    rest = rest.substring(n);
                }
                cur.append(rest);
            }
            out.add(cur.toString());
        }
        return out;
    }
}