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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.ToggleColorInversionPreferenceFragment.KEY_USE_SERVICE_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.Flags;
import android.widget.PopupWindow;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

/** Tests for {@link ToggleColorInversionPreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowFragment.class)
public class ToggleColorInversionPreferenceFragmentTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TestToggleColorInversionPreferenceFragment mFragment;
    private PreferenceScreen mScreen;
    private SettingsMainSwitchPreference mSwitchPreference;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    @Mock
    private FragmentActivity mActivity;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestToggleColorInversionPreferenceFragment(mContext));
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getContentResolver()).thenReturn(mContext.getContentResolver());

        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mScreen.findPreference(KEY_USE_SERVICE_PREFERENCE))
                .thenReturn(mFragment.mToggleServiceSwitchPreference);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        mSwitchPreference = mScreen.findPreference(KEY_USE_SERVICE_PREFERENCE);
    }

    @Test
    public void onResume_colorCorrectEnabled_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, ON);
        mFragment.onAttach(mContext);
        mFragment.onCreate(Bundle.EMPTY);

        mFragment.onResume();

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void onResume_colorCorrectDisabled_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF);
        mFragment.onAttach(mContext);
        mFragment.onCreate(Bundle.EMPTY);

        mFragment.onResume();

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onResume_colorCorrectEnabled_switchPreferenceChecked_notShowTooltips() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, ON);
        mSwitchPreference.setChecked(true);
        mFragment.onAttach(mContext);
        mFragment.onCreate(Bundle.EMPTY);

        mFragment.onResume();

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void onPreferenceToggled_colorCorrectDisabled_shouldReturnTrueAndShowTooltipView() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF);
        mSwitchPreference.setChecked(false);
        mFragment.onAttach(mContext);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onViewCreated(mFragment.getView(), Bundle.EMPTY);

        mFragment.onPreferenceToggled(mSwitchPreference.getKey(), true);

        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF) == ON;
        assertThat(isEnabled).isTrue();
        assertThat(getLatestPopupWindow()).isNotNull();
        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    public void onPreferenceToggled_colorCorrectEnabled_shouldReturnFalseAndNotShowTooltipView() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, ON);
        mSwitchPreference.setChecked(true);
        mFragment.onAttach(mContext);
        mFragment.onCreate(Bundle.EMPTY);

        mFragment.onPreferenceToggled(mSwitchPreference.getKey(), false);

        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF) == ON;
        assertThat(isEnabled).isFalse();
        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_color_inversion_settings);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        assertThat(mFragment.getHelpResource()).isEqualTo(R.string.help_url_color_inversion);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = ToggleColorInversionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_color_inversion_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }

    private static class TestToggleColorInversionPreferenceFragment extends
            ToggleColorInversionPreferenceFragment {
        private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
        private static final String PLACEHOLDER_CLASS_NAME =
                PLACEHOLDER_PACKAGE_NAME + ".placeholder";
        private static final ComponentName PLACEHOLDER_COMPONENT_NAME = new ComponentName(
                PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_CLASS_NAME);
        private static final String PLACEHOLDER_TILE_TOOLTIP_CONTENT =
                PLACEHOLDER_PACKAGE_NAME + "tooltip_content";

        TestToggleColorInversionPreferenceFragment(Context context) {
            super();
            mComponentName = PLACEHOLDER_COMPONENT_NAME;
            final SettingsMainSwitchPreference switchPreference =
                    new SettingsMainSwitchPreference(context);
            switchPreference.setKey(KEY_USE_SERVICE_PREFERENCE);
            mToggleServiceSwitchPreference = switchPreference;
            setArguments(new Bundle());
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // do nothing
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return mock(View.class);
        }

        @Override
        protected void updateShortcutPreference() {
            // UI related function, do nothing in tests
        }

        @Override
        ComponentName getTileComponentName() {
            return PLACEHOLDER_COMPONENT_NAME;
        }

        @Override
        protected CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type) {
            return PLACEHOLDER_TILE_TOOLTIP_CONTENT;
        }

        @Override
        public View getView() {
            return mock(View.class);
        }
    }
}
