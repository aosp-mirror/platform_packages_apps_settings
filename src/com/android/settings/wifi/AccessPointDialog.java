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
import com.android.settings.SecuritySettings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.security.CertTool;
import android.security.Keystore;
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
    private static final int SECURITY_WPA_EAP = 5;
    private static final int SECURITY_IEEE8021X = 6;

    private static final int[] WEP_TYPE_VALUES = {
            AccessPointState.WEP_PASSWORD_AUTO, AccessPointState.WEP_PASSWORD_ASCII,
            AccessPointState.WEP_PASSWORD_HEX
    };
    private static final String NOT_APPLICABLE = "N/A";
    private static final String BLOB_HEADER = "blob://";

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
    private View mEnterpriseView;
    private TextView mPasswordText;
    private EditText mPasswordEdit;
    private CheckBox mShowPasswordCheckBox;

    // Enterprise fields
    private TextView mEapText;
    private Spinner mEapSpinner;
    private TextView mPhase2Text;
    private Spinner mPhase2Spinner;
    private TextView mIdentityText;
    private EditText mIdentityEdit;
    private TextView mAnonymousIdentityText;
    private EditText mAnonymousIdentityEdit;
    private TextView mCaCertText;
    private Spinner mCaCertSpinner;
    private TextView mClientCertText;
    private Spinner mClientCertSpinner;
    private TextView mPrivateKeyPasswdText;
    private EditText mPrivateKeyPasswdEdit;
    private EditText[] mEnterpriseTextFields;

    
    // Info-specific views
    private ViewGroup mTable;
    
    // Configure-specific views
    private EditText mSsidEdit;
    private TextView mSsidText;
    private TextView mSecurityText;
    private Spinner mSecuritySpinner;
    private Spinner mWepTypeSpinner;
    private CertTool mCertTool;

    public AccessPointDialog(Context context, WifiLayer wifiLayer) {
        super(context);

        mWifiLayer = wifiLayer;
        mCertTool = CertTool.getInstance();
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

    public void enableEnterpriseFields() {
        setEnterpriseFieldsVisible(true);
        updateCertificateSelection();
        setGenericPasswordVisible(true);
        // Both WPA and WPA2 show the same caption, so either is ok
        updatePasswordCaption(AccessPointState.WPA);
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

            setEnterpriseFieldsVisible(false);

        } else if (mMode == MODE_INFO) {
            if (mState.isEnterprise() && !mState.configured) {
                setLayout(R.layout.wifi_ap_configure);
                setEnterpriseFieldsVisible(true);
            } else {
                setLayout(R.layout.wifi_ap_info);
            }

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
        mSsidText = (TextView) view.findViewById(R.id.ssid_text);
        mSsidEdit = (EditText) view.findViewById(R.id.ssid_edit);
        mSecurityText = (TextView) view.findViewById(R.id.security_text);
        mSecuritySpinner = (Spinner) view.findViewById(R.id.security_spinner);
        mWepTypeSpinner = (Spinner) view.findViewById(R.id.wep_type_spinner);
        mEnterpriseView = mView.findViewById(R.id.enterprise_wrapper);

        mShowPasswordCheckBox = (CheckBox) view.findViewById(R.id.show_password_checkbox);
        if (mShowPasswordCheckBox != null) {
            mShowPasswordCheckBox.setOnClickListener(this);
        }
        if (mMode == MODE_CONFIGURE) {
            mSecuritySpinner.setOnItemSelectedListener(this);
            mSecuritySpinner.setPromptId(R.string.security);
            setSpinnerAdapter(mSecuritySpinner, mAutoSecurityAllowed ?
                R.array.wifi_security_entries
                : R.array.wifi_security_without_auto_entries);
        } else if (mMode == MODE_INFO) {
            mTable = (ViewGroup) view.findViewById(R.id.table);
        }
        /* for enterprise one */
        if (mMode == MODE_CONFIGURE ||
                (mState.isEnterprise() && !mState.configured)) {
            setEnterpriseFields(view);
            updateCertificateSelection();
        }
    }

    private void updateCertificateSelection() {
        setSpinnerAdapter(mClientCertSpinner, getAllUserCertificateKeys());
        setSpinnerAdapter(mCaCertSpinner, getAllCaCertificateKeys());

        mPhase2Spinner.setSelection(getSelectionIndex(
                R.array.wifi_phase2_entries, mState.getPhase2()));
        mEapSpinner.setSelection(getSelectionIndex(
                R.array.wifi_eap_entries, mState.getEap()));
        mClientCertSpinner.setSelection(getSelectionIndex(
                getAllUserCertificateKeys(), mState.getEnterpriseField(
                AccessPointState.CLIENT_CERT)));
        mCaCertSpinner.setSelection(getSelectionIndex(
                getAllCaCertificateKeys(), mState.getEnterpriseField(
                AccessPointState.CA_CERT)));
    }

    private String[] getAllCaCertificateKeys() {
        return appendEmptyInSelection(mCertTool.getAllCaCertificateKeys());
    }

    private String[] getAllUserCertificateKeys() {
        return appendEmptyInSelection(mCertTool.getAllUserCertificateKeys());
    }

    private String[] appendEmptyInSelection(String[] keys) {
      String[] selections = new String[keys.length + 1];
      System.arraycopy(keys, 0, selections, 0, keys.length);
      selections[keys.length] = NOT_APPLICABLE;
      return selections;
    }

    private void setEnterpriseFields(View view) {
        mIdentityText = (TextView) view.findViewById(R.id.identity_text);
        mIdentityEdit = (EditText) view.findViewById(R.id.identity_edit);
        mAnonymousIdentityText =
                (TextView) view.findViewById(R.id.anonymous_identity_text);
        mAnonymousIdentityEdit =
                (EditText) view.findViewById(R.id.anonymous_identity_edit);
        mClientCertText =
                (TextView) view.findViewById(R.id.client_certificate_text);
        mCaCertText = (TextView) view.findViewById(R.id.ca_certificate_text);
        mPrivateKeyPasswdEdit =
                (EditText) view.findViewById(R.id.private_key_passwd_edit);
        mEapText = (TextView) view.findViewById(R.id.eap_text);
        mEapSpinner = (Spinner) view.findViewById(R.id.eap_spinner);
        mEapSpinner.setOnItemSelectedListener(this);
        mEapSpinner.setPromptId(R.string.please_select_eap);
        setSpinnerAdapter(mEapSpinner, R.array.wifi_eap_entries);

        mPhase2Text = (TextView) view.findViewById(R.id.phase2_text);
        mPhase2Spinner = (Spinner) view.findViewById(R.id.phase2_spinner);
        mPhase2Spinner.setOnItemSelectedListener(this);
        mPhase2Spinner.setPromptId(R.string.please_select_phase2);
        setSpinnerAdapter(mPhase2Spinner, R.array.wifi_phase2_entries);

        mClientCertSpinner =
                (Spinner) view.findViewById(R.id.client_certificate_spinner);
        mClientCertSpinner.setOnItemSelectedListener(this);
        mClientCertSpinner.setPromptId(
                R.string.please_select_client_certificate);
        setSpinnerAdapter(mClientCertSpinner, getAllUserCertificateKeys());

        mCaCertSpinner =
                (Spinner) view.findViewById(R.id.ca_certificate_spinner);
        mCaCertSpinner.setOnItemSelectedListener(this);
        mCaCertSpinner.setPromptId(R.string.please_select_ca_certificate);
        setSpinnerAdapter(mCaCertSpinner, getAllCaCertificateKeys());

        mEnterpriseTextFields = new EditText[] {
            mIdentityEdit, mAnonymousIdentityEdit, mPrivateKeyPasswdEdit
        };

    }

    private void setSpinnerAdapter(Spinner spinner, String[] items) {
        if (items != null) {
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                    getContext(), android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }
    }

    private void setSpinnerAdapter(Spinner spinner, int arrayResId) {
        setSpinnerAdapter(spinner,
            getContext().getResources().getStringArray(arrayResId));
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
            if (mState.configured) {
                mPasswordEdit.setHint(R.string.wifi_password_unchanged);
            }
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

        if (mState.isEnterprise()) {
            if(!mState.configured) {
                updateEnterpriseFields(
                        AccessPointState.WPA_EAP.equals(mState.security) ?
                        SECURITY_WPA_EAP : SECURITY_IEEE8021X);
            }
        }
        updatePasswordField();

        mWifiLayer.connectToNetwork(mState);
    }

    /*
     * If the network is secured and they haven't entered a password, popup an
     * error. Allow empty passwords if the state already has a password set
     * (since in that scenario, an empty password means keep the old password).
     */
    private void updatePasswordField() {

      String password = getEnteredPassword();
      boolean passwordIsEmpty = TextUtils.isEmpty(password);
      /*
       * When 'retry password', they can not enter a blank password. In any
       * other mode, we let them enter a blank password if the state already
       * has a password.
       */
      if (passwordIsEmpty && (!mState.hasPassword() ||
              mMode == MODE_RETRY_PASSWORD) &&
              (mState.security != null) &&
              !mState.security.equals(AccessPointState.OPEN) &&
              !mState.isEnterprise()) {
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
    }

    private void handleSave() {
        replaceStateWithWifiLayerInstance();

        String ssid = mSsidEdit.getText().toString();
        String password = mPasswordEdit.getText().toString();
        
        mState.setSsid(ssid);
        
        int securityType = getSecurityTypeFromSpinner();

        if (!TextUtils.isEmpty(password) && (securityType != SECURITY_WEP)) {
            mState.setPassword(password);
        }

        switch (securityType) {
            case SECURITY_WPA_PERSONAL: {
                mState.setSecurity(AccessPointState.WPA);
                break;
            }

            case SECURITY_WPA2_PERSONAL: {
                mState.setSecurity(AccessPointState.WPA2);
                break;
            }

            case SECURITY_AUTO: {
                break;
            }

            case SECURITY_WEP: {
                mState.setSecurity(AccessPointState.WEP);
                mState.setPassword(password, WEP_TYPE_VALUES[
                        mWepTypeSpinner.getSelectedItemPosition()]);
                    break;
            }

            case SECURITY_WPA_EAP:
                mState.setSecurity(AccessPointState.WPA_EAP);
                break;

            case SECURITY_IEEE8021X:
                mState.setSecurity(AccessPointState.IEEE8021X);
                break;

            case SECURITY_NONE:
            default:
                mState.setSecurity(AccessPointState.OPEN);
                break;
        }

        if (mState.isEnterprise() && !mState.configured) {
            updateEnterpriseFields(
                    AccessPointState.WPA_EAP.equals(mState.security) ?
                    SECURITY_WPA_EAP : SECURITY_IEEE8021X);
        }

        if (!mWifiLayer.saveNetwork(mState)) {
            return;
        }
        
        // Connect right away if they've touched it
        if (!mWifiLayer.connectToNetwork(mState)) {
            return;
        }
        
    }
    
    private int getSelectionIndex(String[] array, String selection) {
        if(selection != null) {
            for (int i = 0 ; i < array.length ; i++) {
                if (selection.contains(array[i])) return i;
            }
        }
        return 0;
    }

    private int getSelectionIndex(int arrayResId, String selection) {
        return getSelectionIndex(
            getContext().getResources().getStringArray(arrayResId), selection);
    }

    private void updateEnterpriseFields(int securityType) {
        int i;
        String value;
        for (i = AccessPointState.IDENTITY ;
                i <= AccessPointState.PRIVATE_KEY_PASSWD ; i++) {
            value = mEnterpriseTextFields[i].getText().toString();
            if (!TextUtils.isEmpty(value) ||
                    (i == AccessPointState.PRIVATE_KEY_PASSWD)) {
                mState.setEnterpriseField(i, value);
            }
        }
        Spinner spinner = mClientCertSpinner;
        int index = spinner.getSelectedItemPosition();
        if (index != (spinner.getCount() - 1)) {
            String key = (String)spinner.getSelectedItem();
            value = mCertTool.getUserCertificate(key);
            if (!TextUtils.isEmpty(value)) {
                mState.setEnterpriseField(AccessPointState.CLIENT_CERT,
                        BLOB_HEADER + value);
            }
            value = mCertTool.getUserPrivateKey(key);
            if (!TextUtils.isEmpty(value)) {
                mState.setEnterpriseField(AccessPointState.PRIVATE_KEY,
                        BLOB_HEADER + value);
            }
        }
        spinner = mCaCertSpinner;
        index = spinner.getSelectedItemPosition();
        if (index != (spinner.getCount() - 1)) {
            String key = (String)spinner.getSelectedItem();
            value = mCertTool.getCaCertificate(key);
            if (!TextUtils.isEmpty(value)) {
                mState.setEnterpriseField(AccessPointState.CA_CERT,
                        BLOB_HEADER + value);
            }
        }
        switch (securityType) {
            case SECURITY_IEEE8021X: 
            case SECURITY_WPA_EAP: {
                if (securityType == SECURITY_WPA_EAP) {
                    mState.setSecurity(AccessPointState.WPA_EAP);
                } else {
                    mState.setSecurity(AccessPointState.IEEE8021X);
                }
                mState.setEap(mEapSpinner.getSelectedItemPosition());
                mState.setPhase2((String)mPhase2Spinner.getSelectedItem());
                break;
            }
            default:
                mState.setSecurity(AccessPointState.OPEN);
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

    private void setEnterpriseFieldsVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        mEnterpriseView.setVisibility(visibility);
        if (visible) {
            setWepVisible(false);
        }
        if (mMode != MODE_CONFIGURE) {
            mSsidText.setVisibility(View.GONE);
            mSsidEdit.setVisibility(View.GONE);
            mSecurityText.setVisibility(View.GONE);
            mSecuritySpinner.setVisibility(View.GONE);
        }
    }

    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        if (parent == mSecuritySpinner) {
            handleSecurityChange(getSecurityTypeFromSpinner());
        }
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private void handleSecurityChange(int security) {
        setEnterpriseFieldsVisible(false);
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
            case SECURITY_WPA_EAP:
            case SECURITY_IEEE8021X: {
                // Unlock the keystore if it is not unlocked yet.
                if (Keystore.getInstance().getState() != Keystore.UNLOCKED) {
                    getContext().startActivity(new Intent(
                            SecuritySettings.ACTION_UNLOCK_CREDENTIAL_STORAGE));
                    return;
                }
                enableEnterpriseFields();
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
