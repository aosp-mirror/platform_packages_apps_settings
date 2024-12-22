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

import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.PREVIOUSLY_SHOWN_LIST_KEY;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.DISPLAYS_LIST_PREFERENCE_KEY;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_RESOLUTION_PREFERENCE_KEY;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_RESOLUTION_TITLE_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_ROTATION_KEY;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_ROTATION_TITLE_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_SETTINGS_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_USE_PREFERENCE_KEY;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_USE_TITLE_RESOURCE;
import static com.android.settingslib.widget.FooterPreference.KEY_FOOTER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.DisplayPreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Unit tests for {@link ExternalDisplayPreferenceFragment}.  */
@RunWith(AndroidJUnit4.class)
public class ExternalDisplayPreferenceFragmentTest extends ExternalDisplayTestBase {
    @Nullable
    private ExternalDisplayPreferenceFragment mFragment;
    private int mPreferenceIdFromResource;
    private int mDisplayIdArg = INVALID_DISPLAY;
    private int mResolutionSelectorDisplayId = INVALID_DISPLAY;
    @Mock
    private MetricsLogger mMockedMetricsLogger;

    @Test
    @UiThreadTest
    public void testCreateAndStart() {
        initFragment();
        assertThat(mPreferenceIdFromResource).isEqualTo(EXTERNAL_DISPLAY_SETTINGS_RESOURCE);
    }

    @Test
    @UiThreadTest
    public void testShowDisplayList() {
        var fragment = initFragment();
        var outState = new Bundle();
        fragment.onSaveInstanceStateCallback(outState);
        assertThat(outState.getBoolean(PREVIOUSLY_SHOWN_LIST_KEY)).isFalse();
        assertThat(mHandler.getPendingMessages().size()).isEqualTo(1);
        PreferenceCategory pref = mPreferenceScreen.findPreference(DISPLAYS_LIST_PREFERENCE_KEY);
        assertThat(pref).isNull();
        verify(mMockedInjector, never()).getAllDisplays();
        mHandler.flush();
        assertThat(mHandler.getPendingMessages().size()).isEqualTo(0);
        verify(mMockedInjector).getAllDisplays();
        pref = mPreferenceScreen.findPreference(DISPLAYS_LIST_PREFERENCE_KEY);
        assertThat(pref).isNotNull();
        assertThat(pref.getPreferenceCount()).isEqualTo(2);
        fragment.onSaveInstanceStateCallback(outState);
        assertThat(outState.getBoolean(PREVIOUSLY_SHOWN_LIST_KEY)).isTrue();
    }

    @Test
    @UiThreadTest
    public void testLaunchDisplaySettingFromList() {
        initFragment();
        mHandler.flush();
        PreferenceCategory pref = mPreferenceScreen.findPreference(DISPLAYS_LIST_PREFERENCE_KEY);
        assertThat(pref).isNotNull();
        DisplayPreference display1Pref = (DisplayPreference) pref.getPreference(0);
        DisplayPreference display2Pref = (DisplayPreference) pref.getPreference(1);
        assertThat(display1Pref.getKey()).isEqualTo("display_id_" + 1);
        assertThat("" + display1Pref.getTitle()).isEqualTo("HDMI");
        assertThat("" + display1Pref.getSummary()).isEqualTo("1920 x 1080");
        display1Pref.onPreferenceClick(display1Pref);
        assertThat(mDisplayIdArg).isEqualTo(1);
        verify(mMockedMetricsLogger).writePreferenceClickMetric(display1Pref);
        assertThat(display2Pref.getKey()).isEqualTo("display_id_" + 2);
        assertThat("" + display2Pref.getTitle()).isEqualTo("Overlay #1");
        assertThat("" + display2Pref.getSummary()).isEqualTo("1240 x 780");
        display2Pref.onPreferenceClick(display2Pref);
        assertThat(mDisplayIdArg).isEqualTo(2);
        verify(mMockedMetricsLogger).writePreferenceClickMetric(display2Pref);
    }

