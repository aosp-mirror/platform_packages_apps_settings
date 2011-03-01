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

import com.android.settings.R;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * Shows simplified UI for configuring a wifi network. Used only in SetupWizard for XLarge
 * screen.
 */
public class WifiConfigUiForSetupWizardXL implements WifiConfigUiBase, OnFocusChangeListener {
    private static final String TAG = "SetupWizard";

    private Button mConnectButton;
    private Button mCancelButton;

    private final WifiSettingsForSetupWizardXL mActivity;
    private View mView;
    private WifiConfigController mController;
    private AccessPoint mAccessPoint;
    private boolean mEdit;
    private Handler mHandler = new Handler();

    private final InputMethodManager mInputMethodManager;

    private LayoutInflater mInflater;

    /**
     * @param activity Activity which creates this object.
     * @param parent Parent ViewGroup (typically some layout) holding a view object created by
     * this object
     * @param accessPoint target AccessPoint to be configured.
     * @param edit
     */
    public WifiConfigUiForSetupWizardXL(
            WifiSettingsForSetupWizardXL activity, ViewGroup parent,
            AccessPoint accessPoint, boolean edit) {
        mActivity = activity;
        mConnectButton = (Button)activity.findViewById(R.id.wifi_setup_connect);
        mCancelButton = (Button)activity.findViewById(R.id.wifi_setup_cancel);
        mAccessPoint = accessPoint;
        mEdit = edit;
        mInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mView = mInflater.inflate(R.layout.wifi_config_ui_for_setup_wizard, parent, true);
        mController = new WifiConfigController(this, mView, mAccessPoint, edit);

        mInputMethodManager = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (mView.findViewById(R.id.security_fields).getVisibility() == View.VISIBLE) {
            requestFocusAndShowKeyboard(R.id.password);
        } else if (mView.findViewById(R.id.type_ssid).getVisibility() == View.VISIBLE) {
            // Add Network flow.
            requestFocusAndShowKeyboard(R.id.ssid);
        }
    }

    /**
     * @param editViewId must be EditView
     */
    public void requestFocusAndShowKeyboard(int editViewId) {
        // Set Focus to password View.
        final View viewToBeFocused = mView.findViewById(editViewId);
        if (viewToBeFocused == null) {
            Log.w(TAG, "password field to be focused not found.");
        } else if (!(viewToBeFocused instanceof EditText)) {
            Log.w(TAG, "password field is not EditText");
        } else {
            if (viewToBeFocused.isFocused()) {
                Log.i(TAG, "Already focused");
                if (!mInputMethodManager.showSoftInput(viewToBeFocused, 0)) {
                    Log.w(TAG, "Failed to show SoftInput");
                }
            } else {
                // After acquiring the focus, we show software keyboard.
                viewToBeFocused.setOnFocusChangeListener(this);
                final boolean requestFocusResult = viewToBeFocused.requestFocus();
                Log.i(TAG, String.format("Focus request: %s",
                        (requestFocusResult ? "successful" : "failed")));
                if (!requestFocusResult) {
                    viewToBeFocused.setOnFocusChangeListener(null);
                }
            }
        }
    }

    public View getView() {
        return mView;
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    @Override
    public WifiConfigController getController() {
        return mController;
    }

    @Override
    public boolean isEdit() {
        return mEdit;
    }

    @Override
    public LayoutInflater getLayoutInflater() {
        return mInflater;
    }

    @Override
    public Button getSubmitButton() {
        return mConnectButton;
    }

    @Override
    public Button getForgetButton() {
        return null;
    }

    @Override
    public Button getCancelButton() {
        return mCancelButton;
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        mConnectButton.setVisibility(View.VISIBLE);
        mConnectButton.setText(text);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        // In XL setup screen, we won't show Forget button for simplifying the UI.
    }

    @Override
    public void setCancelButton(CharSequence text) {
        mCancelButton.setVisibility(View.VISIBLE);
        // We don't want "cancel" label given from caller.
        // mCancelButton.setText(text);
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public void setTitle(int id) {
        Log.d(TAG, "Ignoring setTitle");
    }

    @Override
    public void setTitle(CharSequence title) {
        Log.d(TAG, "Ignoring setTitle");
    }

    private class FocusRunnable implements Runnable {
        final View mViewToBeFocused;
        public FocusRunnable(View viewToBeFocused) {
            mViewToBeFocused = viewToBeFocused;
        }

        @Override
        public void run() {
            // mInputMethodManager.focusIn(mViewToBeFocused);
            final boolean showSoftInputResult =
                    mInputMethodManager.showSoftInput(mViewToBeFocused, 0);
            if (showSoftInputResult) {
                mActivity.setPaddingVisibility(View.GONE);
            } else {
                Log.w(TAG, "Failed to show software keyboard ");
            }
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        view.setOnFocusChangeListener(null);
        if (hasFocus) {
            mHandler.post(new FocusRunnable(view));
        }
    }
}