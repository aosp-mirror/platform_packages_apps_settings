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

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;

import static com.android.settings.accessibility.AccessibilityEditDialogUtils.CustomButton;
import static com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import static com.android.settings.accessibility.MagnificationModePreferenceController.MagnificationModeInfo;
import static com.android.settings.accessibility.MagnificationPreferenceFragment.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;


@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public class MagnificationModePreferenceControllerTest {
    private static final String PREF_KEY = "screen_magnification_mode";
    private static final int MAGNIFICATION_MODE_DEFAULT = MagnificationMode.ALL;

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private PreferenceScreen mScreen;
    private Context mContext;
    private TestMagnificationSettingsFragment mFragment;
    private MagnificationModePreferenceController mController;
    private Preference mModePreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new MagnificationModePreferenceController(mContext, PREF_KEY);
        mScreen = spy(new PreferenceScreen(mContext, null));
        mModePreference = new Preference(mContext);
        mFragment = spy(new TestMagnificationSettingsFragment(mController));

        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        doReturn(mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS)).when(
                mFragment).getChildFragmentManager();
        mContext.setTheme(R.style.Theme_AppCompat);
        doReturn(mModePreference).when(mScreen).findPreference(PREF_KEY);
        MagnificationCapabilities.setCapabilities(mContext, MAGNIFICATION_MODE_DEFAULT);
        showPreferenceOnTheScreen(null);
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);
    }

    @Test
    public void settingsModeIsDefault_checkedModeInDialogIsDefault() {
        assertThat(getCheckedModeFromDialog()).isEqualTo(
                MAGNIFICATION_MODE_DEFAULT);
    }

    @Test
    public void choseModeIsDifferentFromInSettings_checkedModeInDialogIsExpected() {
        performItemClickWith(MagnificationMode.WINDOW);

        assertThat(getCheckedModeFromDialog()).isEqualTo(MagnificationMode.WINDOW);
    }

    @Test
    public void dialogIsReCreated_settingsModeIsAllAndChoseWindowMode_checkedModeIsWindow() {
        showPreferenceOnTheScreen(null);
        performItemClickWith(MagnificationMode.WINDOW);

        reshowPreferenceOnTheScreen();
        mFragment.showDialog(MagnificationModePreferenceController.DIALOG_MAGNIFICATION_MODE);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                MagnificationMode.WINDOW);
    }

    @Test
    public void chooseWindowMode_tripleTapEnabled_showSwitchShortcutDialog() {
        enableTripleTap();

        performItemClickWith(MagnificationMode.WINDOW);

        verify(mFragment).showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
    }

    @Test
    public void chooseModeAll_modeAllInSettingsAndTripleTapEnabled_notShowShortcutDialog() {
        enableTripleTap();
        MagnificationCapabilities.setCapabilities(mContext,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_ALL);
        mFragment.onCreate(Bundle.EMPTY);
        mFragment.onCreateDialog(MagnificationModePreferenceController.DIALOG_MAGNIFICATION_MODE);

        performItemClickWith(MagnificationMode.ALL);

        verify(mFragment, never()).showDialog(
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
        mFragment.onSaveInstanceState(bundle);
        mFragment.onDetach();
        showPreferenceOnTheScreen(bundle);

    }

    private void showPreferenceOnTheScreen(Bundle savedInstanceState) {
        mFragment.onAttach(mContext);
        mFragment.onCreate(savedInstanceState);
        mController.displayPreference(mScreen);
    }

    private static class TestMagnificationSettingsFragment extends MagnificationSettingsFragment {

        TestMagnificationSettingsFragment(AbstractPreferenceController... controllers) {
            // Add given controllers for injection. Although controllers will be added in
            // onAttach(). use(AbstractPreferenceController.class) returns the first added one.
            for (int i = 0; i < controllers.length; i++) {
                addPreferenceController(controllers[i]);
            }
        }

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            // Simulate the observable behaviour because ShadowDashFragment doesn't call
            // super.create.

            getSettingsLifecycle().onCreate(icicle);
            getSettingsLifecycle().handleLifecycleEvent(ON_CREATE);
        }

        @Override
        protected void showDialog(int dialogId) {
            super.showDialog(dialogId);
            // In current fragment architecture, we could assume onCreateDialog is called directly.
            onCreateDialog(dialogId);
        }

        @Override
        protected void addPreferenceController(AbstractPreferenceController controller) {
            super.addPreferenceController(controller);
        }
    }
}
