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

package com.android.settings.deletionhelper;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

/**
 * Preference item for an app with a switch to signify if it should be uninstalled.
 * This shows the name and icon of the app along with the days since its last use.
 */
public class AppDeletionPreference extends SwitchPreference {
    private AppEntry mEntry;
    private Context mContext;

    public AppDeletionPreference(Context context, AppEntry item, ApplicationsState state) {
        super(context);
        mEntry = item;
        mContext = context;
        setLayoutResource(com.android.settings.R.layout.preference_app);
        setWidgetLayoutResource(R.layout.widget_text_views);

        synchronized (item) {
            state.ensureIcon(item);
            if (item.icon != null)
                setIcon(item.icon);
            if (item.label != null)
                setTitle(item.label);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        Switch switchWidget = (Switch) holder.findViewById(com.android.internal.R.id.switch_widget);
        switchWidget.setVisibility(View.VISIBLE);

        TextView summary = (TextView) holder.findViewById(R.id.widget_text1);
        updateSummaryText(summary);
    }

    public String getPackageName() {
        return mEntry.label;
    }

    private void updateSummaryText(TextView summary) {
        if (mEntry.extraInfo == null) return;
        if (mEntry.size == ApplicationsState.SIZE_UNKNOWN ||
                mEntry.size == ApplicationsState.SIZE_INVALID) {
            return;
        }

        long daysSinceLastUse = (long) mEntry.extraInfo;
        String fileSize = Formatter.formatFileSize(mContext, mEntry.size);
        if (daysSinceLastUse == AppStateUsageStatsBridge.NEVER_USED) {
            summary.setText(mContext.getString(R.string.deletion_helper_app_summary_never_used,
                    fileSize));
        } else if (daysSinceLastUse == AppStateUsageStatsBridge.UNKNOWN_LAST_USE) {
            summary.setText(mContext.getString(R.string.deletion_helper_app_summary_unknown_used,
                    fileSize));
        } else {
            summary.setText(mContext.getString(R.string.deletion_helper_app_summary,
                    fileSize,
                    daysSinceLastUse));
        }
    }

}
