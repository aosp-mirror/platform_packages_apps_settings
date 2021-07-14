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

import static com.android.settings.accessibility.AccessibilityDialogUtils.CustomButton;
import static com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import static com.android.settings.accessibility.MagnificationModePreferenceController.MagnificationModeInfo;
import static com.android.settings.accessibility.MagnificationPreferenceFragment.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.DialogCreatable;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link MagnificationModePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class MagnificationModePreferenceControllerTest {
    private static final String PREF_KEY = "screen_magnification_mode";
    private static final int MAGNIFICATION_MODE_DEFAULT = MagnificationMode.ALL;

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    private PreferenceScreen mScreen;
    private Context mContext;
    private MagnificationModePreferenceController mController;
    private Preference mModePreference;
    @Spy
    private TestDialogHelper mDialogHelper = new TestDialogHelper();

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mModePreference = new Preference(mContext);
        mModePreference.setKey(PREF_KEY);
        mScreen.addPreference(mModePreference);
        MagnificationCapabilities.setCapabilities(mContext, MAGNIFICATION_MODE_DEFAULT);
        mController = new MagnificationModePreferenceController(mContext, PREF_KEY);
        showPreferenceOnTheScreen(null);
    }

    @Test
    public void clickPreference_settingsModeIsDefault_checkedModeInDialogIsDefault() {
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                MAGNIFICATION_MODE_DEFAULT);
    }

    @Test
    public void choseModeIsDifferentFromInSettings_checkedModeInDialogIsExpected() {
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        performItemClickWith(MagnificationMode.WINDOW);

        assertThat(getCheckedModeFromDialog()).isEqualTo(MagnificationMode.WINDOW);
    }

    @Test
    public void dialogIsReCreated_settingsModeIsAllAndChoseWindowMode_checkedModeIsWindow() {
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);
        performItemClickWith(MagnificationMode.WINDOW);

        reshowPreferenceOnTheScreen();
        mDialogHelper.showDialog(MagnificationModePreferenceController.DIALOG_MAGNIFICATION_MODE);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                MagnificationMode.WINDOW);
    }

    @Test
    public void chooseWindowMode_tripleTapEnabled_showSwitchShortcutDialog() {
        enableTripleTap();
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        performItemClickWith(MagnificationMode.WINDOW);

        verify(mDialogHelper).showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
    }

    @Test
    public void chooseModeAll_modeAllInSettingsAndTripleTapEnabled_notShowShortcutDialog() {
        enableTripleTap();
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        performItemClickWith(MagnificationMode.ALL);

        verify(mDialogHelper, never()).showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
    }

    @Test
    public void onSwitchShortcutDialogPositiveButtonClicked_TripleTapEnabled_TripleTapDisabled() {
        enableTripleTap();

        mController.onSwitchShortcutDialogButtonClicked(CustomButton.POSITIVE);

        assertThat(MagnificationModePreferenceController.isTripleTapEnabled(mContext)).isFalse();
    }

    @Test
    public void getSummary_saveWindowScreen_shouldReturnWindowScreenSummary() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.WINDOW);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(
                        R.string.accessibility_magnification_area_settings_window_screen_summary));
    }

    @Test
    public void getSummary_saveAll_shouldReturnAllSummary() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(
                        R.string.accessibility_magnification_area_settings_all_summary));
    }

    private int getCheckedModeFromDialog() {
        final ListView listView = mController.mMagnificationModesListView;
        assertThat(listView).isNotNull();

        final int checkedPosition = listView.getCheckedItemPosition();
        final MagnificationModeInfo modeInfo =
                (MagnificationModeInfo) listView.getAdapter().getItem(checkedPosition);
        return modeInfo.mMagnificationMode;
    }

    private void performItemClickWith(@MagnificationMode int mode) {
        final ListView listView = mController.mMagnificationModesListView;
        assertThat(listView).isNotNull();

        int modeIndex = AdapterView.NO_ID;
        // Index 0 is header.
        for (int i = 1; i < listView.getAdapter().getCount(); i++) {
            final MagnificationModeInfo modeInfo =
                    (MagnificationModeInfo) listView.getAdapter().getItem(i);
            if (modeInfo.mMagnificationMode == mode) {
                modeIndex = i;
                break;
            }
        }
        if (modeIndex == AdapterView.NO_ID) {
            throw new RuntimeException("The mode is not in the list.");
        }

        listView.performItemClick(listView.getChildAt(modeIndex), modeIndex, modeIndex);
    }

    private void enableTripleTap() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, ON);
    }

    private void reshowPreferenceOnTheScreen() {
        final Bundle bundle = new Bundle();
        mController.onSaveInstanceState(bundle);
        showPreferenceOnTheScreen(bundle);
    }

    private void showPreferenceOnTheScreen(Bundle savedInstanceState) {
        mController.setDialogHelper(mDialogHelper);
        mController.onCreate(savedInstanceState);
        mController.displayPreference(mScreen);
    }

    private static class TestDialogHelper implements DialogCreatable,
            MagnificationModePreferenceController.DialogHelper {
        private DialogCreatable mDialogDelegate;

        @Override
        public void showDialog(int dialogId) {
            onCreateDialog(dialogId);
        }

        @Override
        public void setDialogDelegate(DialogCreatable delegate) {
            mDialogDelegate = delegate;
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            return mDialogDelegate.onCreateDialog(dialogId);
        }

        @Override
        public int getDialogMetricsCategory(int dialogId) {
            return mDialogDelegate.getDialogMetricsCategory(dialogId);
        }
    }
}
