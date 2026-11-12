package app.takeyourmeds

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import app.takeyourmeds.data.APP_DATABASE_FILE
import app.takeyourmeds.data.backup.checkpointDatabaseTo
import app.takeyourmeds.data.backup.installDatabase
import java.io.File

/** Where the checkpointed copy is staged, relative to `filesDir`. */
private const val STAGING_PATH = "auto-backup-staged.db"

/**
 * Android Auto Backup, taking control of its own file set (ADR-0005): the checkpointed database and
 * nothing else. The preferences file stays out on purpose, so per-install state such as the tip ask
 * (ADR-0007) does not follow the user to a new phone.
 *
 * **Why an agent rather than `backup_rules.xml`.** Static include/exclude XML cannot checkpoint the
 * write-ahead log, and without that the platform is eligible to copy the live database and its
 * `-wal` sidecar *mid-write*: a backup that appears to work and restores corrupt. There is no way to
 * express "flush first" in the XML, so the XML has to go.
 *
 * Registered with `android:fullBackupOnly="true"`. Declaring an agent without it puts the app on the
 * key/value path instead, where [onBackup] below — a deliberate no-op — is what actually runs, and
 * the app silently backs up nothing.
 *
 * **This class holds no state and reaches for nothing.** When the system starts the process for a
 * backup it binds the base `android.app.Application`, **not** [MainApplication]: `app.container` is
 * not there to be had, and a cast to [MainApplication] would be a `ClassCastException` in exactly
 * the conditions nobody tests under. What an agent may use is [android.content.Context] paths —
 * `filesDir`, `getDatabasePath` — which a plain `Application` provides.
 *
 * It also runs **without `MainApplication.onCreate`**, so the wipe guard has not run and no database
 * is open. That is why the checkpoint below opens its own connection rather than asking Room.
 */
class AppBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        val staged = File(filesDir, STAGING_PATH)
        try {
            val databaseFile = getDatabasePath(APP_DATABASE_FILE)
            if (!databaseFile.isFile) return
            checkpointDatabaseTo(databaseFile, staged)
            fullBackupFile(staged, data)
        } finally {
            // `fullBackupFile` consumes the bytes synchronously, so nothing is waiting on it afterwards.
            staged.delete()
        }
    }

    /**
     * The staged copy arrives back under its own name and has to become the live database.
     *
     * Done here rather than by pointing the backup at the real path, because a restore that wrote
     * straight over `databases/app.db` would be doing so in a process where Room may already have
     * opened it.
     */
    override fun onRestoreFinished() {
        val staged = File(filesDir, STAGING_PATH)
        if (!staged.isFile) return
        installDatabase(staged, getDatabasePath(APP_DATABASE_FILE))
        staged.delete()
    }

    /** Key/value backup. Never called while `fullBackupOnly="true"`, and a no-op if it ever is. */
    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) = Unit
}
