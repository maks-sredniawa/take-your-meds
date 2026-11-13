package app.takeyourmeds.ui.wipe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.takeyourmeds.R
import app.takeyourmeds.SchemaMismatch
import app.takeyourmeds.data.APP_SCHEMA_VERSION
import app.takeyourmeds.data.UNREADABLE_USER_VERSION
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SchemaMismatchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val copy = File("db-20261113T080000Z.db")

    @Test
    fun anUnreadableFileIsNotDescribedAsAVersion() {
        show(fromVersion = UNREADABLE_USER_VERSION)

        compose.onNodeWithText(string(R.string.schema_mismatch_unreadable, copy.name)).assertIsDisplayed()
        compose
            .onNodeWithText(string(R.string.schema_mismatch_body, UNREADABLE_USER_VERSION, APP_SCHEMA_VERSION))
            .assertDoesNotExist()
    }

    @Test
    fun aVersionThisBuildCannotOpenIsNamedWithItsCopy() {
        show(fromVersion = APP_SCHEMA_VERSION + 1)

        compose
            .onNodeWithText(string(R.string.schema_mismatch_body, APP_SCHEMA_VERSION + 1, APP_SCHEMA_VERSION))
            .assertIsDisplayed()
        compose.onNodeWithText(string(R.string.schema_mismatch_copy, copy.name)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.schema_mismatch_unreadable, copy.name)).assertDoesNotExist()
    }

    private fun show(fromVersion: Int) {
        val mismatch =
            SchemaMismatch(
                preservedCopy = copy,
                fromVersion = fromVersion,
                toVersion = APP_SCHEMA_VERSION,
                wipeOnConsent = false,
            )
        compose.setContent { SchemaMismatchScreen(mismatch, onContinue = {}) }
    }

    private fun string(
        id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
}
