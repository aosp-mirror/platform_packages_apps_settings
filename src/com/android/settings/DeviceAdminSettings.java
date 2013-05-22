/*
 * Copyright (C) 2010 The Android Open Source Project
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.ListFragment;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeviceAdminSettings extends ListFragment {
    static final String TAG = "DeviceAdminSettings";
    
    static final int DIALOG_WARNING = 1;
    
    DevicePolicyManager mDPM;
    final HashSet<ComponentName> mActiveAdmins = new HashSet<ComponentName>();
    final ArrayList<DeviceAdminInfo> mAvailableAdmins = new ArrayList<DeviceAdminInfo>();
    String mDeviceOwnerPkg;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mDPM = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        return inflater.inflate(R.layout.device_admin_settings, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDeviceOwnerPkg = mDPM.getDeviceOwner();
        if (mDeviceOwnerPkg != null && !mDPM.isDeviceOwner(mDeviceOwnerPkg)) {
            mDeviceOwnerPkg = null;
        }
        updateList();
    }

    void updateList() {
        mActiveAdmins.clear();
        List<ComponentName> cur = mDPM.getActiveAdmins();
        if (cur != null) {
            for (int i=0; i<cur.size(); i++) {
                mActiveAdmins.add(cur.get(i));
            }
        }

        mAvailableAdmins.clear();
        List<ResolveInfo> avail = getActivity().getPackageManager().queryBroadcastReceivers(
                new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                PackageManager.GET_META_DATA | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS);
        if (avail == null) {
            avail = Collections.emptyList();
        }

        // Some admins listed in mActiveAdmins may not have been found by the above query.
        // We thus add them separately.
        Set<ComponentName> activeAdminsNotInAvail = new HashSet<ComponentName>(mActiveAdmins);
        for (ResolveInfo ri : avail) {
            ComponentName riComponentName =
                    new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            activeAdminsNotInAvail.remove(riComponentName);
        }
        if (!activeAdminsNotInAvail.isEmpty()) {
            avail = new ArrayList<ResolveInfo>(avail);
            PackageManager packageManager = getActivity().getPackageManager();
            for (ComponentName unlistedActiveAdmin : activeAdminsNotInAvail) {
                List<ResolveInfo> resolved = packageManager.queryBroadcastReceivers(
                        new Intent().setComponent(unlistedActiveAdmin),
                        PackageManager.GET_META_DATA
                                | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS);
                if (resolved != null) {
                    avail.addAll(resolved);
                }
            }
        }

        for (int i = 0, count = avail.size(); i < count; i++) {
            ResolveInfo ri = avail.get(i);
            try {
                DeviceAdminInfo dpi = new DeviceAdminInfo(getActivity(), ri);
                if (dpi.isVisible() || mActiveAdmins.contains(dpi.getComponent())) {
                    mAvailableAdmins.add(dpi);
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Skipping " + ri.activityInfo, e);
            } catch (IOException e) {
                Log.w(TAG, "Skipping " + ri.activityInfo, e);
            }
        }
        
        getListView().setAdapter(new PolicyListAdapter());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        DeviceAdminInfo dpi = (DeviceAdminInfo)l.getAdapter().getItem(position);
        Intent intent = new Intent();
        intent.setClass(getActivity(), DeviceAdminAdd.class);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, dpi.getComponent());
        startActivity(intent);
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox checkbox;
        TextView description;
    }
    
    class PolicyListAdapter extends BaseAdapter {
        final LayoutInflater mInflater;
        
        PolicyListAdapter() {
            mInflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public boolean hasStableIds() {
            return true;
        }
        
        public int getCount() {
            return mAvailableAdmins.size();
        }

        public Object getItem(int position) {
            return mAvailableAdmins.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            DeviceAdminInfo info = mAvailableAdmins.get(position);
            if (mActiveAdmins.contains(info.getComponent())
                    && info.getPackageName().equals(mDeviceOwnerPkg)) {
                return false;
            } else {
                return true;
            }
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
            View v = mInflater.inflate(R.layout.device_admin_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.icon = (ImageView)v.findViewById(R.id.icon);
            h.name = (TextView)v.findViewById(R.id.name);
            h.checkbox = (CheckBox)v.findViewById(R.id.checkbox);
            h.description = (TextView)v.findViewById(R.id.description);
            v.setTag(h);
            return v;
        }
        
        public void bindView(View view, int position) {
            final Activity activity = getActivity();
            ViewHolder vh = (ViewHolder) view.getTag();
            DeviceAdminInfo item = mAvailableAdmins.get(position);
            vh.icon.setImageDrawable(item.loadIcon(activity.getPackageManager()));
            vh.name.setText(item.loadLabel(activity.getPackageManager()));
            vh.checkbox.setChecked(mActiveAdmins.contains(item.getComponent()));
            final boolean activeOwner = vh.checkbox.isChecked()
                    && item.getPackageName().equals(mDeviceOwnerPkg);
            try {
                vh.description.setText(item.loadDescription(activity.getPackageManager()));
            } catch (Resources.NotFoundException e) {
            }
            vh.checkbox.setEnabled(!activeOwner);
            vh.name.setEnabled(!activeOwner);
            vh.description.setEnabled(!activeOwner);
            vh.icon.setEnabled(!activeOwner);
        }
    }
}
