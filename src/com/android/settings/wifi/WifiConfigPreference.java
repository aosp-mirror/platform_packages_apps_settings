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
import android.content.DialogInterface;
import android.preference.Preference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * Preference letting users modify a setting for Wifi network. This work as an alternative UI
 * for {@link WifiDialog} without shouwing popup Dialog.
 */
public class WifiConfigPreference extends Preference implements WifiConfigUiBase {
    private static final String TAG = "WifiConfigPreference";

    private WifiSettings mWifiSettings;
    private View mView;
    private final DialogInterface.OnClickListener mListener;
    private WifiConfigController mController;
    private AccessPoint mAccessPoint;
    private boolean mEdit;

    // Stores an View id to be focused. Used when view isn't available while setFocus() is called.
    private int mFocusId = -1;

    private LayoutInflater mInflater;

    public WifiConfigPreference(WifiSettings wifiSettings,
            DialogInterface.OnClickListener listener,
            AccessPoint accessPoint, boolean edit) {
        super(wifiSettings.getActivity());
        mWifiSettings = wifiSettings;
        // setLayoutResource(R.layout.wifi_config_preference);
        setLayoutResource(R.layout.wifi_config_preference2);
        mListener = listener;
        mAccessPoint = accessPoint;
        mEdit = edit;
        mInflater = (LayoutInflater)
                wifiSettings.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        // Called every time the list is created.
        if (mView == null) {
            mView = mInflater.inflate(getLayoutResource(), parent, false);
            mController = new WifiConfigController(this, mView, mAccessPoint, mEdit, mListener);
        }

        if (mFocusId >= 0) {
            trySetFocusAndLaunchSoftInput(mFocusId);
            mFocusId = -1;
        }

        return mView;
    }

    @Override
    public WifiConfigController getController() {
        return mController;
    }

    public void setFocus(int id) {
        if (mView != null) {
            trySetFocusAndLaunchSoftInput(id);
            mFocusId = -1;
        } else {
            mFocusId = id;
        }
    }

    private void trySetFocusAndLaunchSoftInput(int id) {
        final View viewToBeFocused = mView.findViewById(id);
        if (viewToBeFocused != null && viewToBeFocused.getVisibility() == View.VISIBLE) {
            viewToBeFocused.requestFocus();
            // TODO: doesn't work.
            if (viewToBeFocused instanceof EditText) {
                Log.d(TAG, "Focused View is EditText. We try showing the software keyboard");
                // viewToBeFocused.performClick();
                final InputMethodManager inputMethodManager =
                        (InputMethodManager)
                                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(viewToBeFocused, 0);
            }
        }
    }

    public View findViewById(int id) {
        return mView.findViewById(id);
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
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
        return (Button)mWifiSettings.getActivity().findViewById(R.id.wifi_setup_connect);
    }

    @Override
    public Button getForgetButton() {
        return (Button)mWifiSettings.getActivity().findViewById(R.id.wifi_setup_forget);
    }

    @Override
    public Button getCancelButton() {
        return (Button)mWifiSettings.getActivity().findViewById(R.id.wifi_setup_cancel);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        final Button button = (Button)
                mWifiSettings.getActivity().findViewById(R.id.wifi_setup_connect);
        button.setVisibility(View.VISIBLE);

        // test
        mWifiSettings.getActivity().findViewById(R.id.wifi_setup_forget).setVisibility(View.GONE);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        final Button button = (Button)
                mWifiSettings.getActivity().findViewById(R.id.wifi_setup_forget);
        button.setVisibility(View.VISIBLE);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        final Button button = (Button)
                mWifiSettings.getActivity().findViewById(R.id.wifi_setup_cancel);
        button.setVisibility(View.VISIBLE);
    }
}