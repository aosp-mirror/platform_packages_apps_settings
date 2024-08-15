/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

/**
 * Tests for {@link DaltonizerRadioButtonPreferenceController}
 */
@RunWith(RobolectricTestRunner.class)
public class DaltonizerRadioButtonPreferenceControllerTest {
    private static final int DALTONIZER_MODE_INDEX = 0;
    private static final String PREF_INVALID_VALUE = "-1";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final String mPrefKey =
            mContext.getResources()
                    .getStringArray(R.array.daltonizer_mode_keys)[DALTONIZER_MODE_INDEX];
    private final String mPrefValue =
            String.valueOf(mContext.getResources()
                    .getIntArray(R.array.daltonizer_type_values)[DALTONIZER_MODE_INDEX]);
    private DaltonizerRadioButtonPreferenceController mController;
    private SelectorWithWidgetPreference mPreference;
    private PreferenceScreen mScreen;

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        // initialize the value as unchecked
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, PREF_INVALID_VALUE);
        mController = new DaltonizerRadioButtonPreferenceController(mContext, mPrefKey);
        mPreference = new SelectorWithWidgetPreference(mContext);
        mPreference.setKey(mPrefKey);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        mController.displayPreference(mScreen);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
    }

    @After
    public void cleanUp() {
        mLifecycle.removeObserver(mController);
    }

    @Test
    public void isAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_valueNotMatched_notChecked() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, PREF_INVALID_VALUE);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_valueMatched_checked() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, mPrefValue);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void onRadioButtonClick_shouldReturnDaltonizerValue() {
        mController.onRadioButtonClicked(mPreference);

        final String accessibilityDaltonizerValue = Settings.Secure.getString(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER);
        assertThat(accessibilityDaltonizerValue).isEqualTo(mPrefValue);
    }

    @Test
    public void onResume_settingsValueChangedToUnmatch_preferenceBecomesUnchecked() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, mPrefValue);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        mLifecycle.addObserver(mController);

        mLifecycle.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, PREF_INVALID_VALUE);
        ShadowLooper.idleMainLooper();

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onPause_settingsValueChangedAndMatch_preferenceStateNotUpdated() {
        assertThat(mPreference.isChecked()).isFalse();
        mLifecycle.addObserver(mController);
        mLifecycle.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME);

        mLifecycle.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_PAUSE);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, mPrefValue);
        ShadowLooper.idleMainLooper();

        assertThat(mPreference.isChecked()).isFalse();
    }
}
