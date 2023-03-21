package com.android.settings.password;

import static com.android.settings.password.TestUtils.COMPONENT_NAME;
import static com.android.settings.password.TestUtils.VALID_REMAINING_ATTEMPTS;
import static com.android.settings.password.TestUtils.createRemoteLockscreenValidationSession;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationSession;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowUtils.class})
public class ChooseLockSettingsHelperTest {

    @Test
    public void testLaunchConfirmationActivityWithExternal() {
        final Activity activity = Robolectric.setupActivity(Activity.class);

        ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(activity);
        builder.setRequestCode(100)
                .setTitle("title")
                .setHeader("header")
                .setDescription("description")
                .setExternal(true)
                .setUserId(UserHandle.myUserId());
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(builder);
        helper.launch();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertEquals(new ComponentName("com.android.settings", ConfirmLockPattern.class.getName()),
                startedIntent.getComponent());
        assertFalse(startedIntent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, false));
        assertTrue((startedIntent.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
        assertFalse(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.DARK_THEME, false));
        assertFalse(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false));
        assertTrue(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false));
    }

    @Test
    public void testLaunchConfirmationActivityInternal() {
        final Activity activity = Robolectric.setupActivity(Activity.class);

        ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(activity);
        builder.setRequestCode(100)
                .setTitle("title")
                .setHeader("header")
                .setDescription("description")
                .setForceVerifyPath(true)
                .setReturnCredentials(true)
                .setUserId(UserHandle.myUserId());
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(builder);
        helper.launch();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertEquals(new ComponentName("com.android.settings",
                        ConfirmLockPattern.InternalActivity.class.getName()),
                startedIntent.getComponent());
        assertTrue(startedIntent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, true));
        assertTrue(startedIntent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_FORCE_VERIFY, true));
        assertFalse((startedIntent.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
        assertFalse(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.DARK_THEME, false));
        assertFalse(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false));
        assertFalse(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false));
    }

    @Test
    public void testLaunchConfirmationActivity_internal_shouldPropagateTheme() {
        Intent intent = new Intent()
                .putExtra(WizardManagerHelper.EXTRA_THEME, ThemeHelper.THEME_GLIF_V2);
        Activity activity = Robolectric.buildActivity(Activity.class, intent).get();

        ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(activity);
        builder.setRequestCode(123)
                .setTitle("test title")
                .setReturnCredentials(true)
                .setUserId(0);
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(builder);
        helper.launch();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        IntentForResult startedActivity = shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity.requestCode).isEqualTo(123);
        assertThat(startedActivity.intent.getStringExtra(WizardManagerHelper.EXTRA_THEME))
                .isEqualTo(ThemeHelper.THEME_GLIF_V2);
    }

    @Test
    public void launchConfirmPattern_ForceVerify_shouldLaunchInternalActivity() {
        final Activity activity = Robolectric.setupActivity(Activity.class);

        ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(activity);
        builder.setRequestCode(100)
                .setForceVerifyPath(true);
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(builder);
        when(helper.mLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        helper.launch();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertEquals(new ComponentName("com.android.settings",
                        ConfirmLockPattern.InternalActivity.class.getName()),
                startedIntent.getComponent());
    }

    @Test
    public void launchConfirmPassword_ForceVerify_shouldLaunchInternalActivity() {
        final Activity activity = Robolectric.setupActivity(Activity.class);

        ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(activity);
        builder.setRequestCode(100)
                .setForceVerifyPath(true);
        ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(builder);
        when(helper.mLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        helper.launch();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertEquals(new ComponentName("com.android.settings",
                        ConfirmLockPassword.InternalActivity.class.getName()),
                startedIntent.getComponent());
    }

    @Test
    public void launchConfirmPassword_remoteValidation_passwordLockType() throws Exception {
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        RemoteLockscreenValidationSession request = createRemoteLockscreenValidationSession(
                KeyguardManager.PASSWORD, VALID_REMAINING_ATTEMPTS);

        ChooseLockSettingsHelper chooseLockSettingsHelper = getChooseLockSettingsHelper(
                new ChooseLockSettingsHelper.Builder(activity)
                        .setRemoteLockscreenValidation(true)
                        .setRemoteLockscreenValidationSession(request)
                        .setRemoteLockscreenValidationServiceComponent(COMPONENT_NAME));
        chooseLockSettingsHelper.launch();

        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertEquals(new ComponentName("com.android.settings",
                ConfirmLockPassword.class.getName()), startedIntent.getComponent());
        assertThat(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.IS_REMOTE_LOCKSCREEN_VALIDATION, false)
        ).isTrue();
        assertThat(startedIntent.getParcelableExtra(
                KeyguardManager.EXTRA_REMOTE_LOCKSCREEN_VALIDATION_SESSION,
                RemoteLockscreenValidationSession.class)
        ).isEqualTo(request);
        assertThat(startedIntent.getParcelableExtra(
                Intent.EXTRA_COMPONENT_NAME, ComponentName.class)
        ).isEqualTo(COMPONENT_NAME);
    }

    @Test
    public void launchConfirmPassword_remoteValidation_pinLockType() throws Exception {
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        RemoteLockscreenValidationSession request = createRemoteLockscreenValidationSession(
                KeyguardManager.PIN, VALID_REMAINING_ATTEMPTS);

        ChooseLockSettingsHelper chooseLockSettingsHelper = getChooseLockSettingsHelper(
                new ChooseLockSettingsHelper.Builder(activity)
                        .setRemoteLockscreenValidation(true)
                        .setRemoteLockscreenValidationSession(request)
                        .setRemoteLockscreenValidationServiceComponent(COMPONENT_NAME));
        chooseLockSettingsHelper.launch();

        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertEquals(new ComponentName("com.android.settings",
                ConfirmLockPassword.class.getName()), startedIntent.getComponent());
        assertThat(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.IS_REMOTE_LOCKSCREEN_VALIDATION, false)
        ).isTrue();
        assertThat(startedIntent.getParcelableExtra(
                KeyguardManager.EXTRA_REMOTE_LOCKSCREEN_VALIDATION_SESSION,
                RemoteLockscreenValidationSession.class)
        ).isEqualTo(request);
        assertThat(startedIntent.getParcelableExtra(
                Intent.EXTRA_COMPONENT_NAME, ComponentName.class)
        ).isEqualTo(COMPONENT_NAME);
    }

    @Test
    public void launchConfirmPattern_remoteValidation_patternLockType() throws Exception {
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        RemoteLockscreenValidationSession request = createRemoteLockscreenValidationSession(
                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS);

        ChooseLockSettingsHelper chooseLockSettingsHelper = getChooseLockSettingsHelper(
                new ChooseLockSettingsHelper.Builder(activity)
                        .setRemoteLockscreenValidation(true)
                        .setRemoteLockscreenValidationSession(request)
                        .setRemoteLockscreenValidationServiceComponent(COMPONENT_NAME));
        chooseLockSettingsHelper.launch();

        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertEquals(new ComponentName("com.android.settings",
                ConfirmLockPattern.class.getName()), startedIntent.getComponent());
        assertThat(startedIntent.getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.IS_REMOTE_LOCKSCREEN_VALIDATION, false)
        ).isTrue();
        assertThat(startedIntent.getParcelableExtra(
                KeyguardManager.EXTRA_REMOTE_LOCKSCREEN_VALIDATION_SESSION,
                RemoteLockscreenValidationSession.class)
        ).isEqualTo(request);
        assertThat(startedIntent.getParcelableExtra(
                Intent.EXTRA_COMPONENT_NAME, ComponentName.class)
        ).isEqualTo(COMPONENT_NAME);
    }

    private ChooseLockSettingsHelper getChooseLockSettingsHelper(
            ChooseLockSettingsHelper.Builder builder) {
        LockPatternUtils mockLockPatternUtils = mock(LockPatternUtils.class);
        when(mockLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);

        ChooseLockSettingsHelper helper = builder.build();
        helper.mLockPatternUtils = mockLockPatternUtils;
        return helper;
    }
}
