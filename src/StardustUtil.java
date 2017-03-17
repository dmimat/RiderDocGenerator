import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Dmitry.Matveev on 16-Mar-17.
 */
public class StardustUtil {

    static String riderDocPath;

    public static String getRiderDocPath(){
        if (riderDocPath != null) return riderDocPath;
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.setDescription("Select Rider documentation root");
        VirtualFile file = FileChooser.chooseFile(descriptor, null, null);
        riderDocPath = file.getPath();
        return riderDocPath;
    }

    public static String normalizeShortcutKeys(String shortcut, String keymap) {
        if (Objects.equals(keymap, "Visual Studio") ||
                Objects.equals(keymap, "ReSharper") ||
                Objects.equals(keymap, "Rider")) return shortcut;

        String normalized = shortcut;
        Map<String, String> replacements = new HashMap<String, String>() {{
            put("Shift", "⇧");
            put("Ctrl", "^");
            put("Meta", "⌘");
            put("Alt", "⌥");
        }};
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized;
    }
}
