package app.takeyourmeds.data

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.DataInputStream
import java.io.File
import java.nio.ByteBuffer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Where preserved copies land, relative to `filesDir`. */
const val PRESERVED_DIRECTORY = "preserved"

/** How a preserved copy is named. Settings matches on these to find them again. */
internal const val PRESERVED_PREFIX = "db-"
internal const val PRESERVED_SUFFIX = ".db"

/**
 * In WAL mode the most recent writes may live only in a sidecar, so a copy of the `.db` alone can be
 * missing the very data worth preserving. Both travel with it, and both have to travel again when
 * the owner shares the copy off the phone.
 */
internal val PRESERVED_SIDECAR_SUFFIXES = listOf("-wal", "-shm")

/**
 * What [readUserVersion] answers for a file that is not a readable database. Non-zero, so the gate
 * copies the file aside rather than handing it to Room, which deletes a database it finds corrupt.
 */
const val UNREADABLE_USER_VERSION = -1

private const val SQLITE_HEADER_BYTES = 100
private const val USER_VERSION_OFFSET = 60
private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

/**
 * Filesystem-safe and sorts chronologically. Colons in a filename are a portability trap.
 *
 * `internal` rather than private because Settings reads the date back *out* of the filename when it
 * lists the copies — the name dates the data, where the copy's own `lastModified` only dates the
 * moment it was written.
 */
internal val PRESERVED_TIMESTAMP_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

/**
 * Reads the schema version of [databaseFile] as SQLite sees it. Under WAL a new version can sit in
 * `-wal` while the file's own header still says 0, so a non-empty log is read through SQLite, and
 * only a database with no log is read straight from its header.
 *
 * Returns 0 for a missing or empty file, which is how a fresh install looks, and
 * [UNREADABLE_USER_VERSION] for anything that is not a readable database. Never checkpoints or
 * writes the database or its log, so Room opens what was read. The one file it can create is a
 * missing `-shm` beside a `-wal`, an index SQLite rebuilds from the log.
 */
fun readUserVersion(databaseFile: File): Int {
    if (!databaseFile.isFile) return 0
    return if (File(databaseFile.path + "-wal").length() > 0) {
        readUserVersionThroughSqlite(databaseFile)
    } else {
        readUserVersionFromHeader(databaseFile)
    }
}

/**
 * Read-only, so closing it does not checkpoint. Without a `-wal` on disk SQLite would create one and
 * a `-shm` beside the database, which is why [readUserVersion] only comes here when the log exists.
 * The no-op error handler matters: the platform's default deletes a file it finds corrupt.
 */
private fun readUserVersionThroughSqlite(databaseFile: File): Int =
    try {
        SQLiteDatabase
            .openDatabase(
                databaseFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
                DatabaseErrorHandler {},
            ).use { it.version }
    } catch (e: SQLiteException) {
        UNREADABLE_USER_VERSION
    }

private fun readUserVersionFromHeader(databaseFile: File): Int {
    val length = databaseFile.length()
    if (length == 0L) return 0
    if (length < SQLITE_HEADER_BYTES) return UNREADABLE_USER_VERSION
    val header = ByteArray(SQLITE_HEADER_BYTES)
    DataInputStream(databaseFile.inputStream()).use { it.readFully(header) }
    if (!header.copyOf(SQLITE_MAGIC.size).contentEquals(SQLITE_MAGIC)) return UNREADABLE_USER_VERSION
    return ByteBuffer.wrap(header, USER_VERSION_OFFSET, Int.SIZE_BYTES).int
}

/**
 * Whether the file on disk is at a version this build cannot simply open — the one rule behind both
 * things that have to know it, stated once.
 *
 * `0` is "there is nothing here": no file, or an empty one, which is exactly how a fresh install
 * looks. A **newer** on-disk version counts as pending too, because Room destroys a downgrade just as
 * thoroughly as it destroys an upgrade, and so is [UNREADABLE_USER_VERSION].
 *
 * Two callers, arrived at from opposite directions. [preserveBeforeWipe] asks it to decide whether
 * there is anything worth copying aside. The daily sweep asks it because it runs with no UI and no user
 * present, and **any worker that touches a repository forces the container**. A sweep that ran
 * anyway would destroy a database in the background, on a phone nobody is looking at.
 */
fun schemaMismatchPending(
    onDiskVersion: Int,
    appSchemaVersion: Int = APP_SCHEMA_VERSION,
): Boolean = onDiskVersion != 0 && onDiskVersion != appSchemaVersion

/**
 * Copies the database aside if opening it with this build would destroy it, and returns the copy.
 * Returns null when there is nothing to preserve — no file yet, or a file already at this version.
 *
 * ADR-0001: a destructive wipe never loses the file. Its **consent** half — the blocking screen in
 * front of this — is wired up by `MainApplication`, which is where this runs: before Room exists,
 * let alone opens anything.
 *
 * The copy is named from the database file's own [File.lastModified] rather than the moment of
 * panic, so a hesitating user who relaunches repeatedly **overwrites one copy instead of minting a
 * new one each time** — nothing has written to the file in between, so its modification time has not
 * moved. The name therefore dates the *data*, not the launch. [timestamp] stays a parameter only so
 * tests can pin it.
 *
 * The preserved file is a **recovery artifact, not a restore**: reading old data into a new schema
 * *is* a migration, so it cannot be re-imported automatically.
 */
fun preserveBeforeWipe(
    databaseFile: File,
    preservedDir: File,
    appSchemaVersion: Int = APP_SCHEMA_VERSION,
    timestamp: Instant = Instant.ofEpochMilli(databaseFile.lastModified()),
): File? {
    if (!schemaMismatchPending(readUserVersion(databaseFile), appSchemaVersion)) return null

    preservedDir.mkdirs()
    val preserved =
        File(preservedDir, "$PRESERVED_PREFIX${PRESERVED_TIMESTAMP_FORMAT.format(timestamp)}$PRESERVED_SUFFIX")
    databaseFile.copyTo(preserved, overwrite = true)

    for (suffix in PRESERVED_SIDECAR_SUFFIXES) {
        val sidecar = File(databaseFile.path + suffix)
        val preservedSidecar = File(preserved.path + suffix)
        if (sidecar.isFile) sidecar.copyTo(preservedSidecar, overwrite = true) else preservedSidecar.delete()
    }
    return preserved
}
