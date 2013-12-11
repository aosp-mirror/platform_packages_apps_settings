/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.mahdi;

import android.content.Context;
import android.os.SystemProperties;
import android.preference.EditTextPreference;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;

public class HostnamePreference extends EditTextPreference {

    private static final String TAG = "HostnamePreference";

    private static final String PROP_HOSTNAME = "net.hostname";

    private String mDefaultHostName;

    InputFilter mHostnameInputFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {

            if (source.length() == 0) {
                return null;
            }

            // remove any character that is not alphanumeric, period, or hyphen
            return source.subSequence(start, end).toString().replaceAll("[^-.a-zA-Z0-9]", "");
        }
    };

    public HostnamePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // determine the default hostname
        String id = Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (id != null && id.length() > 0) {
            mDefaultHostName =  "android-".concat(id);
        } else {
            mDefaultHostName = "";
        }

        setSummary(getText());
        getEditText().setFilters(new InputFilter[] { mHostnameInputFilter });
        getEditText().setHint(mDefaultHostName);
    }

    public HostnamePreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.editTextPreferenceStyle);
    }

    public HostnamePreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String hostname = getEditText().getText().toString();

            // remove any preceding or succeeding periods or hyphens
            hostname = hostname.replaceAll("(?:\\.|-)+$", "");
            hostname = hostname.replaceAll("^(?:\\.|-)+", "");

            if (hostname.length() == 0) {
                if (mDefaultHostName.length() > 0) {
                    // if no hostname is given, use the default
                    hostname = mDefaultHostName;
                } else {
                    // if no other name can be determined
                    // fall back on the current hostname
                    hostname = getText();
                }
            }
            setText(hostname);
        }
    }

    @Override
    public void setText(String text) {
        if (text == null) {
            return;
        }
        SystemProperties.set(PROP_HOSTNAME, text);
        persistHostname(text);
        setSummary(text);
    }

    @Override
    public String getText() {
        return SystemProperties.get(PROP_HOSTNAME);
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String hostname = getText();
        persistHostname(hostname);
    }

    public void persistHostname(String hostname) {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.DEVICE_HOSTNAME, hostname);
    }
}
