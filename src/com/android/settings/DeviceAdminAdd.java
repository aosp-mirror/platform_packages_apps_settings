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
import android.app.DeviceAdminInfo;
import android.app.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AppSecurityPermissions;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class DeviceAdminAdd extends Activity {
    static final String TAG = "DeviceAdminAdd";
    
    DevicePolicyManager mDPM;
    DeviceAdminInfo mDeviceAdmin;
    
    ImageView mActiveIcon;
    TextView mActiveName;
    TextView mActiveDescription;
    TextView mActiveWarning;
    ViewGroup mAdminPolicies;
    
    View mSelectLayout;
    ArrayList<DeviceAdminInfo> mAvailablePolicies
            = new ArrayList<DeviceAdminInfo>();
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        DeviceAdminInfo activeAdmin = mDPM.getActiveAdminInfo();

        ComponentName cn = (ComponentName)getIntent().getParcelableExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        if (cn == null) {
            Log.w(TAG, "No component specified in " + getIntent().getAction());
            finish();
            return;
        }
        if (cn.equals(activeAdmin)) {
            setResult(Activity.RESULT_OK);
            finish();
            return;
        }
        if (activeAdmin != null) {
            Log.w(TAG, "Admin already set, can't do " + getIntent().getAction());
            finish();
            return;
        }
        
        ActivityInfo ai;
        try {
            ai = getPackageManager().getReceiverInfo(cn,
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            finish();
            return;
        }
        
        ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = ai;
        try {
            mDeviceAdmin= new DeviceAdminInfo(this, ri);
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            finish();
            return;
        } catch (IOException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            finish();
            return;
        }
        
        setContentView(R.layout.device_admin_add);
        
        mActiveIcon = (ImageView)findViewById(R.id.active_icon);
        mActiveName = (TextView)findViewById(R.id.active_name);
        mActiveDescription = (TextView)findViewById(R.id.active_description);
        mActiveWarning = (TextView)findViewById(R.id.active_warning);
        mAdminPolicies = (ViewGroup)findViewById(R.id.admin_policies);
        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDPM.setActiveAdmin(mDeviceAdmin.getComponent());
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateInterface();
    }
    
    void updateInterface() {
        mActiveIcon.setImageDrawable(mDeviceAdmin.loadIcon(getPackageManager()));
        mActiveName.setText(mDeviceAdmin.loadLabel(getPackageManager()));
        try {
            mActiveDescription.setText(
                    mDeviceAdmin.loadDescription(getPackageManager()));
        } catch (Resources.NotFoundException e) {
        }
        mActiveWarning.setText(getString(R.string.device_admin_warning,
                mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())));
        ArrayList<DeviceAdminInfo.PolicyInfo> policies = mDeviceAdmin.getUsedPolicies();
        for (int i=0; i<policies.size(); i++) {
            DeviceAdminInfo.PolicyInfo pi = policies.get(i);
            mAdminPolicies.addView(AppSecurityPermissions.getPermissionItemView(
                    this, getText(pi.label), getText(pi.description), true));
        }
    }
    
}
