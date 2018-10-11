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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

@RunWith(SettingsRobolectricTestRunner.class)
public class DefaultAppShortcutPreferenceControllerBaseTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Preference mPreference;

    private Activity mActivity;
    private TestPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(Robolectric.setupActivity(Activity.class));
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new TestPreferenceController(mActivity, mFragment);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
    }

    @Test
    public void getAvailabilityStatus_managedProfile_shouldReturnDisabled() {
        when(mUserManager.isManagedProfile()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_hasAppCapability_shouldReturnAvailable() {
        mController.capable = true;
        when(mUserManager.isManagedProfile()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noAppCapability_shouldReturnDisabled() {
        mController.capable = false;
        when(mUserManager.isManagedProfile()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                mController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_isDefaultApp_shouldSetSummaryToYes() {
        mController.isDefault = true;

        mController.updateState(mPreference);
        String yesString = mActivity.getString(R.string.yes);
        verify(mPreference).setSummary(yesString);
    }

    @Test
    public void updateState_notDefaultApp_shouldSetSummaryToNo() {
        mController.isDefault = false;

        mController.updateState(mPreference);

        String noString = mActivity.getString(R.string.no);
        verify(mPreference).setSummary(noString);
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartDefaultAppSettings() {
        mController.handlePreferenceTreeClick(mPreference);

        verify(mActivity).startActivity(argThat(intent -> intent != null
                && intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT).equals(
                DefaultAppSettings.class.getName())
                && intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY).equals("TestKey")));
    }

    private class TestPreferenceController extends DefaultAppShortcutPreferenceControllerBase {

        private boolean isDefault;
        private boolean capable;

        private TestPreferenceController(Context context, AppInfoDashboardFragment parent) {
            super(context, "TestKey", "TestPackage");
        }

        @Override
        protected boolean hasAppCapability() {
            return capable;
        }

        @Override
        protected boolean isDefaultApp() {
            return isDefault;
        }
    }
}
