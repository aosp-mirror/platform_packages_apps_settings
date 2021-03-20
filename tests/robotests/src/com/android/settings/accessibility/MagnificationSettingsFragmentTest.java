/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import static com.android.settings.accessibility.MagnificationPreferenceFragment.ON;
import static com.android.settings.accessibility.MagnificationSettingsFragment.MagnificationModeInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link MagnificationSettingsFragment} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public class MagnificationSettingsFragmentTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private PreferenceManager mPreferenceManager;

    private static final String EXTRA_CAPABILITY =
            MagnificationSettingsFragment.EXTRA_CAPABILITY;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TestMagnificationSettingsFragment mFragment;

    @Before
    public void setUpFragment() {
        mFragment = spy(new TestMagnificationSettingsFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        doReturn(mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS)).when(
                mFragment).getChildFragmentManager();
        mContext.setTheme(R.style.Theme_AppCompat);
    }

    @Test
    public void onCreateDialog_capabilitiesInBundle_checkedModeInDialogIsExpected() {
        final Bundle windowModeSavedInstanceState = new Bundle();
        windowModeSavedInstanceState.putInt(EXTRA_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW);

        mFragment.onCreate(windowModeSavedInstanceState);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        assertThat(getChoseModeFromDialog()).isEqualTo(MagnificationMode.WINDOW);
    }

    @Test
    public void onCreateDialog_capabilitiesInSetting_checkedModeInDialogIsExpected() {
        MagnificationCapabilities.setCapabilities(mContext,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);
        mFragment.onCreate(Bundle.EMPTY);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        assertThat(getChoseModeFromDialog()).isEqualTo(MagnificationMode.FULLSCREEN);
    }

    @Test
    public void onCreateDialog_choseModeIsDifferentFromInSettings_ShowUsersChoseModeInDialog() {
        final Bundle allModeSavedInstanceState = new Bundle();
        allModeSavedInstanceState.putInt(EXTRA_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_ALL);

        MagnificationCapabilities.setCapabilities(mContext,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW);
        mFragment.onCreate(allModeSavedInstanceState);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        assertThat(getChoseModeFromDialog()).isEqualTo(MagnificationMode.ALL);
    }

    @Test
    public void onCreateDialog_emptySettingsAndBundle_checkedModeInDialogIsDefaultValue() {
        mFragment.onCreate(Bundle.EMPTY);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        assertThat(getChoseModeFromDialog()).isEqualTo(MagnificationMode.FULLSCREEN);
    }

    @Test
    public void chooseWindowMode_tripleTapEnabled_showSwitchShortcutDialog() {
        enableTripleTap();
        final Bundle fullScreenModeSavedInstanceState = new Bundle();
        fullScreenModeSavedInstanceState.putInt(EXTRA_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);
        mFragment.onCreate(fullScreenModeSavedInstanceState);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        performItemClickWith(MagnificationMode.WINDOW);

        verify(mFragment).showDialog(
                MagnificationSettingsFragment.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
    }

    @Test
    public void chooseModeAll_tripleTapEnabled_showSwitchShortcutDialog() {
        enableTripleTap();
        final Bundle fullScreenModeSavedInstanceState = new Bundle();
        fullScreenModeSavedInstanceState.putInt(EXTRA_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);
        mFragment.onCreate(fullScreenModeSavedInstanceState);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        performItemClickWith(MagnificationMode.ALL);

        verify(mFragment).showDialog(
                MagnificationSettingsFragment.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
    }

    @Test
    public void chooseWindowMode_WindowModeInSettingsAndTripleTapEnabled_notShowShortCutDialog() {
        enableTripleTap();
        MagnificationCapabilities.setCapabilities(mContext,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW);
        mFragment.onCreate(Bundle.EMPTY);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        performItemClickWith(MagnificationMode.WINDOW);

        verify(mFragment, never()).showDialog(
                MagnificationSettingsFragment.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
    }

    private int getChoseModeFromDialog() {
        final ListView listView = mFragment.mMagnificationModesListView;
        assertThat(listView).isNotNull();

        final int checkedPosition = listView.getCheckedItemPosition();
        final MagnificationModeInfo modeInfo =
                (MagnificationModeInfo) listView.getAdapter().getItem(
                        checkedPosition);
        return modeInfo.mMagnificationMode;
    }

    private void performItemClickWith(@MagnificationMode int mode) {
        final ListView listView = mFragment.mMagnificationModesListView;
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

    public static class TestMagnificationSettingsFragment extends MagnificationSettingsFragment {
        public TestMagnificationSettingsFragment() {}

        @Override
        protected void showDialog(int dialogId) {
            super.showDialog(dialogId);
        }
    }
}
