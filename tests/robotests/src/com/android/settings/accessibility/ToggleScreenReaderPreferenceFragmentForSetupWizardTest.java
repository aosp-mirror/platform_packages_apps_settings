/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.widget.TopIntroPreference;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link ToggleScreenReaderPreferenceFragmentForSetupWizard}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ToggleScreenReaderPreferenceFragmentForSetupWizardTest {

    private static final String TEST_TITLE = "test_title";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private SettingsActivity mActivity;
    @Mock
    private GlifPreferenceLayout mGlifLayoutView;
    @Mock
    private SettingsMainSwitchBar mSwitchBar;
    @Mock
    private FooterBarMixin mFooterBarMixin;
    private ToggleScreenReaderPreferenceFragmentForSetupWizard mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new TestToggleScreenReaderPreferenceFragmentForSetupWizard(mContext));
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mock(LifecycleOwner.class)).when(mFragment).getViewLifecycleOwner();
        when(mActivity.getSwitchBar()).thenReturn(mSwitchBar);
        doReturn(mFooterBarMixin).when(mGlifLayoutView).getMixin(FooterBarMixin.class);
    }

    @Test
    public void onViewCreated_verifyAction() {
        mFragment.onViewCreated(mGlifLayoutView, null);

        verify(mGlifLayoutView).setHeaderText(TEST_TITLE);
        verify(mGlifLayoutView).setDescriptionText(
                mContext.getString(R.string.talkback_summary));
        verify(mGlifLayoutView).setDividerInsets(Integer.MAX_VALUE, 0);
        verify(mFooterBarMixin).setPrimaryButton(any());
        assertThat(mFragment.mTopIntroPreference.isVisible()).isFalse();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_READER);
    }

    private static class TestToggleScreenReaderPreferenceFragmentForSetupWizard
            extends ToggleScreenReaderPreferenceFragmentForSetupWizard {

        private final Context mContext;
        private final PreferenceManager mPreferenceManager;

        TestToggleScreenReaderPreferenceFragmentForSetupWizard(Context context) {
            super();
            mContext = context;
            mPreferenceManager = new PreferenceManager(context);
            mPreferenceManager.setPreferences(mPreferenceManager.createPreferenceScreen(context));
            mTopIntroPreference = new TopIntroPreference(context);
            mToggleServiceSwitchPreference = new SettingsMainSwitchPreference(context);
            Bundle bundle = new Bundle();
            bundle.putString(AccessibilitySettings.EXTRA_TITLE, TEST_TITLE);
            setArguments(bundle);
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
    }
}
