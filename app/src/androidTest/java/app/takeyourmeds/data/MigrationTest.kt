package app.takeyourmeds.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Read rows back. Do not just assert that nothing threw.** `runMigrationsAndValidate` compares the
 * migrated database's *shape* against the exported JSON and is blind to a table emptied on the way.
 *
 * ⚠️ **These tests open the database directly and so walk straight past the launch gate.** Whether a
 * real user's upgrade is let through is `SchemaGateTest`, a separate claim. Verify an upgrade on a
 * phone too.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
        )

    @Test
    fun theExportedSchemaIsReadableOnTheDevice() {
        // If this fails, it is almost always the assets wiring in app/build.gradle.kts:
        // `variant.androidTest?.sources?.assets?.addStaticSourceDirectory("$projectDir/schemas")`.
        // Room's exported JSON has to be *inside the test APK*, not merely committed to the repo.
        helper.createDatabase(TEST_DB, 1).close()
    }

    @Test
    fun migrate1To2KeepsEveryMedicine() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO medicines (id, name, amount, note, createdAt) " +
                    "VALUES ('a', 'Metformin', '1 tablet', 'with food', 1000), ('b', 'Vitamin D', '2 drops', NULL, 2000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT id, name, amount, note, createdAt FROM medicines ORDER BY id").use {
            assertTrue(it.moveToNext())
            assertEquals(listOf("a", "Metformin", "1 tablet", "with food"), (0..3).map(it::getString))
            assertEquals(1000L, it.getLong(4))
            assertTrue(it.moveToNext())
            assertEquals(listOf("b", "Vitamin D", "2 drops"), (0..2).map(it::getString))
            assertTrue(it.isNull(3))
            assertEquals(2000L, it.getLong(4))
            assertFalse(it.moveToNext())
        }
        db.close()
    }

    @Test
    fun aMigratedDatabaseCascadesSchedulesAndKeepsRecords() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO medicines (id, name, amount, createdAt) VALUES ('a', 'Metformin', '1 tablet', 1)")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            "INSERT INTO schedules (id, medicineId, times, validFrom, createdAt) " +
                "VALUES ('s', 'a', '09:00', '2026-11-13T08:00', 1)",
        )
        db.execSQL(
            "INSERT INTO dose_records VALUES ('a', '2026-11-13T09:00', 'Taken', 1, 'Metformin', '1 tablet')",
        )

        db.execSQL("DELETE FROM medicines WHERE id = 'a'")

        db.query("SELECT COUNT(*) FROM schedules").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM dose_records").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
