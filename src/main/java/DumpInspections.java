import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationNamesInfo;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

public class DumpInspections extends AnAction {

    private static final String noGroupName = "Other inspections";
    private static boolean generateInspectionTopics = false;
    private static boolean hasRootTopic = false;
    private String productName;
    private String inspectionsNodeTitle = "Code Inspection Index";
    private List<String> processedInspections = new ArrayList<>();

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        Map<String, LangContainer> map = new HashMap<>();
        productName = ApplicationNamesInfo.getInstance().getProductName();
        if (productName.toLowerCase().contains("rider"))
            inspectionsNodeTitle = "Code Inspection Index (Web-Related)";
        if (productName.toLowerCase().contains("php")){
            generateInspectionTopics = true;
            hasRootTopic = true;
        }

        String inspections_tree_id = StardustUtil.normalizeForFileName(productName + "_inspections");
        Document inspectionTree = StardustXmlUtil.createXmlDocument();

        Comment comment = inspectionTree.createComment("This file was generated automatically with the Documentation Generator plugin.");
        Element rootElement = inspectionTree.createElement("product-profile");
        rootElement.setAttribute("id", inspections_tree_id);
        rootElement.setAttribute("name", productName + " inspections");
        rootElement.appendChild(comment);
        inspectionTree.appendChild(rootElement);

        Element inspectionListElement = inspectionTree.createElement("toc-element");
        inspectionListElement.setAttribute("include-id", "code_inspection_reference");
        inspectionListElement.setAttribute("hidden", "true");

        Element refPagesElement = inspectionTree.createElement("toc-element");
        if (hasRootTopic)
            refPagesElement.setAttribute("id", "ps_code_inspection_index.xml");
        else
            refPagesElement.setAttribute("toc-title", inspectionsNodeTitle);
        refPagesElement.setAttribute("include-id", "code_inspection_index");
        refPagesElement.setAttribute("sort-children", "ascending");

        List<InspectionToolWrapper> wrappers = InspectionToolRegistrar.getInstance().createTools();
        Comparator<InspectionToolWrapper> compareById = Comparator.comparing(InspectionToolWrapper::getDisplayName);
        wrappers.sort(compareById);

        for (InspectionToolWrapper wrapper : wrappers) {
            if (wrapper.loadDescription() == null) continue;
            String languageKey = wrapper.getGroupPath()[0];
            if (!map.containsKey(languageKey)) {
                map.put(languageKey, new LangContainer(languageKey));
            }
            LangContainer container = map.get(languageKey);
            container.addInspection(wrapper, inspectionTree, inspectionListElement);
        }

        Map<String, LangContainer> treeMap = new TreeMap<>(map);

        for (LangContainer container : treeMap.values()) {
            container.arrangeChapters();
            StardustXmlUtil.saveTopicToFile(container.doc, "Inspections");

            Element inspectionRefElement = inspectionTree.createElement("toc-element");
            inspectionRefElement.setAttribute("id", container.getTopicId() + ".xml");
            if (container.hasChapters)
                inspectionRefElement.setAttribute("show-structure-for", "chapter");
            refPagesElement.appendChild(inspectionRefElement);
        }

        inspectionTree.getDocumentElement().appendChild(refPagesElement);

        if (generateInspectionTopics)
            inspectionTree.getDocumentElement().appendChild(inspectionListElement);

