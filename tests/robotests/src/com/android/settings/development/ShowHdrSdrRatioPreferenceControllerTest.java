/**
 * Copyright (C) 2023 The Android Open Source Project
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowParcel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ShowHdrSdrRatioPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TwoStatePreference mPreference;
    @Mock
    private IBinder mSurfaceFlinger;

    private ShowHdrSdrRatioPreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ShowHdrSdrRatioPreferenceController(mContext, mSurfaceFlinger, true);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void onPreferenceChange_settingEnabled_shouldChecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        assertTrue(mController.isAvailable());
        ShadowParcel.sReadBoolResult = true;
        doReturn(true).when(mSurfaceFlinger)
            .transact(anyInt(), any(), any(), eq(0 /* flags */));
        mController.onPreferenceChange(mPreference, true /* new value */);
        verify(mPreference).setChecked(true);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void onPreferenceChange_settingDisabled_shouldUnchecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        assertTrue(mController.isAvailable());
        ShadowParcel.sReadBoolResult = false;
        doReturn(true).when(mSurfaceFlinger)
            .transact(anyInt(), any(), any(), eq(0 /* flags */));
        mController.onPreferenceChange(mPreference, false /* new value */);
        verify(mPreference).setChecked(false);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void updateState_settingEnabled_shouldChecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        assertTrue(mController.isAvailable());
        ShadowParcel.sReadBoolResult = true;
        doReturn(true).when(mSurfaceFlinger)
            .transact(anyInt(), any(), any(), eq(0 /* flags */));
        mController.updateState(mPreference);
        verify(mPreference).setChecked(true);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void updateState_settingDisabled_shouldUnchecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        assertTrue(mController.isAvailable());
        ShadowParcel.sReadBoolResult = false;
        doReturn(true).when(mSurfaceFlinger)
            .transact(anyInt(), any(), any(), eq(0 /* flags */));
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void settingNotAvailable_isHdrSdrRatioAvailableFalse_flagsOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        mController = new ShowHdrSdrRatioPreferenceController(mContext, mSurfaceFlinger, true);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void settingNotAvailable_isHdrSdrRatioAvailableTrue_flagsOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        mController = new ShowHdrSdrRatioPreferenceController(mContext, mSurfaceFlinger, false);
        assertFalse(mController.isAvailable());
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void onDeveloperOptionsSwitchDisabled_preferenceUnchecked_shouldNotTurnOffPreference()
            throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        assertTrue(mController.isAvailable());
        ShadowParcel.sReadBoolResult = false;
        doReturn(true).when(mSurfaceFlinger)
            .transact(anyInt(), any(), any(), eq(0 /* flags */));
        when(mPreference.isChecked()).thenReturn(false);
        mController.onDeveloperOptionsSwitchDisabled();

        mController.writeShowHdrSdrRatioSetting(true);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void onDeveloperOptionsSwitchDisabled_preferenceChecked_shouldTurnOffPreference()
            throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO);
        assertTrue(mController.isAvailable());
        ShadowParcel.sReadBoolResult = true;
        doReturn(true).when(mSurfaceFlinger)
            .transact(anyInt(), any(), any(), eq(0 /* flags */));
        when(mPreference.isChecked()).thenReturn(true);
        mController.onDeveloperOptionsSwitchDisabled();

        mController.writeShowHdrSdrRatioSetting(false);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }
}

