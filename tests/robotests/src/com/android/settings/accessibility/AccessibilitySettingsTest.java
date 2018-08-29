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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class AccessibilitySettingsTest {

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final List<String> keys = new ArrayList<>();

        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.accessibility_settings));

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testUpdateVibrationSummary_shouldUpdateSummary() {
        MockitoAnnotations.initMocks(this);
        final Context mContext = RuntimeEnvironment.application;
        final AccessibilitySettings mSettings = spy(new AccessibilitySettings());

        final Preference mVibrationPreferenceScreen = new Preference(mContext);
        doReturn(mVibrationPreferenceScreen).when(mSettings).findPreference(anyString());

        doReturn(mContext).when(mSettings).getContext();

        mVibrationPreferenceScreen.setKey("vibration_preference_screen");

        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
                Vibrator.VIBRATION_INTENSITY_OFF);

        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                Vibrator.VIBRATION_INTENSITY_OFF);

        mSettings.updateVibrationSummary(mVibrationPreferenceScreen);
        assertThat(mVibrationPreferenceScreen.getSummary()).isEqualTo(
                VibrationIntensityPreferenceController.getIntensityString(mContext,
                        Vibrator.VIBRATION_INTENSITY_OFF));
    }
}
