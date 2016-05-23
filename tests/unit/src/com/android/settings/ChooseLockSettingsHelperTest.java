package com.android.settings;


import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;
import android.test.AndroidTestCase;

import com.android.internal.widget.LockPatternUtils;

import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChooseLockSettingsHelperTest extends AndroidTestCase {

    private static final String SYSTEM_PROPERTY_DEXMAKER_DEXCACHE = "dexmaker.dexcache";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty(SYSTEM_PROPERTY_DEXMAKER_DEXCACHE, getContext().getCacheDir().getPath());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.clearProperty(SYSTEM_PROPERTY_DEXMAKER_DEXCACHE);
    }

    public void testlaunchConfirmationActivityWithExternalAndChallenge() {

        final int userId = UserHandle.myUserId();
        final int request = 100;
        final long challenge = 10000L;
        {
            // Test external == true
            final boolean external = true;

            final Activity mockActivity = getMockActivity();
            ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(mockActivity);
            helper.launchConfirmationActivityWithExternalAndChallenge(
                    request, // request
                    "title",
                    "header",
                    "description",
                    external,
                    challenge,
                    userId
            );
            final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
            verify(mockActivity, times(1)).startActivity(intentCaptor.capture());
            Intent capturedIntent = getResultIntent(intentCaptor);

            assertEquals(new ComponentName("com.android.settings",
                            ConfirmLockPattern.InternalActivity.class.getName()),
                    capturedIntent.getComponent());
            assertFalse(capturedIntent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, false));
            assertTrue(capturedIntent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false));
            assertEquals(challenge, capturedIntent.getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0L));
            assertEquals(external,
                    (capturedIntent.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.ALLOW_FP_AUTHENTICATION, false));
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.DARK_THEME, false));
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false));
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false));
        }

        {
            // Test external == false
            final boolean external = false;

            final Activity mockActivity = getMockActivity();
            ChooseLockSettingsHelper helper = getChooseLockSettingsHelper(mockActivity);
            helper.launchConfirmationActivityWithExternalAndChallenge(
                    request, // request
                    "title",
                    "header",
                    "description",
                    external,
                    challenge,
                    userId
            );
            final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
            verify(mockActivity, times(1)).startActivityForResult(intentCaptor.capture(),
                    eq(request));
            Intent capturedIntent = getResultIntent(intentCaptor);


            assertEquals(new ComponentName("com.android.settings",
                            ConfirmLockPattern.InternalActivity.class.getName()),
                    capturedIntent.getComponent());
            assertFalse(capturedIntent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, false));
            assertTrue(capturedIntent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false));
            assertEquals(challenge, capturedIntent.getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0L));
            assertEquals(external,
                    (capturedIntent.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.ALLOW_FP_AUTHENTICATION, false));
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.DARK_THEME, false));
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false));
            assertEquals(external, capturedIntent.getBooleanExtra(
                    ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false));
        }
    }


    private ChooseLockSettingsHelper getChooseLockSettingsHelper(Activity mockActivity) {
        LockPatternUtils mockLockPatternUtils = mock(LockPatternUtils.class);
        when(mockLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);

        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(mockActivity);
        helper.mLockPatternUtils = mockLockPatternUtils;
        return helper;
    }

    private Activity getMockActivity() {
        Activity mockActivity = mock(Activity.class);
        when(mockActivity.getSystemService(Context.USER_SERVICE))
                .thenReturn(getContext().getSystemService(UserManager.class));
        when(mockActivity.getContentResolver()).thenReturn(getContext().getContentResolver());
        when(mockActivity.getIntent()).thenReturn(new Intent());
        return mockActivity;
    }



    private static Intent getResultIntent(ArgumentCaptor<Intent> intentCaptor) {
        List<Intent> capturedIntents = intentCaptor.getAllValues();
        assertEquals(1, capturedIntents.size());
        return capturedIntents.get(0);
    }
}
