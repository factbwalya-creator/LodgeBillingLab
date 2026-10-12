package lodge.view;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/** VIEW: the "Save as" dialog used by receipts and CSV export. */
final class FileDialogs {

    private FileDialogs() {
    }

    /** Returns the chosen path, or null if the user cancelled. */
    static Path chooseSave(Component parent, String suggestedName, String description, String extension) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save as");
        fc.setSelectedFile(new File(suggestedName));
        fc.setFileFilter(new FileNameExtensionFilter(description, extension));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            f = new File(f.getAbsoluteFile().getParentFile(), f.getName() + "." + extension);
        }
        if (f.exists() && JOptionPane.showConfirmDialog(parent,
                f.getName() + " already exists. Replace it?", "Confirm replace",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return null;
        }
        return f.toPath();
    }
}
