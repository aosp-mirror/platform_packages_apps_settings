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

package com.android.settings.accessibility.shortcuts;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Test for {@link ShortcutOptionPreferenceTest}
 */
@RunWith(RobolectricTestRunner.class)
public class ShortcutOptionPreferenceTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ShortcutOptionPreference mShortcutOptionPreference;
    private PreferenceViewHolder mViewHolder;
    private ImageView mImageView;

    @Before
    public void setUp() {
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        int layoutId =  mShortcutOptionPreference.getLayoutResource();
        View itemView = LayoutInflater.from(mContext).inflate(layoutId, /* root= */null);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(itemView);
        mImageView = (ImageView) mViewHolder.findViewById(R.id.image);
    }

    @Test
    public void bindViewHolder_imageResNotSet_shouldHideImageView() {
        mShortcutOptionPreference.onBindViewHolder(mViewHolder);

        assertThat(mImageView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewHolder_imageResIdSet_shouldShowImageView() {
        mShortcutOptionPreference.setIntroImageResId(R.drawable.a11y_shortcut_type_hardware);

        mShortcutOptionPreference.onBindViewHolder(mViewHolder);

        assertThat(mImageView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewHolder_imageRawResIdSet_shouldShowImageView() {
        mShortcutOptionPreference.setIntroImageRawResId(
                com.android.settings.R.raw.accessibility_color_inversion_banner);

        mShortcutOptionPreference.onBindViewHolder(mViewHolder);

        assertThat(mImageView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewHolder_shouldUpdateSummaryTextLineHeight() {
        assertThat(mShortcutOptionPreference.getSummaryTextLineHeight()).isEqualTo(0);

        mShortcutOptionPreference.onBindViewHolder(mViewHolder);

        assertThat(mShortcutOptionPreference.getSummaryTextLineHeight()).isNotEqualTo(0);
    }
}
