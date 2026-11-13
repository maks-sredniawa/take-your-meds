package app.takeyourmeds.data.backup

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.takeyourmeds.data.APP_DATABASE_FILE
import app.takeyourmeds.data.medicineNames
import app.takeyourmeds.data.uncheckpointedDatabase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

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
    fun aFileSqliteCannotOpenSurvivesAndNothingIsStaged() {
        val database = temp.newFile(APP_DATABASE_FILE)
        val garbage = ByteArray(8192) { it.toByte() }
        database.writeBytes(garbage)
        val staged = File(temp.newFolder(), "staged.db")

        assertThrows(IOException::class.java) { checkpointDatabaseTo(database, staged) }

        assertArrayEquals(garbage, database.readBytes())
        assertFalse(staged.exists())
    }

    @Test
    fun aBlockedCheckpointStagesNothing() {
        val database = temp.uncheckpointedDatabase(version = 2)
        val staged = File(temp.newFolder(), "staged.db")

        SQLiteDatabase
            .openDatabase(database.path, null, SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)
            .use { writer ->
                writer.beginTransactionNonExclusive()
                try {
                    assertThrows(IOException::class.java) { checkpointDatabaseTo(database, staged) }
                } finally {
                    writer.endTransaction()
                }
            }

        assertFalse(staged.exists())
    }
}
