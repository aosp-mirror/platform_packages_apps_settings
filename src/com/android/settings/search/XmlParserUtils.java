/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import com.android.settings.R;

/**
 * Utility class to parse elements of XML preferences
 */
public class XmlParserUtils {

    private static final String ENTRIES_SEPARATOR = "|";

    public static String getDataKey(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_key);
    }

    public static String getDataTitle(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_title);
    }

    public static String getDataSummary(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_summary);
    }

    public static String getDataSummaryOn(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.CheckBoxPreference,
                com.android.internal.R.styleable.CheckBoxPreference_summaryOn);
    }

    public static String getDataSummaryOff(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.CheckBoxPreference,
                com.android.internal.R.styleable.CheckBoxPreference_summaryOff);
    }

    public static String getDataEntries(Context context, AttributeSet attrs) {
        return getDataEntries(context, attrs,
                com.android.internal.R.styleable.ListPreference,
                com.android.internal.R.styleable.ListPreference_entries);
    }

    public static String getDataKeywords(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference, R.styleable.Preference_keywords);
    }

    public static int getDataIcon(Context context, AttributeSet attrs) {
        final TypedArray ta = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.Preference);
        final int dataIcon = ta.getResourceId(com.android.internal.R.styleable.Icon_icon, 0);
        ta.recycle();
        return dataIcon;
    }

    /**
     * Returns the fragment name if this preference launches a child fragment.
     */
    public static String getDataChildFragment(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference,
                R.styleable.Preference_android_fragment);
    }

    @Nullable
    private static String getData(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray ta = context.obtainStyledAttributes(set, attrs);
        String data = ta.getString(resId);
        ta.recycle();
        return data;
    }

    private static String getDataEntries(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray sa = context.obtainStyledAttributes(set, attrs);
        final TypedValue tv = sa.peekValue(resId);
        sa.recycle();
        String[] data = null;
        if (tv != null && tv.type == TypedValue.TYPE_REFERENCE) {
            if (tv.resourceId != 0) {
                data = context.getResources().getStringArray(tv.resourceId);
            }
        }
        final int count = (data == null ) ? 0 : data.length;
        if (count == 0) {
            return null;
        }
        final StringBuilder result = new StringBuilder();
        for (int n = 0; n < count; n++) {
            result.append(data[n]);
            result.append(ENTRIES_SEPARATOR);
        }
        return result.toString();
    }
}
