package app.takeyourmeds.data.backup

import java.io.File

/*
 * Swapping the live database for a restored one so that a kill at any point leaves the old file or
 * the new one whole (ADR-0005).
 */

private const val PART_SUFFIX = ".part"

private val DATABASE_SIDECAR_SUFFIXES = listOf("-wal", "-shm")

/**
 * Puts [replacement] in place of [databaseFile]. The sidecars go after the slow copy and before the
 * rename: a `-wal` left beside a different `app.db` is a corrupt open on the next launch.
 */
fun installDatabase(
    replacement: File,
    databaseFile: File,
) {
    databaseFile.parentFile?.mkdirs()
    val part = replacement.copyTo(File(databaseFile.path + PART_SUFFIX), overwrite = true)
    for (suffix in DATABASE_SIDECAR_SUFFIXES) File(databaseFile.path + suffix).delete()
    part.moveOver(databaseFile)
}

/** A rename within one directory; a copy, not a rename, is what was slow. */
private fun File.moveOver(target: File) {
    if (!renameTo(target)) {
        copyTo(target, overwrite = true)
        delete()
    }
}
