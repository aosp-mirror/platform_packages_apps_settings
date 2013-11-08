/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.privacyguard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.privacyguard.PrivacyGuardManager.AppInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PrivacyGuardAppListAdapter extends BaseAdapter implements SectionIndexer {

    private LayoutInflater mInflater;
    private PackageManager mPm;

    private List<AppInfo> mApps;
    private String[] mSections;
    private int[] mPositions;
    private ConcurrentHashMap<String, Drawable> mIcons;
    private Drawable mDefaultImg;

    private Context mContext;

    //constructor
    public PrivacyGuardAppListAdapter(Context context, List<AppInfo> apps,
            List<String> sections, List<Integer> positions) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mPm = context.getPackageManager();

        mApps = apps;
        mSections = sections.toArray(new String[sections.size()]);
        mPositions = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            mPositions[i] = positions.get(i);
        }

        // set the default icon till the actual app icon is loaded in async task
        mDefaultImg = mContext.getResources().getDrawable(android.R.mipmap.sym_def_app_icon);
        mIcons = new ConcurrentHashMap<String, Drawable>();

        new LoadIconsTask().execute(apps.toArray(new PrivacyGuardManager.AppInfo[]{}));
    }

    @Override
    public int getCount() {
        return mApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PrivacyGuardAppViewHolder appHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.privacy_guard_manager_list_row, null);

            // creates a ViewHolder and children references
            appHolder = new PrivacyGuardAppViewHolder();
            appHolder.title = (TextView) convertView.findViewById(R.id.app_title);
            appHolder.icon = (ImageView) convertView.findViewById(R.id.app_icon);
            appHolder.privacyGuardIcon = (ImageView) convertView.findViewById(R.id.app_privacy_guard_icon);
            convertView.setTag(appHolder);
        } else {
            appHolder = (PrivacyGuardAppViewHolder) convertView.getTag();
        }

        PrivacyGuardManager.AppInfo app = mApps.get(position);

        appHolder.title.setText(app.title);

        Drawable icon = mIcons.get(app.packageName);
        appHolder.icon.setImageDrawable(icon != null ? icon : mDefaultImg);

        int privacyGuardDrawableResId = app.privacyGuardEnabled
                ? R.drawable.ic_privacy_guard : R.drawable.ic_privacy_guard_off;
        appHolder.privacyGuardIcon.setImageResource(privacyGuardDrawableResId);

        return convertView;
    }

    @Override
    public int getPositionForSection(int section) {
        if (section < 0 || section >= mSections.length) {
            return -1;
        }

        return mPositions[section];
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position < 0 || position >= getCount()) {
            return -1;
        }

        int index = Arrays.binarySearch(mPositions, position);

        /*
         * Consider this example: section positions are 0, 3, 5; the supplied
         * position is 4. The section corresponding to position 4 starts at
         * position 3, so the expected return value is 1. Binary search will not
         * find 4 in the array and thus will return -insertPosition-1, i.e. -3.
         * To get from that number to the expected value of 1 we need to negate
         * and subtract 2.
         */
        return index >= 0 ? index : -index - 2;
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    /**
     * An asynchronous task to load the icons of the installed applications.
     */
    private class LoadIconsTask extends AsyncTask<PrivacyGuardManager.AppInfo, Void, Void> {
        @Override
        protected Void doInBackground(PrivacyGuardManager.AppInfo... apps) {
            for (PrivacyGuardManager.AppInfo app : apps) {
                try {
                    Drawable icon = mPm.getApplicationIcon(app.packageName);
                    mIcons.put(app.packageName, icon);
                    publishProgress();
                } catch (PackageManager.NameNotFoundException e) {
                    // ignored; app will show up with default image
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
            notifyDataSetChanged();
        }
    }

    /**
     * App view holder used to reuse the views inside the list.
     */
    public static class PrivacyGuardAppViewHolder {
        TextView title;
        ImageView icon;
        ImageView privacyGuardIcon;
    }
}
