/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.quicklaunch;

import com.android.settings.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.Preference;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/**
 * Preference type for a shortcut in {@link QuickLaunchSettings}.
 */
public class ShortcutPreference extends Preference implements Comparable<Preference> {

    private static Object sStaticVarsLock = new Object();
    
    // These static fields are used across all instances of ShortcutPreference.
    // There will be many ShortcutPreference instances (~36 for US).
    private static String STRING_ASSIGN_APPLICATION;
    private static String STRING_NO_SHORTCUT;

    private static int sDimAlpha;
    private static ColorStateList sRegularTitleColor;
    private static ColorStateList sDimTitleColor;
    private static ColorStateList sRegularSummaryColor;
    private static ColorStateList sDimSummaryColor;
    
    private char mShortcut;
    private boolean mHasBookmark;
    
    public ShortcutPreference(Context context, char shortcut) {
        super(context);

        synchronized (sStaticVarsLock) {
            // Init statics. This should only happen for the first ShortcutPreference created,
            // the rest will already have them initialized.
            if (STRING_ASSIGN_APPLICATION == null) {
                STRING_ASSIGN_APPLICATION = context.getString(R.string.quick_launch_assign_application);
                STRING_NO_SHORTCUT = context.getString(R.string.quick_launch_no_shortcut);
    
                TypedValue outValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
                sDimAlpha = (int) (outValue.getFloat() * 255);
            }
        }
        
        mShortcut = shortcut;

        setWidgetLayoutResource(R.layout.preference_widget_shortcut);
    }

    public char getShortcut() {
        return mShortcut;
    }

    public void setShortcut(char shortcut) {
        if (shortcut != mShortcut) {
            mShortcut = shortcut;
            notifyChanged();
        }
    }

    public boolean hasBookmark() {
        return mHasBookmark;
    }

    public void setHasBookmark(boolean hasBookmark) {
        if (hasBookmark != mHasBookmark) {
            mHasBookmark = hasBookmark;
            notifyChanged();
        }
    }

    @Override
    public CharSequence getTitle() {
        return mHasBookmark ? super.getTitle() : STRING_ASSIGN_APPLICATION;
    }

    @Override
    public CharSequence getSummary() {
        return mHasBookmark ? super.getSummary() : STRING_NO_SHORTCUT;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        
        TextView shortcutView = (TextView) view.findViewById(R.id.shortcut);
        if (shortcutView != null) {
            shortcutView.setText(String.valueOf(mShortcut));
        }
    
        TextView titleView = (TextView) view.findViewById(android.R.id.title);

        synchronized (sStaticVarsLock) {
            if (sRegularTitleColor == null) {
                sRegularTitleColor = titleView.getTextColors();
                sDimTitleColor = sRegularTitleColor.withAlpha(sDimAlpha);
            }
        }
        
        ColorStateList color = mHasBookmark ? sRegularTitleColor : sDimTitleColor;
        if (color != null) {
            titleView.setTextColor(color);
        }
        
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);

        synchronized (sStaticVarsLock) {
            if (sRegularSummaryColor == null) {
                sRegularSummaryColor = summaryView.getTextColors();
                sDimSummaryColor = sRegularSummaryColor.withAlpha(sDimAlpha);
            }
        }
        
        color = mHasBookmark ? sRegularSummaryColor : sDimSummaryColor;
        if (color != null) {
            summaryView.setTextColor(color);
        }
        
    }

    public int compareTo(Preference another) {
        if (!(another instanceof ShortcutPreference)) return super.compareTo(another);

        // Letters before digits
        char other = ((ShortcutPreference) another).mShortcut;
        if (Character.isDigit(mShortcut) && Character.isLetter(other)) return 1;
        else if (Character.isDigit(other) && Character.isLetter(mShortcut)) return -1;
        else return mShortcut - other;
    }
    
}
