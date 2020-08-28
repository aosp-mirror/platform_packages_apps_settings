package com.android.settings.notification;

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Robolectric.buildActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.widget.RadioButton;

import com.android.settings.R;
import com.android.settings.RestrictedRadioButton;
import com.android.settings.notification.RedactionInterstitial.RedactionInterstitialFragment;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowUserManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUtils.class,
        ShadowRestrictedLockUtilsInternal.class,
})
public class RedactionInterstitialTest {
    private RedactionInterstitial mActivity;
    private RedactionInterstitialFragment mFragment;

    @After
    public void tearDown() {
        ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    public void primaryUserDefaultStateTest() {
        setupSettings(1 /* show */, 1 /* showUnredacted */);
        setupActivity();

        assertHideAllVisible(true);
        assertEnabledButtons(true /* all */, true /* redact */);
        assertSelectedButton(R.id.show_all);
    }

    @Test
    public void primaryUserRedactSensitiveTest() {
        setupSettings(1 /* show */, 0 /* showUnredacted */);
        setupActivity();

        assertHideAllVisible(true);
        assertEnabledButtons(true /* all */, true /* redact */);
        assertSelectedButton(R.id.redact_sensitive);
    }

    @Test
    public void primaryUserHideAllTest() {
        setupSettings(0 /* show */, 0 /* showUnredacted */);
        setupActivity();

        assertHideAllVisible(true);
        assertEnabledButtons(true /* all */, true /* redact */);
        assertSelectedButton(R.id.hide_all);
    }

    @Test
    public void primaryUserUnredactedRestrictionTest() {
        setupSettings(1 /* show */, 1 /* showUnredacted */);
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
        setupActivity();

        assertHideAllVisible(true);
        assertEnabledButtons(false /* all */, true /* redact */);
        assertSelectedButton(R.id.redact_sensitive);
    }

    @Test
    public void primaryUserNotificationRestrictionTest() {
        setupSettings(1 /* show */, 1 /* showUnredacted */);
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        setupActivity();

        assertHideAllVisible(true);
        assertEnabledButtons(false /* all */, false /* redact */);
        assertSelectedButton(R.id.hide_all);
    }

    @Test
    public void managedProfileNoRestrictionsTest() {
        setupSettings(1 /* show */, 1 /* showUnredacted */);
        final ShadowUserManager sum =
                Shadow.extract(RuntimeEnvironment.application.getSystemService(UserManager.class));
        sum.addProfile(
                UserHandle.myUserId(), UserHandle.myUserId(),
                "work-profile"/* profileName */, UserInfo.FLAG_MANAGED_PROFILE);
        setupActivity();

        assertHideAllVisible(false);
        assertEnabledButtons(true /* all */, true /* redact */);
        assertSelectedButton(R.id.show_all);
    }

    @Test
    public void managedProfileUnredactedRestrictionTest() {
        setupSettings(1 /* show */, 1 /* showUnredacted */);
        final ShadowUserManager sum =
                Shadow.extract(RuntimeEnvironment.application.getSystemService(UserManager.class));
        sum.addProfile(
                UserHandle.myUserId(), UserHandle.myUserId(),
                "work-profile"/* profileName */, UserInfo.FLAG_MANAGED_PROFILE);
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
        setupActivity();

        assertHideAllVisible(false);
        assertEnabledButtons(false /* all */, true /* redact */);
        assertSelectedButton(R.id.redact_sensitive);
    }

    private void setupActivity() {
        mActivity = buildActivity(RedactionInterstitial.class, new Intent()).setup().get();
        mFragment = (RedactionInterstitialFragment)
                mActivity.getSupportFragmentManager().findFragmentById(R.id.main_content);
        assertThat(mActivity).isNotNull();
        assertThat(mFragment).isNotNull();
    }

    private void setupSettings(int show, int showUnredacted) {
        final ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putIntForUser(resolver,
                LOCK_SCREEN_SHOW_NOTIFICATIONS, show, UserHandle.myUserId());
        Settings.Secure.putIntForUser(resolver,
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, showUnredacted, UserHandle.myUserId());
    }

    private void assertHideAllVisible(boolean visible) {
        Assert.assertEquals(visible, getButton(R.id.hide_all).getVisibility() != View.GONE);
    }

    private void assertEnabledButtons(boolean all, boolean redact) {
        Assert.assertEquals(all, buttonEnabled(R.id.show_all));
        Assert.assertEquals(redact, buttonEnabled(R.id.redact_sensitive));
    }

    private void assertSelectedButton(int resId) {
        Assert.assertEquals(resId == R.id.show_all, buttonChecked(R.id.show_all));
        Assert.assertEquals(resId == R.id.redact_sensitive, buttonChecked(R.id.redact_sensitive));
        Assert.assertEquals(resId == R.id.hide_all, buttonChecked(R.id.hide_all));
    }

    private boolean buttonChecked(int resource) {
        return getButton(resource).isChecked();
    }

    private boolean buttonEnabled(int resource) {
        return !((RestrictedRadioButton) getButton(resource)).isDisabledByAdmin();
    }

    private RadioButton getButton(int resource) {
        return (RadioButton) mFragment.getView().findViewById(resource);
    }
}
