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
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.cardemulation.HostApduService;
import android.nfc.cardemulation.OffHostApduService;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

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
                CardEmulation.EXTRA_SERVICE_COMPONENT);
        String category = intent.getStringExtra(CardEmulation.EXTRA_CATEGORY);

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

        if (!CardEmulation.CATEGORY_PAYMENT.equals(category)) {
            Log.e(TAG, "Don't support defaults for category " + category);
            return false;
        }

        // Check if passed in service exists
        PaymentAppInfo requestedPaymentApp = null;
        PaymentAppInfo defaultPaymentApp = null;

        List<PaymentAppInfo> services = mBackend.getPaymentAppInfos();
        for (PaymentAppInfo service : services) {
            if (component.equals(service.componentName)) {
                requestedPaymentApp = service;
            }
            if (service.isDefault) {
                defaultPaymentApp = service;
            }
        }

        if (requestedPaymentApp == null) {
            Log.e(TAG, "Component " + component + " is not a registered payment service.");
            return false;
        }

        // Get current mode and default component
        ComponentName defaultComponent = mBackend.getDefaultPaymentApp();
        if (defaultComponent != null && defaultComponent.equals(component)) {
            Log.e(TAG, "Component " + component + " is already default.");
            return false;
        }

        mNewDefault = component;
        // Compose dialog; get
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.nfc_payment_set_default_label);
        if (defaultPaymentApp == null) {
            String formatString = getString(R.string.nfc_payment_set_default);
            String msg = String.format(formatString, requestedPaymentApp.caption);
            p.mMessage = msg;
        } else {
            String formatString = getString(R.string.nfc_payment_set_default_instead_of);
            String msg = String.format(formatString, requestedPaymentApp.caption,
                    defaultPaymentApp.caption);
            p.mMessage = msg;
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();

        return true;
    }

}
