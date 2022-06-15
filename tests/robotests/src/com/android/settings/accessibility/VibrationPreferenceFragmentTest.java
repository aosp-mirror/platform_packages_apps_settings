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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.VibrationPreferenceFragment.KEY_INTENSITY_HIGH;
import static com.android.settings.accessibility.VibrationPreferenceFragment.KEY_INTENSITY_LOW;
import static com.android.settings.accessibility.VibrationPreferenceFragment.KEY_INTENSITY_MEDIUM;
import static com.android.settings.accessibility.VibrationPreferenceFragment.KEY_INTENSITY_OFF;
import static com.android.settings.accessibility.VibrationPreferenceFragment.KEY_INTENSITY_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.accessibility.VibrationPreferenceFragment
        .VibrationIntensityCandidateInfo;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.widget.CandidateInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class VibrationPreferenceFragmentTest {

    private static final Map<Integer, String> INTENSITY_TO_KEY = new HashMap<>(4);
    static {
        INTENSITY_TO_KEY.put(Vibrator.VIBRATION_INTENSITY_OFF, KEY_INTENSITY_OFF);
        INTENSITY_TO_KEY.put(Vibrator.VIBRATION_INTENSITY_LOW, KEY_INTENSITY_LOW);
        INTENSITY_TO_KEY.put(Vibrator.VIBRATION_INTENSITY_MEDIUM, KEY_INTENSITY_MEDIUM);
        INTENSITY_TO_KEY.put(Vibrator.VIBRATION_INTENSITY_HIGH, KEY_INTENSITY_HIGH);
    }

    @Mock
    private UserManager mUserManager;

    private Context mContext;
    private Resources mResources;
    private TestVibrationPreferenceFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();

        mContext = spy(RuntimeEnvironment.application);
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);

        mFragment = spy(new TestVibrationPreferenceFragment());
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
    }

    @Test
    public void changeIntensitySetting_shouldResultInCorrespondingKey() {
        setSupportsMultipleIntensities(true);
        mFragment.onAttach(mContext);
        for (Map.Entry<Integer, String> entry : INTENSITY_TO_KEY.entrySet()) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_INTENSITY, entry.getKey());
            assertThat(mFragment.getDefaultKey()).isEqualTo(entry.getValue());
        }
    }

    @Test
    public void changeIntensitySetting_WithoutMultipleIntensitySupport_shouldResultInOn() {
        setSupportsMultipleIntensities(false);
        mFragment.onAttach(mContext);
        for (int intensity : INTENSITY_TO_KEY.keySet()) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_INTENSITY, intensity);
            final String expectedKey = intensity == Vibrator.VIBRATION_INTENSITY_OFF
                    ? KEY_INTENSITY_OFF
                    : KEY_INTENSITY_ON;
            assertThat(mFragment.getDefaultKey()).isEqualTo(expectedKey);
        }
    }

    @Test
    public void initialDefaultKey_shouldBeMedium() {
        setSupportsMultipleIntensities(true);
        mFragment.onAttach(mContext);
        assertThat(mFragment.getDefaultKey()).isEqualTo(KEY_INTENSITY_MEDIUM);
    }

    @Test
    public void initialDefaultKey_WithoutMultipleIntensitySupport_shouldBeOn() {
        setSupportsMultipleIntensities(false);
        mFragment.onAttach(mContext);
        assertThat(mFragment.getDefaultKey()).isEqualTo(KEY_INTENSITY_ON);
    }

    @Test
    public void candidates_shouldBeSortedByIntensity() {
        setSupportsMultipleIntensities(true);
        mFragment.onAttach(mContext);
        final List<? extends CandidateInfo> candidates = mFragment.getCandidates();
        assertThat(candidates.size()).isEqualTo(INTENSITY_TO_KEY.size());
        VibrationIntensityCandidateInfo prevCandidate =
                (VibrationIntensityCandidateInfo) candidates.get(0);
        for (int i = 1; i < candidates.size(); i++) {
            VibrationIntensityCandidateInfo candidate =
                    (VibrationIntensityCandidateInfo) candidates.get(i);
            assertThat(candidate.getIntensity()).isLessThan(prevCandidate.getIntensity());
        }
    }

    private void setSupportsMultipleIntensities(boolean hasSupport) {
        when(mResources.getBoolean(R.bool.config_vibration_supports_multiple_intensities))
            .thenReturn(hasSupport);
    }

    private class TestVibrationPreferenceFragment extends VibrationPreferenceFragment {
        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        /**
        * Get the setting string of the vibration intensity setting this preference is dealing with.
        */
        @Override
        protected String getVibrationIntensitySetting() {
            return Settings.System.HAPTIC_FEEDBACK_INTENSITY;
        }

        @Override
        protected String getVibrationEnabledSetting() {
            return "";
        }

        @Override
        protected int getDefaultVibrationIntensity() {
            return Vibrator.VIBRATION_INTENSITY_MEDIUM;
        }

        @Override
        public Context getContext() {
            return mContext;
        }
    }
}
