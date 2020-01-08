/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.R;

/** Preference that easier preview by matching name to color. */
public class PaletteListPreference extends Preference {

    /**
     * Constructs a new PaletteListPreference with the given context's theme and the supplied
     * attribute set.
     *
     * @param context The Context this is associated with, through which it can access the current
     *                theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public PaletteListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a new PaletteListPreference with the given context's theme, the supplied
     * attribute set, and default style attribute.
     *
     * @param context The Context this is associated with, through which it can access the
     *                current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default
     *                     values for the view. Can be 0 to not look for
     *                     defaults.
     */
    public PaletteListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.daltonizer_preview);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final View rootView = holder.itemView;
        final ListView listView = rootView.findViewById(R.id.palette_listView);
        listView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final int listViewHeight = listView.getMeasuredHeight();
                        final int listViewWidth = listView.getMeasuredWidth();
                        // Removes the callback after get result of measure view.
                        listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        // Resets layout parameters to display whole items from listView.
                        final FrameLayout.LayoutParams layoutParams =
                                (FrameLayout.LayoutParams) listView.getLayoutParams();
                        layoutParams.height = listViewHeight * listView.getAdapter().getCount();
                        layoutParams.width = listViewWidth;
                        listView.setLayoutParams(layoutParams);
                        listView.invalidateViews();
                    }
                });
    }
}
