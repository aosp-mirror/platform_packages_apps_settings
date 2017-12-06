package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.setupwizardlib.util.WizardManagerHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowUserManager.class,
                ShadowUtils.class
        })
public class ChooseLockSettingsHelperTest {

    @Test
    public void testLaunchConfirmationActivityWithExternalAndChallenge() {

        final int userId = UserHandle.myUserId();
        final int request = 100;
        final long challenge = 10000L;

        final Activity activity = Robolectric.setupActivity(Activity.class);
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(activity);
        helper.launchConfirmationActivityWithExternalAndChallenge(
                request, // request
                "title",
                "header",
                "description",
                true, // external
                challenge,
                userId
        );

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertEquals(new ComponentName("com.android.settings",
                        ConfirmLockPattern.InternalActivity.class.getName()),
                startedIntent.getComponent());
        assertFalse(startedIntent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, false));
        assertTrue(startedIntent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false));
        assertEquals(challenge, startedIntent.getLongExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0L));
        assertEquals(
                true,
                (startedIntent.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
        assertEquals(true, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.ALLOW_FP_AUTHENTICATION, false));
        assertEquals(true, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.DARK_THEME, false));
        assertEquals(true, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false));
        assertEquals(true, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false));
    }

    @Test
    public void testLaunchConfirmationActivityInternalAndChallenge() {

        final int userId = UserHandle.myUserId();
        final int request = 100;
        final long challenge = 10000L;

        final Activity activity = Robolectric.setupActivity(Activity.class);
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(activity);
        helper.launchConfirmationActivityWithExternalAndChallenge(
                request,
                "title",
                "header",
                "description",
                false, // external
                challenge,
                userId
        );
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertEquals(new ComponentName("com.android.settings",
                        ConfirmLockPattern.InternalActivity.class.getName()),
                startedIntent.getComponent());
        assertFalse(startedIntent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, false));
        assertTrue(startedIntent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false));
        assertEquals(challenge, startedIntent.getLongExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0L));
        assertEquals(
                false,
                (startedIntent.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
        assertEquals(false, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.ALLOW_FP_AUTHENTICATION, false));
        assertEquals(false, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.DARK_THEME, false));
        assertEquals(false, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false));
        assertEquals(false, startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false));
    }

    @Test
    public void testLaunchConfirmationActivity_internal_shouldPropagateTheme() {
        Intent intent = new Intent()
                .putExtra(WizardManagerHelper.EXTRA_THEME, WizardManagerHelper.THEME_GLIF_V2);
        Activity activity = Robolectric.buildActivity(Activity.class)
                .withIntent(intent)
                .get();
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(activity);
        helper.launchConfirmationActivity(123, "test title", true, 0 /* userId */);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        IntentForResult startedActivity = shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity.requestCode).isEqualTo(123);
        assertThat(startedActivity.intent.getStringExtra(WizardManagerHelper.EXTRA_THEME))
                .isEqualTo(WizardManagerHelper.THEME_GLIF_V2);
    }


    private ChooseLockSettingsHelper getChooseLockSettingsHelper(Activity activity) {
        LockPatternUtils mockLockPatternUtils = mock(LockPatternUtils.class);
        when(mockLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);

        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(activity);
        helper.mLockPatternUtils = mockLockPatternUtils;
        return helper;
    }
}
