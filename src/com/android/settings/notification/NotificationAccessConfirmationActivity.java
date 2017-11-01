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

import static com.android.internal.notification.NotificationAccessConfirmationActivityContract
        .EXTRA_COMPONENT_NAME;
import static com.android.internal.notification.NotificationAccessConfirmationActivityContract
        .EXTRA_PACKAGE_TITLE;
import static com.android.internal.notification.NotificationAccessConfirmationActivityContract
        .EXTRA_USER_ID;

import android.Manifest;
import android.annotation.Nullable;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.SettingsStringUtil;
import android.util.Slog;
import android.view.accessibility.AccessibilityEvent;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.core.TouchOverlayManager;

/** @hide */
public class NotificationAccessConfirmationActivity extends Activity
        implements DialogInterface {

    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "NotificationAccessConfirmationActivity";

    private int mUserId;
    private ComponentName mComponentName;
    private TouchOverlayManager mTouchOverlayManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTouchOverlayManager = new TouchOverlayManager(this);

        mComponentName = getIntent().getParcelableExtra(EXTRA_COMPONENT_NAME);
        mUserId = getIntent().getIntExtra(EXTRA_USER_ID, UserHandle.USER_NULL);
        String pkgTitle = getIntent().getStringExtra(EXTRA_PACKAGE_TITLE);

        AlertController.AlertParams p = new AlertController.AlertParams(this);
        p.mTitle = getString(
                R.string.notification_listener_security_warning_title,
                pkgTitle);
        p.mMessage = getString(
                R.string.notification_listener_security_warning_summary,
                pkgTitle);
        p.mPositiveButtonText = getString(R.string.allow);
        p.mPositiveButtonListener = (a, b) -> onAllow();
        p.mNegativeButtonText = getString(R.string.deny);
        p.mNegativeButtonListener = (a, b) -> cancel();
        AlertController
                .create(this, this, getWindow())
                .installContent(p);
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

        final SettingsStringUtil.SettingStringHelper setting =
                 new SettingsStringUtil.SettingStringHelper(
                         getContentResolver(),
                         Settings.Secure.ENABLED_NOTIFICATION_LISTENERS,
                         mUserId);
        setting.write(SettingsStringUtil.ComponentNameSet.add(setting.read(), mComponentName));

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

    @Override
    protected void onResume() {
        super.onResume();
        mTouchOverlayManager.setOverlayAllowed(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTouchOverlayManager.setOverlayAllowed(true);
    }
}
