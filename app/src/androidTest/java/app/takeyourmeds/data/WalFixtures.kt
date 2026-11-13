package app.takeyourmeds.data

import android.database.sqlite.SQLiteDatabase
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

/**
 * A copy of a database's files taken while its writer is still open, the way a killed process leaves
 * them: the rows and the version sit in `-wal`, and the main file's header still says 0.
 */
fun TemporaryFolder.uncheckpointedDatabase(
    version: Int,
    medicine: String = "Metformin",
): File {
    val live = File(newFolder(), APP_DATABASE_FILE)
    val copy = File(newFolder(), APP_DATABASE_FILE)
    openWal(live).use { database ->
        database.addMedicine(medicine, version)
        for (suffix in listOf("", "-wal", "-shm")) File(live.path + suffix).copyTo(File(copy.path + suffix))
    }
    return copy
}

/** Closed cleanly, so everything is checkpointed into the main file and no sidecar is left. */
fun TemporaryFolder.checkpointedDatabase(
    version: Int,
    medicine: String = "Metformin",
): File {
    val database = File(newFolder(), APP_DATABASE_FILE)
    openWal(database).use { it.addMedicine(medicine, version) }
    return database
}

fun medicineNames(database: File): List<String> =
    SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY).use { opened ->
        opened.rawQuery("SELECT name FROM medicines", null).use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
        }
    }

fun headerUserVersion(database: File): Int =
    RandomAccessFile(database, "r").use { file ->
        file.seek(60)
        file.readInt()
    }

private fun openWal(file: File): SQLiteDatabase =
    SQLiteDatabase.openDatabase(
        file.path,
        null,
        SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
    )

private fun SQLiteDatabase.addMedicine(
    name: String,
    version: Int,
) {
    execSQL("CREATE TABLE medicines (name TEXT NOT NULL)")
    execSQL("INSERT INTO medicines VALUES (?)", arrayOf(name))
    this.version = version
}
