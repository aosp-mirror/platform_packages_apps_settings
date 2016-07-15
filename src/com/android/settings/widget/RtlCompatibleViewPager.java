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

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

/**
 * A {@link ViewPager} that's aware of RTL changes when used with FragmentPagerAdapter.
 */
public final class RtlCompatibleViewPager extends ViewPager {

    /**
     * Callback interface for responding to changing state of the selected page.
     * Positions supplied will always be the logical position in the adapter -
     * that is, the 0 index corresponds to the left-most page in LTR and the
     * right-most page in RTL.
     */

    public RtlCompatibleViewPager(Context context) {
        this(context, null /* attrs */);
    }

    public RtlCompatibleViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getCurrentItem() {
        return getRtlAwareIndex(super.getCurrentItem());
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(getRtlAwareIndex(item));
    }

    /**
     * Get a "RTL friendly" index. If the locale is LTR, the index is returned as is.
     * Otherwise it's transformed so view pager can render views using the new index for RTL. For
     * example, the second view will be rendered to the left of first view.
     *
     * @param index The logical index.
     */
    public int getRtlAwareIndex(int index) {
        // Using TextUtils rather than View.getLayoutDirection() because LayoutDirection is not
        // defined until onMeasure, and this is called before then.
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL) {
            return getAdapter().getCount() - index - 1;
        }
        return index;
    }
}
