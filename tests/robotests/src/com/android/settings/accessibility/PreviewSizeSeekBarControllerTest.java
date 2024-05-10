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

import static com.android.internal.accessibility.AccessibilityShortcutController.FONT_SIZE_COMPONENT_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.view.LayoutInflater;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.widget.LabeledSeekBarPreference;
import com.android.settingslib.testutils.shadow.ShadowInteractionJankMonitor;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

/**
 * Tests for {@link PreviewSizeSeekBarController}.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {ShadowInteractionJankMonitor.class})
public class PreviewSizeSeekBarControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> rule =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);
    private static final String FONT_SIZE_KEY = "font_size";
    private static final String KEY_SAVED_QS_TOOLTIP_RESHOW = "qs_tooltip_reshow";
    private Activity mContext;
    private PreviewSizeSeekBarController mSeekBarController;
    private FontSizeData mFontSizeData;
    private LabeledSeekBarPreference mSeekBarPreference;

    private PreferenceScreen mPreferenceScreen;
    private TestFragment mFragment;
    private PreferenceViewHolder mHolder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    private SeekBar mSeekBar;

    @Mock
    private PreviewSizeSeekBarController.ProgressInteractionListener mInteractionListener;

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowInteractionJankMonitor.reset();

        rule.getScenario().onActivity(activity -> mContext = activity);
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mFragment = spy(new TestFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        mPreferenceScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mPreferenceScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        mSeekBarPreference = spy(new LabeledSeekBarPreference(mContext, /* attrs= */ null));
        mSeekBarPreference.setKey(FONT_SIZE_KEY);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mHolder = spy(PreferenceViewHolder.createInstanceForTests(inflater.inflate(
                R.layout.preference_labeled_slider, null)));
        mSeekBar = spy(new SeekBar(mContext));
        doReturn(mSeekBar).when(mHolder).findViewById(com.android.internal.R.id.seekbar);
        mSeekBarPreference.onBindViewHolder(mHolder);

        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);

        mFontSizeData = new FontSizeData(mContext);
        mSeekBarController =
                new PreviewSizeSeekBarController(mContext, FONT_SIZE_KEY, mFontSizeData) {
                    @Override
                    ComponentName getTileComponentName() {
                        return FONT_SIZE_COMPONENT_NAME;
                    }

                    @Override
                    CharSequence getTileTooltipContent() {
                        return mContext.getText(
                                R.string.accessibility_font_scaling_auto_added_qs_tooltip_content);
                    }
                };
        mSeekBarController.setInteractionListener(mInteractionListener);
        when(mPreferenceScreen.findPreference(mSeekBarController.getPreferenceKey())).thenReturn(
                mSeekBarPreference);
    }

    @Test
    public void initMax_matchResult() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);

        mSeekBarController.displayPreference(mPreferenceScreen);

        assertThat(mSeekBarPreference.getMax()).isEqualTo(
                mFontSizeData.getValues().size() - 1);
    }

    @Test
    public void initProgress_matchResult() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);

        mSeekBarController.displayPreference(mPreferenceScreen);

        verify(mSeekBarPreference).setProgress(mFontSizeData.getInitialIndex());
    }

    @Test
    public void resetToDefaultState_matchResult() {
        final int defaultProgress =
                mFontSizeData.getValues().indexOf(mFontSizeData.getDefaultValue());
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);

        mSeekBarController.displayPreference(mPreferenceScreen);
        mSeekBarPreference.setProgress(defaultProgress + 1);
        mSeekBarController.resetState();

        assertThat(mSeekBarPreference.getProgress()).isEqualTo(defaultProgress);
    }

    @Test
    public void resetState_verifyOnProgressChanged() {
        mSeekBarController.displayPreference(mPreferenceScreen);
        mSeekBarController.resetState();

        verify(mInteractionListener).onProgressChanged();
    }

    @Test
    public void onProgressChanged_verifyNotifyPreferenceChanged() {
        mSeekBarController.displayPreference(mPreferenceScreen);

        mSeekBarPreference.setProgress(mSeekBarPreference.getMax());
        mSeekBarPreference.onProgressChanged(new SeekBar(mContext), /* progress= */
                0, /* fromUser= */ false);

        verify(mInteractionListener).notifyPreferenceChanged();
    }

    @Test
    public void onProgressChanged_showTooltipView() {
        mSeekBarController.displayPreference(mPreferenceScreen);

        // Simulate changing the progress for the first time
        int newProgress = (mSeekBarPreference.getProgress() != 0) ? 0 : mSeekBarPreference.getMax();
        mSeekBarPreference.setProgress(newProgress);
        mSeekBarPreference.onProgressChanged(new SeekBar(mContext),
                newProgress,
                /* fromUser= */ false);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_REMOVE_QS_TOOLTIP_IN_SUW)
    public void onProgressChanged_inSuw_toolTipShouldNotShown() {
        Intent intent = mContext.getIntent();
        intent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        mContext.setIntent(intent);
        mSeekBarController.displayPreference(mPreferenceScreen);

        // Simulate changing the progress for the first time
        int newProgress = (mSeekBarPreference.getProgress() != 0) ? 0 : mSeekBarPreference.getMax();
        mSeekBarPreference.setProgress(newProgress);
        mSeekBarPreference.onProgressChanged(new SeekBar(mContext),
                newProgress,
                /* fromUser= */ false);

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    public void onProgressChanged_tooltipViewHasBeenShown_notShowTooltipView() {
        mSeekBarController.displayPreference(mPreferenceScreen);
        // Simulate changing the progress for the first time
        int newProgress = (mSeekBarPreference.getProgress() != 0) ? 0 : mSeekBarPreference.getMax();
        mSeekBarPreference.setProgress(newProgress);
        mSeekBarPreference.onProgressChanged(new SeekBar(mContext),
                newProgress,
                /* fromUser= */ false);
        getLatestPopupWindow().dismiss();

        // Simulate progress changing for the second time
        newProgress = (mSeekBarPreference.getProgress() != 0) ? 0 : mSeekBarPreference.getMax();
        mSeekBarPreference.setProgress(newProgress);
        mSeekBarPreference.onProgressChanged(new SeekBar(mContext),
                newProgress,
                /* fromUser= */ false);

        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void restoreValueFromSavedInstanceState_showTooltipView() {
        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, /* value= */ true);
        mSeekBarController.onCreate(savedInstanceState);

        mSeekBarController.displayPreference(mPreferenceScreen);

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    public void onProgressChanged_setCorrespondingCustomizedStateDescription() {
        String[] stateLabels = new String[]{"1", "2", "3", "4", "5"};
        mSeekBarController.setProgressStateLabels(stateLabels);
        mSeekBarController.displayPreference(mPreferenceScreen);

        int progress = 3;
        mSeekBarPreference.setProgress(progress);
        mSeekBarPreference.onProgressChanged(mSeekBar,
                progress,
                /* fromUser= */ false);

        verify(mSeekBarPreference).setSeekBarStateDescription(stateLabels[progress]);
        assertThat(mSeekBar.getStateDescription().toString()).isEqualTo(stateLabels[progress]);
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
