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

package com.android.settings.display;

import com.android.settings.R;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppGridView extends GridView {
    public AppGridView(Context context) {
        this(context, null);
    }

    public AppGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleResId) {
        super(context, attrs, defStyleAttr, defStyleResId);

        setNumColumns(AUTO_FIT);

        final int columnWidth = getResources().getDimensionPixelSize(
                R.dimen.screen_zoom_preview_app_icon_width);
        setColumnWidth(columnWidth);

        setAdapter(new AppsAdapter(context, R.layout.screen_zoom_preview_app_icon,
                android.R.id.text1, android.R.id.icon1));
    }

    /**
     * Loads application labels and icons.
     */
    private static class AppsAdapter extends ArrayAdapter<ActivityEntry> {
        private final PackageManager mPackageManager;
        private final int mIconResId;

        public AppsAdapter(Context context, int layout, int textResId, int iconResId) {
            super(context, layout, textResId);

            mIconResId = iconResId;
            mPackageManager = context.getPackageManager();

            loadAllApps();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            final ActivityEntry entry = getItem(position);
            final ImageView iconView = (ImageView) view.findViewById(mIconResId);
            final Drawable icon = entry.info.loadIcon(mPackageManager);
            iconView.setImageDrawable(icon);
            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        private void loadAllApps() {
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final PackageManager pm = mPackageManager;
            final ArrayList<ActivityEntry> results = new ArrayList<>();
            final List<ResolveInfo> infos = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : infos) {
                final CharSequence label = info.loadLabel(pm);
                if (label != null) {
                    results.add(new ActivityEntry(info, label.toString()));
                }
            }

            Collections.sort(results);

            addAll(results);
        }
    }

    /**
     * Class used for caching the activity label and icon.
     */
    private static class ActivityEntry implements Comparable<ActivityEntry> {
        public final ResolveInfo info;
        public final String label;

        public ActivityEntry(ResolveInfo info, String label) {
            this.info = info;
            this.label = label;
        }

        @Override
        public int compareTo(ActivityEntry entry) {
            return label.compareToIgnoreCase(entry.label);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
