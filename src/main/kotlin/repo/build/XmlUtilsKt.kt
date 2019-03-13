package repo.build

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class XmlUtilsAction(val action: (Node) -> Unit) {

    operator fun invoke(node: Node) {
        return action(node)
    }

}

class XmlUtilsKt {

    companion object {
        fun parse(xmlFile: File): Document {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(xmlFile)
            doc.documentElement.normalize()
            return doc
        }

        fun modifyWithPreserveFormatting(xml: String, action: (Node) -> Unit): String {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(InputSource(StringReader(xml)))
            val root = doc.documentElement
            action(root)
            val sw = StringWriter()
            val source = DOMSource(root)
            val target = StreamResult(sw)
            val factory = TransformerFactory.newInstance()
            try {
                factory.setAttribute("indent-number", 2)
            } catch (ignore: IllegalArgumentException) {}
            val transformer = factory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.METHOD, "xml")
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml")
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.transform(source, target)
            return sw.toString()
        }

    }

}