        StardustXmlUtil.saveXmlDocumentToFile(
                inspectionTree, StardustUtil.getRiderDocPath() + "\\" + inspections_tree_id + ".tree");
    }

    private Node descriptionToXmlNode(String description) {
        org.jsoup.nodes.Document jsoupdoc = Jsoup.parse(description);
        W3CDom w3cDom = new W3CDom();
        org.w3c.dom.Document doc = w3cDom.fromJsoup(jsoupdoc);
        NodeList nodes = doc.getElementsByTagName("body");
        Node descNode = nodes.item(0);
        doc.renameNode(descNode, null, "p");
        return nodes.item(0);
    }

    @NotNull
    private String getCleanDescription(InspectionToolWrapper wrapper) {
        String description = wrapper.loadDescription().trim();
        if (!description.startsWith("<"))
            description = "<body>" + description.replaceAll("\n", "<br/>") + "</body>";
        description = description.replaceAll("<br>", "<br/>");
        description = description.replaceAll("<ul>", "<list>");
        description = description.replaceAll("<ol>", "<list type=\"decimal\">");
        description = description.replaceAll("</ul>", "</list>");
        description = description.replaceAll("</ol>", "</list>");
        description = description.replaceAll("<em>", "<control>");
        description = description.replaceAll("</em>", "</control>");
        description = description.replaceAll("<small>", "<emphasis>");
        description = description.replaceAll("</small>", "</emphasis>");
        description = description.replaceAll("<tt>", "<code>");
        description = description.replaceAll("</tt>", "</code>");
        description = description.replaceAll("<p>", "<br/><br/>");
        description = description.replaceAll("<p id=\"footer\">", "<br/><br/>");
        description = description.replaceAll("</p>", "");
        description = description.replaceAll("&(?!.{0,3};)", "&amp;");
        return description;
    }

    private void createTopicForInspection(Node content, String topicId, String name) {
        Document doc = StardustXmlUtil.createTopic(topicId, "Code Inspection: " + name);
        doc.getDocumentElement().appendChild(
                StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "inspection_header", doc, true));
        doc.adoptNode(content);
        doc.getDocumentElement().appendChild(content);
        doc.getDocumentElement().appendChild(
                StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", topicId, doc, true));
        doc.getDocumentElement().appendChild(
                StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "inspection_footer", doc, true));

        StardustXmlUtil.saveTopicToFile(doc, "Inspections\\InspectionTopics");
    }

    private class LangContainer {
        String languageKey;
        Document doc;
        Map<String, Element> categories = new HashMap<>();
        boolean hasChapters = false;

        LangContainer(String languageKey) {
            this.languageKey = languageKey;
            this.doc = StardustXmlUtil.createTopic(getTopicId(), "Code Inspections in " + languageKey);
            Element header = StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "inspections_lang_intro", doc, false);
            Element varLangElement = doc.createElement("var");
            varLangElement.setAttribute("name", "lang");
            varLangElement.setAttribute("value", languageKey);
            header.appendChild(varLangElement);
            doc.getDocumentElement().appendChild(header);
        }

        private Element initTableForCategory() {
            Element table = doc.createElement("table");
            addTableRowThreeCol(doc, table, doc.createTextNode("Inspection"), doc.createTextNode("Description"),
                    doc.createTextNode("Default Severity"));
            return table;
        }

        public void addInspection(InspectionToolWrapper wrapper, Document inspectionTree, Element rootTocElement) {
            String category = wrapper.getGroupDisplayName();
            if (category.isEmpty() || category.equals(languageKey))
                category = noGroupName;
            if (!categories.containsKey(category)) {
                categories.put(category, initTableForCategory());
            }

            Element table = categories.get(category);

            Node descriptionNode = descriptionToXmlNode(getCleanDescription(wrapper));
            doc.adoptNode(descriptionNode);
            String text = wrapper.getDisplayName();
            String inspectionTopicId = StardustUtil.normalizeForFileName(languageKey + "_" + text);
            if (processedInspections.contains(inspectionTopicId))
                return;
            else
                processedInspections.add(inspectionTopicId);
            Node inspectionTextWithLink =
                    StardustXmlUtil.createLink(inspectionTopicId, text, null, doc, true);
            addTableRowThreeCol(
                    doc, table, inspectionTextWithLink, descriptionNode, getSeverityLink(wrapper, doc));

            if (!generateInspectionTopics) return;

            try {
                createTopicForInspection(descriptionNode.cloneNode(true), inspectionTopicId, text);
                Element inspectionTocElement = inspectionTree.createElement("toc-element");
                inspectionTocElement.setAttribute("id", inspectionTopicId + ".xml");
                rootTocElement.appendChild(inspectionTocElement);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String getTopicId() {
            return "Code_Inspections_in_" + StardustUtil.normalizeForFileName(languageKey);
        }

        public void arrangeChapters() {
            if (categories.size() == 1) {
                doc.getDocumentElement().appendChild(categories.get(categories.keySet().toArray()[0]));
                return;
            }

            hasChapters = true;

            Element uncategorizedTable = categories.get(noGroupName);

            for (Map.Entry<String, Element> entry : categories.entrySet()) {
                String category = entry.getKey();
                if (category.equals(noGroupName)) continue;
                appendChapter(entry.getValue(), category);
            }

            if (uncategorizedTable != null)
                appendChapter(uncategorizedTable, noGroupName);
        }

        private void appendChapter(Element table, String category) {
            Element chapter = doc.createElement("chapter");
            chapter.setAttribute("title", category);
            chapter.setAttribute("id", StardustUtil.normalizeForFileName(category));
            chapter.appendChild(table);
            doc.getDocumentElement().appendChild(chapter);
        }
    }

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

    private static Node getSeverityLink(InspectionToolWrapper wrapper, Document doc) {
        if (wrapper == null)
            return doc.createTextNode("");
        if (!wrapper.isEnabledByDefault())
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "severity_disabled", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("ERROR"))
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "severity_error", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("WARNING"))
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "severity_warning", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("WEAK WARNING"))
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "severity_weak_warning", doc, false);
        if (wrapper.getDefaultLevel().getName().equals("INFORMATION"))
            return StardustXmlUtil.createInclude("INSPECTIONS_STATIC_CHUNKS.xml", "severity_information", doc, false);
        return doc.createTextNode("");
    }

    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }


}
