import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * Created by Dmitry.Matveev on 27-Jan-17.
 */
class StardustXmlUtil {

    static Element createChunk(String includeId, Document doc){
        Element chunk = doc.createElement("chunk");
        chunk.setAttribute("include-id", includeId);
        return chunk;
    }

    static Element createInclude(String src, String includeId, Document doc, Boolean nullable){
        Element include = doc.createElement("include");
        include.setAttribute("include-id", includeId);
        include.setAttribute("src", src);
        if (nullable)
            include.setAttribute("nullable", "true");
        return include;
    }

    static Element createLink(String topicId, String text, String anchor, Document doc, boolean nullable){
        Element link = doc.createElement("a");
        /*if (origin != null && !origin.isEmpty())
            link.setAttribute("origin", origin);*/
        if (anchor != null && !anchor.isEmpty())
            link.setAttribute("anchor", anchor);
        link.setAttribute("href", topicId + ".xml");
        if (nullable)
            link.setAttribute("nullable", "true");
        if(text != null && !text.isEmpty())
            link.appendChild(doc.createTextNode(text));
        return link;
    }

    static Document createTopic(String topicId, String topicTitle){
        Document doc = createXmlDocument();
        Comment comment = doc.createComment("This topic was generated automatically with the Documentation Generator plugin.");
        Element rootElement = doc.createElement("topic");
        rootElement.setAttribute("xsi:noNamespaceSchemaLocation",
                "http://helpserver.labs.intellij.net/help/topic.v2.xsd");
        rootElement.setAttribute("xmlns:xsi",
                "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute("id", topicId);
        rootElement.setAttribute("title", topicTitle);
        rootElement.appendChild(comment);
        doc.appendChild(rootElement);
        return doc;
    }

    static Document createXmlDocument(){
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return docBuilder.newDocument();
    }

    static void saveXmlDocumentToFile(Document doc, String path){
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(path));
            transformer.transform(source, result);
            System.out.println("XML file generated at " + path);
        } catch (javax.xml.transform.TransformerException e) {
            e.printStackTrace();
        }
    }

    static void saveTopicToFile(Document topic, String subfolder){
        String mainPath = StardustUtil.getRiderDocPath() + "\\topics\\Generated\\";
        if (subfolder != null)
            mainPath = mainPath + subfolder + "\\";
        saveXmlDocumentToFile(topic, mainPath + topic.getDocumentElement().getAttribute("id") + ".xml");
    }

}


