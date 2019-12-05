import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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

    public static String normalizeForFileName(String s){
        return s.replaceAll("\\W+", "_");
    }

    public static String normalizeShortcutKeys(String shortcut, String keymap) {
        String normalized = shortcut;

        Map<String, String> replacements = new HashMap<String, String>() {{
            put("Button1 Click", "Click");
            put("Button2 Click", "Right-click");
            put("+Back Slash", "+\\");
            put("+Slash", "+/");
            put("Open Bracket", "[");
            put("Close Bracket", "]");
            put("Comma", ",");
            put("+NumPad -", "+NumPad Minus");
            put("Button4 Click", "Mouse Back");
            put("Button5 Click", "Mouse Forward");
            put("Back Quote", "'");
            put(";", "Semicolon");
//            put("Semicolon", ";");
            put("Equals", "=");
        }};
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }

        if (Objects.equals(keymap, "Visual Studio") ||
                Objects.equals(keymap, "ReSharper") ||
                Objects.equals(keymap, "$default")) return normalized;


        Map<String, String> replacementsIos = new HashMap<String, String>() {{
            put("Shift", Character.toString((char)8679)); // ⇧
            put("Ctrl", Character.toString((char)8963)); // ⌃
            put("Meta", Character.toString((char)8984)); // ⌘
            put("Alt", Character.toString((char)8997)); // ⌥
        }};
        for (Map.Entry<String, String> entry : replacementsIos.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized;
    }
}
