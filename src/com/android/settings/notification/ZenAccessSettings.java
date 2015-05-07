/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.ListFragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.List;

public class ZenAccessSettings extends ListFragment {
    private static final boolean SHOW_PACKAGE_NAME = false;

    private Context mContext;
    private PackageManager mPkgMan;
    private NotificationManager mNoMan;
    private Adapter mAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPkgMan = mContext.getPackageManager();
        mNoMan = mContext.getSystemService(NotificationManager.class);
        mAdapter = new Adapter(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v =  inflater.inflate(R.layout.managed_service_settings, container, false);
        final TextView empty = (TextView) v.findViewById(android.R.id.empty);
        empty.setText(R.string.zen_access_empty_text);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadList();
    }

    private void reloadList() {
        mAdapter.clear();
        final ArraySet<String> requesting = mNoMan.getPackagesRequestingNotificationPolicyAccess();
        if (requesting != null && !requesting.isEmpty()) {
            final List<ApplicationInfo> apps = mPkgMan.getInstalledApplications(0);
            if (apps != null) {
                for (ApplicationInfo app : apps) {
                    if (requesting.contains(app.packageName)) {
                        mAdapter.add(app);
                    }
                }
            }
        }
        mAdapter.sort(new PackageItemInfo.DisplayNameComparator(mPkgMan));
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final ApplicationInfo info = mAdapter.getItem(position);
        final boolean hasAccess = hasAccess(info.packageName);
        setAccess(info.packageName, !hasAccess);
        mAdapter.notifyDataSetChanged();
    }

    private boolean hasAccess(String pkg) {
        return mNoMan.isNotificationPolicyAccessGrantedForPackage(pkg);
    }

    private void setAccess(String pkg, boolean access) {
        mNoMan.setNotificationPolicyAccessGranted(pkg, access);
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox checkbox;
        TextView description;
    }

    private final class Adapter extends ArrayAdapter<ApplicationInfo> {
        final LayoutInflater mInflater;

        Adapter(Context context) {
            super(context, 0, 0);
            mInflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public boolean hasStableIds() {
            return true;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.managed_service_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.icon = (ImageView) v.findViewById(R.id.icon);
            h.name = (TextView) v.findViewById(R.id.name);
            h.checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            h.description = (TextView) v.findViewById(R.id.description);
            v.setTag(h);
            return v;
        }

        public void bindView(View view, int position) {
            ViewHolder vh = (ViewHolder) view.getTag();
            ApplicationInfo info = getItem(position);

            vh.icon.setImageDrawable(info.loadIcon(mPkgMan));
            vh.name.setText(info.loadLabel(mPkgMan));
            if (SHOW_PACKAGE_NAME) {
                vh.description.setText(info.packageName);
                vh.description.setVisibility(View.VISIBLE);
            } else {
                vh.description.setVisibility(View.GONE);
            }
            vh.checkbox.setChecked(hasAccess(info.packageName));
        }

    }

}
