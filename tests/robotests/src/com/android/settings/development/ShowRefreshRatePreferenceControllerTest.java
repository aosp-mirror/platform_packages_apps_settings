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

import static com.android.settings.development.ShowRefreshRatePreferenceController
        .SURFACE_FLINGER_CODE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowParcel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ShowRefreshRatePreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private IBinder mSurfaceFlinger;

    private ShowRefreshRatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = spy(new ShowRefreshRatePreferenceController(mContext));
        ReflectionHelpers.setField(mController, "mSurfaceFlinger", mSurfaceFlinger);
        doNothing().when(mController).writeShowRefreshRateSetting(anyBoolean());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_settingToggledOn_shouldWriteTrueToShowRefreshRateSetting() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        verify(mController).writeShowRefreshRateSetting(true);
    }

    @Test
    public void onPreferenceChange_settingToggledOff_shouldWriteFalseToShowRefreshRateSetting() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        verify(mController).writeShowRefreshRateSetting(false);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void updateState_settingEnabled_shouldCheckPreference() throws RemoteException {
        ShadowParcel.sReadBoolResult = true;
        doReturn(true).when(mSurfaceFlinger)
            .transact(eq(SURFACE_FLINGER_CODE), any(), any(), eq(0 /* flags */));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    @Config(shadows = {ShadowParcel.class})
    public void updateState_settingDisabled_shouldUnCheckPreference() throws RemoteException {
        ShadowParcel.sReadBoolResult = false;
        doReturn(true).when(mSurfaceFlinger)
            .transact(eq(SURFACE_FLINGER_CODE), any(), any(), eq(0 /* flags */));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceUnchecked_shouldNotTurnOffPreference() {
        when(mPreference.isChecked()).thenReturn(false);
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mController, never()).writeShowRefreshRateSetting(anyBoolean());
        verify(mPreference, never()).setChecked(anyBoolean());
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceChecked_shouldTurnOffPreference() {
        when(mPreference.isChecked()).thenReturn(true);
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mController).writeShowRefreshRateSetting(false);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }
}
