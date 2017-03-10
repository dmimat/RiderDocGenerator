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

    static Document createTopic(String topicId, String topicTitle){
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("topic");
        rootElement.setAttribute("xsi:noNamespaceSchemaLocation",
                "http://helpserver.labs.intellij.net/help/topic.v2.xsd");
        rootElement.setAttribute("xmlns:xsi",
                "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute("id", topicId);
        rootElement.setAttribute("title", topicTitle);
        doc.appendChild(rootElement);
        return doc;
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
            System.out.println("File saved!");
        } catch (javax.xml.transform.TransformerException e) {
            e.printStackTrace();
        }
    }

}


