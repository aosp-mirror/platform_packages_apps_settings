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
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import com.android.settings.nfc.PaymentBackend.PaymentInfo;

import java.util.List;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

public final class PaymentDefaultDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    public static final String TAG = "PaymentDefaultDialog";
    private static final int PAYMENT_APP_MAX_CAPTION_LENGTH = 40;

    private PaymentBackend mBackend;
    private PaymentInfo mNewDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addPrivateFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        try {
            mBackend = new PaymentBackend(this);
        } catch (NullPointerException e) {
            finish();
        }
        Intent intent = getIntent();
        ComponentName component = intent.getParcelableExtra(
                CardEmulation.EXTRA_SERVICE_COMPONENT);
        String category = intent.getStringExtra(CardEmulation.EXTRA_CATEGORY);
        int userId = intent.getIntExtra(CardEmulation.EXTRA_USERID, UserHandle.myUserId());

        setResult(RESULT_CANCELED);
        if (!buildDialog(component, category, userId)) {
            finish();
        }

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                mBackend.setDefaultPaymentApp(mNewDefault.componentName, mNewDefault.userId);
                setResult(RESULT_OK);
                break;
            case BUTTON_NEGATIVE:
                break;
        }
    }

    private boolean buildDialog(ComponentName component, String category, int userId) {
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
            // check if userId matches
            if (component.equals(service.componentName)
                    && service.userHandle.getIdentifier() == userId) {
                requestedPaymentApp = service;
            }
            if (service.isDefault && service.userHandle.getIdentifier() == userId) {
                defaultPaymentApp = service;
            }
        }

        if (requestedPaymentApp == null) {
            Log.e(TAG, "Component " + component + " is not a registered payment service.");
            return false;
        }

        // Get current mode and default component
        PaymentInfo defaultComponent = mBackend.getDefaultPaymentApp();
        if (defaultComponent != null && defaultComponent.componentName.equals(component)
                && defaultComponent.userId == userId) {
            Log.e(TAG, "Component " + component + " is already default.");
            return false;
        }

        mNewDefault = new PaymentInfo();
        mNewDefault.componentName = component;
        mNewDefault.userId = userId;

        // Compose dialog; get
        final AlertController.AlertParams p = mAlertParams;
        if (defaultPaymentApp == null) {
            p.mTitle = getString(R.string.nfc_payment_set_default_label);
            String formatString = getString(R.string.nfc_payment_set_default);
            String msg = String.format(formatString,
                    sanitizePaymentAppCaption(requestedPaymentApp.label.toString()));
            p.mMessage = msg;
            p.mPositiveButtonText = getString(R.string.nfc_payment_btn_text_set_deault);
        } else {
            p.mTitle = getString(R.string.nfc_payment_update_default_label);
            String formatString = getString(R.string.nfc_payment_set_default_instead_of);
            String msg = String.format(formatString,
                    sanitizePaymentAppCaption(requestedPaymentApp.label.toString()),
                    sanitizePaymentAppCaption(defaultPaymentApp.label.toString()));
            p.mMessage = msg;
            p.mPositiveButtonText = getString(R.string.nfc_payment_btn_text_update);
        }
        p.mNegativeButtonText = getString(R.string.cancel);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();

        return true;
    }

    private String sanitizePaymentAppCaption(String input) {
        String sanitizedString = input.replace('\n', ' ').replace('\r', ' ').trim();


        if (sanitizedString.length() > PAYMENT_APP_MAX_CAPTION_LENGTH) {
            return sanitizedString.substring(0, PAYMENT_APP_MAX_CAPTION_LENGTH);
        }

        return sanitizedString;
    }

}
