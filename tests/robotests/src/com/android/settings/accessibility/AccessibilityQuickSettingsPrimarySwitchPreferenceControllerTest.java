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

import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_SAVED_QS_TOOLTIP_RESHOW;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

/**
 * Tests for {@link AccessibilityQuickSettingsPrimarySwitchPreferenceController}.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class AccessibilityQuickSettingsPrimarySwitchPreferenceControllerTest {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_TILE_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final ComponentName PLACEHOLDER_TILE_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME);
    private static final CharSequence PLACEHOLDER_TILE_CONTENT =
            PLACEHOLDER_TILE_CLASS_NAME + ".tile.content";
    private static final String TEST_KEY = "test_pref_key";
    private static final String TEST_TITLE = "test_title";

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private TestAccessibilityQuickSettingsPrimarySwitchPreferenceController mController;
    private PrimarySwitchPreference mPreference;
    private TestFragment mFragment;
    private PreferenceScreen mScreen;
    private PreferenceViewHolder mHolder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mFragment = spy(new TestFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();

        mPreference = new PrimarySwitchPreference(mContext);
        mPreference.setKey(TEST_KEY);
        mPreference.setTitle(TEST_TITLE);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mHolder = PreferenceViewHolder.createInstanceForTests(inflater.inflate(
                com.android.settingslib.widget.preference.twotarget.R.layout.preference_two_target, null));
        LinearLayout mWidgetView = mHolder.itemView.findViewById(android.R.id.widget_frame);
        inflater.inflate(R.layout.preference_widget_primary_switch, mWidgetView, true);
        mPreference.onBindViewHolder(mHolder);

        mController = new TestAccessibilityQuickSettingsPrimarySwitchPreferenceController(mContext,
                TEST_KEY);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void setChecked_showTooltipView() {
        mController.displayPreference(mScreen);

        mController.setChecked(true);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    public void setChecked_notCallDisplayPreference_notShowTooltipView() {
        // Simulates the slice highlight menu that does not call {@link #displayPreference} before
        // {@link #setChecked} called.
        mController.setChecked(true);

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    public void setChecked_tooltipViewShown_notShowTooltipView() {
        mController.displayPreference(mScreen);
        mController.setChecked(true);
        getLatestPopupWindow().dismiss();
        mController.setChecked(false);

        mController.setChecked(true);

        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void restoreValueFromSavedInstanceState_showTooltipView() {
        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, /* value= */ true);
        mController.onCreate(savedInstanceState);

        mController.displayPreference(mScreen);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    public static class TestAccessibilityQuickSettingsPrimarySwitchPreferenceController
            extends AccessibilityQuickSettingsPrimarySwitchPreferenceController {

        public TestAccessibilityQuickSettingsPrimarySwitchPreferenceController(Context context,
                String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        ComponentName getTileComponentName() {
            return PLACEHOLDER_TILE_COMPONENT_NAME;
        }

        @Override
        CharSequence getTileTooltipContent() {
            return PLACEHOLDER_TILE_CONTENT;
        }
    }

    private static class TestFragment extends SettingsPreferenceFragment {

        @Override
        protected boolean shouldSkipForInitialSUW() {
            return false;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }
}
