/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.notification.AppNotificationSettings.Backend;
import com.android.settings.notification.AppNotificationSettings.AppRow;

public class AppNotificationDialog extends AlertActivity {
    private static final String TAG = "AppNotificationDialog";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    /**
     * Show a checkbox in the per-app notification control dialog to allow the user to
     * selectively redact this app's notifications on the lockscreen.
     */
    private static final boolean ENABLE_APP_NOTIFICATION_PRIVACY_OPTION = false;

    private final Context mContext = this;
    private final Backend mBackend = new Backend();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + getIntent());
        if (!buildDialog()) {
            Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean buildDialog() {
        final Intent intent = getIntent();
        if (intent != null) {
            final int uid = intent.getIntExtra(Settings.EXTRA_APP_UID, -1);
            final String pkg = intent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
            if (uid != -1 && !TextUtils.isEmpty(pkg)) {
                if (DEBUG) Log.d(TAG, "Load details for pkg=" + pkg + " uid=" + uid);
                final PackageManager pm = getPackageManager();
                final PackageInfo info = findPackageInfo(pm, pkg, uid);
                if (info != null) {
                    final AppRow row = AppNotificationSettings.loadAppRow(pm, info, mBackend);
                    final AlertController.AlertParams p = mAlertParams;
                    p.mView = getLayoutInflater().inflate(R.layout.notification_app_dialog,
                            null, false);
                    p.mPositiveButtonText = getString(R.string.app_notifications_dialog_done);
                    bindDialog(p.mView, row);
                    setupAlert();
                    return true;
                } else {
                    Log.w(TAG, "Failed to find package info");
                }
            } else {
                Log.w(TAG, "Missing extras: " + Settings.EXTRA_APP_PACKAGE + " was " + pkg + ", "
                        + Settings.EXTRA_APP_UID + " was " + uid);
            }
        } else {
            Log.w(TAG, "No intent");
        }
        return false;
    }

    private static PackageInfo findPackageInfo(PackageManager pm, String pkg, int uid) {
        final String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            final int N = packages.length;
            for (int i = 0; i < N; i++) {
                final String p = packages[i];
                if (pkg.equals(p)) {
                    try {
                        return pm.getPackageInfo(pkg, 0);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }

    private void bindDialog(final View v, final AppRow row) {
        final ImageView icon = (ImageView) v.findViewById(android.R.id.icon);
        icon.setImageDrawable(row.icon);
        final TextView title = (TextView) v.findViewById(android.R.id.title);
        title.setText(row.label);
        final CheckBox showNotifications = (CheckBox) v.findViewById(android.R.id.button1);
        final CheckBox highPriority = (CheckBox) v.findViewById(android.R.id.button2);
        final CheckBox sensitive = (CheckBox) v.findViewById(android.R.id.button3);

        if (!ENABLE_APP_NOTIFICATION_PRIVACY_OPTION) {
            sensitive.setVisibility(View.GONE);
        }

        showNotifications.setChecked(!row.banned);
        final OnCheckedChangeListener showListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean success = mBackend.setNotificationsBanned(row.pkg, row.uid, !isChecked);
                if (success) {
                    row.banned = !isChecked;
                    highPriority.setEnabled(!row.banned);
                    sensitive.setEnabled(!row.banned);
                } else {
                    showNotifications.setOnCheckedChangeListener(null);
                    showNotifications.setChecked(!isChecked);
                    showNotifications.setOnCheckedChangeListener(this);
                }
            }
        };
        showNotifications.setOnCheckedChangeListener(showListener);

        highPriority.setChecked(row.priority);
        final OnCheckedChangeListener priListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean success = mBackend.setHighPriority(row.pkg, row.uid, isChecked);
                if (success) {
                    row.priority = isChecked;
                } else {
                    highPriority.setOnCheckedChangeListener(null);
                    highPriority.setChecked(!isChecked);
                    highPriority.setOnCheckedChangeListener(this);
                }
            }
        };
        highPriority.setOnCheckedChangeListener(priListener);

        sensitive.setChecked(row.sensitive);
        final OnCheckedChangeListener senListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean success = mBackend.setSensitive(row.pkg, row.uid, isChecked);
                if (success) {
                    row.sensitive = isChecked;
                } else {
                    sensitive.setOnCheckedChangeListener(null);
                    sensitive.setChecked(!isChecked);
                    sensitive.setOnCheckedChangeListener(this);
                }
            }
        };
        sensitive.setOnCheckedChangeListener(senListener);

        highPriority.setEnabled(!row.banned);
        sensitive.setEnabled(!row.banned);
    }
}
