/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.MainSwitchPreference;

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
public class DreamMainSwitchPreferenceControllerTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    private DreamMainSwitchPreferenceController mController;
    private MainSwitchPreference mPreference;
    private DreamBackend mBackend;
    private ShadowContentResolver mShadowContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mShadowContentResolver = Shadow.extract(mContext.getContentResolver());
        mBackend = DreamBackend.getInstance(mContext);
        mController = new DreamMainSwitchPreferenceController(mContext, "key");
        mPreference = new MainSwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        ShadowSettings.ShadowSecure.reset();
        mController.onStop();
    }

    @Test
    public void testIsChecked_returnsFalse() {
        mBackend.setEnabled(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testIsChecked_returnsTrue() {
        mBackend.setEnabled(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testSetChecked_setFalse_disablesSetting() {
        mBackend.setEnabled(true);
        mController.setChecked(false);
        assertThat(mBackend.isEnabled()).isFalse();
    }

    @Test
    public void testSetChecked_setTrue_enablesSetting() {
        mBackend.setEnabled(false);
        mController.setChecked(true);
        assertThat(mBackend.isEnabled()).isTrue();
    }

    @Test
    public void testIsSliceable_returnsFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void testRegisterAndUnregister() {
        mController.onStart();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.SCREENSAVER_ENABLED))).hasSize(1);

        mController.onStop();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.SCREENSAVER_ENABLED))).isEmpty();
    }

    @Test
    public void testUpdateState() {
        mController.onStart();

        mBackend.setEnabled(true);
        triggerOnChangeListener();
        assertThat(mPreference.isChecked()).isTrue();

        mBackend.setEnabled(false);
        triggerOnChangeListener();
        assertThat(mPreference.isChecked()).isFalse();

        mBackend.setEnabled(true);
        triggerOnChangeListener();
        assertThat(mPreference.isChecked()).isTrue();
    }

    private void triggerOnChangeListener() {
        mShadowContentResolver.getContentObservers(
                        Settings.Secure.getUriFor(Settings.Secure.SCREENSAVER_ENABLED))
                .forEach(contentObserver -> contentObserver.onChange(false));
    }
}
