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

package com.android.settings.applications.specialaccess.pictureinpicture;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PictureInPictureDetailPreferenceControllerTest {

    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Preference mPreference;
    @Mock
    private PackageManager mManager;

    private Context mContext;
    private PictureInPictureDetailPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mController = spy(new PictureInPictureDetailPreferenceController(mContext, "test_key"));
        mController.setPackageName("Package1");
        mController.setParentFragment(mFragment);

        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
        when(mContext.getPackageManager()).thenReturn(mManager);
        when(mManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)).thenReturn(true);
    }

    @Test
    public void getAvailabilityStatus_noPictureInPictureActivities_shouldReturnDisabled() {
        doReturn(false).when(mController).hasPictureInPictureActivites();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_hasPictureInPictureActivities_shouldReturnAvailable() {
        doReturn(true).when(mController).hasPictureInPictureActivites();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noPictureInPictureFeature_shouldReturnUnSupported() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)).thenReturn(
                false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnPictureInPictureDetails() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(PictureInPictureDetails.class);
    }

    @Test
    public void updateState_shouldSetSummary() {
        final int summary = R.string.app_permission_summary_allowed;
        doReturn(summary).when(mController).getPreferenceSummary();

        mController.updateState(mPreference);

        verify(mPreference).setSummary(summary);
    }
}
