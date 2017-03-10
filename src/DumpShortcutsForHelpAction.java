
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Dmitry.Matveev on 26-Jan-17.
 */
public class DumpShortcutsForHelpAction extends AnAction {

    public static final String[] activeKeymapIds = new String[]{"Visual Studio", "ReSharper", "Rider", "$default",
            "Visual Studio OSX", "ReSharper OSX", "Rider OSX", "Mac OS X"};

    @Override
    public void actionPerformed(AnActionEvent e) {

        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        String[] registeredActionIds = actionManager.getActionIds("");

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            exception.printStackTrace();
        }

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("Keymap");
        doc.appendChild(rootElement);

        for (String id : registeredActionIds) {
            AnAction action = actionManager.getAction(id);
            if (action == null) continue;
            String text = action.getTemplatePresentation().getText();
            if (text == null || text.isEmpty()) text = id;
            Element actionElement = doc.createElement("Action");
            actionElement.setAttribute("id", id);
            actionElement.setAttribute("title", text);
            rootElement.appendChild(actionElement);

            for (String keymapId : activeKeymapIds){
                Keymap keymap = KeymapManagerEx.getInstanceEx().getKeymap(keymapId);
                Shortcut[] shortcuts = keymap.getShortcuts(id);
                if (shortcuts.length == 0) continue;
                Shortcut shortcut = shortcuts[0];
                if (shortcut.isKeyboard()) {
                    Element shortCutElement = doc.createElement("Shortcut");
                    shortCutElement.setAttribute("layout", keymap.getName());
                    shortCutElement.setTextContent(KeymapUtil.getShortcutText(shortcut));
                    actionElement.appendChild(shortCutElement);
                }
            }
        }

        StardustXmlUtil.saveXmlDocumentToFile(doc,"C:\\Temp\\keymap.xml");
    }

}

