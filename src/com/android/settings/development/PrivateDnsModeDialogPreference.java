/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.development;

import static android.net.ConnectivityManager.PRIVATE_DNS_DEFAULT_MODE;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.PreferenceDialogFragment;
import android.support.v7.preference.DialogPreference;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreference;


public class PrivateDnsModeDialogPreference extends CustomDialogPreference
        implements OnCheckedChangeListener, TextWatcher, OnEditorActionListener {
    private static final String TAG = PrivateDnsModeDialogPreference.class.getSimpleName();

    private static final String MODE_KEY = Settings.Global.PRIVATE_DNS_MODE;
    private static final String HOSTNAME_KEY = Settings.Global.PRIVATE_DNS_SPECIFIER;
    private String mMode;
    private EditText mEditText;

    public static String getSummaryStringForModeFromSettings(ContentResolver cr, Resources res) {
        final String mode = getModeFromSettings(cr);
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return res.getString(R.string.private_dns_mode_off);
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return res.getString(R.string.private_dns_mode_opportunistic);
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return getHostnameFromSettings(cr);
            default:
                return "unknown";
        }
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PrivateDnsModeDialogPreference(Context context) {
        super(context);
    }

    // This is called first when the dialog is launched.
    @Override
    protected void onBindDialogView(View view) {
        final String mode = getModeFromSettings();

        RadioButton rb = (RadioButton) view.findViewById(R.id.private_dns_mode_off);
        if (mode.equals(PRIVATE_DNS_MODE_OFF)) rb.setChecked(true);
        rb.setOnCheckedChangeListener(this);

        rb = (RadioButton) view.findViewById(R.id.private_dns_mode_opportunistic);
        if (mode.equals(PRIVATE_DNS_MODE_OPPORTUNISTIC)) rb.setChecked(true);
        rb.setOnCheckedChangeListener(this);

        rb = (RadioButton) view.findViewById(R.id.private_dns_mode_provider);
        if (mode.equals(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) rb.setChecked(true);
        rb.setOnCheckedChangeListener(this);

        mEditText = (EditText) view.findViewById(R.id.private_dns_mode_provider_hostname);
        mEditText.setOnEditorActionListener(this);
        mEditText.addTextChangedListener(this);

        // (Mostly) Fix the EditText field's indentation to align underneath the
        // displayed radio button text, and not under the radio button itself.
        final int padding = rb.isLayoutRtl()
                ? rb.getCompoundPaddingRight()
                : rb.getCompoundPaddingLeft();
        final MarginLayoutParams marginParams = (MarginLayoutParams) mEditText.getLayoutParams();
        marginParams.setMarginStart(marginParams.getMarginStart() + padding);
        mEditText.setLayoutParams(marginParams);
        mEditText.setText(getHostnameFromSettings());

        setDialogValue(mode);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (!positiveResult) return;

        saveDialogValue();
        setSummary(getSummaryStringForModeFromSettings(
                getContext().getContentResolver(), getContext().getResources()));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) return;

        switch (buttonView.getId()) {
            case R.id.private_dns_mode_off:
                setDialogValue(PRIVATE_DNS_MODE_OFF);
                break;
            case R.id.private_dns_mode_opportunistic:
                setDialogValue(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                break;
            case R.id.private_dns_mode_provider:
                setDialogValue(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                break;
            default:
                // Unknown button; ignored.
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView tv, int actionId, KeyEvent k) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            saveDialogValue();
            getDialog().dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { return; }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { return; }

    @Override
    public void afterTextChanged(Editable s) {
        final String hostname = s.toString();
        final boolean appearsValid = isWeaklyValidatedHostname(hostname);
        // TODO: Disable the "positive button" ("Save") when appearsValid is false.
    }

    private void setDialogValue(String mode) {
        mMode = mode;
        final boolean txtEnabled = mMode.equals(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        mEditText.setEnabled(txtEnabled);
    }

    private void saveDialogValue() {
        if (!isValidMode(mMode)) {
            mMode = PRIVATE_DNS_DEFAULT_MODE;
        }

        if (mMode.equals(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            final String hostname = mEditText.getText().toString();
            if (isWeaklyValidatedHostname(hostname)) {
                saveHostnameToSettings(hostname);
            } else {
                // TODO: Once quasi-validation of hostnames works and acceptable
                // user signaling is working, this can be deleted.
                mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
                if (TextUtils.isEmpty(hostname)) saveHostnameToSettings("");
            }
        }

        saveModeToSettings(mMode);
    }

    private String getModeFromSettings() {
        return getModeFromSettings(getContext().getContentResolver());
    }

    private void saveModeToSettings(String value) {
        Settings.Global.putString(getContext().getContentResolver(), MODE_KEY, value);
    }

    private String getHostnameFromSettings() {
        return getHostnameFromSettings(getContext().getContentResolver());
    }

    private void saveHostnameToSettings(String hostname) {
        Settings.Global.putString(getContext().getContentResolver(), HOSTNAME_KEY, hostname);
    }

    private static String getModeFromSettings(ContentResolver cr) {
        final String mode = Settings.Global.getString(cr, MODE_KEY);
        return isValidMode(mode) ? mode : PRIVATE_DNS_DEFAULT_MODE;
    }

    private static boolean isValidMode(String mode) {
        return !TextUtils.isEmpty(mode) && (
                mode.equals(PRIVATE_DNS_MODE_OFF) ||
                mode.equals(PRIVATE_DNS_MODE_OPPORTUNISTIC) ||
                mode.equals(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME));
    }

    private static String getHostnameFromSettings(ContentResolver cr) {
        return Settings.Global.getString(cr, HOSTNAME_KEY);
    }

    private static boolean isWeaklyValidatedHostname(String hostname) {
        // TODO: Find and use a better validation method.  Specifically:
        //     [1] this should reject IP string literals, and
        //     [2] do the best, simplest, future-proof verification that
        //         the input approximates a DNS hostname.
        final String WEAK_HOSTNAME_REGEX = "^[a-zA-Z0-9_.-]+$";
        return hostname.matches(WEAK_HOSTNAME_REGEX);
    }
}
