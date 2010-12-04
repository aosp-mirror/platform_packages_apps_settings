/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;

/**
 * BluetoothNamePreference is the preference type for editing the device's
 * Bluetooth name. It asks the user for a name, and persists it via the
 * Bluetooth API.
 */
public class BluetoothNamePreference extends EditTextPreference implements TextWatcher {
    private static final String TAG = "BluetoothNamePreference";
    // max. length reduced from 248 to 246 bytes to work around Bluez bug
    private static final int BLUETOOTH_NAME_MAX_LENGTH_BYTES = 246;

    private LocalBluetoothManager mLocalManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                setSummaryToName();
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) &&
                    (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                            BluetoothAdapter.STATE_ON)) {
                setSummaryToName();
            }
        }
    };

    public BluetoothNamePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLocalManager = LocalBluetoothManager.getInstance(context);

        setSummaryToName();
    }

    public void resume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        getContext().registerReceiver(mReceiver, filter);

        // Make sure the OK button is disabled (if necessary) after rotation
        EditText et = getEditText();
        if (et != null) {
            et.setFilters(new InputFilter[] {
                    new Utf8ByteLengthFilter(BLUETOOTH_NAME_MAX_LENGTH_BYTES)
            });

            et.addTextChangedListener(this);
            Dialog d = getDialog();
            if (d instanceof AlertDialog) {
                Button b = ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);
                b.setEnabled(et.getText().length() > 0);
            }
        }
    }

    public void pause() {
        EditText et = getEditText();
        if (et != null) {
            et.removeTextChangedListener(this);
        }
        getContext().unregisterReceiver(mReceiver);
    }

    private void setSummaryToName() {
        BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();
        if (adapter.isEnabled()) {
            setSummary(adapter.getName());
        }
    }

    @Override
    protected boolean persistString(String value) {
        BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();
        adapter.setName(value);
        return true;
    }

    @Override
    protected void onClick() {
        super.onClick();

        // The dialog should be created by now
        EditText et = getEditText();
        if (et != null) {
            et.setText(mLocalManager.getBluetoothAdapter().getName());
        }
    }

    // TextWatcher interface
    public void afterTextChanged(Editable s) {
        Dialog d = getDialog();
        if (d instanceof AlertDialog) {
            ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(s.length() > 0);
        }
    }

    // TextWatcher interface
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // not used
    }

    // TextWatcher interface
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // not used
    }

    /**
     * This filter will constrain edits so that the text length is not
     * greater than the specified number of bytes using UTF-8 encoding.
     * <p>The JNI method used by {@link android.server.BluetoothService}
     * to convert UTF-16 to UTF-8 doesn't support surrogate pairs,
     * therefore code points outside of the basic multilingual plane
     * (0000-FFFF) will be encoded as a pair of 3-byte UTF-8 characters,
     * rather than a single 4-byte UTF-8 encoding. Dalvik implements this
     * conversion in {@code convertUtf16ToUtf8()} in
     * {@code dalvik/vm/UtfString.c}.
     * <p>This JNI method is unlikely to change in the future due to
     * backwards compatibility requirements. It's also unclear whether
     * the installed base of Bluetooth devices would correctly handle the
     * encoding of surrogate pairs in UTF-8 as 4 bytes rather than 6.
     * However, this filter will still work in scenarios where surrogate
     * pairs are encoded as 4 bytes, with the caveat that the maximum
     * length will be constrained more conservatively than necessary.
     */
    public static class Utf8ByteLengthFilter implements InputFilter {
        private int mMaxBytes;

        public Utf8ByteLengthFilter(int maxBytes) {
            mMaxBytes = maxBytes;
        }

        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            int srcByteCount = 0;
            // count UTF-8 bytes in source substring
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                srcByteCount += (c < 0x0080) ? 1 : (c < 0x0800 ? 2 : 3);
            }
            int destLen = dest.length();
            int destByteCount = 0;
            // count UTF-8 bytes in destination excluding replaced section
            for (int i = 0; i < destLen; i++) {
                if (i < dstart || i >= dend) {
                    char c = dest.charAt(i);
                    destByteCount += (c < 0x0080) ? 1 : (c < 0x0800 ? 2 : 3);
                }
            }
            int keepBytes = mMaxBytes - destByteCount;
            if (keepBytes <= 0) {
                return "";
            } else if (keepBytes >= srcByteCount) {
                return null; // use original dest string
            } else {
                // find end position of largest sequence that fits in keepBytes
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    keepBytes -= (c < 0x0080) ? 1 : (c < 0x0800 ? 2 : 3);
                    if (keepBytes < 0) {
                        return source.subSequence(start, i);
                    }
                }
                // If the entire substring fits, we should have returned null
                // above, so this line should not be reached. If for some
                // reason it is, return null to use the original dest string.
                return null;
            }
        }
    }
}
