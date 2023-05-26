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

package com.android.settings.dream;

import static com.android.settingslib.dream.DreamBackend.COMPLICATION_TYPE_HOME_CONTROLS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.util.ArraySet;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.dream.DreamBackend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowSettings;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSettings.ShadowSecure.class})
public class DreamHomeControlsPreferenceControllerTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    private DreamHomeControlsPreferenceController mController;
    private SwitchPreference mPreference;
    private DreamBackend mBackend;
    private ShadowContentResolver mShadowContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mShadowContentResolver = Shadow.extract(mContext.getContentResolver());
        mBackend = new DreamBackend(mContext);
        mController = new DreamHomeControlsPreferenceController(mContext, "key", mBackend);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);

        // Make home controls supported by default
        mBackend.setSupportedComplications(
                new ArraySet<>(new Integer[]{COMPLICATION_TYPE_HOME_CONTROLS}));
    }

    @After
    public void tearDown() {
        ShadowSettings.ShadowSecure.reset();
    }

    @Test
    public void testSetChecked_setTrue_enablesSetting() {
        setControlsEnabledOnLockscreen(true);
        mBackend.setHomeControlsEnabled(false);
        assertThat(mBackend.getEnabledComplications())
                .doesNotContain(COMPLICATION_TYPE_HOME_CONTROLS);

        mController.setChecked(true);
        assertThat(mBackend.getEnabledComplications())
                .contains(COMPLICATION_TYPE_HOME_CONTROLS);
    }

    @Test
    public void testSetChecked_setFalse_disablesSetting() {
        setControlsEnabledOnLockscreen(true);
        mBackend.setHomeControlsEnabled(true);
        assertThat(mBackend.getEnabledComplications())
                .contains(COMPLICATION_TYPE_HOME_CONTROLS);

        mController.setChecked(false);
        assertThat(mBackend.getEnabledComplications())
                .doesNotContain(COMPLICATION_TYPE_HOME_CONTROLS);
    }

    @Test
    public void testIsChecked_returnsFalse() {
        setControlsEnabledOnLockscreen(true);
        mBackend.setHomeControlsEnabled(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testIsChecked_returnsTrue() {
        setControlsEnabledOnLockscreen(true);
        mBackend.setHomeControlsEnabled(true);
        assertThat(mBackend.getEnabledComplications())
                .contains(COMPLICATION_TYPE_HOME_CONTROLS);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_lockScreenDisabled_returnsFalse() {
        setControlsEnabledOnLockscreen(false);
        mBackend.setHomeControlsEnabled(true);
        assertThat(mBackend.getEnabledComplications())
                .doesNotContain(COMPLICATION_TYPE_HOME_CONTROLS);
        assertThat(mController.isChecked()).isFalse();
    }

    private void setControlsEnabledOnLockscreen(boolean enabled) {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCKSCREEN_SHOW_CONTROLS,
                enabled ? 1 : 0);
    }
}
