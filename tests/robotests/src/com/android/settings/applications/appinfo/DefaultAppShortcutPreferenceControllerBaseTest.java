/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.role.RoleControllerManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowUserManager;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUserManager.class)
public class DefaultAppShortcutPreferenceControllerBaseTest {

    private static final String TEST_PREFERENCE_KEY = "TestKey";
    private static final String TEST_ROLE_NAME = "TestRole";
    private static final String TEST_PACKAGE_NAME = "TestPackage";

    @Mock
    private RoleManager mRoleManager;
    @Mock
    private RoleControllerManager mRoleControllerManager;
    @Mock
    private Preference mPreference;

    private Activity mActivity;
    private ShadowUserManager mShadowUserManager;

    private TestRolePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.ROLE_SERVICE, mRoleManager);
        shadowApplication.setSystemService(Context.ROLE_CONTROLLER_SERVICE, mRoleControllerManager);
        mActivity = Robolectric.setupActivity(Activity.class);
        mShadowUserManager = shadowOf(mActivity.getSystemService(UserManager.class));
        mController = new TestRolePreferenceController(mActivity);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void constructor_callsIsApplicationVisibleForRole() {
        verify(mRoleControllerManager).isApplicationVisibleForRole(eq(TEST_ROLE_NAME), eq(
                TEST_PACKAGE_NAME), any(Executor.class), any(Consumer.class));
    }

    @Test
    public void getAvailabilityStatus_isManagedProfile_shouldReturnDisabled() {
        mShadowUserManager.setManagedProfile(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultAppShortcutPreferenceControllerBase.DISABLED_FOR_USER);
    }

    @Test
    public void
    getAvailabilityStatus_noCallback_shouldReturnUnsupported() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultAppShortcutPreferenceControllerBase.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void
    getAvailabilityStatus_noCallbackForIsRoleNotVisible_shouldReturnUnsupported() {
        setApplicationIsVisibleForRole(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultAppShortcutPreferenceControllerBase.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_RoleIsNotVisible_shouldReturnUnsupported() {
        setRoleIsVisible(false);
        setApplicationIsVisibleForRole(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultAppShortcutPreferenceControllerBase.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void
    getAvailabilityStatus_noCallbackForIsApplicationVisibleForRole_shouldReturnUnsupported() {
        setRoleIsVisible(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultAppShortcutPreferenceControllerBase.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_applicationIsNotVisibleForRole_shouldReturnUnsupported() {
        setRoleIsVisible(true);
        setApplicationIsVisibleForRole(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultAppShortcutPreferenceControllerBase.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_RoleVisibleAndApplicationVisible_shouldReturnAvailable() {
        setRoleIsVisible(true);
        setApplicationIsVisibleForRole(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultAppShortcutPreferenceControllerBase.AVAILABLE);
    }

    private void setRoleIsVisible(boolean visible) {
        final ArgumentCaptor<Consumer<Boolean>> callbackCaptor = ArgumentCaptor.forClass(
                Consumer.class);
        verify(mRoleControllerManager).isRoleVisible(eq(TEST_ROLE_NAME), any(Executor.class),
                callbackCaptor.capture());
        final Consumer<Boolean> callback = callbackCaptor.getValue();
        callback.accept(visible);
    }

    private void setApplicationIsVisibleForRole(boolean visible) {
        final ArgumentCaptor<Consumer<Boolean>> callbackCaptor = ArgumentCaptor.forClass(
                Consumer.class);
        verify(mRoleControllerManager).isApplicationVisibleForRole(eq(TEST_ROLE_NAME), eq(
                TEST_PACKAGE_NAME), any(Executor.class), callbackCaptor.capture());
        final Consumer<Boolean> callback = callbackCaptor.getValue();
        callback.accept(visible);
    }

    @Test
    public void updateState_isRoleHolder_shouldSetSummaryToYes() {
        when(mRoleManager.getRoleHolders(eq(TEST_ROLE_NAME))).thenReturn(Collections.singletonList(
                TEST_PACKAGE_NAME));
        final CharSequence yesText = mActivity.getText(R.string.yes);
        mController.updateState(mPreference);

        verify(mPreference).setSummary(yesText);
    }

    @Test
    public void updateState_notRoleHoler_shouldSetSummaryToNo() {
        when(mRoleManager.getRoleHolders(eq(TEST_ROLE_NAME))).thenReturn(Collections.emptyList());
        final CharSequence noText = mActivity.getText(R.string.no);
        mController.updateState(mPreference);

        verify(mPreference).setSummary(noText);
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartManageDefaultAppIntent() {
        final ShadowActivity shadowActivity = shadowOf(mActivity);
        mController.handlePreferenceTreeClick(mPreference);
        final Intent intent = shadowActivity.getNextStartedActivity();

        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MANAGE_DEFAULT_APP);
        assertThat(intent.getStringExtra(Intent.EXTRA_ROLE_NAME)).isEqualTo(TEST_ROLE_NAME);
    }

    private class TestRolePreferenceController extends DefaultAppShortcutPreferenceControllerBase {

        private TestRolePreferenceController(Context context) {
            super(context, TEST_PREFERENCE_KEY, TEST_ROLE_NAME, TEST_PACKAGE_NAME);
        }
    }
}
