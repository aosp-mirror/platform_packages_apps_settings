/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.connecteddevice.display;

import static android.view.Display.INVALID_DISPLAY;

import static com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.DISPLAY_MODE_LIMIT_OVERRIDE_PROP;
import static com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE;
import static com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.MORE_OPTIONS_KEY;
import static com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.TOP_OPTIONS_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Unit tests for {@link ResolutionPreferenceFragment}.  */
@RunWith(AndroidJUnit4.class)
public class ResolutionPreferenceFragmentTest extends ExternalDisplayTestBase {
    @Nullable
    private ResolutionPreferenceFragment mFragment;
    private int mPreferenceIdFromResource;
    private int mDisplayIdArg = INVALID_DISPLAY;
    @Mock
    private MetricsLogger mMockedMetricsLogger;

    @Test
    @UiThreadTest
    public void testCreateAndStart() {
        initFragment();
        mHandler.flush();
        assertThat(mPreferenceIdFromResource).isEqualTo(
                EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE);
        var pref = mPreferenceScreen.findPreference(TOP_OPTIONS_KEY);
        assertThat(pref).isNull();
        pref = mPreferenceScreen.findPreference(MORE_OPTIONS_KEY);
        assertThat(pref).isNull();
    }

    @Test
    @UiThreadTest
    public void testCreateAndStartDefaultDisplayNotAllowed() {
        mDisplayIdArg = 0;
        initFragment();
        mHandler.flush();
        var pref = mPreferenceScreen.findPreference(TOP_OPTIONS_KEY);
        assertThat(pref).isNull();
        pref = mPreferenceScreen.findPreference(MORE_OPTIONS_KEY);
        assertThat(pref).isNull();
    }

    @Test
    @UiThreadTest
    public void testModePreferences_modeLimitFlagIsOn_noOverride() {
        doReturn(true).when(mMockedInjector).isModeLimitForExternalDisplayEnabled();
        doReturn(null).when(mMockedInjector).getSystemProperty(
                DISPLAY_MODE_LIMIT_OVERRIDE_PROP);
        var topAndMorePref = runTestModePreferences();
        PreferenceCategory topPref = topAndMorePref.first, morePref = topAndMorePref.second;
        assertThat(topPref.getPreferenceCount()).isEqualTo(3);
        assertThat(morePref.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void testModePreferences_noModeLimitFlag_overrideIsTrue() {
        doReturn(false).when(mMockedInjector).isModeLimitForExternalDisplayEnabled();
        doReturn("true").when(mMockedInjector).getSystemProperty(
                DISPLAY_MODE_LIMIT_OVERRIDE_PROP);
        var topAndMorePref = runTestModePreferences();
        PreferenceCategory topPref = topAndMorePref.first, morePref = topAndMorePref.second;
        assertThat(topPref.getPreferenceCount()).isEqualTo(3);
        assertThat(morePref.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void testModePreferences_noModeLimitFlag_noOverride() {
        doReturn(false).when(mMockedInjector).isModeLimitForExternalDisplayEnabled();
        doReturn(null).when(mMockedInjector).getSystemProperty(
                DISPLAY_MODE_LIMIT_OVERRIDE_PROP);
        var topAndMorePref = runTestModePreferences();
        PreferenceCategory topPref = topAndMorePref.first, morePref = topAndMorePref.second;
        assertThat(topPref.getPreferenceCount()).isEqualTo(3);
        assertThat(morePref.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    @UiThreadTest
    public void testModePreferences_modeLimitFlagIsOn_butOverrideIsFalse() {
        doReturn(true).when(mMockedInjector).isModeLimitForExternalDisplayEnabled();
        doReturn("false").when(mMockedInjector).getSystemProperty(
                DISPLAY_MODE_LIMIT_OVERRIDE_PROP);
        var topAndMorePref = runTestModePreferences();
        PreferenceCategory topPref = topAndMorePref.first, morePref = topAndMorePref.second;
        assertThat(topPref.getPreferenceCount()).isEqualTo(3);
        assertThat(morePref.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    @UiThreadTest
    public void testModeChange() {
        mDisplayIdArg = 1;
        initFragment();
        mHandler.flush();
        PreferenceCategory topPref = mPreferenceScreen.findPreference(TOP_OPTIONS_KEY);
        assertThat(topPref).isNotNull();
        var modePref = (SelectorWithWidgetPreference) topPref.getPreference(1);
        modePref.onClick();
        var mode = mDisplays[mDisplayIdArg].getSupportedModes()[1];
        verify(mMockedInjector).setUserPreferredDisplayMode(mDisplayIdArg, mode);
    }

    private Pair<PreferenceCategory, PreferenceCategory> runTestModePreferences() {
        mDisplayIdArg = 1;
        initFragment();
        mHandler.flush();
        PreferenceCategory topPref = mPreferenceScreen.findPreference(TOP_OPTIONS_KEY);
        assertThat(topPref).isNotNull();
        PreferenceCategory morePref = mPreferenceScreen.findPreference(MORE_OPTIONS_KEY);
        assertThat(morePref).isNotNull();
        return new Pair<>(topPref, morePref);
    }

    private void initFragment() {
        if (mFragment != null) {
            return;
        }
        mFragment = new TestableResolutionPreferenceFragment();
        mFragment.onCreateCallback(null);
        mFragment.onActivityCreatedCallback(null);
        mFragment.onStartCallback();
    }

    private class TestableResolutionPreferenceFragment extends ResolutionPreferenceFragment {
        private final View mMockedRootView;
        private final TextView mEmptyView;
        private final Resources mMockedResources;
        private final MetricsLogger mLogger;
        TestableResolutionPreferenceFragment() {
            super(mMockedInjector);
            mMockedResources = mock(Resources.class);
            doReturn(61).when(mMockedResources).getInteger(
                    com.android.internal.R.integer.config_externalDisplayPeakRefreshRate);
            doReturn(1920).when(mMockedResources).getInteger(
                    com.android.internal.R.integer.config_externalDisplayPeakWidth);
            doReturn(1080).when(mMockedResources).getInteger(
                    com.android.internal.R.integer.config_externalDisplayPeakHeight);
            doReturn(true).when(mMockedResources).getBoolean(
                    com.android.internal.R.bool.config_refreshRateSynchronizationEnabled);
            mMockedRootView = mock(View.class);
            mEmptyView = new TextView(mContext);
            doReturn(mEmptyView).when(mMockedRootView).findViewById(android.R.id.empty);
            mLogger = mMockedMetricsLogger;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceScreen;
        }

        @Override
        public View getView() {
            return mMockedRootView;
        }

        @Override
        public void setEmptyView(View view) {
            assertThat(view).isEqualTo(mEmptyView);
        }

        @Override
        public View getEmptyView() {
            return mEmptyView;
        }

        @Override
        public void addPreferencesFromResource(int resource) {
            mPreferenceIdFromResource = resource;
        }

        @Override
        protected int getDisplayIdArg() {
            return mDisplayIdArg;
        }

        @Override
        protected void writePreferenceClickMetric(Preference preference) {
            mLogger.writePreferenceClickMetric(preference);
        }

        @Override
        @NonNull
        protected Resources getResources(@NonNull Context context) {
            return mMockedResources;
        }
    }

    /**
     * Interface allowing to mock and spy on log events.
     */
    public interface MetricsLogger {
        /**
         * On preference click metric
         */
        void writePreferenceClickMetric(Preference preference);
    }
}
