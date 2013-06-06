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
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DeviceAdminAdd extends Activity {
    static final String TAG = "DeviceAdminAdd";
    
    static final int DIALOG_WARNING = 1;

    private static final int MAX_ADD_MSG_LINES_PORTRAIT = 5;
    private static final int MAX_ADD_MSG_LINES_LANDSCAPE = 2;
    private static final int MAX_ADD_MSG_LINES = 15;
    
    Handler mHandler;
    
    DevicePolicyManager mDPM;
    DeviceAdminInfo mDeviceAdmin;
    CharSequence mAddMsgText;
    
    ImageView mAdminIcon;
    TextView mAdminName;
    TextView mAdminDescription;
    TextView mAddMsg;
    ImageView mAddMsgExpander;
    boolean mAddMsgEllipsized = true;
    TextView mAdminWarning;
    ViewGroup mAdminPolicies;
    Button mActionButton;
    Button mCancelButton;
    
    final ArrayList<View> mAddingPolicies = new ArrayList<View>();
    final ArrayList<View> mActivePolicies = new ArrayList<View>();
    
    boolean mAdding;
    boolean mRefreshing;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new Handler(getMainLooper());
        
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        if ((getIntent().getFlags()&Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            Log.w(TAG, "Cannot start ADD_DEVICE_ADMIN as a new task");
            finish();
            return;
        }
        
        ComponentName cn = (ComponentName)getIntent().getParcelableExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        if (cn == null) {
            Log.w(TAG, "No component specified in " + getIntent().getAction());
            finish();
            return;
        }

        ActivityInfo ai;
        try {
            ai = getPackageManager().getReceiverInfo(cn, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            finish();
            return;
        }

        // When activating, make sure the given component name is actually a valid device admin.
        // No need to check this when deactivating, because it is safe to deactivate an active
        // invalid device admin.
        if (!mDPM.isAdminActive(cn)) {
            List<ResolveInfo> avail = getPackageManager().queryBroadcastReceivers(
                    new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                    PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS);
            int count = avail == null ? 0 : avail.size();
            boolean found = false;
            for (int i=0; i<count; i++) {
                ResolveInfo ri = avail.get(i);
                if (ai.packageName.equals(ri.activityInfo.packageName)
                        && ai.name.equals(ri.activityInfo.name)) {
                    try {
                        // We didn't retrieve the meta data for all possible matches, so
                        // need to use the activity info of this specific one that was retrieved.
                        ri.activityInfo = ai;
                        DeviceAdminInfo dpi = new DeviceAdminInfo(this, ri);
                        found = true;
                    } catch (XmlPullParserException e) {
                        Log.w(TAG, "Bad " + ri.activityInfo, e);
                    } catch (IOException e) {
                        Log.w(TAG, "Bad " + ri.activityInfo, e);
                    }
                    break;
                }
            }
            if (!found) {
                Log.w(TAG, "Request to add invalid device admin: " + cn);
                finish();
                return;
            }
        }

        ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = ai;
        try {
            mDeviceAdmin = new DeviceAdminInfo(this, ri);
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            finish();
            return;
        } catch (IOException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            finish();
            return;
        }
        
        // This admin already exists, an we have two options at this point.  If new policy
        // bits are set, show the user the new list.  If nothing has changed, simply return
        // "OK" immediately.
        if (DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN.equals(getIntent().getAction())) {
            mRefreshing = false;
            if (mDPM.isAdminActive(cn)) {
                ArrayList<DeviceAdminInfo.PolicyInfo> newPolicies = mDeviceAdmin.getUsedPolicies();
                for (int i = 0; i < newPolicies.size(); i++) {
                    DeviceAdminInfo.PolicyInfo pi = newPolicies.get(i);
                    if (!mDPM.hasGrantedPolicy(cn, pi.ident)) {
                        mRefreshing = true;
                        break;
                    }
                }
                if (!mRefreshing) {
                    // Nothing changed (or policies were removed) - return immediately
                    setResult(Activity.RESULT_OK);
                    finish();
                    return;
                }
            }
        }
        mAddMsgText = getIntent().getCharSequenceExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION);

        setContentView(R.layout.device_admin_add);
        
        mAdminIcon = (ImageView)findViewById(R.id.admin_icon);
        mAdminName = (TextView)findViewById(R.id.admin_name);
        mAdminDescription = (TextView)findViewById(R.id.admin_description);

        mAddMsg = (TextView)findViewById(R.id.add_msg);
        mAddMsgExpander = (ImageView) findViewById(R.id.add_msg_expander);
        mAddMsg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleMessageEllipsis(v);
            }
        });

        // toggleMessageEllipsis also handles initial layout:
        toggleMessageEllipsis(mAddMsg);

        mAdminWarning = (TextView) findViewById(R.id.admin_warning);
        mAdminPolicies = (ViewGroup) findViewById(R.id.admin_policies);
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        mActionButton = (Button) findViewById(R.id.action_button);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mAdding) {
                    try {
                        mDPM.setActiveAdmin(mDeviceAdmin.getComponent(), mRefreshing);
                        setResult(Activity.RESULT_OK);
                    } catch (RuntimeException e) {
                        // Something bad happened...  could be that it was
                        // already set, though.
                        Log.w(TAG, "Exception trying to activate admin "
                                + mDeviceAdmin.getComponent(), e);
                        if (mDPM.isAdminActive(mDeviceAdmin.getComponent())) {
                            setResult(Activity.RESULT_OK);
                        }
                    }
                    finish();
                } else {
                    try {
                        // Don't allow the admin to put a dialog up in front
                        // of us while we interact with the user.
                        ActivityManagerNative.getDefault().stopAppSwitches();
                    } catch (RemoteException e) {
                    }
                    mDPM.getRemoveWarning(mDeviceAdmin.getComponent(),
                            new RemoteCallback(mHandler) {
                        @Override
                        protected void onResult(Bundle bundle) {
                            CharSequence msg = bundle != null
                                    ? bundle.getCharSequence(
                                            DeviceAdminReceiver.EXTRA_DISABLE_WARNING)
                                    : null;
                            if (msg == null) {
                                try {
                                    ActivityManagerNative.getDefault().resumeAppSwitches();
                                } catch (RemoteException e) {
                                }
                                mDPM.removeActiveAdmin(mDeviceAdmin.getComponent());
                                finish();
                            } else {
                                Bundle args = new Bundle();
                                args.putCharSequence(
                                        DeviceAdminReceiver.EXTRA_DISABLE_WARNING, msg);
                                showDialog(DIALOG_WARNING, args);
                            }
                        }
                    });
                }
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateInterface();
    }
    
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_WARNING: {
                CharSequence msg = args.getCharSequence(DeviceAdminReceiver.EXTRA_DISABLE_WARNING);
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        DeviceAdminAdd.this);
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mDPM.removeActiveAdmin(mDeviceAdmin.getComponent());
                        finish();
                    }
                });
                builder.setNegativeButton(R.string.dlg_cancel, null);
                return builder.create();
            }
            default:
                return super.onCreateDialog(id, args);
                    
        }
    }
    
    static void setViewVisibility(ArrayList<View> views, int visibility) {
        final int N = views.size();
        for (int i=0; i<N; i++) {
            views.get(i).setVisibility(visibility);
        }
    }
    
    void updateInterface() {
        mAdminIcon.setImageDrawable(mDeviceAdmin.loadIcon(getPackageManager()));
        mAdminName.setText(mDeviceAdmin.loadLabel(getPackageManager()));
        try {
            mAdminDescription.setText(
                    mDeviceAdmin.loadDescription(getPackageManager()));
            mAdminDescription.setVisibility(View.VISIBLE);
        } catch (Resources.NotFoundException e) {
            mAdminDescription.setVisibility(View.GONE);
        }
        if (mAddMsgText != null) {
            mAddMsg.setText(mAddMsgText);
            mAddMsg.setVisibility(View.VISIBLE);
        } else {
            mAddMsg.setVisibility(View.GONE);
            mAddMsgExpander.setVisibility(View.GONE);
        }
        if (!mRefreshing && mDPM.isAdminActive(mDeviceAdmin.getComponent())) {
            if (mActivePolicies.size() == 0) {
                ArrayList<DeviceAdminInfo.PolicyInfo> policies = mDeviceAdmin.getUsedPolicies();
                for (int i=0; i<policies.size(); i++) {
                    DeviceAdminInfo.PolicyInfo pi = policies.get(i);
                    View view = AppSecurityPermissions.getPermissionItemView(
                            this, getText(pi.label), "", true);
                    mActivePolicies.add(view);
                    mAdminPolicies.addView(view);
                }
            }
            setViewVisibility(mActivePolicies, View.VISIBLE);
            setViewVisibility(mAddingPolicies, View.GONE);
            mAdminWarning.setText(getString(R.string.device_admin_status,
                    mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())));
            setTitle(getText(R.string.active_device_admin_msg));
            mActionButton.setText(getText(R.string.remove_device_admin));
            mAdding = false;
        } else {
            if (mAddingPolicies.size() == 0) {
                ArrayList<DeviceAdminInfo.PolicyInfo> policies = mDeviceAdmin.getUsedPolicies();
                for (int i=0; i<policies.size(); i++) {
                    DeviceAdminInfo.PolicyInfo pi = policies.get(i);
                    View view = AppSecurityPermissions.getPermissionItemView(
                            this, getText(pi.label), getText(pi.description), true);
                    mAddingPolicies.add(view);
                    mAdminPolicies.addView(view);
                }
            }
            setViewVisibility(mAddingPolicies, View.VISIBLE);
            setViewVisibility(mActivePolicies, View.GONE);
            mAdminWarning.setText(getString(R.string.device_admin_warning,
                    mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())));
            setTitle(getText(R.string.add_device_admin_msg));
            mActionButton.setText(getText(R.string.add_device_admin));
            mAdding = true;
        }
    }


    void toggleMessageEllipsis(View v) {
        TextView tv = (TextView) v;

        mAddMsgEllipsized = ! mAddMsgEllipsized;
        tv.setEllipsize(mAddMsgEllipsized ? TruncateAt.END : null);
        tv.setMaxLines(mAddMsgEllipsized ? getEllipsizedLines() : MAX_ADD_MSG_LINES);

        mAddMsgExpander.setImageResource(mAddMsgEllipsized ?
            com.android.internal.R.drawable.expander_ic_minimized :
            com.android.internal.R.drawable.expander_ic_maximized);
    }

    int getEllipsizedLines() {
        Display d = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();

        return d.getHeight() > d.getWidth() ?
            MAX_ADD_MSG_LINES_PORTRAIT : MAX_ADD_MSG_LINES_LANDSCAPE;
    }

}
