package app.takeyourmeds

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import app.takeyourmeds.work.BootReceiver
import app.takeyourmeds.work.ExactAlarmGrantReceiver
import app.takeyourmeds.work.PackageReplacedReceiver
import app.takeyourmeds.work.ReminderActionReceiver
import app.takeyourmeds.work.ReminderAlarmReceiver
import app.takeyourmeds.work.TimeChangeReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What Android reads off the installed manifest. Nothing in Kotlin references these receivers — the
 * OS finds them by their intent filters — so a renamed class or a dropped filter compiles cleanly and
 * leaves the app deaf to the broadcast. Add a case here for every component the OS starts on its own.
 */
@RunWith(AndroidJUnit4::class)
class ManifestWiringTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun the_boot_receiver_is_private_and_hears_boot() {
        assertPrivateReceiverHears(BootReceiver::class.java, Intent.ACTION_BOOT_COMPLETED)
    }

    @Test
    fun the_update_receiver_is_private_and_hears_the_update() {
        assertPrivateReceiverHears(PackageReplacedReceiver::class.java, Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    @Test
    fun the_time_receiver_is_private_and_hears_clock_and_zone_changes() {
        assertPrivateReceiverHears(TimeChangeReceiver::class.java, Intent.ACTION_TIME_CHANGED)
        assertPrivateReceiverHears(TimeChangeReceiver::class.java, Intent.ACTION_TIMEZONE_CHANGED)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun the_grant_receiver_is_private_and_hears_the_exact_alarm_grant() {
        assertPrivateReceiverHears(
            ExactAlarmGrantReceiver::class.java,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }

    @Test
    fun the_alarm_and_action_receivers_are_private() {
        for (receiver in listOf(ReminderAlarmReceiver::class.java, ReminderActionReceiver::class.java)) {
            assertFalse(context.packageManager.getReceiverInfo(ComponentName(context, receiver), 0).exported)
        }
    }

    @Test
    fun the_boot_permission_is_requested() {
        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in requested)
    }

    private fun assertPrivateReceiverHears(
        receiver: Class<*>,
        action: String,
    ) {
        assertFalse(context.packageManager.getReceiverInfo(ComponentName(context, receiver), 0).exported)
        val names =
            context.packageManager
                .queryBroadcastReceivers(Intent(action).setPackage(context.packageName), 0)
                .map { it.activityInfo.name }
        assertTrue("$action reaches $names", receiver.name in names)
    }
}
