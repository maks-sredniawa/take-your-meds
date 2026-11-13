package app.takeyourmeds.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.takeyourmeds.data.APP_DATABASE_FILE
import app.takeyourmeds.data.medicineNames
import app.takeyourmeds.data.uncheckpointedDatabase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WalCheckpointTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun theStagedCopyHoldsRowsThatWereOnlyInTheWal() {
        val database = temp.uncheckpointedDatabase(version = 2)
        val staged = File(temp.newFolder(), "staged.db")

        checkpointDatabaseTo(database, staged)

        assertEquals(listOf("Metformin"), medicineNames(staged))
    }

    @Test
    fun aFileSqliteCannotOpenSurvivesTheBackup() {
        val database = temp.newFile(APP_DATABASE_FILE)
        val garbage = ByteArray(8192) { it.toByte() }
        database.writeBytes(garbage)
        val staged = File(temp.newFolder(), "staged.db")

        checkpointDatabaseTo(database, staged)

        assertArrayEquals(garbage, database.readBytes())
        assertArrayEquals(garbage, staged.readBytes())
    }
}
