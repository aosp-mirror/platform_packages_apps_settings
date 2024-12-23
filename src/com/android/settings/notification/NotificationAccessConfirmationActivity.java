/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_APPS_CANNOT_ACCESS_NOTIFICATION_SETTINGS;
import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import static com.android.internal.notification.NotificationAccessConfirmationActivityContract.EXTRA_COMPONENT_NAME;
import static com.android.internal.notification.NotificationAccessConfirmationActivityContract.EXTRA_USER_ID;

import android.Manifest;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.Slog;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

import java.util.List;

/** @hide */
public class NotificationAccessConfirmationActivity extends Activity
        implements DialogInterface {

    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "NotificationAccessConfirmationActivity";

    private int mUserId;
    private ComponentName mComponentName;
    private NotificationManager mNm;

    private DevicePolicyManager mDpm;
    private UserManager mUm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        mUm = getSystemService(UserManager.class);
        mDpm = getSystemService(DevicePolicyManager.class);

        if (mUm.isManagedProfile()) {
            Slog.w(LOG_TAG, "Apps in the work profile do not support notification listeners");
            Toast.makeText(this,
                    mDpm.getResources().getString(WORK_APPS_CANNOT_ACCESS_NOTIFICATION_SETTINGS,
                            () -> getString(R.string.notification_settings_work_profile)),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mNm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mComponentName = getIntent().getParcelableExtra(EXTRA_COMPONENT_NAME);
        mUserId = getIntent().getIntExtra(EXTRA_USER_ID, UserHandle.USER_NULL);
        CharSequence mAppLabel;

        if (mComponentName == null || mComponentName.getPackageName() == null
                || mComponentName.flattenToString().length()
                > NotificationManager.MAX_SERVICE_COMPONENT_NAME_LENGTH) {
            finish();
            return;
        }

        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                    mComponentName.getPackageName(), 0);
            mAppLabel = applicationInfo.loadSafeLabel(getPackageManager(),
                    PackageItemInfo.DEFAULT_MAX_LABEL_SIZE_PX,
                    PackageItemInfo.SAFE_LABEL_FLAG_TRIM
                            | PackageItemInfo.SAFE_LABEL_FLAG_FIRST_LINE);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(LOG_TAG, "Couldn't find app with package name for " + mComponentName, e);
            finish();
            return;
        }

        if (TextUtils.isEmpty(mAppLabel)) {
            finish();
            return;
        }

        // Check NLS service info.
        String requiredPermission = Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE;
        Intent NLSIntent = new Intent(NotificationListenerService.SERVICE_INTERFACE);
        List<ResolveInfo> matchedServiceList = getPackageManager().queryIntentServicesAsUser(
                NLSIntent, /* flags */ 0, mUserId);
        boolean hasNLSIntentFilter = false;
        for (ResolveInfo service : matchedServiceList) {
            if (service.serviceInfo.packageName.equals(mComponentName.getPackageName())) {
                if (!requiredPermission.equals(service.serviceInfo.permission)) {
                    Slog.e(LOG_TAG, "Service " + mComponentName + " lacks permission "
                            + requiredPermission);
                    finish();
                    return;
                }
                hasNLSIntentFilter = true;
                break;
            }
        }
        if (!hasNLSIntentFilter) {
            Slog.e(LOG_TAG, "Service " + mComponentName + " lacks an intent-filter action "
                    + "for android.service.notification.NotificationListenerService.");
            finish();
            return;
        }

        AlertController.AlertParams p = new AlertController.AlertParams(this);
        p.mTitle = getString(
                R.string.notification_listener_security_warning_title,
                mAppLabel);
        p.mMessage = getString(
                R.string.notification_listener_security_warning_summary,
                mAppLabel);
        p.mPositiveButtonText = getString(R.string.allow);
        p.mPositiveButtonListener = (a, b) -> onAllow();
        p.mNegativeButtonText = getString(R.string.deny);
        p.mNegativeButtonListener = (a, b) -> cancel();
        AlertController
                .create(this, this, getWindow())
                .installContent(p);
        // Consistent with the permission dialog
        // Used instead of p.mCancelable as that is only honored for AlertDialog
        getWindow().setCloseOnTouchOutside(false); 
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(
                WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
    }

    @Override
    public void onPause() {
        getWindow().clearFlags(
                WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        super.onPause();
    }

    private void onAllow() {
        mNm.setNotificationListenerAccessGranted(mComponentName, true);

        finish();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return AlertActivity.dispatchPopulateAccessibilityEvent(this, event);
    }

    @Override
    public void cancel() {
        finish();
    }

    @Override
    public void dismiss() {
        // This is called after the click, since we finish when handling the
        // click, don't do that again here.
        if (!isFinishing()) {
            finish();
        }
    }
}
