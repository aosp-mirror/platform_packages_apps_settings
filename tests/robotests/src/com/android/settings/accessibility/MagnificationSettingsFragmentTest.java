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

import static com.android.settings.accessibility.MagnificationPreferenceFragment.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

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
    public void onCreateDialog_capabilitiesInBundle_matchCheckBoxStatus() {
        final Bundle windowModeSavedInstanceState = new Bundle();
        windowModeSavedInstanceState.putInt(EXTRA_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW);

        mFragment.onCreate(windowModeSavedInstanceState);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        assertThat(mFragment.mMagnifyFullScreenCheckBox.isChecked()).isFalse();
        assertThat(mFragment.mMagnifyWindowCheckBox.isChecked()).isTrue();
    }

    @Test
    public void onCreateDialog_capabilitiesInSettings_matchCheckBoxStatus() {
        MagnificationCapabilities.setCapabilities(mContext,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);
        mFragment.onCreate(Bundle.EMPTY);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        assertThat(mFragment.mMagnifyFullScreenCheckBox.isChecked()).isTrue();
        assertThat(mFragment.mMagnifyWindowCheckBox.isChecked()).isFalse();
    }

    @Test
    public void onCreateDialog_capabilitiesInSettingsAndBundle_matchBundleValueCheckBoxStatus() {
        final Bundle allModeSavedInstanceState = new Bundle();
        allModeSavedInstanceState.putInt(EXTRA_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_ALL);

        MagnificationCapabilities.setCapabilities(mContext,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW);
        mFragment.onCreate(allModeSavedInstanceState);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        assertThat(mFragment.mMagnifyFullScreenCheckBox.isChecked()).isTrue();
        assertThat(mFragment.mMagnifyWindowCheckBox.isChecked()).isTrue();
    }

    @Test
    public void onCreateDialog_emptySettingsAndBundle_matchDefaultValueCheckBoxStatus() {
        mFragment.onCreate(Bundle.EMPTY);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        // Compare to default Capabilities
        assertThat(mFragment.mMagnifyFullScreenCheckBox.isChecked()).isTrue();
        assertThat(mFragment.mMagnifyWindowCheckBox.isChecked()).isFalse();
    }

    @Test
    public void checkWindowModeCheckBox_tripleTapEnabled_showSwitchShortcutDialog() {
        final Bundle fullScreenModeSavedInstanceState = new Bundle();
        fullScreenModeSavedInstanceState.putInt(EXTRA_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);
        mFragment.onCreate(fullScreenModeSavedInstanceState);
        mFragment.onCreateDialog(MagnificationSettingsFragment.DIALOG_MAGNIFICATION_CAPABILITY);

        enableTripleTap();
        final View dialogWidowView = mFragment.mDialog.findViewById(R.id.magnify_window_screen);
        final View dialogWindowTextArea = dialogWidowView.findViewById(R.id.container);
        dialogWindowTextArea.performClick();

        verify(mFragment).showDialog(
                MagnificationSettingsFragment.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
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
