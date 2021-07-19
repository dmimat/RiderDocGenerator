import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.QuickListsManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.ui.ActionsTreeUtil;
import com.intellij.openapi.keymap.impl.ui.Group;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Dmitry.Matveev on 26-Jan-17.
 */
public class DumpMainMenuActionsAction extends AnAction {

    public static List<Object[]> getMainMenuPaths() {
        Group rootGroup = ActionsTreeUtil.createMainGroup(null, null, QuickListsManager.getInstance().getAllQuickLists());
        List<Object[]> result = new ArrayList<>();
        for (Object group : rootGroup.getChildren()) {
            if (Objects.equals(group.toString(), ActionsTreeUtil.getMainMenuTitle()))
                for (Object path : ((Group) group).getChildren()) {
                    createPaths(path, new ArrayList<>(), result);
                    result.add(new String[]{"============================="});
                }
        }
        return result;
    }


    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        List<Object[]> paths = getMainMenuPaths();

        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream("C:\\Temp\\menu.txt"));
            for (Object[] path : paths) {
                String str = "";
                for (Object node : path) {
                    String text = node.toString();
                    if (node instanceof String) {
                        AnAction action = actionManager.getAction((String) node);
                        if (action != null)
                            text = action.getTemplatePresentation().getText();
                    }
                    str += text + " | ";
                }
                str = str.substring(0, str.length() - 3);
                pw.println(str);
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Map<String, String> menuMap =  getMenuPathList(paths, actionManager);

        Document topic = StardustXmlUtil.createTopic("Menupath_by_ID", "Menupath_by_ID Chunks");
        Document introTopic = StardustXmlUtil.createTopic("AccessIntro_by_ID", "AccessIntro_by_ID Chunks");

        for (Map.Entry<String, String> entry : menuMap.entrySet())
        {
         //   System.out.println(entry.getKey() + "/" + entry.getValue());
            Element chunk = StardustXmlUtil.createChunk(entry.getKey() , topic);
            Element menuPath = topic.createElement("menupath");
            menuPath.appendChild(topic.createTextNode(entry.getValue()));
            chunk.appendChild(menuPath);
            topic.getDocumentElement().appendChild(chunk);
        }

        for (String id : actionManager.getActionIdList("")) {
            Element introChunk = StardustXmlUtil.createChunk(id , introTopic);
            Element menuPath = null;
            if(menuMap.containsKey(id)){
                menuPath = introTopic.createElement("menupath");
                menuPath.appendChild(introTopic.createTextNode(menuMap.get(id)));
            }
            List<Shortcut []> shortcuts = new ArrayList<Shortcut[]>();
            for (String keymapId : DumpShortcutsForHelpAction.activeKeymapIds){
                Keymap keymap = KeymapManagerEx.getInstanceEx().getKeymap(keymapId);
                if (keymap == null)
                    continue;
                Shortcut[] sh = keymap.getShortcuts(id);
                if (sh.length == 0) continue;
                shortcuts.add(sh);
            }
            if (menuPath != null || shortcuts.size() > 0)
            {
                Element par = introTopic.createElement("p");
                if (menuPath != null) {
                    par.appendChild(menuPath);
                    par.appendChild(introTopic.createElement("br"));
                }
                if (shortcuts.size() > 0) {
                    Element shortCutElement = introTopic.createElement("shortcut");
                    shortCutElement.setAttribute("key", id);
                    par.appendChild(shortCutElement);
                }
                introChunk.appendChild(par);
                introTopic.getDocumentElement().appendChild(introChunk);
            }
        }
//
//        for (Object[] path : paths) {
//            if(path.length == 1) continue;
//            String str = "";
//            String actionId = "";
//            for (Object node : path) {
//                String text = node.toString();
//                if (node instanceof String) {
//                    actionId = (String) node;
//                    AnAction action = actionManager.getAction((String) node);
//                    if (action != null)
//                        text = action.getTemplatePresentation().getText();
//                }
//                str += text + " | ";
//            }
//            Element chunk = StardustXmlUtil.createChunk(actionId, topic);
//            Element menuPath = topic.createElement("menupath");
//            menuPath.appendChild(topic.createTextNode(str.substring(0, str.length() - 3)));
//            chunk.appendChild(menuPath);
//            topic.getDocumentElement().appendChild(chunk);
//      }

        StardustXmlUtil.saveTopicToFile(topic, null);
        StardustXmlUtil.saveTopicToFile(introTopic, null);
    }

    private static SortedMap<String, String> getMenuPathList(List<Object[]> paths, ActionManagerEx actionManager){
        SortedMap<String, String> map = new TreeMap<>();
        for (Object[] path : paths) {
            if(path.length == 1) continue;
            String str = "";
            String actionId = "";
            for (Object node : path) {
                String text = node.toString();
                if (node instanceof String) {
                    actionId = (String) node;
                    AnAction action = actionManager.getAction((String) node);
                    if (action != null)
                        text = action.getTemplatePresentation().getText();
                    text = StardustUtil.replaceActonName(text);
                }
                str += text + " | ";
            }
            map.put(actionId, str.substring(0, str.length() - 3));
        }
        return map;
    }

    private static void createPaths(Object node, List<Object> path, List<Object[]> paths) {
        if (node == null) {
            paths.add(path.toArray());
            return;
        }
        path.add(node);

        if (node instanceof String) {
            createPaths(null, path, paths);
        }
        if (node instanceof Group) {
            for (Object child : ((Group) node).getChildren()) {
                createPaths(child, path, paths);
            }
        }
        path.remove(node);
    }


}
