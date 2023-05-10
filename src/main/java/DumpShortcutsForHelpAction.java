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
import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by Dmitry.Matveev on 26-Jan-17.
 */
public class DumpShortcutsForHelpAction extends AnAction {

//    public static final String[] activeKeymapIds = new String[]{
//            "Visual Studio",
//            "$default"
//    };

    @Override
    public void actionPerformed(AnActionEvent e) {

        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        String[] registeredActionIds = actionManager.getActionIds("");
        Arrays.sort(registeredActionIds);

        Document doc = StardustXmlUtil.createXmlDocument();
        Element rootElement = doc.createElement("Keymap");
        rootElement.setAttribute("id", "rdr");
        doc.appendChild(rootElement);

        for (String id : registeredActionIds) {
            AnAction action = actionManager.getAction(id);
            if (action == null) continue;
            String text = action.getTemplatePresentation().getText();
            if (text == null || text.isEmpty()) text = id;
            Element actionElement = doc.createElement("Action");
            actionElement.setAttribute("id", id);
            rootElement.appendChild(actionElement);

            Element descriptionElement = doc.createElement("Description");
            descriptionElement.setTextContent(text);
            actionElement.appendChild(descriptionElement);

            Stream<Keymap> sortedKeymaps = Arrays.stream(KeymapManagerEx.getInstanceEx().getAllKeymaps()).sorted(
                    (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())
            );
            for (Keymap keymap : sortedKeymaps.toList()) {
                if(keymap == null)
                    continue;
                Shortcut[] shortcuts = keymap.getShortcuts(id);
                if (shortcuts.length == 0) continue;
                Shortcut shortcut = shortcuts[0];
//                if (shortcut.isKeyboard()) {
                    Element shortCutElement = doc.createElement("Shortcut");
                    shortCutElement.setAttribute("layout", keymap.getName());
                    shortCutElement.setTextContent(
                            KeymapUtil.getShortcutText(shortcut)
                            //StardustUtil.normalizeShortcutKeys(KeymapUtil.getShortcutText(shortcut), keymap.getName())
                    );
                    actionElement.appendChild(shortCutElement);
//                }
            }
        }

        StardustXmlUtil.saveXmlDocumentToFile(doc,StardustUtil.getRiderDocPath() + "\\keymap.xml");

       // createKeymaps(activeKeymapIds, registeredActionIds, e.getProject(), actionManager);
    }

    private void createKeymaps(String[] activeKeymapIds, String[] registeredActionIds, Project pjt, ActionManagerEx actionManager) {

        String rootDir = StardustUtil.getRiderDocPath();
        File actionMapFile = new File(rootDir + "\\nonProject\\ActionMap.xml");
        File keymapFile = new File(rootDir + "\\nonProject\\keymap-rdr.xml");

        Document keymapChunksTopic = StardustXmlUtil.createTopic("Keymap_Chunks", "Keymap Chunks");

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

        NodeList actionNodesFromKeymap = null;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(keymapFile);
            doc.getDocumentElement().normalize();
            actionNodesFromKeymap = doc.getElementsByTagName("Action");

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (actionNodesFromKeymap == null) return;

        for (int i = 0; i < categories.getLength(); i++) {
            Element category = (Element) categories.item(i);
            Element chunk =
                    StardustXmlUtil.createChunk(category.getAttribute("name"), keymapChunksTopic);
            NodeList actionNodes = category.getElementsByTagName("action");
            for (int j = 0; j < actionNodes.getLength(); j++) {
                Element actionElement = (Element) actionNodes.item(j);
                String actionId = actionElement.getAttribute("id");
                Element tr = keymapChunksTopic.createElement("tr");
                Element td1 = keymapChunksTopic.createElement("td");
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
                    }
                }
                if (text == null || text.isEmpty()) text = actionId;

                Element link =
                        StardustXmlUtil.createLink(actionElement.getAttribute("topic"),
                                text, actionElement.getAttribute("anchor"),
                                //actionElement.getAttribute("origin"),
                                keymapChunksTopic, false);
                td1.appendChild(link);
                tr.appendChild(td1);

                String trFilters = "";

                for (String keymapId : activeKeymapIds) {
                    Element td2 = keymapChunksTopic.createElement("td");
                    td2.setAttribute("filter", keymapId);
                    td2.setAttribute("width", "25%");

                    for (int n = 0; n < actionNodesFromKeymap.getLength(); n++) {
                        Element actionEl = (Element) actionNodesFromKeymap.item(n);
                        NodeList shortcutNodes = actionEl.getElementsByTagName("Shortcut");
                        for (int m = 0; j < shortcutNodes.getLength(); m++) {
                            Element shortcutEl = (Element) actionNodes.item(m);
                            String layout = shortcutEl.getAttribute("layout");
                            if (layout.equals(keymapId))
                            {
                                addShortcut(
                                        StardustUtil.normalizeShortcutKeys(shortcutEl.getTextContent(), keymapId),
                                        td2, keymapChunksTopic);
                            }
                        }

                    }
                    String hardShortcut = actionElement.getAttribute("shortcut");
                    if (hardShortcut != null && !hardShortcut.isEmpty())
                        addShortcut(StardustUtil.normalizeShortcutKeys(hardShortcut, keymapId), td2, keymapChunksTopic);
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
                    Element shortcutSw = keymapChunksTopic.createElement("shortcut");
                    shortcutSw.setAttribute("key", actionId);
                    Element td3 = keymapChunksTopic.createElement("td");
                    td3.appendChild(shortcutSw);
                    td3.setAttribute("filter", "switchable");
                    td3.setAttribute("width", "25%");
                    tr.appendChild(td3);
                }
                chunk.appendChild(tr);
            }
            keymapChunksTopic.getDocumentElement().appendChild(chunk);
        }
        StardustXmlUtil.saveTopicToFile(keymapChunksTopic, null);
    }

    private void addShortcut(String text, Element parent, Document topic) {
        Element shortcutEl = topic.createElement("shortcut");
        shortcutEl.setTextContent(text);
        parent.appendChild(shortcutEl);
    }

}