    @Test
    @UiThreadTest
    public void testShowDisplayListForOnlyOneDisplay_PreviouslyShownList() {
        var fragment = initFragment();
        // Previously shown list of displays
        fragment.onActivityCreatedCallback(createBundleForPreviouslyShownList());
        // Only one display available
        doReturn(new Display[] {mDisplays[1]}).when(mMockedInjector).getAllDisplays();
        mHandler.flush();
        PreferenceCategory pref = mPreferenceScreen.findPreference(DISPLAYS_LIST_PREFERENCE_KEY);
        assertThat(pref).isNotNull();
        assertThat(pref.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void testShowEnabledDisplay_OnlyOneDisplayAvailable() {
        doReturn(true).when(mMockedInjector).isDisplayEnabled(any());
        // Only one display available
        doReturn(new Display[] {mDisplays[1]}).when(mMockedInjector).getAllDisplays();
        // Init
        initFragment();
        mHandler.flush();
        PreferenceCategory list = mPreferenceScreen.findPreference(DISPLAYS_LIST_PREFERENCE_KEY);
        assertThat(list).isNull();
        var pref = mPreferenceScreen.findPreference(EXTERNAL_DISPLAY_RESOLUTION_PREFERENCE_KEY);
        assertThat(pref).isNotNull();
        pref = mPreferenceScreen.findPreference(EXTERNAL_DISPLAY_ROTATION_KEY);
        assertThat(pref).isNotNull();
        var footerPref = (FooterPreference) mPreferenceScreen.findPreference(KEY_FOOTER);
        assertThat(footerPref).isNotNull();
        verify(footerPref).setTitle(EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE);
    }

    @Test
    @UiThreadTest
    public void testShowOneEnabledDisplay_FewAvailable() {
        mDisplayIdArg = 1;
        doReturn(true).when(mMockedInjector).isDisplayEnabled(any());
        initFragment();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        mHandler.flush();
        verify(mMockedInjector).getDisplay(mDisplayIdArg);
        var pref = mPreferenceScreen.findPreference(EXTERNAL_DISPLAY_RESOLUTION_PREFERENCE_KEY);
        assertThat(pref).isNotNull();
        pref = mPreferenceScreen.findPreference(EXTERNAL_DISPLAY_ROTATION_KEY);
        assertThat(pref).isNotNull();
        var footerPref = (FooterPreference) mPreferenceScreen.findPreference(KEY_FOOTER);
        assertThat(footerPref).isNotNull();
        verify(footerPref).setTitle(EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE);
    }

    @Test
    @UiThreadTest
    public void testShowDisabledDisplay() {
        mDisplayIdArg = 1;
        initFragment();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        mHandler.flush();
        verify(mMockedInjector).getDisplay(mDisplayIdArg);
        var mainPref = (MainSwitchPreference) mPreferenceScreen.findPreference(
                EXTERNAL_DISPLAY_USE_PREFERENCE_KEY);
        assertThat(mainPref).isNotNull();
        assertThat("" + mainPref.getTitle()).isEqualTo(
                getText(EXTERNAL_DISPLAY_USE_TITLE_RESOURCE));
        assertThat(mainPref.isChecked()).isFalse();
        assertThat(mainPref.isEnabled()).isTrue();
        assertThat(mainPref.getOnPreferenceChangeListener()).isNotNull();
        var pref = mPreferenceScreen.findPreference(EXTERNAL_DISPLAY_RESOLUTION_PREFERENCE_KEY);
        assertThat(pref).isNull();
        pref = mPreferenceScreen.findPreference(EXTERNAL_DISPLAY_ROTATION_KEY);
        assertThat(pref).isNull();
        var footerPref = (FooterPreference) mPreferenceScreen.findPreference(KEY_FOOTER);
        assertThat(footerPref).isNull();
    }

    @Test
    @UiThreadTest
    public void testNoDisplays() {
        doReturn(new Display[0]).when(mMockedInjector).getAllDisplays();
        initFragment();
        mHandler.flush();
        var mainPref = (MainSwitchPreference) mPreferenceScreen.findPreference(
                EXTERNAL_DISPLAY_USE_PREFERENCE_KEY);
        assertThat(mainPref).isNotNull();
        assertThat("" + mainPref.getTitle()).isEqualTo(
                getText(EXTERNAL_DISPLAY_USE_TITLE_RESOURCE));
        assertThat(mainPref.isChecked()).isFalse();
        assertThat(mainPref.isEnabled()).isFalse();
        assertThat(mainPref.getOnPreferenceChangeListener()).isNull();
        var footerPref = (FooterPreference) mPreferenceScreen.findPreference(KEY_FOOTER);
        assertThat(footerPref).isNotNull();
        verify(footerPref).setTitle(EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE);
    }

    @Test
    @UiThreadTest
    public void testDisplayRotationPreference() {
        mDisplayIdArg = 1;
        doReturn(true).when(mMockedInjector).isDisplayEnabled(any());
        var fragment = initFragment();
        mHandler.flush();
        var pref = fragment.getRotationPreference(mContext);
        assertThat(pref.getKey()).isEqualTo(EXTERNAL_DISPLAY_ROTATION_KEY);
        assertThat("" + pref.getTitle()).isEqualTo(
                getText(EXTERNAL_DISPLAY_ROTATION_TITLE_RESOURCE));
        assertThat(pref.getEntries().length).isEqualTo(4);
        assertThat(pref.getEntryValues().length).isEqualTo(4);
        assertThat(pref.getEntryValues()[0].toString()).isEqualTo("0");
        assertThat(pref.getEntryValues()[1].toString()).isEqualTo("1");
        assertThat(pref.getEntryValues()[2].toString()).isEqualTo("2");
        assertThat(pref.getEntryValues()[3].toString()).isEqualTo("3");
        assertThat(pref.getEntries()[0].length()).isGreaterThan(0);
        assertThat(pref.getEntries()[1].length()).isGreaterThan(0);
        assertThat("" + pref.getSummary()).isEqualTo(pref.getEntries()[0].toString());
        assertThat(pref.getValue()).isEqualTo("0");
        assertThat(pref.getOnPreferenceChangeListener()).isNotNull();
        assertThat(pref.isEnabled()).isTrue();
        var rotation = 1;
        doReturn(true).when(mMockedInjector).freezeDisplayRotation(mDisplayIdArg, rotation);
        assertThat(pref.getOnPreferenceChangeListener().onPreferenceChange(pref, rotation + ""))
                .isTrue();
        verify(mMockedInjector).freezeDisplayRotation(mDisplayIdArg, rotation);
        assertThat(pref.getValue()).isEqualTo(rotation + "");
        verify(mMockedMetricsLogger).writePreferenceClickMetric(pref);
    }

    @Test
    @UiThreadTest
    public void testDisplayResolutionPreference() {
        mDisplayIdArg = 1;
        doReturn(true).when(mMockedInjector).isDisplayEnabled(any());
        var fragment = initFragment();
        mHandler.flush();
        var pref = fragment.getResolutionPreference(mContext);
        assertThat(pref.getKey()).isEqualTo(EXTERNAL_DISPLAY_RESOLUTION_PREFERENCE_KEY);
        assertThat("" + pref.getTitle()).isEqualTo(
                getText(EXTERNAL_DISPLAY_RESOLUTION_TITLE_RESOURCE));
        assertThat("" + pref.getSummary()).isEqualTo("1920 x 1080");
        assertThat(pref.isEnabled()).isTrue();
        assertThat(pref.getOnPreferenceClickListener()).isNotNull();
        assertThat(pref.getOnPreferenceClickListener().onPreferenceClick(pref)).isTrue();
        assertThat(mResolutionSelectorDisplayId).isEqualTo(mDisplayIdArg);
        verify(mMockedMetricsLogger).writePreferenceClickMetric(pref);
    }

    @Test
    @UiThreadTest
    public void testUseDisplayPreference_EnabledDisplay() {
        mDisplayIdArg = 1;
        doReturn(true).when(mMockedInjector).isDisplayEnabled(any());
        doReturn(true).when(mMockedInjector).enableConnectedDisplay(mDisplayIdArg);
        doReturn(true).when(mMockedInjector).disableConnectedDisplay(mDisplayIdArg);
        var fragment = initFragment();
        mHandler.flush();
        var pref = fragment.getUseDisplayPreference(mContext);
        assertThat(pref.getKey()).isEqualTo(EXTERNAL_DISPLAY_USE_PREFERENCE_KEY);
        assertThat("" + pref.getTitle()).isEqualTo(getText(EXTERNAL_DISPLAY_USE_TITLE_RESOURCE));
        assertThat(pref.isEnabled()).isTrue();
        assertThat(pref.isChecked()).isTrue();
        assertThat(pref.getOnPreferenceChangeListener()).isNotNull();
        assertThat(pref.getOnPreferenceChangeListener().onPreferenceChange(pref, false)).isTrue();
        verify(mMockedInjector).disableConnectedDisplay(mDisplayIdArg);
        assertThat(pref.isChecked()).isFalse();
        assertThat(pref.getOnPreferenceChangeListener().onPreferenceChange(pref, true)).isTrue();
        verify(mMockedInjector).enableConnectedDisplay(mDisplayIdArg);
        assertThat(pref.isChecked()).isTrue();
        verify(mMockedMetricsLogger, times(2)).writePreferenceClickMetric(pref);
    }

    @NonNull
    private ExternalDisplayPreferenceFragment initFragment() {
        if (mFragment != null) {
            return mFragment;
        }
        mFragment = new TestableExternalDisplayPreferenceFragment();
        mFragment.onCreateCallback(null);
        mFragment.onActivityCreatedCallback(null);
        mFragment.onStartCallback();
        return mFragment;
    }

    @NonNull
    private Bundle createBundleForPreviouslyShownList() {
        var state = new Bundle();
        state.putBoolean(PREVIOUSLY_SHOWN_LIST_KEY, true);
        return state;
    }

    @NonNull
    private String getText(int id) {
        return mContext.getResources().getText(id).toString();
    }

    private class TestableExternalDisplayPreferenceFragment extends
            ExternalDisplayPreferenceFragment {
        private final View mMockedRootView;
        private final TextView mEmptyView;
        private final Activity mMockedActivity;
        private final FooterPreference mMockedFooterPreference;
        private final MetricsLogger mLogger;

        TestableExternalDisplayPreferenceFragment() {
            super(mMockedInjector);
            mMockedActivity = mock(Activity.class);
            mMockedRootView = mock(View.class);
            mMockedFooterPreference = mock(FooterPreference.class);
            doReturn(KEY_FOOTER).when(mMockedFooterPreference).getKey();
            mEmptyView = new TextView(mContext);
            doReturn(mEmptyView).when(mMockedRootView).findViewById(android.R.id.empty);
            mLogger = mMockedMetricsLogger;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceScreen;
        }

        @Override
        protected Activity getCurrentActivity() {
            return mMockedActivity;
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
        @NonNull
        FooterPreference getFooterPreference(@NonNull Context context) {
            return mMockedFooterPreference;
        }

        @Override
        protected int getDisplayIdArg() {
            return mDisplayIdArg;
        }

        @Override
        protected void launchResolutionSelector(@NonNull Context context, int displayId) {
            mResolutionSelectorDisplayId = displayId;
        }

        @Override
        protected void launchDisplaySettings(final int displayId) {
            mDisplayIdArg = displayId;
        }

        @Override
        protected void writePreferenceClickMetric(Preference preference) {
            mLogger.writePreferenceClickMetric(preference);
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
