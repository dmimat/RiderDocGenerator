
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.project.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;

/**
 * Created by Dmitry.Matveev on 26-Jan-17.
 */
public class DumpShortcutsForHelpAction extends AnAction {

    public static final String[] activeKeymapIds = new String[]{"Visual Studio", "ReSharper", "Rider",
            "Visual Studio OSX", "ReSharper OSX", "Rider OSX"};

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

            for (String keymapId : activeKeymapIds) {
                Keymap keymap = KeymapManagerEx.getInstanceEx().getKeymap(keymapId);
                Shortcut[] shortcuts = keymap.getShortcuts(id);
                if (shortcuts.length == 0) continue;
                Shortcut shortcut = shortcuts[0];
                if (shortcut.isKeyboard()) {
                    Element shortCutElement = doc.createElement("Shortcut");
                    shortCutElement.setAttribute("layout", keymap.getName());
                    shortCutElement.setTextContent(
                            StardustUtil.normalizeShortcutKeys(KeymapUtil.getShortcutText(shortcut), keymapId));
                    actionElement.appendChild(shortCutElement);
                }
            }
        }

        StardustXmlUtil.saveXmlDocumentToFile(doc,StardustUtil.getRiderDocPath() + "\\keymap.xml");

        createKeymaps(activeKeymapIds, registeredActionIds, e.getProject(), actionManager);
    }

    private void createKeymaps(String[] activeKeymapIds, String[] registeredActionIds, Project pjt, ActionManagerEx actionManager) {

        String rootDir = StardustUtil.getRiderDocPath();
        File actionMapFile = new File(rootDir + "\\nonProject\\ActionMap.xml");

        Document topic = StardustXmlUtil.createTopic("Keymap_Chunks", "Keymap Chunks");

        NodeList categories = null;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(actionMapFile);
            doc.getDocumentElement().normalize();
            categories = doc.getElementsByTagName("category");

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (categories == null) return;


        for (int i = 0; i < categories.getLength(); i++) {
            Element category = (Element) categories.item(i);
            Element chunk =
                    StardustXmlUtil.createChunk(category.getAttribute("name"), topic);
            NodeList actionNodes = category.getElementsByTagName("action");
            for (int j = 0; j < actionNodes.getLength(); j++) {
                Element actionElement = (Element) actionNodes.item(j);
                String actionId = actionElement.getAttribute("id");
                Element tr = topic.createElement("tr");
                Element td1 = topic.createElement("td");
                AnAction action = actionManager.getAction(actionId);
                String text = null;
                String hardText = actionElement.getAttribute("text");
                if (hardText != null && !hardText.isEmpty())
                    text = hardText;
                else {
                    try {
                        text = action.getTemplatePresentation().getText();
                    } catch (Exception e) {
                        System.out.println("Action '" + actionId + "' doesn't exist");
                        throw e;
                    }
                }
                if (text == null || text.isEmpty()) text = actionId;

                Element link =
                        StardustXmlUtil.createLink(actionElement.getAttribute("topic"),
                                text, actionElement.getAttribute("anchor"),
                                actionElement.getAttribute("origin"),
                                topic);
                td1.appendChild(link);
                tr.appendChild(td1);

                String trFilters = "";

                for (String keymapId : activeKeymapIds) {
                    Element td2 = topic.createElement("td");
                    Keymap keymap = KeymapManagerEx.getInstanceEx().getKeymap(keymapId);
                    Shortcut[] shortcuts = keymap.getShortcuts(actionId);

                    td2.setAttribute("filter", keymapId);
                    td2.setAttribute("width", "25%");
                    for (Shortcut shortcut : shortcuts)
                        addShortcut(
                                StardustUtil.normalizeShortcutKeys(
                                        KeymapUtil.getShortcutText(shortcut), keymapId), td2, topic);
                    String hardShortcut = actionElement.getAttribute("shortcut");
                    if (hardShortcut != null && !hardShortcut.isEmpty())
                        addShortcut(StardustUtil.normalizeShortcutKeys(hardShortcut, keymapId), td2, topic);
                    tr.appendChild(td2);
                    if (td2.hasChildNodes()) {
                        if (!trFilters.isEmpty())
                            trFilters += ",";
                        trFilters += keymapId;
                    }
                }
                if (!trFilters.isEmpty()) {
                    trFilters += ",switchable";
                    tr.setAttribute("filter", trFilters);
                    Element shortcutSw = topic.createElement("shortcut");
                    shortcutSw.setAttribute("key", actionId);
                    Element td3 = topic.createElement("td");
                    td3.appendChild(shortcutSw);
                    td3.setAttribute("filter", "switchable");
                    td3.setAttribute("width", "25%");
                    tr.appendChild(td3);
                }
                chunk.appendChild(tr);
            }
            topic.getDocumentElement().appendChild(chunk);
        }
        StardustXmlUtil.saveTopicToFile(topic);
    }

    private void addShortcut(String text, Element parent, Document topic) {
        Element shortcutEl = topic.createElement("shortcut");
        shortcutEl.setTextContent(text);
        parent.appendChild(shortcutEl);
    }

}

