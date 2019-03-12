package repo.build

import com.google.common.io.Resources
import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.w3c.dom.Node

import java.nio.charset.Charset


/**
 */
class XmlUtilsTest extends GroovyTestCase {


    void testSetParentVersion() {
        def a = Resources.toString(Resources.getResource("testSetParentA.xml"), Charset.forName("utf-8"))
        def b = Resources.toString(Resources.getResource("testSetParentB.xml"), Charset.forName("utf-8"))

        def c = XmlUtils.modifyWithPreserveFormatting(a,
                new XmlUtilsAction(new Function1<Node, Unit>() {
                    @Override
                    Unit invoke(Node root) {
                        root.parent.version[0].value = "7.0.13-SNAPSHOT"
                        return null
                    }
                })
        )

        assertEquals(b, c)
    }
}
