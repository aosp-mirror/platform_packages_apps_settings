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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.viewpager.widget.ViewPager;

import com.android.settings.R;
import com.android.settings.display.PreviewPagerAdapter;

import org.junit.Assume;
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
    private int mPreviewSampleCount;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        final int[] previewSamples = TextReadingPreviewController.getPreviewSampleLayouts(context);
        mPreviewSampleCount = previewSamples.length;
        final Configuration[] configurations = createConfigurations(mPreviewSampleCount);
        mTextReadingPreviewPreference = new TextReadingPreviewPreference(context);
        mPreviewPagerAdapter =
                spy(new PreviewPagerAdapter(context, /* isLayoutRtl= */ false,
                        previewSamples, configurations));
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
        final int currentItem = mPreviewSampleCount - 1;
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.setCurrentItem(currentItem);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        mTextReadingPreviewPreference.setPreviewAdapter(null);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        assertThat(mTextReadingPreviewPreference.getCurrentItem()).isEqualTo(0);
    }

    @Test
    public void setCurrentItem_success() {
        final int currentItem = mPreviewSampleCount - 1;
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        mTextReadingPreviewPreference.setCurrentItem(currentItem);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        assertThat(mViewPager.getCurrentItem()).isEqualTo(currentItem);
    }

    @Test(expected = NullPointerException.class)
    public void setCurrentItemBeforeSetPreviewAdapter_throwNPE() {
        final int currentItem = mPreviewSampleCount + 2;

        mTextReadingPreviewPreference.setCurrentItem(currentItem);
    }

    @Test(expected = NullPointerException.class)
    public void updatePagerWithoutPreviewAdapter_throwNPE() {
        final int index = mPreviewSampleCount - 1;

        mTextReadingPreviewPreference.notifyPreviewPagerChanged(index);
    }

    @Test
    public void notifyPreviewPager_setPreviewLayer() {
        // The preview pager cannot switch page if there is only one preview layout, so skip the
        // test if so
        Assume.assumeTrue(mPreviewSampleCount > 1);

        final int index = mPreviewSampleCount - 1;
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        mTextReadingPreviewPreference.notifyPreviewPagerChanged(index);

        verify(mPreviewPagerAdapter).setPreviewLayer(eq(index), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    public void afterPagerChange_updateCurrentItem() {
        final int currentItem = mPreviewSampleCount - 1;
        mTextReadingPreviewPreference.setPreviewAdapter(mPreviewPagerAdapter);
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);

        mViewPager.setCurrentItem(currentItem);

        assertThat(mTextReadingPreviewPreference.getCurrentItem()).isEqualTo(currentItem);
    }

    @Test
    public void adjustPaddings_setMinPaddingsLessThanXMLValue_paddingsNotIncreased() {
        // get the default xml padding value
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);
        final FrameLayout previewLayout = (FrameLayout) mHolder.itemView;
        final LinearLayout backgroundView = previewLayout.findViewById(R.id.preview_background);

        final int currentLayoutPaddingStart = previewLayout.getPaddingStart();
        final int currentBackgroundPaddingStart = backgroundView.getPaddingStart();
        mTextReadingPreviewPreference.setLayoutMinHorizontalPadding(
                currentLayoutPaddingStart - 10);
        mTextReadingPreviewPreference.setBackgroundMinHorizontalPadding(
                currentBackgroundPaddingStart - 10);
        mTextReadingPreviewPreference.adjustPaddings(previewLayout, backgroundView);

        assertThat(previewLayout.getPaddingStart()).isEqualTo(currentLayoutPaddingStart);
        assertThat(backgroundView.getPaddingStart()).isEqualTo(currentBackgroundPaddingStart);
    }

    @Test
    public void adjustPaddings_setMinPaddingsLargerThanXMLValue_paddingsIncreased() {
        // get the default xml padding value
        mTextReadingPreviewPreference.onBindViewHolder(mHolder);
        final FrameLayout previewLayout = (FrameLayout) mHolder.itemView;
        final LinearLayout backgroundView = previewLayout.findViewById(R.id.preview_background);

        final int currentLayoutPaddingStart = previewLayout.getPaddingStart();
        final int currentBackgroundPaddingStart = backgroundView.getPaddingStart();
        mTextReadingPreviewPreference.setLayoutMinHorizontalPadding(
                currentLayoutPaddingStart + 10);
        mTextReadingPreviewPreference.setBackgroundMinHorizontalPadding(
                currentBackgroundPaddingStart + 10);
        mTextReadingPreviewPreference.adjustPaddings(previewLayout, backgroundView);

        assertThat(previewLayout.getPaddingStart()).isEqualTo(currentLayoutPaddingStart + 10);
        assertThat(backgroundView.getPaddingStart()).isEqualTo(currentBackgroundPaddingStart + 10);
    }

    private static Configuration[] createConfigurations(int count) {
        final Configuration[] configurations = new Configuration[count];
        for (int i = 0; i < count; i++) {
            configurations[i] = new Configuration();
        }

        return configurations;
    }
}
