package repo.build

import com.google.common.io.Resources
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.nio.charset.Charset

class XmlUtilsTestKt {

    @Test
    fun `should set parent version`() {
        val a = Resources.toString(Resources.getResource("testSetParentA.xml"), Charset.forName("utf-8"))
        val b = Resources.toString(Resources.getResource("testSetParentB.xml"), Charset.forName("utf-8"))

        val c = XmlUtils.modifyWithPreserveFormatting(a,
                XmlUtilsAction { root ->
                    val parent = (root as Element).getElementsByTagName("parent").item(0)
                    val version = (parent as Element).getElementsByTagName("version").item(0)
                    version.textContent = "7.0.13-SNAPSHOT"
                }
        )
        assertEquals(b, c)
    }

    @Test
    fun `should set parent version kt`() {
        val a = Resources.toString(Resources.getResource("testSetParentA.xml"), Charset.forName("utf-8"))
        val b = Resources.toString(Resources.getResource("testSetParentB.xml"), Charset.forName("utf-8"))

        val c = XmlUtilsKt.modifyWithPreserveFormatting(a) { root ->
            val parent = (root as Element).getElementsByTagName("parent").item(0)
            val version = (parent as Element).getElementsByTagName("version").item(0)
            version.textContent = "7.0.13-SNAPSHOT"
        }
        assertEquals(b, c)
    }

}