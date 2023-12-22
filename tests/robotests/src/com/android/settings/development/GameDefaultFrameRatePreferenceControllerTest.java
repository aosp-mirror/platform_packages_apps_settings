/*
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

import static com.android.settings.development.GameDefaultFrameRatePreferenceController.Injector;
import static com.android.settings.development.GameDefaultFrameRatePreferenceController.PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IGameManagerService;
import android.content.Context;
import android.os.RemoteException;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GameDefaultFrameRatePreferenceControllerTest {
    @Mock
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TwoStatePreference mPreference;
    @Mock
    private IGameManagerService mGameManagerService;
    @Mock
    private DevelopmentSystemPropertiesWrapper mSysPropsMock;

    private GameDefaultFrameRatePreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new GameDefaultFrameRatePreferenceController(mContext, mGameManagerService,
                new Injector(){
                    @Override
                    public DevelopmentSystemPropertiesWrapper createSystemPropertiesWrapper() {
                        return mSysPropsMock;
                    }
                });
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_shouldChecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        assertTrue(mController.isAvailable());
        when(mSysPropsMock.getBoolean(
                ArgumentMatchers.eq(PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED),
                ArgumentMatchers.eq(false)))
                .thenReturn(true);

        mController.onPreferenceChange(mPreference, true /* new value */);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void onPreferenceChange_settingDisabled_shouldUnchecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        assertTrue(mController.isAvailable());
        when(mSysPropsMock.getBoolean(
                ArgumentMatchers.eq(PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED),
                ArgumentMatchers.eq(false)))
                .thenReturn(false);
        mController.onPreferenceChange(mPreference, false /* new value */);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_settingEnabled_shouldChecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        assertTrue(mController.isAvailable());
        when(mSysPropsMock.getBoolean(
                ArgumentMatchers.eq(PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED),
                ArgumentMatchers.eq(false)))
                .thenReturn(true);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_shouldUnchecked() throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        assertTrue(mController.isAvailable());
        when(mSysPropsMock.getBoolean(
                ArgumentMatchers.eq(PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED),
                ArgumentMatchers.eq(false)))
                .thenReturn(false);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void settingNotAvailable_flagsOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        mController = new GameDefaultFrameRatePreferenceController(
                mContext, mGameManagerService, new Injector());
        assertFalse(mController.isAvailable());
    }

    @Test
    public void settingAvailable_flagsOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        mController = new GameDefaultFrameRatePreferenceController(
                mContext, mGameManagerService, new Injector());
        assertTrue(mController.isAvailable());
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceUnchecked_shouldNotTurnOffPreference()
            throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        when(mSysPropsMock.getBoolean(
                ArgumentMatchers.eq(PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED),
                ArgumentMatchers.eq(false)))
                .thenReturn(false);
        assertTrue(mController.isAvailable());
        when(mPreference.isChecked()).thenReturn(false);
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceChecked_shouldTurnOffPreference()
            throws RemoteException {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEVELOPMENT_GAME_DEFAULT_FRAME_RATE);
        when(mSysPropsMock.getBoolean(
                ArgumentMatchers.eq(PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED),
                ArgumentMatchers.eq(false)))
                .thenReturn(true);
        assertTrue(mController.isAvailable());

        when(mPreference.isChecked()).thenReturn(true);
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }
}
