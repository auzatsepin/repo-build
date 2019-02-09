package repo.build

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CliOptionsTestKt {

    @Test
    fun `should check all mvn switches`() {
        val cli = CliBuilderFactory.build(null)
        val options = CliOptions(cli.parse(listOf("-me",
                "-mfae",
                "-mgs", "mgs",
                "-mlr", "mlr",
                "-mo",
                "-mP", "mP1,mP2",
                "-ms", "ms",
                "-mT", "mT",
                "-mU")))

        assertTrue(options.hasMe())
        assertTrue(options.hasMfae())
        assertEquals(File("mgs"), options.mgs)
        assertEquals(File("mlr"), options.mlr)
        assertTrue(options.hasMo())
        assertEquals(listOf("mP1", "mP2"), options.mp)
        assertEquals(File("ms"), options.ms)
        assertEquals("mT", options.mt)
        assertTrue(options.hasMU())
    }

    @Test
    fun `should check all not mvn switches`() {
        val cli = CliBuilderFactory.build(null)
        val options = CliOptions(cli.parse(listOf<String>()))

        assertFalse(options.hasMe())
        assertFalse(options.hasMfae())
        assertNull(options.mgs)
        assertNull(options.mlr)
        assertFalse(options.hasMo())
        assertNull(options.mp)
        assertNull(options.ms)
        assertNull(options.mt)
        assertFalse(options.hasMU())
    }
    
}