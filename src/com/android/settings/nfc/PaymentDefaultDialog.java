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

package com.android.settings.nfc;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.cardemulation.CardEmulationManager;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;

import java.util.List;

public final class PaymentDefaultDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    public static final String TAG = "PaymentDefaultDialog";

    private PaymentBackend mBackend;
    private ComponentName mNewDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackend = new PaymentBackend(this);
        Intent intent = getIntent();
        ComponentName component = intent.getParcelableExtra(
                CardEmulationManager.EXTRA_SERVICE_COMPONENT);
        String category = intent.getStringExtra(CardEmulationManager.EXTRA_CATEGORY);

        setResult(RESULT_CANCELED);
        if (!buildDialog(component, category)) {
            finish();
        }

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                mBackend.setDefaultPaymentApp(mNewDefault);
                setResult(RESULT_OK);
                break;
            case BUTTON_NEGATIVE:
                break;
        }
    }

    private boolean buildDialog(ComponentName component, String category) {
        if (component == null || category == null) {
            Log.e(TAG, "Component or category are null");
            return false;
        }

        if (!CardEmulationManager.CATEGORY_PAYMENT.equals(category)) {
            Log.e(TAG, "Don't support defaults for category " + category);
            return false;
        }

        // Check if passed in service exists
        boolean found = false;

        List<PaymentAppInfo> services = mBackend.getPaymentAppInfos();
        for (PaymentAppInfo service : services) {
            if (component.equals(service.componentName)) {
                found = true;
                break;
            }
        }

        if (!found) {
            Log.e(TAG, "Component " + component + " is not a registered payment service.");
            return false;
        }

        // Get current mode and default component
        ComponentName defaultComponent = mBackend.getDefaultPaymentApp();
        if (defaultComponent != null && defaultComponent.equals(component)) {
            Log.e(TAG, "Component " + component + " is already default.");
            return false;
        }

        PackageManager pm = getPackageManager();
        ApplicationInfo newAppInfo;
        try {
            newAppInfo = pm.getApplicationInfo(component.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "PM could not load app info for " + component);
            return false;
        }
        ApplicationInfo defaultAppInfo = null;
        try {
            if (defaultComponent != null) {
                defaultAppInfo = pm.getApplicationInfo(defaultComponent.getPackageName(), 0);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "PM could not load app info for " + defaultComponent);
            // Continue intentionally
        }

        mNewDefault = component;

        // Compose dialog; get
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.nfc_payment_set_default);
        if (defaultAppInfo == null) {
            p.mMessage = "Always use " + newAppInfo.loadLabel(pm) + " when you tap and pay?";
        } else {
            p.mMessage = "Always use " + newAppInfo.loadLabel(pm) + " instead of " +
                    defaultAppInfo.loadLabel(pm) + " when you tap and pay?";
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();

        return true;
    }

}
