/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.IconDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

/** Adapter for the high usage app list */
public class HighUsageAdapter extends RecyclerView.Adapter<HighUsageAdapter.ViewHolder> {
    private final Context mContext;
    private final IconDrawableFactory mIconDrawableFactory;
    private final PackageManager mPackageManager;
    private final List<AppInfo> mHighUsageAppList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View view;
        public ImageView appIcon;
        public TextView appName;
        public TextView appTime;

        public ViewHolder(View v) {
            super(v);
            view = v;
            appIcon = v.findViewById(R.id.app_icon);
            appName = v.findViewById(R.id.app_name);
            appTime = v.findViewById(R.id.app_screen_time);
        }
    }

    public HighUsageAdapter(Context context, List<AppInfo> highUsageAppList) {
        mContext = context;
        mHighUsageAppList = highUsageAppList;
        mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view =
                LayoutInflater.from(mContext).inflate(R.layout.app_high_usage_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final AppInfo app = mHighUsageAppList.get(position);
        holder.appIcon.setImageDrawable(
                Utils.getBadgedIcon(
                        mIconDrawableFactory,
                        mPackageManager,
                        app.packageName,
                        UserHandle.getUserId(app.uid)));
        CharSequence label = Utils.getApplicationLabel(mContext, app.packageName);
        if (label == null) {
            label = app.packageName;
        }

        holder.appName.setText(label);
    }

    @Override
    public int getItemCount() {
        return mHighUsageAppList.size();
    }
}
