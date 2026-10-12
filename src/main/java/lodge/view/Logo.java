package lodge.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * VIEW: loads the lodge logo once and hands out copies at the size each screen needs.
 *
 * To use your own logo, put a file named logo.png (or logo.jpg / logo.jpeg / logo.gif) in
 *     src/main/resources/
 * No code change is needed. If there is no logo file the app simply runs without one.
 */
public final class Logo {

    private static final String[] FILES = {"/logo.png", "/logo.jpg", "/logo.jpeg", "/logo.gif"};
    private static final BufferedImage ORIGINAL = load();
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private Logo() {
    }

    public static boolean isAvailable() {
        return ORIGINAL != null;
    }

    /** An icon that fits inside maxW x maxH (never enlarged), or null if no logo was added. */
    static Icon icon(int maxW, int maxH) {
        BufferedImage img = scaled(maxW, maxH);
        return img == null ? null : new ImageIcon(img);
    }

    /** The logo shrunk to fit inside maxW x maxH with its proportions kept, or null if none. */
    static synchronized BufferedImage scaled(int maxW, int maxH) {
        if (ORIGINAL == null) return null;
        return CACHE.computeIfAbsent(maxW + "x" + maxH, key -> {
            double s = Math.min(1.0, Math.min((double) maxW / ORIGINAL.getWidth(),
                    (double) maxH / ORIGINAL.getHeight()));
            int w = Math.max(1, (int) Math.round(ORIGINAL.getWidth() * s));
            int h = Math.max(1, (int) Math.round(ORIGINAL.getHeight() * s));
            return shrink(ORIGINAL, w, h);
        });
    }

    /** Taskbar / title-bar icon: the logo centred on a square, or the built-in "L" if no logo. */
    static Image windowIcon() {
        BufferedImage fit = scaled(64, 64);
        if (fit == null) return Icons.appIcon();
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(fit, (64 - fit.getWidth()) / 2, (64 - fit.getHeight()) / 2, null);
        g.dispose();
        return out;
    }

    // ---------- loading and scaling ----------

    private static BufferedImage load() {
        for (String name : FILES) {
            try (InputStream in = Logo.class.getResourceAsStream(name)) {
                if (in == null) continue;
                BufferedImage img = ImageIO.read(in);
                if (img != null) return img;
                System.err.println("Logo file " + name + " is not a readable image; ignoring it.");
            } catch (IOException | RuntimeException e) {
                System.err.println("Logo file " + name + " could not be read: " + e.getMessage());
            }
        }
        return null;
    }

    /** Halves the image step by step, which keeps edges smooth (one big jump looks jagged). */
    private static BufferedImage shrink(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        while (cur.getWidth() > w * 2 && cur.getHeight() > h * 2) {
            cur = draw(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        }
        return draw(cur, w, h);
    }

    private static BufferedImage draw(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}