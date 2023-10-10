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

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import static com.android.internal.notification.NotificationAccessConfirmationActivityContract.EXTRA_COMPONENT_NAME;
import static com.android.internal.notification.NotificationAccessConfirmationActivityContract.EXTRA_USER_ID;

import android.Manifest;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/** @hide */
public class NotificationAccessConfirmationActivity extends Activity
        implements DialogInterface {

    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "NotificationAccessConfirmationActivity";

    private int mUserId;
    private ComponentName mComponentName;
    private NotificationManager mNm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

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
        String requiredPermission = Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE;
        try {
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(mComponentName, 0);
            if (!requiredPermission.equals(serviceInfo.permission)) {
                Slog.e(LOG_TAG,
                        "Service " + mComponentName + " lacks permission " + requiredPermission);
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(LOG_TAG, "Failed to get service info for " + mComponentName, e);
            return;
        }

        mNm.setNotificationListenerAccessGranted(mComponentName, true);

        finish();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return AlertActivity.dispatchPopulateAccessibilityEvent(this, event);
    }

    @Override
    public void onBackPressed() {
        // Suppress finishing the activity on back button press,
        // consistently with the permission dialog behavior
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
