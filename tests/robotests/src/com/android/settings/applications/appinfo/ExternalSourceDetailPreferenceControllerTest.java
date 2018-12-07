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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ExternalSourceDetailPreferenceControllerTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Preference mPreference;

    private Context mContext;
    private ExternalSourceDetailPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = spy(new ExternalSourceDetailPreferenceController(mContext, "test_key"));
        mController.setPackageName("Package1");
        mController.setParentFragment(mFragment);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
    }

    @Test
    public void getAvailabilityStatus_managedProfile_shouldReturnDisabled() {
        when(mUserManager.isManagedProfile()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_notPotentialAppSource_shouldReturnDisabled() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        doReturn(false).when(mController).isPotentialAppSource();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_isPotentialAppSource_shouldReturnAvailable() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        doReturn(true).when(mController).isPotentialAppSource();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnExternalSourcesDetails() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(ExternalSourcesDetails.class);
    }

    @Test
    public void updateState_shouldSetSummary() {
        final String summary = "test summary";
        doReturn(summary).when(mController).getPreferenceSummary();

        mController.updateState(mPreference);

        verify(mPreference).setSummary(summary);
    }

    @Test
    public void isPotentialAppSource_nullPackageInfo_shouldNotCrash() {
        when(mUserManager.isManagedProfile()).thenReturn(false);

        mController.isPotentialAppSource();
        // no crash
    }
}
