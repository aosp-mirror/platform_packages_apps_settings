/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ProgressBar;
import com.android.settingslib.AppItem;
import com.android.settingslib.net.UidDetail;
import com.android.settingslib.net.UidDetailProvider;

import static com.android.internal.util.Preconditions.checkNotNull;

public class AppDataUsagePreference extends Preference {

    private final AppItem mItem;
    private final int mPercent;

    public AppDataUsagePreference(Context context, AppItem item, int percent,
            UidDetailProvider provider) {
        super(context);
        mItem = item;
        mPercent = percent;
        setLayoutResource(com.android.settings.R.layout.data_usage_item);
        setWidgetLayoutResource(com.android.settings.R.layout.widget_progress_bar);
        if (item.restricted && item.total <= 0) {
            setSummary(com.android.settings.R.string.data_usage_app_restricted);
        } else {
            setSummary(Formatter.formatFileSize(context, item.total));
        }

        // kick off async load of app details
        UidDetailTask.bindView(provider, item, this);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final ProgressBar progress = (ProgressBar) holder.findViewById(
                android.R.id.progress);

        if (mItem.restricted && mItem.total <= 0) {
            progress.setVisibility(View.GONE);
        } else {
            progress.setVisibility(View.VISIBLE);
        }
        progress.setProgress(mPercent);
    }

    public AppItem getItem() {
        return mItem;
    }

    /**
     * Background task that loads {@link UidDetail}, binding to
     * {@link DataUsageAdapter} row item when finished.
     */
    private static class UidDetailTask extends AsyncTask<Void, Void, UidDetail> {
        private final UidDetailProvider mProvider;
        private final AppItem mItem;
        private final AppDataUsagePreference mTarget;

        private UidDetailTask(UidDetailProvider provider, AppItem item,
                AppDataUsagePreference target) {
            mProvider = checkNotNull(provider);
            mItem = checkNotNull(item);
            mTarget = checkNotNull(target);
        }

        public static void bindView(UidDetailProvider provider, AppItem item,
                AppDataUsagePreference target) {
            final UidDetail cachedDetail = provider.getUidDetail(item.key, false);
            if (cachedDetail != null) {
                bindView(cachedDetail, target);
            } else {
                new UidDetailTask(provider, item, target).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        private static void bindView(UidDetail detail, Preference target) {
            if (detail != null) {
                target.setIcon(detail.icon);
                target.setTitle(detail.label);
            } else {
                target.setIcon(null);
                target.setTitle(null);
            }
        }

        @Override
        protected void onPreExecute() {
            bindView(null, mTarget);
        }

        @Override
        protected UidDetail doInBackground(Void... params) {
            return mProvider.getUidDetail(mItem.key, true);
        }

        @Override
        protected void onPostExecute(UidDetail result) {
            bindView(result, mTarget);
        }
    }
}
