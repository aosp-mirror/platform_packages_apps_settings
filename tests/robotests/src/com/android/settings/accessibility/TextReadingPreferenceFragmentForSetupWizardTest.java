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

import static com.android.settings.accessibility.TextReadingPreferenceFragment.PREVIEW_KEY;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.RESET_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link TextReadingPreferenceFragmentForSetupWizard}. */
@RunWith(RobolectricTestRunner.class)
public class TextReadingPreferenceFragmentForSetupWizardTest {

    @Rule
    public final MockitoRule mMockito = MockitoJUnit.rule();

    @Mock
    private GlifPreferenceLayout mGlifLayoutView;

    @Mock
    private FooterBarMixin mFooterBarMixin;

    @Mock
    private FragmentActivity mActivity;

    @Mock
    private TextReadingPreviewPreference mPreviewPreference;

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TextReadingPreferenceFragmentForSetupWizard mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new TextReadingPreferenceFragmentForSetupWizard());
        final LayoutPreference resetPreference =
                new LayoutPreference(mContext, R.layout.accessibility_text_reading_reset_button);
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mock(LifecycleOwner.class)).when(mFragment).getViewLifecycleOwner();
        doReturn(resetPreference).when(mFragment).findPreference(RESET_KEY);
        doReturn(mPreviewPreference).when(mFragment).findPreference(PREVIEW_KEY);
        doReturn(mFooterBarMixin).when(mGlifLayoutView).getMixin(FooterBarMixin.class);
    }

    @Test
    public void setHeaderText_onViewCreated_verifyAction() {
        final String title = "title";
        doReturn(title).when(mContext).getString(
                R.string.accessibility_text_reading_options_title);

        mFragment.onViewCreated(mGlifLayoutView, null);

        verify(mGlifLayoutView).setHeaderText(title);
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.SUW_ACCESSIBILITY_TEXT_READING_OPTIONS);
    }

    @Test
    public void getHelpResource_shouldNotHaveHelpResource() {
        assertThat(mFragment.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void onViewCreated_verifyAction() {
        mFragment.onViewCreated(mGlifLayoutView, null);

        verify(mFooterBarMixin).setPrimaryButton(any());
        verify(mFooterBarMixin).setSecondaryButton(any());
    }

    @Test
    public void adjustPreviewPaddingsForSetupWizard_setPreviewLayoutPaddings() {
        mFragment.adjustPreviewPaddingsForSetupWizard();

        verify(mPreviewPreference).setLayoutMinHorizontalPadding(anyInt());
        verify(mPreviewPreference).setBackgroundMinHorizontalPadding(anyInt());
    }
}
