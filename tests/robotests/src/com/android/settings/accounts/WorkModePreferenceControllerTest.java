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
package com.android.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.SwitchPreference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WorkModePreferenceControllerTest {

    private static final String PREF_KEY = "work_mode";

    @Mock
    private UserManager mUserManager;
    @Mock
    private UserHandle mManagedUser;

    private Context mContext;
    private WorkModePreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mController = new WorkModePreferenceController(mContext, PREF_KEY);
        mController.setManagedUser(mManagedUser);
        mPreference = new SwitchPreference(mContext);
    }

    @Test
    public void getAvailabilityStatus_noManagedUser_DISABLED() {
        mController.setManagedUser(null);
        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(WorkModePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasManagedUser_AVAILABLE() {
        mController.setManagedUser(mManagedUser);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(WorkModePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_shouldRefreshContent() {
        when(mUserManager.isQuietModeEnabled(any(UserHandle.class)))
                .thenReturn(false);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.work_mode_on_summary));

        when(mUserManager.isQuietModeEnabled(any(UserHandle.class)))
                .thenReturn(true);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.work_mode_off_summary));
    }

    @Test
    public void onPreferenceChange_shouldRequestQuietModeEnabled() {
        mController.onPreferenceChange(mPreference, true);
        verify(mUserManager).requestQuietModeEnabled(false, mManagedUser);

        mController.onPreferenceChange(mPreference, false);
        verify(mUserManager).requestQuietModeEnabled(true, mManagedUser);
    }

    @Test
    public void onStart_shouldRegisterReceiver() {
        mController.onStart();
        verify(mContext).registerReceiver(eq(mController.mReceiver), any());
    }

    @Test
    public void onStop_shouldUnregisterReceiver() {
        // register it first
        mContext.registerReceiver(mController.mReceiver, null);

        mController.onStop();
        verify(mContext).unregisterReceiver(mController.mReceiver);
    }
}