package app.takeyourmeds.data.backup

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import app.takeyourmeds.data.keepCorruptDatabase
import java.io.File
import java.io.IOException

/**
 * Copy [databaseFile] to [target] with its write-ahead log folded in first.
 *
 * Room runs in WAL mode, so the most recent committed writes may live only in the `-wal` sidecar.
 * Copying the `.db` alone therefore hands over a file that is missing the very data the user asked
 * to keep — and copying the sidecars alongside it is the other half of the same trap, because a
 * `-wal` captured mid-write restores corrupt. Checkpointing collapses the log into the main file,
 * after which the `.db` is the whole database on its own and the sidecars are irrelevant.
 *
 * `TRUNCATE` rather than `PASSIVE`: a passive checkpoint gives up quietly when a reader is in the
 * way, which would leave exactly the silently-incomplete copy this exists to prevent.
 *
 * ⚠️ This is also the trap on the *reading* side: a database file pulled off a device with `adb` and
 * without its `-wal` sidecar is **stale**, so a row you just deleted still reads as present. Pull
 * both, or checkpoint first.
 *
 * Opening a second connection alongside Room's own is safe — that is what SQLite's locking is for —
 * and takes no `Context`, so the agent can call it.
 *
 * @throws IOException when the checkpoint is blocked or fails, before anything is staged (ADR-0005).
 */
fun checkpointDatabaseTo(
    databaseFile: File,
    target: File,
) {
    checkpoint(databaseFile)
    target.parentFile?.mkdirs()
    databaseFile.copyTo(target, overwrite = true)
}

private fun checkpoint(databaseFile: File) {
    val completed =
        try {
            SQLiteDatabase
                .openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE, keepCorruptDatabase)
                .use { database ->
                    // A PRAGMA that returns rows has to be run as a query; `execSQL` would refuse it.
                    database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { result ->
                        result.moveToFirst() && result.getInt(0) == 0
                    }
                }
        } catch (e: SQLiteException) {
            throw IOException("Could not checkpoint ${databaseFile.name}", e)
        }
    if (!completed) throw IOException("The checkpoint of ${databaseFile.name} was blocked")
}
