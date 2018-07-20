import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class DumpInspections extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        Map<String, LangContainer> map = new HashMap<>();

        for (InspectionToolWrapper wrapper : InspectionToolRegistrar.getInstance().createTools()) {

            InspectionEP ep = wrapper.getExtension();

            if (ep.isInternal)
                continue;

            String languageKey = ep.groupPath;
            if (languageKey == null)
                languageKey = ep.groupDisplayName;
            if (!map.containsKey(languageKey)) {
                map.put(languageKey, new LangContainer(languageKey));
            }

            String text = ep.displayName;
            LangContainer container = map.get(languageKey);
            Document topic = container.doc;
            Node newNode = topic.createTextNode("");

            String description =  wrapper.loadDescription().trim();
            if (!description.startsWith("<"))
                description = "<body>" + description.replaceAll("\n", "<br/>") + "</body>";

            description = description.replaceAll("<br>", "<br/>");
            description = description.replaceAll("<ul>", "<list>");
            description = description.replaceAll("<ol>", "<list type=\"decimal\">");
            description = description.replaceAll("</ul>", "</list>");
            description = description.replaceAll("</ol>", "</list>");
            description = description.replaceAll("<em>", "<control>");
            description = description.replaceAll("</em>", "</control>");
            description = description.replaceAll("<tt>", "<code>");
            description = description.replaceAll("</tt>", "</code>");
            description = description.replaceAll("&(?!.{0,3};)", "&amp;");


            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new ByteArrayInputStream(description.getBytes("UTF-8")));

                NodeList nodes = doc.getElementsByTagName("body");
                newNode = nodes.item(0).cloneNode(true);
                topic.adoptNode(newNode);
                topic.renameNode(newNode, null, "p");
                //text = text + nodes.item(0).getTextContent();

                //text = text;
            } catch (Exception e) {
                e.printStackTrace();
            }


            addTableRowThreeCol(topic, container.table, text, newNode, getSeverityLink(ep, topic));

//            InspectionProfileEntry entry = ep.instantiateTool();
//            String description = entry.loadDescription();

            String x = "fdasfa";
        }

        for (LangContainer container : map.values()) {
            StardustXmlUtil.saveTopicToFile(container.doc, "Inspections");
        }

    }

    private class LangContainer {
        String languageKey;
        Document doc;
        Element table;

        LangContainer(String languageKey) {
            this.languageKey = languageKey;
            this.doc = StardustXmlUtil.createTopic("Code_Inspections_in_" + StardustUtil.normalizeForFileName(languageKey),
                    "Code Inspections in " + languageKey);
            table = doc.createElement("table");
            addTableRowThreeCol(doc, table, "Inspection", doc.createTextNode("Description"),
                    doc.createTextNode("Default Severity"));
            doc.getDocumentElement().appendChild(table);
        }
    }

//    private void addTableRowTwoCol(Document doc, Element table, String left, Node right) {
//        addTableRowTwoCol(doc, table, doc.createTextNode(left), right);
//    }

    private void addTableRowThreeCol(Document doc, Element table, String left, Node middle, Node right) {
        Element tr = doc.createElement("tr");
        Element tdLeft = doc.createElement("td");
        tdLeft.appendChild(doc.createTextNode(left));
        Element tdMiddle = doc.createElement("td");
        tdMiddle.appendChild(middle);
        Element tdRight = doc.createElement("td");
        tdRight.appendChild(right);
        tr.appendChild(tdLeft);
        tr.appendChild(tdMiddle);
        tr.appendChild(tdRight);
        table.appendChild(tr);
    }

//    private void addTableRowTwoCol(Document doc, Element table, Node left, Node right) {
//        Element tr = doc.createElement("tr");
//        Element tdLeft = doc.createElement("td");
//        tdLeft.appendChild(left);
//        Element tdRight = doc.createElement("td");
//        tdRight.appendChild(right);
//        tr.appendChild(tdLeft);
//        tr.appendChild(tdRight);
//        table.appendChild(tr);
//    }



    private static Node getSeverityLink(InspectionEP inspection, Document doc) {
        if (inspection == null || inspection.level == null)
            return doc.createTextNode("");
        if (!inspection.enabledByDefault)
            return StardustXmlUtil.createLink("Code_Analysis__Configuring_Warnings", "Disabled", "disable", doc);
        if (inspection.level.equals("ERROR"))
            return StardustXmlUtil.createLink("Code_Analysis__Code_Inspections", "Error", "errors", doc);
        if (inspection.level.equals("WARNING"))
            return StardustXmlUtil.createLink("Code_Analysis__Code_Inspections", "Warning", "warnings", doc);
        if (inspection.level.equals("WEAK WARNING"))
            return StardustXmlUtil.createLink("Code_Analysis__Code_Inspections", "Weak warning", "warnings", doc);
        if (inspection.level.equals("INFORMATION"))
            return StardustXmlUtil.createLink("Code_Analysis__Code_Inspections", "Information", "hints", doc);

        return doc.createTextNode("");
    }

    public static Document loadXMLFromString(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }


}
