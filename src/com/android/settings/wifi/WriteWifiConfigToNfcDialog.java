/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;

import java.io.IOException;

class WriteWifiConfigToNfcDialog extends AlertDialog
        implements TextWatcher, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String NFC_TOKEN_MIME_TYPE = "application/vnd.wfa.wsc";

    private static final String TAG = WriteWifiConfigToNfcDialog.class.getName().toString();
    private static final String PASSWORD_FORMAT = "102700%s%s";
    private static final int HEX_RADIX = 16;
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private final PowerManager.WakeLock mWakeLock;

    private AccessPoint mAccessPoint;
    private View mView;
    private Button mSubmitButton;
    private Button mCancelButton;
    private Handler mOnTextChangedHandler;
    private TextView mPasswordView;
    private TextView mLabelView;
    private CheckBox mPasswordCheckBox;
    private ProgressBar mProgressBar;
    private WifiManager mWifiManager;
    private String mWpsNfcConfigurationToken;
    private Context mContext;

    WriteWifiConfigToNfcDialog(Context context, AccessPoint accessPoint,
            WifiManager wifiManager) {
        super(context);

        mContext = context;
        mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WriteWifiConfigToNfcDialog:wakeLock");
        mAccessPoint = accessPoint;
        mOnTextChangedHandler = new Handler();
        mWifiManager = wifiManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.write_wifi_config_to_nfc, null);

        setView(mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.setup_wifi_nfc_tag);
        setCancelable(true);
        setButton(DialogInterface.BUTTON_NEUTRAL,
                mContext.getResources().getString(R.string.write_tag), (OnClickListener) null);
        setButton(DialogInterface.BUTTON_NEGATIVE,
                mContext.getResources().getString(com.android.internal.R.string.cancel),
                (OnClickListener) null);

        mPasswordView = (TextView) mView.findViewById(R.id.password);
        mLabelView = (TextView) mView.findViewById(R.id.password_label);
        mPasswordView.addTextChangedListener(this);
        mPasswordCheckBox = (CheckBox) mView.findViewById(R.id.show_password);
        mPasswordCheckBox.setOnCheckedChangeListener(this);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.progress_bar);

        super.onCreate(savedInstanceState);

        mSubmitButton = getButton(DialogInterface.BUTTON_NEUTRAL);
        mSubmitButton.setOnClickListener(this);
        mSubmitButton.setEnabled(false);

        mCancelButton = getButton(DialogInterface.BUTTON_NEGATIVE);
    }

    @Override
    public void onClick(View v) {
        mWakeLock.acquire();

        String password = mPasswordView.getText().toString();
        String wpsNfcConfigurationToken
                = mWifiManager.getWpsNfcConfigurationToken(mAccessPoint.networkId);
        String passwordHex = byteArrayToHexString(password.getBytes());

        String passwordLength = password.length() >= HEX_RADIX
                ? Integer.toString(password.length(), HEX_RADIX)
                : "0" + Character.forDigit(password.length(), HEX_RADIX);

        passwordHex = String.format(PASSWORD_FORMAT, passwordLength, passwordHex).toUpperCase();

        if (wpsNfcConfigurationToken.contains(passwordHex)) {
            mWpsNfcConfigurationToken = wpsNfcConfigurationToken;

            Activity activity = getOwnerActivity();
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);

            nfcAdapter.enableReaderMode(activity, new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(Tag tag) {
                    handleWriteNfcEvent(tag);
                }
            }, NfcAdapter.FLAG_READER_NFC_A |
                    NfcAdapter.FLAG_READER_NFC_B |
                    NfcAdapter.FLAG_READER_NFC_BARCODE |
                    NfcAdapter.FLAG_READER_NFC_F |
                    NfcAdapter.FLAG_READER_NFC_V,
                    null);

            mPasswordView.setVisibility(View.GONE);
            mPasswordCheckBox.setVisibility(View.GONE);
            mSubmitButton.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager)
                    getOwnerActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);

            mLabelView.setText(R.string.status_awaiting_tap);

            mView.findViewById(R.id.password_layout).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mLabelView.setText(R.string.status_invalid_password);
        }
    }

    private void handleWriteNfcEvent(Tag tag) {
        Ndef ndef = Ndef.get(tag);

        if (ndef != null) {
            if (ndef.isWritable()) {
                NdefRecord record = NdefRecord.createMime(
                        NFC_TOKEN_MIME_TYPE,
                        hexStringToByteArray(mWpsNfcConfigurationToken));
                try {
                    ndef.connect();
                    ndef.writeNdefMessage(new NdefMessage(record));
                    getOwnerActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                        }
                    });
                    setViewText(mLabelView, R.string.status_write_success);
                    setViewText(mCancelButton, com.android.internal.R.string.done_label);
                } catch (IOException e) {
                    setViewText(mLabelView, R.string.status_failed_to_write);
                    Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e);
                    return;
                } catch (FormatException e) {
                    setViewText(mLabelView, R.string.status_failed_to_write);
                    Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e);
                    return;
                }
            } else {
                setViewText(mLabelView, R.string.status_tag_not_writable);
                Log.e(TAG, "Tag is not writable");
            }
        } else {
            setViewText(mLabelView, R.string.status_tag_not_writable);
            Log.e(TAG, "Tag does not support NDEF");
        }
    }

    @Override
    public void dismiss() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        super.dismiss();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mOnTextChangedHandler.post(new Runnable() {
            @Override
            public void run() {
                enableSubmitIfAppropriate();
            }
        });
    }

    private void enableSubmitIfAppropriate() {

        if (mPasswordView != null) {
            if (mAccessPoint.security == AccessPoint.SECURITY_WEP) {
                mSubmitButton.setEnabled(mPasswordView.length() > 0);
            } else if (mAccessPoint.security == AccessPoint.SECURITY_PSK) {
                mSubmitButton.setEnabled(mPasswordView.length() >= 8);
            }
        } else {
            mSubmitButton.setEnabled(false);
        }

    }

    private void setViewText(final TextView view, final int resid) {
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(resid);
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mPasswordView.setInputType(
                InputType.TYPE_CLASS_TEXT |
                (isChecked
                        ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), HEX_RADIX) << 4)
                    + Character.digit(s.charAt(i + 1), HEX_RADIX));
        }

        return data;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable s) {}
}
