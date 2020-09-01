/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.annotation.StyleRes;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.wifi.AccessPoint;

/**
 * Dialog for users to edit a Wi-Fi network.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in {@link WifiDialog2}.
 */
public class WifiDialog extends AlertDialog implements WifiConfigUiBase,
        DialogInterface.OnClickListener {

    public interface WifiDialogListener {
        default void onForget(WifiDialog dialog) {
        }

        default void onSubmit(WifiDialog dialog) {
        }

        default void onScan(WifiDialog dialog, String ssid) {
        }
    }

    private static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
    private static final int BUTTON_FORGET = DialogInterface.BUTTON_NEUTRAL;

    private final int mMode;
    private final WifiDialogListener mListener;
    private final AccessPoint mAccessPoint;

    private View mView;
    private WifiConfigController mController;
    private boolean mHideSubmitButton;

    /**
     * Creates a WifiDialog with no additional style. It displays as a dialog above the current
     * view.
     */
    public static WifiDialog createModal(Context context, WifiDialogListener listener,
            AccessPoint accessPoint, int mode) {
        return new WifiDialog(context, listener, accessPoint, mode, 0 /* style */,
                mode == WifiConfigUiBase.MODE_VIEW /* hideSubmitButton */);
    }

    /**
     * Creates a WifiDialog with customized style. It displays as a dialog above the current
     * view.
     */
    public static WifiDialog createModal(Context context, WifiDialogListener listener,
        AccessPoint accessPoint, int mode, @StyleRes int style) {
        return new WifiDialog(context, listener, accessPoint, mode, style,
                mode == WifiConfigUiBase.MODE_VIEW /* hideSubmitButton */);
    }

    /* package */ WifiDialog(Context context, WifiDialogListener listener, AccessPoint accessPoint,
            int mode, @StyleRes int style, boolean hideSubmitButton) {
        super(context, style);
        mMode = mode;
        mListener = listener;
        mAccessPoint = accessPoint;
        mHideSubmitButton = hideSubmitButton;
    }

    @Override
    public WifiConfigController getController() {
        return mController;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.wifi_dialog, /* root */ null);
        setView(mView);
        mController = new WifiConfigController(this, mView, mAccessPoint, mMode);
        super.onCreate(savedInstanceState);

        if (mHideSubmitButton) {
            mController.hideSubmitButton();
        } else {
            /* During creation, the submit button can be unavailable to determine
             * visibility. Right after creation, update button visibility */
            mController.enableSubmitIfAppropriate();
        }

        if (mAccessPoint == null) {
            mController.hideForgetButton();
        }
    }

    @Override
    protected void onStart() {
        final ImageButton ssidScannerButton = findViewById(R.id.ssid_scanner_button);
        if (mHideSubmitButton) {
            ssidScannerButton.setVisibility(View.GONE);
            return;
        }

        View.OnClickListener onClickScannerButtonListener = v -> {
            if (mListener == null) {
                return;
            }

            final TextView ssidEditText = findViewById(R.id.ssid);
            final String ssid = ssidEditText.getText().toString();
            mListener.onScan(/* WifiDialog */ this, ssid);
        };
        ssidScannerButton.setOnClickListener(onClickScannerButtonListener);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mController.updatePassword();
    }

    @Override
    public void dispatchSubmit() {
        if (mListener != null) {
            mListener.onSubmit(this);
        }
        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int id) {
        if (mListener != null) {
            switch (id) {
                case BUTTON_SUBMIT:
                    mListener.onSubmit(this);
                    break;
                case BUTTON_FORGET:
                    if (WifiUtils.isNetworkLockedDown(getContext(), mAccessPoint.getConfig())) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                                RestrictedLockUtilsInternal.getDeviceOwner(getContext()));
                        return;
                    }
                    mListener.onForget(this);
                    break;
            }
        }
    }

    @Override
    public int getMode() {
        return mMode;
    }

    @Override
    public Button getSubmitButton() {
        return getButton(BUTTON_SUBMIT);
    }

    @Override
    public Button getForgetButton() {
        return getButton(BUTTON_FORGET);
    }

    @Override
    public Button getCancelButton() {
        return getButton(BUTTON_NEGATIVE);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        setButton(BUTTON_SUBMIT, text, this);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        setButton(BUTTON_FORGET, text, this);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(BUTTON_NEGATIVE, text, this);
    }
}
