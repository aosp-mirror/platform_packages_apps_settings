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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Proxy;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * To start the Proxy Selector activity, create the following intent.
 *
 * <code>
 *      Intent intent = new Intent();
 *      intent.setClassName("com.android.browser.ProxySelector");
 *      startActivity(intent);
 * </code>
 *
 * you can add extra options to the intent by using
 *
 * <code>
 *   intent.putExtra(key, value);
 * </code>
 *
 * the extra options are:
 *
 * button-label: a string label to display for the okay button
 * title:        the title of the window
 * error-text:   If not null, will be used as the label of the error message.
 */
public class ProxySelector extends Activity
{
    private final static String LOGTAG = "Settings";

    EditText    mHostnameField;
    EditText    mPortField;
    Button      mOKButton;

    // Matches blank input, ips, and domain names
    private static final String HOSTNAME_REGEXP = "^$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$";
    private static final Pattern HOSTNAME_PATTERN;
    static {
        HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
    }

    private static final int ERROR_DIALOG_ID = 0;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (android.util.Config.LOGV) Log.v(LOGTAG, "[ProxySelector] onStart");

        setContentView(R.layout.proxy);
        initView();
        populateFields(false);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ERROR_DIALOG_ID) {
            String hostname = mHostnameField.getText().toString().trim();
            String portStr = mPortField.getText().toString().trim();
            String msg = getString(validate(hostname, portStr));

            return new AlertDialog.Builder(this)
                    .setTitle(R.string.proxy_error)
                    .setPositiveButton(R.string.proxy_error_dismiss, null)
                    .setMessage(msg)
                    .create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        if (id == ERROR_DIALOG_ID) {
            String hostname = mHostnameField.getText().toString().trim();
            String portStr = mPortField.getText().toString().trim();
            String msg = getString(validate(hostname, portStr));
            ((AlertDialog)dialog).setMessage(msg);
        }
    }

    void initView() {

        mHostnameField = (EditText)findViewById(R.id.hostname);
        mHostnameField.setOnFocusChangeListener(mOnFocusChangeHandler);

        mPortField = (EditText)findViewById(R.id.port);
        mPortField.setOnClickListener(mOKHandler);
        mPortField.setOnFocusChangeListener(mOnFocusChangeHandler);

        mOKButton = (Button)findViewById(R.id.action);
        mOKButton.setOnClickListener(mOKHandler);

        Button b = (Button)findViewById(R.id.clear);
        b.setOnClickListener(mClearHandler);

        b = (Button)findViewById(R.id.defaultView);
        b.setOnClickListener(mDefaultHandler);
    }

    void populateFields(boolean useDefault) {
        String hostname = null;
        int port = -1;
        if (useDefault) {
            // Use the default proxy settings provided by the carrier
            hostname = Proxy.getDefaultHost();
            port = Proxy.getDefaultPort();
        } else {
            // Use the last setting given by the user
            hostname = Proxy.getHost(this);
            port = Proxy.getPort(this);
        }

        if (hostname == null) {
            hostname = "";
        }

        mHostnameField.setText(hostname);

        String portStr = port == -1 ? "" : Integer.toString(port);
        mPortField.setText(portStr);

        Intent intent = getIntent();

        String buttonLabel = intent.getStringExtra("button-label");
        if (!TextUtils.isEmpty(buttonLabel)) {
            mOKButton.setText(buttonLabel);
        }

        String title = intent.getStringExtra("title");
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }
    }

    /**
     * validate syntax of hostname and port entries
     * @return 0 on success, string resource ID on failure
     */
    int validate(String hostname, String port) {
        Matcher match = HOSTNAME_PATTERN.matcher(hostname);

        if (!match.matches()) return R.string.proxy_error_invalid_host;

        if (hostname.length() > 0 && port.length() == 0) {
            return R.string.proxy_error_empty_port;
        }

        if (port.length() > 0) {
            if (hostname.length() == 0) {
                return R.string.proxy_error_empty_host_set_port;
            }
            int portVal = -1;
            try {
                portVal = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                return R.string.proxy_error_invalid_port;
            }
            if (portVal <= 0 || portVal > 0xFFFF) {
                return R.string.proxy_error_invalid_port;
            }
        }
        return 0;
    }

    /**
     * returns true on success, false if the user must correct something
     */
    boolean saveToDb() {

        String hostname = mHostnameField.getText().toString().trim();
        String portStr = mPortField.getText().toString().trim();
        int port = -1;

        int result = validate(hostname, portStr);
        if (result > 0) {
            showDialog(ERROR_DIALOG_ID);
            return false;
        }

        if (portStr.length() > 0) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                return false;
            }
        }

        // FIXME: The best solution would be to make a better UI that would
        // disable editing of the text boxes if the user chooses to use the
        // default settings. i.e. checking a box to always use the default
        // carrier. http:/b/issue?id=756480
        // FIXME: This currently will not work if the default host is blank and
        // the user has cleared the input boxes in order to not use a proxy.
        // This is a UI problem and can be solved with some better form
        // controls.
        // FIXME: If the user types in a proxy that matches the default, should
        // we keep that setting? Can be fixed with a new UI.
        ContentResolver res = getContentResolver();
        if (hostname.equals(Proxy.getDefaultHost())
                && port == Proxy.getDefaultPort()) {
            // If the user hit the default button and didn't change any of
            // the input boxes, treat it as if the user has not specified a
            // proxy.
            hostname = null;
        }

        if (!TextUtils.isEmpty(hostname)) {
            hostname += ':' + portStr;
        }
        Settings.Secure.putString(res, Settings.Secure.HTTP_PROXY, hostname);
        sendBroadcast(new Intent(Proxy.PROXY_CHANGE_ACTION));

        return true;
    }

    OnClickListener mOKHandler = new OnClickListener() {
            public void onClick(View v) {
                if (saveToDb()) {
                    finish();
                }
            }
        };

    OnClickListener mClearHandler = new OnClickListener() {
            public void onClick(View v) {
                mHostnameField.setText("");
                mPortField.setText("");
            }
        };

    OnClickListener mDefaultHandler = new OnClickListener() {
            public void onClick(View v) {
                populateFields(true);
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
}
