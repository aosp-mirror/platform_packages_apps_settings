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
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class ShowAdminSupportDetailsDialog extends Activity
        implements DialogInterface.OnDismissListener {

    private static final String TAG = "AdminSupportDialog";

    private EnforcedAdmin mEnforcedAdmin;
    private View mDialogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEnforcedAdmin = getAdminDetailsFromIntent(getIntent());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mDialogView = LayoutInflater.from(builder.getContext()).inflate(
                R.layout.admin_support_details_dialog, null);
        initializeDialogViews(mDialogView, mEnforcedAdmin.component, mEnforcedAdmin.userId);
        builder.setOnDismissListener(this)
                .setPositiveButton(R.string.okay, null)
                .setView(mDialogView)
                .show();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        EnforcedAdmin admin = getAdminDetailsFromIntent(intent);
        if (!mEnforcedAdmin.equals(admin)) {
            mEnforcedAdmin = admin;
            initializeDialogViews(mDialogView, mEnforcedAdmin.component, mEnforcedAdmin.userId);
        }
    }

    private EnforcedAdmin getAdminDetailsFromIntent(Intent intent) {
        EnforcedAdmin admin = new EnforcedAdmin(null, UserHandle.myUserId());
        if (intent == null) {
            return admin;
        }
        admin.component = intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        admin.userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        return admin;
    }

    private void initializeDialogViews(View root, ComponentName admin, int userId) {
        if (admin != null) {
            if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(this, admin)
                    || !RestrictedLockUtils.isCurrentUserOrProfile(this, userId)) {
                admin = null;
            } else {
                ActivityInfo ai = null;
                try {
                    ai = AppGlobals.getPackageManager().getReceiverInfo(admin, 0 /* flags */,
                            userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Missing reciever info", e);
                }
                if (ai != null) {
                    Drawable icon = ai.loadIcon(getPackageManager());
                    Drawable badgedIcon = getPackageManager().getUserBadgedIcon(
                            icon, new UserHandle(userId));
                    ((ImageView) root.findViewById(R.id.admin_support_icon)).setImageDrawable(
                            badgedIcon);
                }
            }
        }

        setAdminSupportDetails(this, root, new EnforcedAdmin(admin, userId), true);
    }

    public static void setAdminSupportDetails(final Activity activity, View root,
            final EnforcedAdmin enforcedAdmin, final boolean finishActivity) {
        if (enforcedAdmin == null) {
            return;
        }

        if (enforcedAdmin.component != null) {
            DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                    Context.DEVICE_POLICY_SERVICE);
            if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(activity,
                    enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(
                    activity, enforcedAdmin.userId)) {
                enforcedAdmin.component = null;
            } else {
                if (enforcedAdmin.userId == UserHandle.USER_NULL) {
                    enforcedAdmin.userId = UserHandle.myUserId();
                }
                CharSequence supportMessage = null;
                if (UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
                    supportMessage = dpm.getShortSupportMessageForUser(
                            enforcedAdmin.component, enforcedAdmin.userId);
                }
                if (supportMessage != null) {
                    TextView textView = (TextView) root.findViewById(R.id.admin_support_msg);
                    textView.setText(supportMessage);
                }
            }
        }

        root.findViewById(R.id.admins_policies_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        if (enforcedAdmin.component != null) {
                            intent.setClass(activity, DeviceAdminAdd.class);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                    enforcedAdmin.component);
                            intent.putExtra(DeviceAdminAdd.EXTRA_CALLED_FROM_SUPPORT_DIALOG, true);
                            // DeviceAdminAdd class may need to run as managed profile.
                            activity.startActivityAsUser(intent,
                                    new UserHandle(enforcedAdmin.userId));
                        } else {
                            intent.setClass(activity, Settings.DeviceAdminSettingsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            // Activity merges both managed profile and parent users
                            // admins so show as same user as this activity.
                            activity.startActivity(intent);
                        }
                        if (finishActivity) {
                            activity.finish();
                        }
                    }
                });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
