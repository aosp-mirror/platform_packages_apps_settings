/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_FINANCED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.pm.UserInfo;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.testutils.CustomActivity;
import com.android.settings.testutils.shadow.ShadowActivity;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowProcess;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowDevicePolicyManager.class,
        ShadowUserManager.class,
        ShadowActivity.class
})
public class ActionDisabledByAdminDialogHelperTest {
    private static final ComponentName ADMIN_COMPONENT =
            new ComponentName("some.package.name", "some.package.name.SomeClass");
    private static final int USER_ID = 123;
    private static final EnforcedAdmin ENFORCED_ADMIN =
            new EnforcedAdmin(ADMIN_COMPONENT, new UserHandle(USER_ID));
    private ActionDisabledByAdminDialogHelper mHelper;
    private Activity mActivity;
    private org.robolectric.shadows.ShadowActivity mActivityShadow;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(CustomActivity.class);
        mActivityShadow = Shadow.extract(mActivity);
        mHelper = new ActionDisabledByAdminDialogHelper(mActivity);
    }

    @Ignore
    @Test
    public void testSetAdminSupportTitle() {
        final ViewGroup view = new FrameLayout(mActivity);
        final TextView textView = createAdminSupportDialogTitleTextView(view, mActivity);

        mHelper.setAdminSupportTitle(view, UserManager.DISALLOW_ADJUST_VOLUME);
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title_adjust_volume));

        mHelper.setAdminSupportTitle(view, UserManager.DISALLOW_OUTGOING_CALLS);
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title_outgoing_calls));

        mHelper.setAdminSupportTitle(view, UserManager.DISALLOW_SMS);
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title_sms));

        mHelper.setAdminSupportTitle(view, DevicePolicyManager.POLICY_DISABLE_CAMERA);
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title_camera));

        mHelper.setAdminSupportTitle(view, DevicePolicyManager.POLICY_DISABLE_SCREEN_CAPTURE);
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title_screen_capture));

        mHelper.setAdminSupportTitle(view, DevicePolicyManager.POLICY_SUSPEND_PACKAGES);
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title_suspend_packages));

        mHelper.setAdminSupportTitle(view, "another restriction");
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title));

        mHelper.setAdminSupportTitle(view, null);
        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title));
    }

    @Test
    public void testSetAdminSupportTitleForFinancedDevice() {
        final ShadowDevicePolicyManager dpmShadow = ShadowDevicePolicyManager.getShadow();
        final ViewGroup view = new FrameLayout(mActivity);
        final TextView textView = createAdminSupportDialogTitleTextView(view, mActivity);
        setupFinancedDevice(dpmShadow);
        mHelper = new ActionDisabledByAdminDialogHelper(mActivity);

        mHelper.setAdminSupportTitle(view, null);

        assertEquals(Shadows.shadowOf(textView).innerText(),
                mActivity.getString(R.string.disabled_by_policy_title_financed_device));
    }

    @Test
    public void testSetAdminSupportDetails() {
        final ShadowDevicePolicyManager dpmShadow = ShadowDevicePolicyManager.getShadow();
        final UserManager userManager = RuntimeEnvironment.application.getSystemService(
                UserManager.class);
        final ShadowUserManager userManagerShadow = Shadow.extract(userManager);
        final ViewGroup view = new FrameLayout(mActivity);
        final ComponentName component = new ComponentName("some.package.name",
                "some.package.name.SomeClass");
        final EnforcedAdmin admin = new EnforcedAdmin(component, UserHandle.of(123));
        final TextView textView = createAdminSupportTextView(view, mActivity);
        dpmShadow.setShortSupportMessageForUser(component, 123, "some message");
        dpmShadow.setIsAdminActiveAsUser(true);
        createShadowWorkProfile(userManagerShadow);

        mHelper.setAdminSupportDetails(mActivity, view, admin);

        assertNotNull(admin.component);
        assertEquals("some message", Shadows.shadowOf(textView).innerText());
    }

    @Test
    public void testSetAdminSupportDetails_shortSupportMessageIsNull() {
        final ShadowDevicePolicyManager dpmShadow = ShadowDevicePolicyManager.getShadow();
        final UserManager userManager = RuntimeEnvironment.application.getSystemService(
                UserManager.class);
        final ShadowUserManager userManagerShadow = Shadow.extract(userManager);
        final ViewGroup view = new FrameLayout(mActivity);
        final ComponentName component = new ComponentName("some.package.name",
                "some.package.name.SomeClass");
        final EnforcedAdmin admin = new EnforcedAdmin(component, UserHandle.of(123));
        final TextView textView = createAdminSupportTextView(view, mActivity);
        dpmShadow.setShortSupportMessageForUser(component, 123, null);
        dpmShadow.setIsAdminActiveAsUser(true);
        createShadowWorkProfile(userManagerShadow);

        mHelper.setAdminSupportDetails(mActivity, view, admin);

        assertNotNull(admin.component);
        assertEquals(mActivity.getString(R.string.default_admin_support_msg),
                Shadows.shadowOf(textView).innerText());
    }

    @Test
    public void testSetAdminSupportDetailsNotAdmin() {
        final ShadowDevicePolicyManager dpmShadow = ShadowDevicePolicyManager.getShadow();
        final UserManager userManager = RuntimeEnvironment.application.getSystemService(
                UserManager.class);
        final ShadowUserManager userManagerShadow = Shadow.extract(userManager);
        final ViewGroup view = new FrameLayout(mActivity);
        final ComponentName component = new ComponentName("some.package.name",
                "some.package.name.SomeClass");
        final EnforcedAdmin admin = new EnforcedAdmin(component, UserHandle.of(123));
        final TextView textView = createAdminSupportTextView(view, mActivity);
        dpmShadow.setShortSupportMessageForUser(component, 123, "some message");
        dpmShadow.setIsAdminActiveAsUser(false);
        userManagerShadow.addProfile(new UserInfo(123, null, 0));

        mHelper.setAdminSupportDetails(mActivity, textView, admin);

        assertNull(admin.component);
        assertEquals(mActivity.getString(R.string.default_admin_support_msg),
                Shadows.shadowOf(textView).innerText());
    }

    @Ignore
    @Test
    public void testMaybeSetLearnMoreButton() {
        UserManager userManager = RuntimeEnvironment.application
                .getSystemService(UserManager.class);
        ShadowUserManager userManagerShadow = Shadow.extract(userManager);
        // Set up for shadow call.
        userManagerShadow.getSameProfileGroupIds().put(USER_ID, 0);

        // Test that the button is shown when user IDs are in the same profile group
        AlertDialog.Builder builder = mock(AlertDialog.Builder.class);
        mHelper.prepareDialogBuilder(builder, /* restriction= */ null, ENFORCED_ADMIN);
        verify(builder).setNeutralButton(anyInt(), any());

        // Test that the button is not shown when user IDs are not in the same profile group
        userManagerShadow.getSameProfileGroupIds().clear();
        builder = mock(AlertDialog.Builder.class);
        mHelper.prepareDialogBuilder(builder, /* restriction= */ null, ENFORCED_ADMIN);
        verify(builder, never()).setNeutralButton(anyInt(), any());
    }

    private static TextView createAdminSupportDialogTitleTextView(final ViewGroup view,
            final Activity activity) {
        final TextView textView = new TextView(activity);
        textView.setId(R.id.admin_support_dialog_title);
        view.addView(textView);
        return textView;
    }

    private static TextView createAdminSupportTextView(final ViewGroup view,
            final Activity activity) {
        final TextView textView = new TextView(activity);
        textView.setId(R.id.admin_support_msg);
        view.addView(textView);
        return textView;
    }

    private static void createShadowWorkProfile(final ShadowUserManager userManagerShadow) {
        userManagerShadow.addProfile(new UserInfo(123, null, 0));
        userManagerShadow.addUserProfile(new UserHandle(123));
        ShadowProcess.setUid(Process.SYSTEM_UID);
    }

    private static void setupFinancedDevice(final ShadowDevicePolicyManager dpmShadow) {
        final ComponentName component = new ComponentName("some.package.name",
                "some.package.name.SomeClass");
        dpmShadow.setDeviceOwner(component);
        dpmShadow.setDeviceOwnerComponentOnAnyUser(component);
        dpmShadow.setDeviceOwnerType(component, DEVICE_OWNER_TYPE_FINANCED);
        dpmShadow.setShortSupportMessageForUser(component, 123, null);
        dpmShadow.setIsAdminActiveAsUser(true);
    }
}
