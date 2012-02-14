/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.android.settings.applications.AppViewHolder;

import android.app.ActivityManagerNative;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AppPicker extends ListActivity {
    private AppListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mAdapter = new AppListAdapter(this);
        if (mAdapter.getCount() <= 0) {
            finish();
        } else {
            setListAdapter(mAdapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        MyApplicationInfo app = mAdapter.getItem(position);
        Intent intent = new Intent();
        if (app.info != null) intent.setAction(app.info.packageName);
        setResult(RESULT_OK, intent);
        finish();
    }

    class MyApplicationInfo {
        ApplicationInfo info;
        CharSequence label;
    }

    public class AppListAdapter extends ArrayAdapter<MyApplicationInfo> {
        private final List<MyApplicationInfo> mPackageInfoList = new ArrayList<MyApplicationInfo>();
        private final LayoutInflater mInflater;

        public AppListAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            List<ApplicationInfo> pkgs = context.getPackageManager().getInstalledApplications(0);
            for (int i=0; i<pkgs.size(); i++) {
                ApplicationInfo ai = pkgs.get(i);
                if (ai.uid == Process.SYSTEM_UID) {
                    continue;
                }
                // On a user build, we only allow debugging of apps that
                // are marked as debuggable.  Otherwise (for platform development)
                // we allow all apps.
                if ((ai.flags&ApplicationInfo.FLAG_DEBUGGABLE) == 0
                        && "user".equals(Build.TYPE)) {
                    continue;
                }
                MyApplicationInfo info = new MyApplicationInfo();
                info.info = ai;
                info.label = info.info.loadLabel(getPackageManager()).toString();
                mPackageInfoList.add(info);
            }
            Collections.sort(mPackageInfoList, sDisplayNameComparator);
            MyApplicationInfo info = new MyApplicationInfo();
            info.label = context.getText(R.string.no_application);
            mPackageInfoList.add(0, info);
            addAll(mPackageInfoList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            AppViewHolder holder = AppViewHolder.createOrRecycle(mInflater, convertView);
            convertView = holder.rootView;
            MyApplicationInfo info = getItem(position);
            holder.appName.setText(info.label);
            if (info.info != null) {
                holder.appIcon.setImageDrawable(info.info.loadIcon(getPackageManager()));
                holder.appSize.setText(info.info.packageName);
            } else {
                holder.appIcon.setImageDrawable(null);
                holder.appSize.setText("");
            }
            holder.disabled.setVisibility(View.GONE);
            holder.checkBox.setVisibility(View.GONE);
            return convertView;
        }
    }

    private final static Comparator<MyApplicationInfo> sDisplayNameComparator
            = new Comparator<MyApplicationInfo>() {
        public final int
        compare(MyApplicationInfo a, MyApplicationInfo b) {
            return collator.compare(a.label, b.label);
        }

        private final Collator collator = Collator.getInstance();
    };
}
