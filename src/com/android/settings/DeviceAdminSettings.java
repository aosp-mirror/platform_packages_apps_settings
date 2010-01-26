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
import android.app.DeviceAdmin;
import android.app.DeviceAdminInfo;
import android.app.DevicePolicyManager;
import android.app.ListActivity;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceAdminSettings extends ListActivity {
    static final String TAG = "DeviceAdminSettings";
    
    DevicePolicyManager mDPM;
    DeviceAdminInfo mCurrentAdmin;
    
    View mActiveLayout;
    ImageView mActiveIcon;
    TextView mActiveName;
    TextView mActiveDescription;
    
    View mSelectLayout;
    ArrayList<DeviceAdminInfo> mAvailablePolicies
            = new ArrayList<DeviceAdminInfo>();
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mCurrentAdmin = mDPM.getActiveAdminInfo();
        
        setContentView(R.layout.device_admin_settings);
        
        mActiveLayout = findViewById(R.id.active_layout);
        mActiveIcon = (ImageView)findViewById(R.id.active_icon);
        mActiveName = (TextView)findViewById(R.id.active_name);
        mActiveDescription = (TextView)findViewById(R.id.active_description);
        findViewById(R.id.remove_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCurrentAdmin != null) {
                    mDPM.removeActiveAdmin(mCurrentAdmin.getComponent());
                    finish();
                }
            }
        });
        
        mSelectLayout = findViewById(R.id.select_layout);

        if (DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN.equals(getIntent().getAction())) {
            ComponentName cn = (ComponentName)getIntent().getParcelableExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN);
            if (cn == null) {
                Log.w(TAG, "No component specified in " + getIntent().getAction());
                finish();
                return;
            }
            if (cn.equals(mCurrentAdmin)) {
                setResult(Activity.RESULT_OK);
                finish();
                return;
            }
            if (mCurrentAdmin != null) {
                Log.w(TAG, "Admin already set, can't do " + getIntent().getAction());
                finish();
                return;
            }
            
            try {
                mDPM.setActiveAdmin(cn);
                setResult(Activity.RESULT_OK);
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to set admin " + cn, e);
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateLayout();
    }

    void updateLayout() {
        if (mCurrentAdmin != null) {
            mActiveLayout.setVisibility(View.VISIBLE);
            mSelectLayout.setVisibility(View.GONE);
            mActiveIcon.setImageDrawable(mCurrentAdmin.loadIcon(getPackageManager()));
            mActiveName.setText(mCurrentAdmin.loadLabel(getPackageManager()));
            try {
                mActiveDescription.setText(
                        mCurrentAdmin.loadDescription(getPackageManager()));
            } catch (Resources.NotFoundException e) {
            }
        } else {
            mActiveLayout.setVisibility(View.GONE);
            mSelectLayout.setVisibility(View.VISIBLE);
            mAvailablePolicies.clear();
            List<ResolveInfo> avail = getPackageManager().queryBroadcastReceivers(
                    new Intent(DeviceAdmin.ACTION_DEVICE_ADMIN_ENABLED),
                    PackageManager.GET_META_DATA);
            for (int i=0; i<avail.size(); i++) {
                ResolveInfo ri = avail.get(i);
                try {
                    DeviceAdminInfo dpi = new DeviceAdminInfo(this, ri);
                    mAvailablePolicies.add(dpi);
                } catch (XmlPullParserException e) {
                    Log.w(TAG, "Skipping " + ri.activityInfo, e);
                } catch (IOException e) {
                    Log.w(TAG, "Skipping " + ri.activityInfo, e);
                }
            }
            getListView().setAdapter(new PolicyListAdapter());
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        DeviceAdminInfo dpi = (DeviceAdminInfo)l.getAdapter().getItem(position);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, dpi.getComponent());
        startActivity(intent);
    }
    
    static class ViewHolder {
        ImageView icon;
        TextView name;
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
            return mAvailablePolicies.size();
        }

        public Object getItem(int position) {
            return mAvailablePolicies.get(position);
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
            v.setTag(h);
            return v;
        }
        
        public void bindView(View view, int position) {
            ViewHolder vh = (ViewHolder) view.getTag();
            DeviceAdminInfo item = mAvailablePolicies.get(position);
            vh.icon.setImageDrawable(item.loadIcon(getPackageManager()));
            vh.name.setText(item.loadLabel(getPackageManager()));
        }
    }
}
