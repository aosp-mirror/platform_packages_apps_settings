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

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteCallback;
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
import java.util.HashSet;
import java.util.List;

public class DeviceAdminSettings extends ListActivity {
    static final String TAG = "DeviceAdminSettings";
    
    static final int DIALOG_WARNING = 1;
    
    DevicePolicyManager mDPM;
    final HashSet<ComponentName> mActiveAdmins = new HashSet<ComponentName>();
    final ArrayList<DeviceAdminInfo> mAvailableAdmins = new ArrayList<DeviceAdminInfo>();
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        
        setContentView(R.layout.device_admin_settings);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
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
        List<ResolveInfo> avail = getPackageManager().queryBroadcastReceivers(
                new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                PackageManager.GET_META_DATA);
        int count = avail == null ? 0 : avail.size();
        for (int i=0; i<count; i++) {
            ResolveInfo ri = avail.get(i);
            try {
                DeviceAdminInfo dpi = new DeviceAdminInfo(this, ri);
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        DeviceAdminInfo dpi = (DeviceAdminInfo)l.getAdapter().getItem(position);
        Intent intent = new Intent();
        intent.setClass(this, DeviceAdminAdd.class);
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
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            return true;
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
            ViewHolder vh = (ViewHolder) view.getTag();
            DeviceAdminInfo item = mAvailableAdmins.get(position);
            vh.icon.setImageDrawable(item.loadIcon(getPackageManager()));
            vh.name.setText(item.loadLabel(getPackageManager()));
            vh.checkbox.setChecked(mActiveAdmins.contains(item.getComponent()));
            try {
                vh.description.setText(item.loadDescription(getPackageManager()));
            } catch (Resources.NotFoundException e) {
            }
        }
    }
}
