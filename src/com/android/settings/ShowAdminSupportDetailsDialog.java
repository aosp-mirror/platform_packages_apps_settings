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

package com.android.settings;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ShowAdminSupportDetailsDialog extends Activity
        implements DialogInterface.OnDismissListener {

    private final String TAG = "AdminSupportDialog";

    private DevicePolicyManager mDpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDpm = getSystemService(DevicePolicyManager.class);
        ComponentName admin = null;
        int userId = UserHandle.myUserId();
        Intent intent = getIntent();
        if (intent != null) {
            // Only allow apps with MANAGE_DEVICE_ADMINS permission to specify admin and user.
            if (checkIfCallerHasPermission(android.Manifest.permission.MANAGE_DEVICE_ADMINS)) {
                admin = intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);
                userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
            }
        }

        View rootView = LayoutInflater.from(this).inflate(
                R.layout.admin_support_details_dialog, null);
        setAdminSupportDetails(rootView, admin, userId);

        new AlertDialog.Builder(this)
                .setView(rootView)
                .setPositiveButton(R.string.okay, null)
                .setOnDismissListener(this)
                .show();
    }

    private boolean checkIfCallerHasPermission(String permission) {
        IActivityManager am = ActivityManagerNative.getDefault();
        try {
            final int uid = am.getLaunchedFromUid(getActivityToken());
            return AppGlobals.getPackageManager().checkUidPermission(permission, uid)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (RemoteException e) {
            Log.e(TAG, "Could not talk to activity manager.", e);
        }
        return false;
    }

    private void setAdminSupportDetails(View root, final ComponentName admin, final int userId) {
        if (admin != null) {
            CharSequence supportMessage = mDpm.getShortSupportMessageForUser(admin, userId);
            if (supportMessage != null) {
                TextView textView = (TextView) root.findViewById(R.id.admin_support_msg);
                textView.setText(supportMessage);
            }

            ActivityInfo ai = null;
            try {
                ai = AppGlobals.getPackageManager().getReceiverInfo(admin, 0 /* flags */, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Missing reciever info" , e);
            }
            if (ai != null) {
                Drawable icon = ai.loadIcon(getPackageManager());
                Drawable badgedIcon = getPackageManager().getUserBadgedIcon(
                        icon, new UserHandle(userId));
                ((ImageView) root.findViewById(R.id.admin_support_icon)).setImageDrawable(
                        badgedIcon);
            }
        }

        root.findViewById(R.id.admins_policies_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        if (admin != null) {
                            intent.setClass(ShowAdminSupportDetailsDialog.this,
                                    DeviceAdminAdd.class);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                            // DeviceAdminAdd class may need to run as managed profile.
                            startActivityAsUser(intent, new UserHandle(userId));
                        } else {
                            intent.setClass(ShowAdminSupportDetailsDialog.this,
                                    Settings.DeviceAdminSettingsActivity.class);
                            // Activity merges both managed profile and parent users
                            // admins so show as same user as this activity.
                            startActivity(intent);
                        }
                        finish();
                    }
                });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
