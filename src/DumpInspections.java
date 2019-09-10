import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class DumpInspections extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        Map<String, LangContainer> map = new HashMap<>();

        String inspections_tree_id = "product_inspections";
        Document inspectionTree = StardustXmlUtil.createXmlDocument();

        Comment comment = inspectionTree.createComment("This file was generated automatically with the Documentation Generator plugin.");
        Element rootElement = inspectionTree.createElement("product-profile");
        rootElement.setAttribute("id", inspections_tree_id);
        rootElement.setAttribute("name","product inspections");
        rootElement.appendChild(comment);
        inspectionTree.appendChild(rootElement);

        Element rootTocElement = inspectionTree.createElement("toc-element");
        rootTocElement.setAttribute("include-id", "code_inspection_reference");
        rootTocElement.setAttribute("hidden", "true");

        Element refPagesElement = inspectionTree.createElement("toc-element");
        refPagesElement.setAttribute("toc-title", "Code Inspection Index");
        refPagesElement.setAttribute("include-id", "code_inspection_reference");
        refPagesElement.setAttribute("sort-children", "ascending");

        for (InspectionToolWrapper wrapper : InspectionToolRegistrar.getInstance().createTools()) {

            if (wrapper.loadDescription() == null) continue;

            String languageKey = wrapper.getGroupPath()[0];
            if (!map.containsKey(languageKey)) {
                map.put(languageKey, new LangContainer(languageKey));
            }


            String text = wrapper.getDisplayName();
            LangContainer container = map.get(languageKey);
            Document topic = container.doc;
            Node descriptionNode = topic.createTextNode("");

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

            String inspectionTopicId = StardustUtil.normalizeForFileName(languageKey + "_" + text);

            try {
                org.jsoup.nodes.Document jsoupdoc = Jsoup.parse(description);
                W3CDom w3cDom = new W3CDom();
                org.w3c.dom.Document doc = w3cDom.fromJsoup(jsoupdoc);
                NodeList nodes = doc.getElementsByTagName("body");
                descriptionNode = nodes.item(0).cloneNode(true);
                topic.adoptNode(descriptionNode);
                topic.renameNode(descriptionNode, null, "p");
                createTopicForInspection(nodes.item(0).cloneNode(true), inspectionTopicId, text);

                Element inspectionTocElement = inspectionTree.createElement("toc-element");
                inspectionTocElement.setAttribute("id", inspectionTopicId + ".xml");
                rootTocElement.appendChild(inspectionTocElement);

            } catch (Exception e) {
                e.printStackTrace();
                descriptionNode = topic.createComment(
                        "Problems in the inspection description: " + e.getLocalizedMessage());
                topic.adoptNode(descriptionNode);
            }

            Node inspectionTextWithLink =
                    StardustXmlUtil.createLink(inspectionTopicId, text, null, topic, true);
            addTableRowThreeCol(
                    topic, container.table, inspectionTextWithLink, descriptionNode, getSeverityLink(wrapper, topic));
        }



        for (LangContainer container : map.values()) {
            StardustXmlUtil.saveTopicToFile(container.doc, "Inspections");

            Element inspectionRefElement = inspectionTree.createElement("toc-element");
            inspectionRefElement.setAttribute("id", container.getTopicId() + ".xml");
            refPagesElement.appendChild(inspectionRefElement);
        }

        inspectionTree.getDocumentElement().appendChild(refPagesElement);
        inspectionTree.getDocumentElement().appendChild(rootTocElement);
        StardustXmlUtil.saveXmlDocumentToFile(
                inspectionTree,StardustUtil.getRiderDocPath() + "\\" + inspections_tree_id + ".tree");
    }

    private void createTopicForInspection(Node content, String topicId, String name){
        Document doc = StardustXmlUtil.createTopic(topicId, "Code Inspection: " + name);
        doc.getDocumentElement().appendChild(
                StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "inspection_header", doc, false));
        doc.adoptNode(content);
        doc.renameNode(content, null, "p");
        doc.getDocumentElement().appendChild(content);
        doc.getDocumentElement().appendChild(
                StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", topicId, doc, true));
        doc.getDocumentElement().appendChild(
                StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "inspection_footer", doc, false));

        StardustXmlUtil.saveTopicToFile(doc, "Inspections\\InspectionTopics");
    }


    private class LangContainer {
        String languageKey;
        Document doc;
        Element table;

        LangContainer(String languageKey) {
            this.languageKey = languageKey;
            this.doc = StardustXmlUtil.createTopic(getTopicId(),"Code Inspections in " + languageKey);
            table = doc.createElement("table");
            addTableRowThreeCol(doc, table, doc.createTextNode("Inspection"), doc.createTextNode("Description"),
                    doc.createTextNode("Default Severity"));
            doc.getDocumentElement().appendChild(table);
        }

        public String getTopicId(){
            return "Code_Inspections_in_" + StardustUtil.normalizeForFileName(languageKey);
        }
    }

//    private void addTableRowTwoCol(Document doc, Element table, String left, Node right) {
//        addTableRowTwoCol(doc, table, doc.createTextNode(left), right);
//    }

    private void addTableRowThreeCol(Document doc, Element table, Node left, Node middle, Node right) {
        Element tr = doc.createElement("tr");
        Element tdLeft = doc.createElement("td");
        tdLeft.appendChild(left);
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



    private static Node getSeverityLink(InspectionToolWrapper wrapper, Document doc) {
        if (wrapper == null)
            return doc.createTextNode("");
        if (!wrapper.isEnabledByDefault())
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml","severity_disabled", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("ERROR"))
        return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml","severity_error", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("WARNING"))
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml","severity_warning", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("WEAK WARNING"))
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml","severity_weak_warning", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("INFORMATION"))
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml","severity_information", doc, false);
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
