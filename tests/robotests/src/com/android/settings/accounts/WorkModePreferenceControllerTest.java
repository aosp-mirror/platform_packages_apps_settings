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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.widget.Switch;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WorkModePreferenceControllerTest {

    private static final String PREF_KEY = "work_mode";
    private static final int MANAGED_USER_ID = 10;

    private Context mContext;
    private WorkModePreferenceController mController;
    private MainSwitchPreference mPreference;

    @ParameterizedRobolectricTestRunner.Parameters
    public static List<?> params() {
        return Arrays.asList(true, false);
    }
    final boolean mEnable;
    @Mock
    private UserManager mUserManager;
    @Mock
    private UserHandle mManagedUser;
    @Mock
    private UserInfo mUserInfo;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    Switch mSwitch;

    public WorkModePreferenceControllerTest(boolean enable) {
        mEnable = enable;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        mPreference = new MainSwitchPreference(mContext);
        when(mUserInfo.isManagedProfile()).thenReturn(true);
        when(mUserManager.getUserInfo(anyInt())).thenReturn(mUserInfo);
        when(mUserManager.getProcessUserId()).thenReturn(0);
        when(mUserManager.getUserProfiles()).thenReturn(Collections.singletonList(mManagedUser));
        when(mManagedUser.getIdentifier()).thenReturn(MANAGED_USER_ID);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new WorkModePreferenceController(mContext, PREF_KEY);
        mController.displayPreference(mScreen);
    }

    @Test
    public void getAvailabilityStatus_noManagedUser_DISABLED() {
        when(mUserManager.getProcessUserId()).thenReturn(MANAGED_USER_ID);
        mController = new WorkModePreferenceController(mContext, PREF_KEY);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(WorkModePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasManagedUser_AVAILABLE() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(WorkModePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_shouldRefreshContent() {
        when(mUserManager.isQuietModeEnabled(any(UserHandle.class)))
                .thenReturn(mEnable);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isEqualTo(!mEnable);
    }

    @Test
    public void onPreferenceChange_shouldRequestQuietModeEnabled() {
        when(mUserManager.isQuietModeEnabled(any(UserHandle.class))).thenReturn(mEnable);

        mController.onCheckedChanged(mSwitch, mEnable);

        verify(mUserManager).requestQuietModeEnabled(!mEnable, mManagedUser);
    }
}