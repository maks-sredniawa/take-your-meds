package app.takeyourmeds.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class FileSwapTest {
    @get:Rule val folder = TemporaryFolder()

    private fun liveDatabase(): File {
        val databaseFile = File(folder.newFolder("databases"), "app.db").apply { writeText("old") }
        File(databaseFile.path + "-wal").writeText("old log")
        File(databaseFile.path + "-shm").writeText("old index")
        return databaseFile
    }

    private fun replacement() = File(folder.root, "restored.db").apply { writeText("new") }

    @Test
    fun `the swap removes the old sidecars and leaves no part file`() {
        val databaseFile = liveDatabase()

        installDatabase(replacement(), databaseFile)

        assertEquals("new", databaseFile.readText())
        assertFalse(File(databaseFile.path + "-wal").exists())
        assertFalse(File(databaseFile.path + "-shm").exists())
        assertFalse(File(databaseFile.path + ".part").exists())
    }

    @Test
    fun `a swap that fails mid-copy leaves the live database and its log alone`() {
        val databaseFile = liveDatabase()
        File(databaseFile.path + ".part").mkdirs()
        File(databaseFile.path + ".part/blocker").writeText("")

        assertThrows(IOException::class.java) { installDatabase(replacement(), databaseFile) }

        assertEquals("old", databaseFile.readText())
        assertEquals("old log", File(databaseFile.path + "-wal").readText())
        assertEquals("old index", File(databaseFile.path + "-shm").readText())
    }
}
