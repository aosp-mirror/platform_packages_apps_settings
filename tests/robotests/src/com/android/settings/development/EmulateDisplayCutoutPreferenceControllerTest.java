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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EmulateDisplayCutoutPreferenceControllerTest {

    @Mock Context mContext;
    @Mock IOverlayManager mOverlayManager;
    @Mock TwoStatePreference mPreference;
    EmulateDisplayCutoutPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(DISABLED);
        mController = new EmulateDisplayCutoutPreferenceController(mContext, mOverlayManager);
        mController.setPreference(mPreference);
    }

    @Test
    public void isAvailable_true() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(DISABLED);

        assertThat(new EmulateDisplayCutoutPreferenceController(mContext, mOverlayManager)
                .isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_false() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(null);

        assertThat(new EmulateDisplayCutoutPreferenceController(mContext, mOverlayManager)
                .isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_enable() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(DISABLED);

        mController.onPreferenceChange(null, true);

        verify(mOverlayManager).setEnabled(any(), eq(true), anyInt());
    }

    @Test
    public void onPreferenceChange_disable() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(ENABLED);

        mController.onPreferenceChange(null, false);

        verify(mOverlayManager).setEnabled(any(), eq(false), anyInt());
    }

    @Test
    public void updateState_enabled() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(ENABLED);

        mController.updateState(null);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_disabled() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(DISABLED);

        mController.updateState(null);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(DISABLED);

        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setEnabled(true);
        verify(mOverlayManager, never()).setEnabled(any(), eq(true), anyInt());
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled() throws Exception {
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(ENABLED);

        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
        verify(mOverlayManager).setEnabled(any(), eq(false), anyInt());
    }

    static final OverlayInfo ENABLED = new OverlayInfo() {
        @Override
        public boolean isEnabled() {
            return true;
        }
    };

    static final OverlayInfo DISABLED = new OverlayInfo() {
        @Override
        public boolean isEnabled() {
            return false;
        }
    };

}