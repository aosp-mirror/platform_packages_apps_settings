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

import static com.android.settings.accessibility.TextReadingPreviewController.PREVIEW_SAMPLE_RES_IDS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.viewpager.widget.ViewPager;

import com.android.settings.R;
import com.android.settings.display.PreviewPagerAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link TextReadingPreferenceFragment}.
 */
@RunWith(RobolectricTestRunner.class)
public class TextReadingPreviewPreferenceTest {

    private TextReadingPreviewPreference mTextReadingPreviewPreference;
    private PreferenceViewHolder mHolder;
    private ViewPager mViewPager;
    private PreviewPagerAdapter mPreviewPagerAdapter;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        final Configuration[] configurations = createConfigurations(PREVIEW_SAMPLE_RES_IDS.length);
        mTextReadingPreviewPreference = new TextReadingPreviewPreference(context);
        mPreviewPagerAdapter =
                spy(new PreviewPagerAdapter(context, /* isLayoutRtl= */ false,
                        PREVIEW_SAMPLE_RES_IDS, configurations));
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view =
                inflater.inflate(mTextReadingPreviewPreference.getLayoutResource(),
                        new LinearLayout(context), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
        mViewPager = view.findViewById(R.id.preview_pager);
    }

    @Test
    public void setPreviewerAdapter_success() {
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        assertThat(mViewPager.getAdapter()).isEqualTo(mPreviewPagerAdapter);
    }

    @Test
    public void setPreviewAdapterWithNull_resetCurrentItem() {
        final int currentItem = 2;
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.setCurrentItem(currentItem);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        mTextReadingPreviewPreference.setPreviewAdapter(null);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        assertThat(mTextReadingPreviewPreference.getCurrentItem()).isEqualTo(0);
    }

    @Test
    public void setCurrentItem_success() {
        final int currentItem = 1;
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        mTextReadingPreviewPreference.setCurrentItem(currentItem);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        assertThat(mViewPager.getCurrentItem()).isEqualTo(currentItem);
    }

    @Test(expected = NullPointerException.class)
    public void setCurrentItemBeforeSetPreviewAdapter_throwNPE() {
        final int currentItem = 5;

        mTextReadingPreviewPreference.setCurrentItem(currentItem);
    }

    @Test(expected = NullPointerException.class)
    public void updatePagerWithoutPreviewAdapter_throwNPE() {
        final int index = 1;

        mTextReadingPreviewPreference.notifyPreviewPagerChanged(index);
    }

    @Test
    public void notifyPreviewPager_setPreviewLayer() {
        final int index = 2;
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        mTextReadingPreviewPreference.notifyPreviewPagerChanged(index);

        verify(mPreviewPagerAdapter).setPreviewLayer(eq(index), anyInt(), anyInt(), anyBoolean());
    }

    private static Configuration[] createConfigurations(int count) {
        final Configuration[] configurations = new Configuration[count];
        for (int i = 0; i < count; i++) {
            configurations[i] = new Configuration();
        }

        return configurations;
    }
}
