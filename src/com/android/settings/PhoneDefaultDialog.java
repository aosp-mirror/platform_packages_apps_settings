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

package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.telecomm.PhoneApplication;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * Displays the dialog that provides a list of installed phone applications to allow the user to
 * select a default phone application.
 */
public final class PhoneDefaultDialog extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String TAG = "PhoneDefaultDialog";
    private ComponentName mNewComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String packageName = getIntent().getStringExtra(Intents.EXTRA_PACKAGE_NAME);

        setResult(RESULT_CANCELED);
        if (!buildDialog(packageName)) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                PhoneApplication.setDefaultPhoneApplication(mNewComponent.getPackageName(),
                        this);
                setResult(RESULT_OK);
                break;
            case BUTTON_NEGATIVE:
                break;
        }
    }

    private boolean buildDialog(String packageName) {
        final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (!tm.isVoiceCapable()) {
            Log.w(TAG, "Dialog launched but device is not voice capable.");
            return false;
        }

        mNewComponent = PhoneApplication.getPhoneApplicationForPackageName(this,
                packageName);
        if (mNewComponent == null) {
            Log.w(TAG,
                    "Provided package name does not correspond to an installed Phone application.");
            return false;
        }

        final ComponentName oldComponent = PhoneApplication.getDefaultPhoneApplication(this);
        if (oldComponent != null &&
                TextUtils.equals(oldComponent.getPackageName(), mNewComponent.getPackageName())) {
            return false;
        }

        final PackageManager pm = getPackageManager();
        final String newComponentLabel =
                getApplicationLabelForPackageName(pm, mNewComponent.getPackageName());
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.phone_change_default_dialog_title);
        if (oldComponent != null) {
            String oldComponentLabel =
                    getApplicationLabelForPackageName(pm, oldComponent.getPackageName());
            p.mMessage = getString(R.string.sms_change_default_dialog_text,
                    newComponentLabel,
                    oldComponentLabel);
        } else {
            p.mMessage = getString(R.string.sms_change_default_no_previous_dialog_text,
                    newComponentLabel);
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();

        return true;
    }

    /**
     * Returns the application label that corresponds to the given package name
     *
     * @param pm An instance of a {@link PackageManager}.
     * @param packageName A valid package name.
     *
     * @return Application label for the given package name, or null if not found.
     */
    private String getApplicationLabelForPackageName(PackageManager pm, String packageName) {
        ApplicationInfo info = null;
        try {
            info = pm.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Application info not found for packageName " + packageName);
        }
        if (info == null) {
            return packageName;
        } else {
            return info.loadLabel(pm).toString();
        }
    }
}