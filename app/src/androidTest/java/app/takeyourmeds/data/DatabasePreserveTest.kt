package app.takeyourmeds.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DatabasePreserveTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun aVersionStillInTheWalIsRead() {
        val database = temp.uncheckpointedDatabase(version = 7)
        assertEquals("fixture: the header must not hold the version yet", 0, headerUserVersion(database))

        assertEquals(7, readUserVersion(database))
    }

    @Test
    fun readingAnUncheckpointedDatabaseChangesNeitherTheFileNorItsLog() {
        val database = temp.uncheckpointedDatabase(version = 7)
        val before = contents(database)
        val filesBefore = siblings(database)

        readUserVersion(database)

        assertEquals(filesBefore, siblings(database))
        assertEquals(before, contents(database))
    }

    @Test
    fun readingACheckpointedDatabaseCreatesNoSidecar() {
        val database = temp.checkpointedDatabase(version = 7)
        assertEquals("fixture: closing checkpoints", setOf(APP_DATABASE_FILE), siblings(database))

        assertEquals(7, readUserVersion(database))
        assertEquals(setOf(APP_DATABASE_FILE), siblings(database))
    }

    @Test
    fun aLogWithoutItsIndexIsStillRead() {
        val database = temp.uncheckpointedDatabase(version = 7)
        File(database.path + "-shm").delete()

        assertEquals(7, readUserVersion(database))
    }

    @Test
    fun aMissingFileIsNothingThereAndStaysMissing() {
        val database = File(temp.newFolder(), APP_DATABASE_FILE)

        assertEquals(0, readUserVersion(database))
        assertEquals(emptySet<String>(), siblings(database))
    }

    @Test
    fun anEmptyFileIsNothingThere() {
        assertEquals(0, readUserVersion(temp.newFile(APP_DATABASE_FILE)))
    }

    @Test
    fun aFileSqliteCannotOpenIsKeptAndPreserved() {
        val database = temp.newFile(APP_DATABASE_FILE)
        val garbage = ByteArray(8192) { it.toByte() }
        database.writeBytes(garbage)

        assertEquals(UNREADABLE_USER_VERSION, readUserVersion(database))
        assertArrayEquals(garbage, database.readBytes())

        val preserved = preserveBeforeWipe(database, temp.newFolder("preserved"), appSchemaVersion = 2)
        assertNotNull(preserved)
        assertArrayEquals(garbage, preserved!!.readBytes())
    }

    @Test
    fun aFileSqliteCannotOpenIsKeptWhenALogSitsBesideIt() {
        val database = temp.newFile(APP_DATABASE_FILE)
        database.writeBytes(ByteArray(8192) { it.toByte() })
        File(database.path + "-wal").writeBytes(ByteArray(4096) { 7 })
        val before = contents(database)

        assertEquals(UNREADABLE_USER_VERSION, readUserVersion(database))
        assertEquals(before, contents(database))
    }

    @Test
    fun anUncheckpointedOlderDatabaseIsPreservedWithItsRows() {
        val database = temp.uncheckpointedDatabase(version = 1)

        val preserved = preserveBeforeWipe(database, temp.newFolder("preserved"), appSchemaVersion = 2)

        assertNotNull(preserved)
        assertEquals(listOf("Metformin"), medicineNames(preserved!!))
    }

    @Test
    fun aRepeatedCopyLeavesNoLogFromTheLastOneBesideIt() {
        val preservedDir = temp.newFolder("preserved")
        val sameName = Instant.parse("2026-11-13T10:00:00Z")
        preserveBeforeWipe(temp.uncheckpointedDatabase(version = 1), preservedDir, 2, sameName)

        val preserved =
            preserveBeforeWipe(
                temp.checkpointedDatabase(version = 1, medicine = "Vitamin D"),
                preservedDir,
                2,
                sameName,
            )

        assertFalse(File(preserved!!.path + "-wal").exists())
        assertEquals(listOf("Vitamin D"), medicineNames(preserved))
    }

    private fun siblings(database: File): Set<String> = database.parentFile!!.list()!!.toSet()

    private fun contents(database: File): Map<String, List<Byte>> =
        listOf("", "-wal").associateWith { File(database.path + it).readBytes().toList() }
}
