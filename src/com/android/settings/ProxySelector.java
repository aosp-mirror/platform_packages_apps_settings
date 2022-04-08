/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Proxy;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.SettingsPreferenceFragment.SettingsDialogFragment;
import com.android.settings.core.InstrumentedFragment;

public class ProxySelector extends InstrumentedFragment implements DialogCreatable {
    private static final String TAG = "ProxySelector";

    EditText    mHostnameField;
    EditText    mPortField;
    EditText    mExclusionListField;
    Button      mOKButton;
    Button      mClearButton;
    Button      mDefaultButton;

    private static final int ERROR_DIALOG_ID = 0;

    private SettingsDialogFragment mDialogFragment;
    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.proxy, container, false);
        initView(mView);
        // TODO: Populate based on connection status
        populateFields();
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final DevicePolicyManager dpm =
                (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        final boolean userSetGlobalProxy = (dpm.getGlobalProxyAdmin() == null);
        // Disable UI if the Global Proxy is being controlled by a Device Admin
        mHostnameField.setEnabled(userSetGlobalProxy);
        mPortField.setEnabled(userSetGlobalProxy);
        mExclusionListField.setEnabled(userSetGlobalProxy);
        mOKButton.setEnabled(userSetGlobalProxy);
        mClearButton.setEnabled(userSetGlobalProxy);
        mDefaultButton.setEnabled(userSetGlobalProxy);
    }

    // Dialog management

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == ERROR_DIALOG_ID) {
            String hostname = mHostnameField.getText().toString().trim();
            String portStr = mPortField.getText().toString().trim();
            String exclList = mExclusionListField.getText().toString().trim();
            String msg = getActivity().getString(validate(hostname, portStr, exclList));

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.proxy_error)
                    .setPositiveButton(R.string.proxy_error_dismiss, null)
                    .setMessage(msg)
                    .create();
        }
        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        return SettingsEnums.DIALOG_PROXY_SELECTOR_ERROR;
    }

    private void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = SettingsDialogFragment.newInstance(this, dialogId);
        mDialogFragment.show(getActivity().getSupportFragmentManager(), Integer.toString(dialogId));
    }

    private void initView(View view) {
        mHostnameField = (EditText)view.findViewById(R.id.hostname);
        mHostnameField.setOnFocusChangeListener(mOnFocusChangeHandler);

        mPortField = (EditText)view.findViewById(R.id.port);
        mPortField.setOnClickListener(mOKHandler);
        mPortField.setOnFocusChangeListener(mOnFocusChangeHandler);

        mExclusionListField = (EditText)view.findViewById(R.id.exclusionlist);
        mExclusionListField.setOnFocusChangeListener(mOnFocusChangeHandler);

        mOKButton = (Button)view.findViewById(R.id.action);
        mOKButton.setOnClickListener(mOKHandler);

        mClearButton = (Button)view.findViewById(R.id.clear);
        mClearButton.setOnClickListener(mClearHandler);

        mDefaultButton = (Button)view.findViewById(R.id.defaultView);
        mDefaultButton.setOnClickListener(mDefaultHandler);
    }

    void populateFields() {
        final Activity activity = getActivity();
        String hostname = "";
        int port = -1;
        String exclList = "";
        // Use the last setting given by the user
        ConnectivityManager cm =
                (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        ProxyInfo proxy = cm.getGlobalProxy();
        if (proxy != null) {
            hostname = proxy.getHost();
            port = proxy.getPort();
            exclList = proxy.getExclusionListAsString();
        }

        if (hostname == null) {
            hostname = "";
        }

        mHostnameField.setText(hostname);

        String portStr = port == -1 ? "" : Integer.toString(port);
        mPortField.setText(portStr);

        mExclusionListField.setText(exclList);

        final Intent intent = activity.getIntent();

        String buttonLabel = intent.getStringExtra("button-label");
        if (!TextUtils.isEmpty(buttonLabel)) {
            mOKButton.setText(buttonLabel);
        }

        String title = intent.getStringExtra("title");
        if (!TextUtils.isEmpty(title)) {
            activity.setTitle(title);
        } else {
            activity.setTitle(R.string.proxy_settings_title);
        }
    }

    /**
     * validate syntax of hostname and port entries
     * @return 0 on success, string resource ID on failure
     */
    public static int validate(String hostname, String port, String exclList) {
        switch (Proxy.validate(hostname, port, exclList)) {
            case Proxy.PROXY_VALID:
                return 0;
            case Proxy.PROXY_HOSTNAME_EMPTY:
                return R.string.proxy_error_empty_host_set_port;
            case Proxy.PROXY_HOSTNAME_INVALID:
                return R.string.proxy_error_invalid_host;
            case Proxy.PROXY_PORT_EMPTY:
                return R.string.proxy_error_empty_port;
            case Proxy.PROXY_PORT_INVALID:
                return R.string.proxy_error_invalid_port;
            case Proxy.PROXY_EXCLLIST_INVALID:
                return R.string.proxy_error_invalid_exclusion_list;
            default:
                // should neven happen
                Log.e(TAG, "Unknown proxy settings error");
                return -1;
        }
    }

    /**
     * returns true on success, false if the user must correct something
     */
    boolean saveToDb() {

        String hostname = mHostnameField.getText().toString().trim();
        String portStr = mPortField.getText().toString().trim();
        String exclList = mExclusionListField.getText().toString().trim();
        int port = 0;

        int result = validate(hostname, portStr, exclList);
        if (result != 0) {
            showDialog(ERROR_DIALOG_ID);
            return false;
        }

        if (portStr.length() > 0) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                // should never happen - caught by validate above
                return false;
            }
        }
        ProxyInfo p = new ProxyInfo(hostname, port, exclList);
        // FIXME: The best solution would be to make a better UI that would
        // disable editing of the text boxes if the user chooses to use the
        // default settings. i.e. checking a box to always use the default
        // carrier. http:/b/issue?id=756480
        // FIXME: If the user types in a proxy that matches the default, should
        // we keep that setting? Can be fixed with a new UI.
        ConnectivityManager cm =
                (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        cm.setGlobalProxy(p);
        return true;
    }

    OnClickListener mOKHandler = new OnClickListener() {
            public void onClick(View v) {
                if (saveToDb()) {
                    getActivity().onBackPressed();
                }
            }
        };

    OnClickListener mClearHandler = new OnClickListener() {
            public void onClick(View v) {
                mHostnameField.setText("");
                mPortField.setText("");
                mExclusionListField.setText("");
            }
        };

    OnClickListener mDefaultHandler = new OnClickListener() {
            public void onClick(View v) {
                // TODO: populate based on connection status
                populateFields();
            }
        };

    OnFocusChangeListener mOnFocusChangeHandler = new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    TextView textView = (TextView) v;
                    Selection.selectAll((Spannable) textView.getText());
                }
            }
        };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PROXY_SELECTOR;
    }
}
