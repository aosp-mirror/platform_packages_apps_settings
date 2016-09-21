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

import android.annotation.Nullable;
import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.TestConfig;
import com.android.settings.shadow.ShadowTextUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

import java.util.Locale;

import android.support.v4.view.PagerAdapter;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RtlCompatibleViewPagerTest {
    private ViewPagerTestActivity mTestActivity;
    private RtlCompatibleViewPager mTestViewPager;
    private Locale mLocaleEn;
    private Locale mLocaleHe;

    @Before
    public void setUp() {
        mTestActivity = Robolectric.setupActivity(ViewPagerTestActivity.class);
        mTestViewPager = mTestActivity.getViewPager();
        mLocaleEn = new Locale("en");
        mLocaleHe = new Locale("he");
    }

    @Test
    @Config(shadows = {ShadowTextUtils.class})
    public void testRtlCompatible() {
        testRtlCompatibleInner(0);
        testRtlCompatibleInner(1);
    }

    private void testRtlCompatibleInner(int currentItem) {
        // Set up the environment
        Locale.setDefault(mLocaleEn);
        assertThat(TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()))
                .isEqualTo(View.LAYOUT_DIRECTION_LTR);
        mTestViewPager.setCurrentItem(currentItem);
        assertThat(mTestViewPager.getCurrentItem()).isEqualTo(currentItem);

        // Simulate to change the language to RTL
        Parcelable savedInstance = mTestViewPager.onSaveInstanceState();
        Locale.setDefault(mLocaleHe);
        assertThat(TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()))
                .isEqualTo(View.LAYOUT_DIRECTION_RTL);
        mTestViewPager.onRestoreInstanceState(savedInstance);

        assertThat(mTestViewPager.getCurrentItem()).isEqualTo(currentItem);
    }


    /**
     * Test activity that contains RTL viewpager
     */
    private static class ViewPagerTestActivity extends Activity {
        private RtlCompatibleViewPager mViewPager;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mViewPager = new RtlCompatibleViewPager(ViewPagerTestActivity.this);
            mViewPager.setAdapter(new ViewPagerAdapter());

            setContentView(mViewPager);
        }

        public RtlCompatibleViewPager getViewPager() {
            return mViewPager;
        }
    }

    /**
     * Test viewpager adapter, uses 2 view to test it
     */
    private static final class ViewPagerAdapter extends PagerAdapter {

        private static final int ITEM_COUNT = 2;

        public ViewPagerAdapter() {
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
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
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
