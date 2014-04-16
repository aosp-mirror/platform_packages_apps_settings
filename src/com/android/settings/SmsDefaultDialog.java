/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.telephony.TelephonyManager;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;
import com.android.settings.R;

public final class SmsDefaultDialog extends AlertActivity implements
        DialogInterface.OnClickListener {
    private ComponentName mNewDefault;
    private SmsApplicationData mNewSmsApplicationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String packageName = intent.getStringExtra(Intents.EXTRA_PACKAGE_NAME);

        setResult(RESULT_CANCELED);
        if (!buildDialog(packageName)) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                SmsApplication.setDefaultApplication(mNewSmsApplicationData.mPackageName, this);
                setResult(RESULT_OK);
                break;
            case BUTTON_NEGATIVE:
                break;
        }
    }

    private boolean buildDialog(String packageName) {
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (!tm.isSmsCapable()) {
            // No phone, no SMS
            return false;
        }

        mNewSmsApplicationData = SmsApplication.getSmsApplicationData(packageName, this);
        if (mNewSmsApplicationData == null) {
            return false;
        }

        SmsApplicationData oldSmsApplicationData = null;
        ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(this, true);
        if (oldSmsComponent != null) {
            oldSmsApplicationData =
                    SmsApplication.getSmsApplicationData(oldSmsComponent.getPackageName(), this);
            if (oldSmsApplicationData.mPackageName.equals(mNewSmsApplicationData.mPackageName)) {
                return false;
            }
        }

        // Compose dialog; get
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.sms_change_default_dialog_title);
        if (oldSmsApplicationData != null) {
            p.mMessage = getString(R.string.sms_change_default_dialog_text,
                    mNewSmsApplicationData.mApplicationName,
                    oldSmsApplicationData.mApplicationName);
        } else {
            p.mMessage = getString(R.string.sms_change_default_no_previous_dialog_text,
                    mNewSmsApplicationData.mApplicationName);
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();

        return true;
    }
}