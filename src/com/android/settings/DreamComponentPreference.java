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

import static android.provider.Settings.Secure.SCREENSAVER_COMPONENT;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DreamComponentPreference extends Preference {
    private static final String TAG = "DreamComponentPreference";

    private static final boolean SHOW_DOCK_APPS = false;
    private static final boolean SHOW_DREAM_SERVICES = true;
    private static final boolean SHOW_DREAM_ACTIVITIES = false;

    private final PackageManager pm;
    private final ContentResolver resolver;
    private final Collator   sCollator = Collator.getInstance();

    public DreamComponentPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        pm = getContext().getPackageManager();
        resolver = getContext().getContentResolver();

        refreshFromSettings();
    }

    private void refreshFromSettings() {
        ComponentName cn = null;
        IDreamManager dm = IDreamManager.Stub.asInterface(
                ServiceManager.getService("dreams"));
        try {
            cn = dm.getDreamComponent();
        } catch (RemoteException ex) {
            setSummary("(unknown)");
            return;
        }

        try {
            setSummary(pm.getActivityInfo(cn, 0).loadLabel(pm));
        } catch (PackageManager.NameNotFoundException ex) {
            try {
                setSummary(pm.getServiceInfo(cn, 0).loadLabel(pm));
            } catch (PackageManager.NameNotFoundException ex2) {
                setSummary("(unknown)");
            }
        }
    }

    // Group by package, then by name.
    Comparator<ResolveInfo> sResolveInfoComparator = new Comparator<ResolveInfo>() {
        @Override
        public int compare(ResolveInfo a, ResolveInfo b) {
            CharSequence sa, sb;
            
            ApplicationInfo aia = a.activityInfo != null ? a.activityInfo.applicationInfo : a.serviceInfo.applicationInfo;
            ApplicationInfo aib = b.activityInfo != null ? b.activityInfo.applicationInfo : b.serviceInfo.applicationInfo;
            
            if (!aia.equals(aib)) {
                sa = pm.getApplicationLabel(aia);
                sb = pm.getApplicationLabel(aib);
            } else {
                sa = a.loadLabel(pm);
                if (sa == null) {
                    sa = (a.activityInfo != null) ? a.activityInfo.name : a.serviceInfo.name;
                }
                sb = b.loadLabel(pm);
                if (sb == null) {
                    sb = (b.activityInfo != null) ? b.activityInfo.name : b.serviceInfo.name;
                }
            }
            return sCollator.compare(sa.toString(), sb.toString());
        }
    };

    public class DreamListAdapter extends BaseAdapter implements ListAdapter {
        private ArrayList<ResolveInfo> results;
        private final LayoutInflater inflater;

        public DreamListAdapter(Context context) {
            Intent choosy = new Intent(Intent.ACTION_MAIN)
                        .addCategory("android.intent.category.DREAM");

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            results = new ArrayList<ResolveInfo>();
            
            if (SHOW_DREAM_ACTIVITIES) {
                results.addAll(pm.queryIntentActivities(choosy, PackageManager.GET_META_DATA));
            }
            
            if (SHOW_DREAM_SERVICES) {
                results.addAll(pm.queryIntentServices(choosy, PackageManager.GET_META_DATA));
            }

            // Group by package
            Collections.sort(results, sResolveInfoComparator);

            if (SHOW_DOCK_APPS) {
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

        private CharSequence loadDescription(ResolveInfo ri) {
            CharSequence desc = null;
            if (ri != null) {
                Bundle metaData = (ri.activityInfo != null) ? ri.activityInfo.metaData : ri.serviceInfo.metaData;
                Log.d(TAG, "loadDescription: ri=" + ri + " metaData=" + metaData);
                if (metaData != null) {
                    desc = metaData.getCharSequence("android.screensaver.description");
                    Log.d(TAG, "loadDescription: desc=" + desc);
                    if (desc != null) {
                        desc = desc.toString().trim();
                    }
                }
            }
            return desc;
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
                        String pn = (ri.activityInfo != null) ? ri.activityInfo.applicationInfo.packageName
                                : ri.serviceInfo.applicationInfo.packageName;
                        String n = (ri.activityInfo != null) ? ri.activityInfo.name : ri.serviceInfo.name;
                        ComponentName cn = new ComponentName(pn, n);

                        setSummary(ri.loadLabel(pm));
                        //getContext().startActivity(intent);
                        
                        IDreamManager dm = IDreamManager.Stub.asInterface(
                                ServiceManager.getService("dreams"));
                        try {
                            dm.setDreamComponent(cn);
                        } catch (RemoteException ex) {
                            // too bad, so sad, oh mom, oh dad
                        }
                    }
                })
            .create();
        alert.show();
    }
}
