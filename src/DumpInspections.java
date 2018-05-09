import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionEP;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

public class DumpInspections extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        Map<String, Document> map = new HashMap<>();

        for (LocalInspectionEP ep : LocalInspectionEP.LOCAL_INSPECTION.getExtensions()) {
            if (ep.isInternal)
                continue;
            String languageKey = ep.groupPath;
            if (languageKey == null)
                languageKey = ep.groupDisplayName;
            if(!map.containsKey(languageKey))
            {
                map.put(languageKey,
                        StardustXmlUtil.createTopic(StardustUtil.normalizeForFileName(languageKey), languageKey));
            }
            Document topic = map.get(languageKey);
            Element inspectionEl = topic.createElement("p");
            inspectionEl.appendChild(topic.createTextNode(ep.displayName));
            topic.getDocumentElement().appendChild(inspectionEl);

            InspectionProfileEntry entry = ep.instantiateTool();

            String description = entry.getStaticDescription();

            String x = "fdasfa";

        }

        for (Document doc : map.values()) {
            StardustXmlUtil.saveTopicToFile(doc);
        }

    }

}
