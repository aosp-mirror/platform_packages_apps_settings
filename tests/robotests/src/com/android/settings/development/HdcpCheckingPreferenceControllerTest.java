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

package com.android.settings.development;

import static com.android.settings.development.HdcpCheckingPreferenceController
        .HDCP_CHECKING_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HdcpCheckingPreferenceControllerTest {

    private static final String USER_DEBUG = "userdebug";

    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private HdcpCheckingPreferenceController mController;

    /**
     * Array Values Key
     *
     * 0: Never Check
     * 1: Check for DRM content only
     * 2: Always Check
     */
    private String[] mValues;
    private String[] mSummaries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mValues = mContext.getResources().getStringArray(R.array.hdcp_checking_values);
        mSummaries = mContext.getResources().getStringArray(R.array.hdcp_checking_summaries);
        mController = new HdcpCheckingPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_isUserBuildType_shouldReturnFalse() {
        mController = spy(mController);
        doReturn(HdcpCheckingPreferenceController.USER_BUILD_TYPE).when(mController).getBuildType();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_isUserDebugBuildType_shouldReturnTrue() {
        mController = spy(mController);
        doReturn(USER_DEBUG).when(mController).getBuildType();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onPreferenceChange_setNeverCheckHdcp_shouldEnableNeverCheckHdcp() {
        mController.onPreferenceChange(mPreference, mValues[0]);

        assertThat(SystemProperties.get(HDCP_CHECKING_PROPERTY)).isEqualTo(mValues[0]);
    }

    @Test
    public void onPreferenceChange_setCheckDrm_shouldEnableCheckDrm() {
        mController.onPreferenceChange(mPreference, mValues[1]);

        assertThat(SystemProperties.get(HDCP_CHECKING_PROPERTY)).isEqualTo(mValues[1]);
    }

    @Test
    public void updateState_neverCheckHdcp_shouldEnableNeverCheckHdcp() {
        SystemProperties.set(HDCP_CHECKING_PROPERTY, mValues[0]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mValues[0]);
        verify(mPreference).setSummary(mSummaries[0]);
    }

    @Test
    public void updateState_checkDrm_shouldEnableCheckDrm() {
        SystemProperties.set(HDCP_CHECKING_PROPERTY, mValues[1]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mValues[1]);
        verify(mPreference).setSummary(mSummaries[1]);
    }

    @Test
    public void updateState_noValueSet_shouldEnableCheckDrmAsDefault() {
        SystemProperties.set(HDCP_CHECKING_PROPERTY, null);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mValues[1]);
        verify(mPreference).setSummary(mSummaries[1]);
    }
}
