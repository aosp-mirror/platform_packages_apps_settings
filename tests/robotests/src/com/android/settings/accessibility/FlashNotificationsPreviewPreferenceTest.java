/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.Utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class FlashNotificationsPreviewPreferenceTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FlashNotificationsPreviewPreference mFlashNotificationsPreviewPreference;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                        R.layout.flash_notification_preview_preference, null));
        mFlashNotificationsPreviewPreference = new FlashNotificationsPreviewPreference(mContext);
    }

    @Test
    public void setEnabled_true_verifyEnabledUi() {
        @ColorInt final int textColorEnabled = ((TextView) mPreferenceViewHolder.findViewById(
                android.R.id.title)).getCurrentTextColor();

        mFlashNotificationsPreviewPreference.setEnabled(true);
        mFlashNotificationsPreviewPreference.onBindViewHolder(mPreferenceViewHolder);

        final View frame = mPreferenceViewHolder.findViewById(R.id.frame);
        final int backgroundResId = Shadows.shadowOf(frame.getBackground()).getCreatedFromResId();
        assertThat(backgroundResId).isEqualTo(
                com.android.settingslib.widget.mainswitch.R.drawable.settingslib_switch_bar_bg_on);
        final TextView title = (TextView) mPreferenceViewHolder.findViewById(android.R.id.title);
        assertThat(title.getAlpha()).isEqualTo(1f);
        assertThat(title.getCurrentTextColor()).isEqualTo(textColorEnabled);
    }

    @Test
    public void setEnabled_false_verifyDisabledUi() {
        @ColorInt final int textColorDisabled = Utils.getColorAttrDefaultColor(mContext,
                android.R.attr.textColorPrimary);

        mFlashNotificationsPreviewPreference.setEnabled(false);
        mFlashNotificationsPreviewPreference.onBindViewHolder(mPreferenceViewHolder);

        final View frame = mPreferenceViewHolder.findViewById(R.id.frame);
        final int backgroundResId = Shadows.shadowOf(frame.getBackground()).getCreatedFromResId();
        assertThat(backgroundResId).isEqualTo(R.drawable.switch_bar_bg_disabled);
        final TextView title = (TextView) mPreferenceViewHolder.findViewById(android.R.id.title);
        assertThat(title.getAlpha()).isEqualTo(0.38f);
        assertThat(title.getCurrentTextColor()).isEqualTo(textColorDisabled);
    }
}