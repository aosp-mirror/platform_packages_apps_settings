/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class RtlCompatibleViewPagerTest {

    private Locale mLocaleEn;
    private Locale mLocaleHe;
    private RtlCompatibleViewPager mViewPager;

    @Before
    public void setUp() {
        mViewPager = new RtlCompatibleViewPager(RuntimeEnvironment.application);
        mViewPager.setAdapter(new ViewPagerAdapter());
        mLocaleEn = new Locale("en");
        mLocaleHe = new Locale("he");
    }

    @Test
    public void testGetCurrentItem_shouldMaintainIndexDuringLocaleChange() {
        testRtlCompatibleInner(0);
        testRtlCompatibleInner(1);
    }

    private void testRtlCompatibleInner(int currentItem) {
        // Set up the environment
        Locale.setDefault(mLocaleEn);
        mViewPager.setCurrentItem(currentItem);

        assertThat(TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()))
                .isEqualTo(View.LAYOUT_DIRECTION_LTR);
        assertThat(mViewPager.getCurrentItem()).isEqualTo(currentItem);

        // Simulate to change the language to RTL
        Parcelable savedInstance = mViewPager.onSaveInstanceState();
        Locale.setDefault(mLocaleHe);
        mViewPager.onRestoreInstanceState(savedInstance);

        assertThat(TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()))
                .isEqualTo(View.LAYOUT_DIRECTION_RTL);
        assertThat(mViewPager.getCurrentItem()).isEqualTo(currentItem);
    }

    /**
     * Test viewpager adapter, uses 2 view to test it
     */
    private static final class ViewPagerAdapter extends PagerAdapter {

        private static final int ITEM_COUNT = 2;

        private ViewPagerAdapter() {
        }

        @Override
        public int getCount() {
            return ITEM_COUNT;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }
}
