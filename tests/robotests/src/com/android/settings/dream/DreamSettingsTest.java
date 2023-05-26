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

package com.android.settings.dream;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.WhenToDream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Config(shadows = ShadowFragment.class)
@RunWith(RobolectricTestRunner.class)
public class DreamSettingsTest {

    private static final List<String> KEYS = Arrays.asList(
            DreamSettings.WHILE_CHARGING_ONLY,
            DreamSettings.WHILE_DOCKED_ONLY,
            DreamSettings.EITHER_CHARGING_OR_DOCKED,
            DreamSettings.NEVER_DREAM
    );

    @WhenToDream
    private static final int[] SETTINGS = {
            DreamBackend.WHILE_CHARGING,
            DreamBackend.WHILE_DOCKED,
            DreamBackend.EITHER,
            DreamBackend.NEVER,
    };

    private static final int[] RES_IDS = {
            R.string.screensaver_settings_summary_sleep,
            R.string.screensaver_settings_summary_dock,
            R.string.screensaver_settings_summary_either_long,
            R.string.screensaver_settings_summary_never
    };

    private static final int[] RES_IDS_NO_BATTERY = {
            R.string.screensaver_settings_summary_sleep,
            R.string.screensaver_settings_summary_dock_and_charging,
            R.string.screensaver_settings_summary_either_long,
            R.string.screensaver_settings_summary_never
    };

    @Mock
    private Preference mDreamPickerPref;
    @Mock
    private Preference mComplicationsTogglePref;
    @Mock
    private DreamPickerController mDreamPickerController;
    @Captor
    private ArgumentCaptor<DreamPickerController.Callback> mDreamPickerCallbackCaptor;

    @Test
    public void getSettingFromPrefKey() {
        for (int i = 0; i < KEYS.size(); i++) {
            assertThat(DreamSettings.getSettingFromPrefKey(KEYS.get(i))).isEqualTo(SETTINGS[i]);
        }
        // Default case
        assertThat(DreamSettings.getSettingFromPrefKey("garbage value"))
                .isEqualTo(DreamBackend.NEVER);
    }

    @Test
    public void getKeyFromSetting() {
        for (int i = 0; i < SETTINGS.length; i++) {
            assertThat(DreamSettings.getKeyFromSetting(SETTINGS[i])).isEqualTo(KEYS.get(i));
        }
        // Default
        assertThat(DreamSettings.getKeyFromSetting(-1))
                .isEqualTo(DreamSettings.NEVER_DREAM);
    }

    @Test
    public void getDreamSettingDescriptionResId() {
        for (int i = 0; i < SETTINGS.length; i++) {
            assertThat(DreamSettings.getDreamSettingDescriptionResId(
                    SETTINGS[i], /* enabledOnBattery= */ false))
                    .isEqualTo(RES_IDS_NO_BATTERY[i]);
            assertThat(DreamSettings.getDreamSettingDescriptionResId(
                    SETTINGS[i], /* enabledOnBattery= */ true))
                    .isEqualTo(RES_IDS[i]);
        }
        // Default
        assertThat(DreamSettings.getDreamSettingDescriptionResId(-1, /* enabledOnBattery= */ false))
                .isEqualTo(R.string.screensaver_settings_summary_never);
        assertThat(DreamSettings.getDreamSettingDescriptionResId(-1, /* enabledOnBattery= */ true))
                .isEqualTo(R.string.screensaver_settings_summary_never);
    }

    @Test
    public void summaryText_whenDreamsAreOff() {
        final String fakeSummaryOff = "test dream off";
        final DreamBackend mockBackend = mock(DreamBackend.class);
        final Context mockContext = mock(Context.class);
        when(mockBackend.isEnabled()).thenReturn(false);
        when(mockContext.getString(R.string.screensaver_settings_summary_off)).thenReturn(
                fakeSummaryOff);

        assertThat(DreamSettings.getSummaryTextFromBackend(mockBackend, mockContext)).isEqualTo(
                fakeSummaryOff);
    }

    @Test
    public void summaryTest_WhenDreamsAreOn() {
        final String fakeName = "test_name";
        final DreamBackend mockBackend = mock(DreamBackend.class);
        final Context mockContext = mock(Context.class);
        when(mockBackend.isEnabled()).thenReturn(true);
        when(mockBackend.getActiveDreamName()).thenReturn(fakeName);
        when(mockContext.getString(eq(R.string.screensaver_settings_summary_on), anyString()))
                .thenAnswer(i -> i.getArgument(1) + " test dream is on");

        assertThat(DreamSettings.getSummaryTextFromBackend(mockBackend, mockContext)).isEqualTo(
                fakeName + " test dream is on");
    }

