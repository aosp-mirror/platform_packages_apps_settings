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
package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.app.ColorDisplayController;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.widget.CandidateInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class ColorModePreferenceFragmentTest {

    private ColorModePreferenceFragment mFragment;

    @Mock
    private ColorDisplayController mController;

    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new ColorModePreferenceFragmentTestable(mScreen));
        ReflectionHelpers.setField(mFragment, "mController", mController);
    }

    @Test
    public void verifyMetricsConstant() {
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.COLOR_MODE_SETTINGS);
    }

    @Test
    public void getCandidates() {
        when(mFragment.getContext()).thenReturn(RuntimeEnvironment.application);
        List<? extends CandidateInfo> candidates = mFragment.getCandidates();

        assertThat(candidates.size()).isEqualTo(4);
        assertThat(candidates.get(0).getKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);
        assertThat(candidates.get(1).getKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);
        assertThat(candidates.get(2).getKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);
        assertThat(candidates.get(3).getKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_AUTOMATIC);
    }

    @Test
    public void getKey_natural() {
        when(mController.getColorMode())
            .thenReturn(ColorDisplayController.COLOR_MODE_NATURAL);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);
    }

    @Test
    public void getKey_boosted() {
        when(mController.getColorMode())
            .thenReturn(ColorDisplayController.COLOR_MODE_BOOSTED);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);
    }

    @Test
    public void getKey_saturated() {
        when(mController.getColorMode())
            .thenReturn(ColorDisplayController.COLOR_MODE_SATURATED);

        assertThat(mFragment.getDefaultKey())
            .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);
    }

    @Test
    public void setKey_natural() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);
        verify(mController).setColorMode(ColorDisplayController.COLOR_MODE_NATURAL);
    }

    @Test
    public void setKey_boosted() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);
        verify(mController).setColorMode(ColorDisplayController.COLOR_MODE_BOOSTED);
    }

    @Test
    public void setKey_saturated() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);
        verify(mController).setColorMode(ColorDisplayController.COLOR_MODE_SATURATED);
    }

    @Test
    public void onCreatePreferences_useNewTitle_shouldAddColorModePreferences() {
        doNothing().when(mFragment).addPreferencesFromResource(anyInt());
        doNothing().when(mFragment).updateCandidates();

        mFragment.onCreatePreferences(Bundle.EMPTY, null /* rootKey */);

        verify(mFragment).addPreferencesFromResource(R.xml.color_mode_settings);
    }

    @Test
    public void addStaticPreferences_shouldAddPreviewImage() {
        PreferenceScreen mockPreferenceScreen = mock(PreferenceScreen.class);
        LayoutPreference mockPreview = mock(LayoutPreference.class);

        ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(Preference.class);

        mFragment.configureAndInstallPreview(mockPreview, mockPreferenceScreen);
        verify(mockPreview, times(1)).setSelectable(false);
        verify(mockPreferenceScreen, times(1)).addPreference(preferenceCaptor.capture());

        assertThat(preferenceCaptor.getValue()).isEqualTo(mockPreview);
    }

    @Test
    public void onAccessibilityTransformChanged_toggles() {
        final int radioPrefsCount = 3;
        List<RadioButtonPreference> radioPrefs = new ArrayList<>();
        for (int i = 0; i < radioPrefsCount; i++) {
            radioPrefs.add(mock(RadioButtonPreference.class));
        }

        when(mScreen.getPreferenceCount()).thenReturn(radioPrefs.size());
        when(mScreen.getPreference(anyInt())).thenAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            return radioPrefs.get((int) args[0]);
        });

        mFragment.onAccessibilityTransformChanged(true /* state */);
        for (int i = 0; i < radioPrefsCount; i++) {
            verify(radioPrefs.get(i)).setEnabled(false);
        }

        mFragment.onAccessibilityTransformChanged(false /* state */);
        for (int i = 0; i < radioPrefsCount; i++) {
            verify(radioPrefs.get(i)).setEnabled(true);
        }
    }

    private static class ColorModePreferenceFragmentTestable
            extends ColorModePreferenceFragment {

        private final PreferenceScreen mPreferenceScreen;

        private ColorModePreferenceFragmentTestable(PreferenceScreen screen) {
            mPreferenceScreen = screen;
        }

        /**
         * A method to return a mock PreferenceScreen.
         * A real ColorModePreferenceFragment calls super.getPreferenceScreen() to get its
         * PreferenceScreen handle, which internally dereferenced a PreferenceManager. But in this
         * test scenario, the PreferenceManager object is uninitialized, so we need to supply the
         * PreferenceScreen directly.
         *
         * @return a mock PreferenceScreen
         */
        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceScreen;
        }
    }
}
