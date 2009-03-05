/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

public class AccessPointDialog extends AlertDialog implements DialogInterface.OnClickListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final String TAG = "AccessPointDialog";
    private static final String INSTANCE_KEY_ACCESS_POINT_STATE =
            "com.android.settings.wifi.AccessPointDialog:accessPointState";
    private static final String INSTANCE_KEY_MODE =
            "com.android.settings.wifi.AccessPointDialog:mode";
    private static final String INSTANCE_KEY_CUSTOM_TITLE =
            "com.android.settings.wifi.AccessPointDialog:customTitle";
    private static final String INSTANCE_KEY_AUTO_SECURITY_ALLOWED =
            "com.android.settings.wifi.AccessPointDialog:autoSecurityAllowed";
    
    private static final int POSITIVE_BUTTON = BUTTON1;
    private static final int NEGATIVE_BUTTON = BUTTON2;
    private static final int NEUTRAL_BUTTON = BUTTON3;
    
    /** The dialog should show info connectivity functionality */
    public static final int MODE_INFO = 0;
    /** The dialog should configure the detailed AP properties */
    public static final int MODE_CONFIGURE = 1;
    /** The dialog should have the password field and connect/cancel */
    public static final int MODE_RETRY_PASSWORD = 2;
    
    // These should be matched with the XML. Both arrays in XML depend on this
    // ordering!
    private static final int SECURITY_AUTO = 0;
    private static final int SECURITY_NONE = 1;
    private static final int SECURITY_WEP = 2;
    private static final int SECURITY_WPA_PERSONAL = 3;
    private static final int SECURITY_WPA2_PERSONAL = 4;

    private static final int[] WEP_TYPE_VALUES = {
            AccessPointState.WEP_PASSWORD_AUTO, AccessPointState.WEP_PASSWORD_ASCII,
            AccessPointState.WEP_PASSWORD_HEX
    };
    
    // Button positions, default to impossible values
    private int mConnectButtonPos = Integer.MAX_VALUE; 
    private int mForgetButtonPos = Integer.MAX_VALUE;
    private int mSaveButtonPos = Integer.MAX_VALUE;

    // Client configurable items. Generally, these should be saved in instance state
    private int mMode = MODE_INFO;
    private boolean mAutoSecurityAllowed = true;
    private CharSequence mCustomTitle;
    // This does not need to be saved in instance state.
    private WifiLayer mWifiLayer;
    private AccessPointState mState;
    
    // General views
    private View mView;
    private TextView mPasswordText;
    private EditText mPasswordEdit;
    private CheckBox mShowPasswordCheckBox;
    
    // Info-specific views
    private ViewGroup mTable;
    
    // Configure-specific views
    private EditText mSsidEdit;
    private Spinner mSecuritySpinner;
    private Spinner mWepTypeSpinner;
    
    public AccessPointDialog(Context context, WifiLayer wifiLayer) {
        super(context);

        mWifiLayer = wifiLayer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onLayout();
        onFill();

        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Set to a class loader that can find AccessPointState
        savedInstanceState.setClassLoader(getClass().getClassLoader());
        
        mState = savedInstanceState.getParcelable(INSTANCE_KEY_ACCESS_POINT_STATE);
        mState.setContext(getContext());
        
        mMode = savedInstanceState.getInt(INSTANCE_KEY_MODE, mMode);
        mAutoSecurityAllowed = savedInstanceState.getBoolean(INSTANCE_KEY_AUTO_SECURITY_ALLOWED,
                mAutoSecurityAllowed);
        mCustomTitle = savedInstanceState.getCharSequence(INSTANCE_KEY_CUSTOM_TITLE);
        if (mCustomTitle != null) {
            setTitle(mCustomTitle);
        }

        // This is called last since it depends on the above values 
        super.onRestoreInstanceState(savedInstanceState);
        
        if (mShowPasswordCheckBox != null) {
            // Restore the show-password-state on the edit text
            setShowPassword(mShowPasswordCheckBox.isChecked());
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        bundle.putParcelable(INSTANCE_KEY_ACCESS_POINT_STATE, mState);
        bundle.putInt(INSTANCE_KEY_MODE, mMode);
        bundle.putBoolean(INSTANCE_KEY_AUTO_SECURITY_ALLOWED, mAutoSecurityAllowed);
        bundle.putCharSequence(INSTANCE_KEY_CUSTOM_TITLE, mCustomTitle);
        return bundle;
    }

    /**
     * Sets state to show in this dialog.
     * 
     * @param state The state.
     */
    public void setState(AccessPointState state) {
        mState = state;
    }

    /**
     * Sets the dialog mode.
     * @param mode One of {@link #MODE_CONFIGURE} or {@link #MODE_INFO}
     */
    public void setMode(int mode) {
        mMode = mode;
    }

    public void setAutoSecurityAllowed(boolean autoSecurityAllowed) {
        mAutoSecurityAllowed = autoSecurityAllowed;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mCustomTitle = title;
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getContext().getString(titleId));
    }

    /** Called after flags are set, the dialog's layout/etc should be set up here */
    private void onLayout() {
        final Context context = getContext();
        final String ssid = mState.getHumanReadableSsid();
        
        int positiveButtonResId = 0;
        int negativeButtonResId = R.string.cancel;
        int neutralButtonResId = 0;

        if (mCustomTitle == null) {
            // Generic title is the SSID
            // We don't want to trigger this as a custom title, so call super's
            super.setTitle(ssid);
        }
        setInverseBackgroundForced(true);

        boolean defaultPasswordVisibility = true;
        
        if (mMode == MODE_CONFIGURE) {
            setLayout(R.layout.wifi_ap_configure);

            positiveButtonResId = R.string.wifi_save_config;
            mSaveButtonPos = POSITIVE_BUTTON;
            
        } else if (mMode == MODE_INFO) {
            setLayout(R.layout.wifi_ap_info);

            if (mState.isConnectable()) {
                if (mCustomTitle == null) {
                    // We don't want to trigger this as a custom title, so call super's
                    super.setTitle(context.getString(R.string.connect_to_blank, ssid));
                }
                positiveButtonResId = R.string.connect;
                mConnectButtonPos = POSITIVE_BUTTON;
            }

            if (mState.isForgetable()) {
                if (positiveButtonResId == 0) {
                    positiveButtonResId = R.string.forget_network;
                    mForgetButtonPos = POSITIVE_BUTTON;
                } else {
                    neutralButtonResId = R.string.forget_network;
                    mForgetButtonPos = NEUTRAL_BUTTON;
                }
            }
        } else if (mMode == MODE_RETRY_PASSWORD) {
            setLayout(R.layout.wifi_ap_retry_password);
            
            positiveButtonResId = R.string.connect;
            mConnectButtonPos = POSITIVE_BUTTON;
            
            setGenericPasswordVisible(true);
            defaultPasswordVisibility = false;
        }

        if (defaultPasswordVisibility) {
            if (!mState.configured && mState.seen && mState.hasSecurity()) {
                setGenericPasswordVisible(true);
            } else {
                setGenericPasswordVisible(false);
            }
        }
        
        setButtons(positiveButtonResId, negativeButtonResId, neutralButtonResId);
    }

    /** Called when we need to set our member variables to point to the views. */
    private void onReferenceViews(View view) {
        mPasswordText = (TextView) view.findViewById(R.id.password_text);
        mPasswordEdit = (EditText) view.findViewById(R.id.password_edit);
        
        mShowPasswordCheckBox = (CheckBox) view.findViewById(R.id.show_password_checkbox);
        if (mShowPasswordCheckBox != null) {
            mShowPasswordCheckBox.setOnClickListener(this);
        }
        
        if (mMode == MODE_CONFIGURE) {
            mSsidEdit = (EditText) view.findViewById(R.id.ssid_edit);
            mSecuritySpinner = (Spinner) view.findViewById(R.id.security_spinner);
            mSecuritySpinner.setOnItemSelectedListener(this);
            setSecuritySpinnerAdapter();
            mWepTypeSpinner = (Spinner) view.findViewById(R.id.wep_type_spinner);
            
        } else if (mMode == MODE_INFO) {
            mTable = (ViewGroup) view.findViewById(R.id.table);
        }
        
    }
    
    private void setSecuritySpinnerAdapter() {
        Context context = getContext();
        int arrayResId = mAutoSecurityAllowed ? R.array.wifi_security_entries
                : R.array.wifi_security_without_auto_entries;         

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context,
                android.R.layout.simple_spinner_item,
                context.getResources().getStringArray(arrayResId));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSecuritySpinner.setAdapter(adapter);
    }
    
    /** Called when the widgets are in-place waiting to be filled with data */
    private void onFill() {

        // Appears in the order added
        if (mMode == MODE_INFO) {
            if (mState.primary) {
                addInfoRow(R.string.wifi_status, mState.getSummarizedStatus());
                addInfoRow(R.string.wifi_link_speed, mState.linkSpeed + WifiInfo.LINK_SPEED_UNITS);
            }
    
            if (mState.seen) {
                addInfoRow(R.string.signal, getSignalResId(mState.signal));
            }
            
            if (mState.security != null) {
                addInfoRow(R.string.security, mState.getHumanReadableSecurity());
            }
    
            if (mState.primary && mState.ipAddress != 0) {
                addInfoRow(R.string.ip_address, Formatter.formatIpAddress(mState.ipAddress));
            }
            
        } else if (mMode == MODE_CONFIGURE) {
            String ssid = mState.getHumanReadableSsid();
            if (!TextUtils.isEmpty(ssid)) {
                mSsidEdit.setText(ssid);
            }
            
            mPasswordEdit.setHint(R.string.wifi_password_unchanged);
        }

        updatePasswordCaption(mState.security);
    }

    private void updatePasswordCaption(String security) {
        
        if (mPasswordText != null && security != null
                && security.equals(AccessPointState.WEP)) {
            mPasswordText.setText(R.string.please_type_hex_key);
        } else {
            mPasswordText.setText(R.string.please_type_passphrase);
        }
    }
    
    private void addInfoRow(int nameResId, String value) {
        View rowView = getLayoutInflater().inflate(R.layout.wifi_ap_info_row, mTable, false);
        ((TextView) rowView.findViewById(R.id.name)).setText(nameResId);
        ((TextView) rowView.findViewById(R.id.value)).setText(value);
        mTable.addView(rowView);
    }
        
    private void addInfoRow(int nameResId, int valueResId) {
        addInfoRow(nameResId, getContext().getString(valueResId));
    }
    
    private void setButtons(int positiveResId, int negativeResId, int neutralResId) {
        final Context context = getContext();
        
        if (positiveResId > 0) {
            setButton(context.getString(positiveResId), this);
        }
        
        if (negativeResId > 0) {
            setButton2(context.getString(negativeResId), this);
        }

        if (neutralResId > 0) {
            setButton3(context.getString(neutralResId), this);
        }
    }
    
    private void setLayout(int layoutResId) {
        setView(mView = getLayoutInflater().inflate(layoutResId, null));
        onReferenceViews(mView);
    }
    
    public void onClick(DialogInterface dialog, int which) {
        if (which == mForgetButtonPos) {
            handleForget();
        } else if (which == mConnectButtonPos) {
            handleConnect();
        } else if (which == mSaveButtonPos) {
            handleSave();
        }
    }
    
    private void handleForget() {
        if (!replaceStateWithWifiLayerInstance()) return;
        mWifiLayer.forgetNetwork(mState);
    }
    
    private void handleConnect() {
        if (!replaceStateWithWifiLayerInstance()) {
            Log.w(TAG, "Assuming connecting to a new network.");
        }
        
        /*
         * If the network is secured and they haven't entered a password, popup
         * an error. Allow empty passwords if the state already has a password
         * set (since in that scenario, an empty password means keep the old
         * password).
         */
        String password = getEnteredPassword();
        boolean passwordIsEmpty = TextUtils.isEmpty(password);
        
        /*
         * When 'retry password', they can not enter a blank password. In any
         * other mode, we let them enter a blank password if the state already
         * has a password.
         */
        if (passwordIsEmpty && (!mState.hasPassword() || mMode == MODE_RETRY_PASSWORD)
                && (mState.security != null) && !mState.security.equals(AccessPointState.OPEN)) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_password_incorrect_error)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        
        if (!passwordIsEmpty) { 
            mState.setPassword(password);
        }
        
        mWifiLayer.connectToNetwork(mState);            
    }
    
    private void handleSave() {
        replaceStateWithWifiLayerInstance();

        String ssid = mSsidEdit.getText().toString();
        String password = mPasswordEdit.getText().toString();
        
        mState.setSsid(ssid);
        
        int securityType = getSecurityTypeFromSpinner();
        
        if (!TextUtils.isEmpty(password)) {
            switch (securityType) {
             
                case SECURITY_WPA_PERSONAL: {
                    mState.setSecurity(AccessPointState.WPA);
                    mState.setPassword(password);
                    break;
                }
                    
                case SECURITY_WPA2_PERSONAL: {
                    mState.setSecurity(AccessPointState.WPA2);
                    mState.setPassword(password);
                    break;
                }
                
                case SECURITY_AUTO: {
                    mState.setPassword(password);
                    break;
                }
                    
                case SECURITY_WEP: {
                    mState.setSecurity(AccessPointState.WEP);
                    mState.setPassword(password,
                            WEP_TYPE_VALUES[mWepTypeSpinner.getSelectedItemPosition()]);
                    break;
                }
                
            }
        } else {
            mState.setSecurity(AccessPointState.OPEN);
        }
        
        if (securityType == SECURITY_NONE) {
            mState.setSecurity(AccessPointState.OPEN);
        }
            
        if (!mWifiLayer.saveNetwork(mState)) {
            return;
        }
        
        // Connect right away if they've touched it
        if (!mWifiLayer.connectToNetwork(mState)) {
            return;
        }
        
    }
    
    /**
     * Replaces our {@link #mState} with the equal WifiLayer instance.  This is useful after
     * we unparceled the state previously and before we are calling methods on {@link #mWifiLayer}.
     * 
     * @return Whether WifiLayer was able to find an equal state in its set.
     */
    private boolean replaceStateWithWifiLayerInstance() {
        AccessPointState state = mWifiLayer.getWifiLayerApInstance(mState);
        if (state == null) {
            return false;
        }
        
        mState = state;
        return true;
    }
    
    private int getSecurityTypeFromSpinner() {
        int position = mSecuritySpinner.getSelectedItemPosition();
        // If there is no AUTO choice, the position needs 1 added to get
        // to the proper spinner position -> security constants mapping
        return mAutoSecurityAllowed ? position : position + 1;
    }
    
    private String getEnteredPassword() {
        return mPasswordEdit != null ? mPasswordEdit.getText().toString() : null;
    }
    
    /**
     * Call the one you want to hide first.
     */
    private void setWepVisible(boolean visible) {
        setGenericPasswordVisible(visible);
        int visibility = visible ? View.VISIBLE : View.GONE;
        mWepTypeSpinner.setVisibility(visibility);
    }
    
    /**
     * @see #setWepVisible(boolean)
     */
    private void setGenericPasswordVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        mPasswordText.setVisibility(visibility);
        mPasswordEdit.setVisibility(visibility);
        mShowPasswordCheckBox.setVisibility(visibility);
    }
    
    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        if (parent == mSecuritySpinner) {
            handleSecurityChange(getSecurityTypeFromSpinner());
        }
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private void handleSecurityChange(int security) {
        
        switch (security) {
            
            case SECURITY_NONE: {
                setWepVisible(false);
                setGenericPasswordVisible(false);
                break;
            }
            
            case SECURITY_WEP: {
                setGenericPasswordVisible(false);
                setWepVisible(true);
                updatePasswordCaption(AccessPointState.WEP);
                break;
            }
            
            case SECURITY_AUTO: {
                setWepVisible(false);
                setGenericPasswordVisible(mState.hasSecurity());
                // Shows the generic 'wireless password'
                updatePasswordCaption(AccessPointState.WPA);
                break;
            }
            
            case SECURITY_WPA_PERSONAL:
            case SECURITY_WPA2_PERSONAL: {
                setWepVisible(false);
                setGenericPasswordVisible(true);
                // Both WPA and WPA2 show the same caption, so either is ok
                updatePasswordCaption(AccessPointState.WPA);
                break;
            }
        }
    }

    private static int getSignalResId(int signal) {
        switch (WifiManager.calculateSignalLevel(signal, 4)) {
            case 0: {
                return R.string.wifi_signal_0;
            }
            case 1: {
                return R.string.wifi_signal_1;
            }
            case 2: {
                return R.string.wifi_signal_2;
            }
            case 3: {
                return R.string.wifi_signal_3;
            }
        }
        
        return 0;
    }
    

    public void onClick(View v) {
        if (v == mShowPasswordCheckBox) {
            setShowPassword(mShowPasswordCheckBox.isChecked());
        }
    }
    
    private void setShowPassword(boolean showPassword) {
        if (mPasswordEdit != null) {
            mPasswordEdit.setInputType(InputType.TYPE_CLASS_TEXT |
                    (showPassword ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            : InputType.TYPE_TEXT_VARIATION_PASSWORD));
        }
    }
    
}