    @Test
    public void complicationsToggle_addAndRemoveActiveDreamChangeCallback() {
        MockitoAnnotations.initMocks(this);

        final Context context = ApplicationProvider.getApplicationContext();
        final DreamSettings dreamSettings = prepareDreamSettings(context);

        dreamSettings.onAttach(context);
        dreamSettings.onCreate(Bundle.EMPTY);
        verify(mDreamPickerController).addCallback(mDreamPickerCallbackCaptor.capture());

        dreamSettings.onDestroy();
        verify(mDreamPickerController).removeCallback(mDreamPickerCallbackCaptor.getValue());
    }

    @Test
    public void complicationsToggle_showWhenDreamSupportsComplications() {
        MockitoAnnotations.initMocks(this);

        final Context context = ApplicationProvider.getApplicationContext();
        final DreamSettings dreamSettings = prepareDreamSettings(context);

        // Active dream supports complications
        final DreamBackend.DreamInfo activeDream = new DreamBackend.DreamInfo();
        activeDream.supportsComplications = true;
        when(mDreamPickerController.getActiveDreamInfo()).thenReturn(activeDream);

        dreamSettings.onAttach(context);
        dreamSettings.onCreate(Bundle.EMPTY);

        // Verify dream complications toggle is visible
        verify(mComplicationsTogglePref).setVisible(true);
    }

    @Test
    public void complicationsToggle_hideWhenDreamDoesNotSupportComplications() {
        MockitoAnnotations.initMocks(this);

        final Context context = ApplicationProvider.getApplicationContext();
        final DreamSettings dreamSettings = prepareDreamSettings(context);

        // Active dream does not support complications
        final DreamBackend.DreamInfo activeDream = new DreamBackend.DreamInfo();
        activeDream.supportsComplications = false;
        when(mDreamPickerController.getActiveDreamInfo()).thenReturn(activeDream);

        dreamSettings.onAttach(context);
        dreamSettings.onCreate(Bundle.EMPTY);

        // Verify dream complications toggle is hidden
        verify(mComplicationsTogglePref).setVisible(false);
    }

    @Test
    public void complicationsToggle_showWhenSwitchToDreamSupportsComplications() {
        MockitoAnnotations.initMocks(this);

        final Context context = ApplicationProvider.getApplicationContext();
        final DreamSettings dreamSettings = prepareDreamSettings(context);

        // Active dream does not support complications
        final DreamBackend.DreamInfo activeDream = new DreamBackend.DreamInfo();
        activeDream.supportsComplications = false;
        when(mDreamPickerController.getActiveDreamInfo()).thenReturn(activeDream);

        dreamSettings.onAttach(context);
        dreamSettings.onCreate(Bundle.EMPTY);

        // Verify dream complications toggle is hidden
        verify(mComplicationsTogglePref).setVisible(false);
        verify(mDreamPickerController).addCallback(mDreamPickerCallbackCaptor.capture());

        // Active dream changes to one that supports complications
        activeDream.supportsComplications = true;
        mDreamPickerCallbackCaptor.getValue().onActiveDreamChanged();

        // Verify dream complications toggle is shown
        verify(mComplicationsTogglePref).setVisible(true);
    }

    private DreamSettings prepareDreamSettings(Context context) {
        final TestDreamSettings dreamSettings = new TestDreamSettings(context);
        when(mDreamPickerController.getPreferenceKey()).thenReturn(DreamPickerController.PREF_KEY);
        when(mDreamPickerPref.getExtras()).thenReturn(new Bundle());
        when(mDreamPickerPref.getKey()).thenReturn(DreamPickerController.PREF_KEY);
        when(mComplicationsTogglePref.getKey()).thenReturn(
                DreamComplicationPreferenceController.PREF_KEY);

        dreamSettings.addPreference(DreamPickerController.PREF_KEY, mDreamPickerPref);
        dreamSettings.addPreference(DreamComplicationPreferenceController.PREF_KEY,
                mComplicationsTogglePref);
        dreamSettings.setDreamPickerController(mDreamPickerController);

        return dreamSettings;
    }

    private static class TestDreamSettings extends DreamSettings {

        private final Context mContext;
        private final PreferenceManager mPreferenceManager;

        private final HashMap<String, Preference> mPreferences = new HashMap<>();

        TestDreamSettings(Context context) {
            super();
            mContext = context;
            mPreferenceManager = new PreferenceManager(context);
            mPreferenceManager.setPreferences(mPreferenceManager.createPreferenceScreen(context));
        }

        @Override
        public int getPreferenceScreenResId() {
            return R.xml.placeholder_prefs;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceManager.getPreferenceScreen();
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public <T extends Preference> T findPreference(CharSequence key) {
            return (T) mPreferences.get(key);
        }

        void addPreference(String key, Preference preference) {
            mPreferences.put(key, preference);
        }
    }
}
