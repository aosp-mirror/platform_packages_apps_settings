/*
 * Copyright (C) 2011 The Android Open Source Project
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

import static android.provider.Settings.Secure.DREAM_COMPONENT;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DreamComponentPreference extends Preference {
    private static final String TAG = "DreamComponentPreference";

    private static final boolean SHOW_DOCK_APPS_TOO = true;
    
    private final PackageManager pm;
    private final ContentResolver resolver;

    public DreamComponentPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        pm = getContext().getPackageManager();
        resolver = getContext().getContentResolver();

        refreshFromSettings();
    }

    private void refreshFromSettings() {
        String component = Settings.Secure.getString(resolver, DREAM_COMPONENT);
        if (component == null) {
            component = getContext().getResources().getString(
                com.android.internal.R.string.config_defaultDreamComponent);
        }
        if (component != null) {
            ComponentName cn = ComponentName.unflattenFromString(component);
            try {
                setSummary(pm.getActivityInfo(cn, 0).loadLabel(pm));
            } catch (PackageManager.NameNotFoundException ex) {
                setSummary("(unknown)");
            }
        }
    }

    final static Comparator<ResolveInfo> sResolveInfoComparator = new Comparator<ResolveInfo>() {
        @Override
        public int compare(ResolveInfo a, ResolveInfo b) {
            int cmp = a.activityInfo.applicationInfo.packageName.compareTo(
                    b.activityInfo.applicationInfo.packageName);
            if (cmp == 0) {
                cmp = a.activityInfo.name.compareTo(b.activityInfo.name);
            }
            return cmp;
        }
    };

    public class DreamListAdapter extends BaseAdapter implements ListAdapter {
        private ArrayList<ResolveInfo> results;
        private final LayoutInflater inflater;

        public DreamListAdapter(Context context) {
            Intent choosy = new Intent(Intent.ACTION_MAIN)
                        .addCategory("android.intent.category.DREAM");

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            results = new ArrayList<ResolveInfo>(pm.queryIntentActivities(choosy, 0));

            // Group by package
            Collections.sort(results, sResolveInfoComparator);

            if (SHOW_DOCK_APPS_TOO) {
                choosy = new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_DESK_DOCK);

                List<ResolveInfo> dockApps = pm.queryIntentActivities(choosy, 0);
                for (ResolveInfo app : dockApps) {
                    // do not insert duplicate packages
                    int pos = Collections.binarySearch(results, app, sResolveInfoComparator);
                    if (pos < 0) {
                        results.add(-1-pos, app);
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public Object getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId (int position) {
            return (long) position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = (convertView != null) 
                ? convertView
                : inflater.inflate(R.layout.dream_picker_row, parent, false);
            ResolveInfo ri = results.get(position);
            ((TextView)row.findViewById(R.id.title)).setText(ri.loadLabel(pm));
            ((ImageView)row.findViewById(R.id.icon)).setImageDrawable(ri.loadIcon(pm));
            return row;
        }
    }

    @Override
    protected void onClick() {
        final DreamListAdapter list = new DreamListAdapter(getContext());
        AlertDialog alert = new AlertDialog.Builder(getContext())
            .setAdapter(
                list,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ResolveInfo ri = (ResolveInfo)list.getItem(which);
                        ActivityInfo act = ri.activityInfo;
                        ComponentName cn = new ComponentName(
                            act.applicationInfo.packageName,
                            act.name);
                        Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(cn);
                        
                        setSummary(ri.loadLabel(pm));
                        //getContext().startActivity(intent);
                        
                        Settings.Secure.putString(resolver, DREAM_COMPONENT, cn.flattenToString());
                    }
                })
            .create();
        alert.show();
    }
}
