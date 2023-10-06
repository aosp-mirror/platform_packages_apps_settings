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

package com.android.settings.network.apn;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.network.ProxySubscriptionManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Use to edit apn settings. */
public class ApnEditor extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnKeyListener {

    private static final String TAG = ApnEditor.class.getSimpleName();
    private static final boolean VDBG = false;   // STOPSHIP if true

    private static final String KEY_AUTH_TYPE = "auth_type";
    private static final String KEY_APN_TYPE = "apn_type";
    private static final String KEY_PROTOCOL = "apn_protocol";
    private static final String KEY_ROAMING_PROTOCOL = "apn_roaming_protocol";
    private static final String KEY_CARRIER_ENABLED = "carrier_enabled";
    private static final String KEY_BEARER_MULTI = "bearer_multi";
    private static final String KEY_MVNO_TYPE = "mvno_type";
    private static final String KEY_PASSWORD = "apn_password";

    @VisibleForTesting
    static final int MENU_DELETE = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CANCEL = Menu.FIRST + 2;

    @VisibleForTesting
    static String sNotSet;
    @VisibleForTesting
    EditTextPreference mName;
    @VisibleForTesting
    EditTextPreference mApn;
    @VisibleForTesting
    EditTextPreference mProxy;
    @VisibleForTesting
    EditTextPreference mPort;
    @VisibleForTesting
    EditTextPreference mUser;
    @VisibleForTesting
    EditTextPreference mServer;
    @VisibleForTesting
    EditTextPreference mPassword;
    @VisibleForTesting
    EditTextPreference mMmsc;
    @VisibleForTesting
    EditTextPreference mMcc;
    @VisibleForTesting
    EditTextPreference mMnc;
    @VisibleForTesting
    EditTextPreference mMmsProxy;
    @VisibleForTesting
    EditTextPreference mMmsPort;
    @VisibleForTesting
    ListPreference mAuthType;
    @VisibleForTesting
    EditTextPreference mApnType;
    @VisibleForTesting
    ListPreference mProtocol;
    @VisibleForTesting
    ListPreference mRoamingProtocol;
    @VisibleForTesting
    SwitchPreference mCarrierEnabled;
    @VisibleForTesting
    MultiSelectListPreference mBearerMulti;
    @VisibleForTesting
    ListPreference mMvnoType;
    @VisibleForTesting
    EditTextPreference mMvnoMatchData;

    @VisibleForTesting
    ApnData mApnData;

    private String mCurMnc;
    private String mCurMcc;

    private boolean mNewApn;
    private int mSubId;
    @VisibleForTesting
    ProxySubscriptionManager mProxySubscriptionMgr;
    private int mBearerInitialVal = 0;
    private String mMvnoTypeStr;
    private String mMvnoMatchDataStr;
    @VisibleForTesting
    String[] mReadOnlyApnTypes;
    @VisibleForTesting
    String[] mDefaultApnTypes;
    @VisibleForTesting
    String mDefaultApnProtocol;
    @VisibleForTesting
    String mDefaultApnRoamingProtocol;
    private String[] mReadOnlyApnFields;
    private boolean mReadOnlyApn;
    /**
     * The APN deletion feature within menu is aligned with the APN adding feature.
     * Having only one of them could lead to a UX which not that make sense from user's
     * perspective.
     *
     * mIsAddApnAllowed stores the configuration value reading from
     * CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL to support the presentation
     * control of the menu options. When false, delete option would be invisible to
     * the end user.
     */
    private boolean mIsAddApnAllowed;
    private Uri mCarrierUri;
    private boolean mIsCarrierIdApn;

    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br/>
     * APN_TYPE_ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    public static final String APN_TYPE_ALL = "*";
    /** APN type for default data traffic */
    public static final String APN_TYPE_DEFAULT = "default";
    /** APN type for MMS traffic */
    public static final String APN_TYPE_MMS = "mms";
    /** APN type for SUPL assisted GPS */
    public static final String APN_TYPE_SUPL = "supl";
    /** APN type for DUN traffic */
    public static final String APN_TYPE_DUN = "dun";
    /** APN type for HiPri traffic */
    public static final String APN_TYPE_HIPRI = "hipri";
    /** APN type for FOTA */
    public static final String APN_TYPE_FOTA = "fota";
    /** APN type for IMS */
    public static final String APN_TYPE_IMS = "ims";
    /** APN type for CBS */
    public static final String APN_TYPE_CBS = "cbs";
    /** APN type for IA Initial Attach APN */
    public static final String APN_TYPE_IA = "ia";
    /** APN type for Emergency PDN. This is not an IA apn, but is used
     * for access to carrier services in an emergency call situation. */
    public static final String APN_TYPE_EMERGENCY = "emergency";
    /** APN type for Mission Critical Services */
    public static final String APN_TYPE_MCX = "mcx";
    /** APN type for XCAP */
    public static final String APN_TYPE_XCAP = "xcap";
    /** Array of all APN types */
    public static final String[] APN_TYPES = {APN_TYPE_DEFAULT,
            APN_TYPE_MMS,
            APN_TYPE_SUPL,
            APN_TYPE_DUN,
            APN_TYPE_HIPRI,
            APN_TYPE_FOTA,
            APN_TYPE_IMS,
            APN_TYPE_CBS,
            APN_TYPE_IA,
            APN_TYPE_EMERGENCY,
            APN_TYPE_MCX,
            APN_TYPE_XCAP,
    };

    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] sProjection = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY, // 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.PROTOCOL, // 16
            Telephony.Carriers.CARRIER_ENABLED, // 17
            Telephony.Carriers.BEARER, // 18
            Telephony.Carriers.BEARER_BITMASK, // 19
            Telephony.Carriers.ROAMING_PROTOCOL, // 20
            Telephony.Carriers.MVNO_TYPE,   // 21
            Telephony.Carriers.MVNO_MATCH_DATA,  // 22
            Telephony.Carriers.EDITED_STATUS,   // 23
            Telephony.Carriers.USER_EDITABLE,   // 24
            Telephony.Carriers.CARRIER_ID       // 25
    };

    private static final int ID_INDEX = 0;
    @VisibleForTesting
    static final int NAME_INDEX = 1;
    @VisibleForTesting
    static final int APN_INDEX = 2;
    private static final int PROXY_INDEX = 3;
    private static final int PORT_INDEX = 4;
    private static final int USER_INDEX = 5;
    private static final int SERVER_INDEX = 6;
    private static final int PASSWORD_INDEX = 7;
    private static final int MMSC_INDEX = 8;
    @VisibleForTesting
    static final int MCC_INDEX = 9;
    @VisibleForTesting
    static final int MNC_INDEX = 10;
    private static final int MMSPROXY_INDEX = 12;
    private static final int MMSPORT_INDEX = 13;
    private static final int AUTH_TYPE_INDEX = 14;
    @VisibleForTesting
    static final int TYPE_INDEX = 15;
    @VisibleForTesting
    static final int PROTOCOL_INDEX = 16;
    @VisibleForTesting
    static final int CARRIER_ENABLED_INDEX = 17;
    private static final int BEARER_INDEX = 18;
    private static final int BEARER_BITMASK_INDEX = 19;
    @VisibleForTesting
    static final int ROAMING_PROTOCOL_INDEX = 20;
    private static final int MVNO_TYPE_INDEX = 21;
    private static final int MVNO_MATCH_DATA_INDEX = 22;
    private static final int EDITED_INDEX = 23;
    private static final int USER_EDITABLE_INDEX = 24;
    private static final int CARRIER_ID_INDEX = 25;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (isUserRestricted()) {
            Log.e(TAG, "This setting isn't available due to user restriction.");
            finish();
            return;
        }

        setLifecycleForAllControllers();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            finish();
            return;
        }
        mSubId = intent.getIntExtra(ApnSettings.SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        initApnEditorUi();
        getCarrierCustomizedConfig(getContext());

        Uri uri = null;
        if (action.equals(Intent.ACTION_EDIT)) {
            uri = intent.getData();
            if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Edit request not for carrier table. Uri: " + uri);
                finish();
                return;
            }
        } else if (action.equals(Intent.ACTION_INSERT)) {
            mCarrierUri = intent.getData();
            if (!mCarrierUri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Insert request not for carrier table. Uri: " + mCarrierUri);
                finish();
                return;
            }
            mNewApn = true;
            mMvnoTypeStr = intent.getStringExtra(ApnSettings.MVNO_TYPE);
            mMvnoMatchDataStr = intent.getStringExtra(ApnSettings.MVNO_MATCH_DATA);
        } else {
            finish();
            return;
        }

        // Creates an ApnData to store the apn data temporary, so that we don't need the cursor to
        // get the apn data. The uri is null if the action is ACTION_INSERT, that mean there is no
        // record in the database, so create a empty ApnData to represent a empty row of database.
        if (uri != null) {
            mApnData = getApnDataFromUri(uri);
        } else {
            mApnData = new ApnData(sProjection.length);
        }
        final int carrierId = mApnData.getInteger(CARRIER_ID_INDEX,
                TelephonyManager.UNKNOWN_CARRIER_ID);
        mIsCarrierIdApn = (carrierId > TelephonyManager.UNKNOWN_CARRIER_ID);

        final boolean isUserEdited = mApnData.getInteger(EDITED_INDEX,
                Telephony.Carriers.USER_EDITED) == Telephony.Carriers.USER_EDITED;

        Log.d(TAG, "onCreate: EDITED " + isUserEdited);
        // if it's not a USER_EDITED apn, check if it's read-only
        if (!isUserEdited && (mApnData.getInteger(USER_EDITABLE_INDEX, 1) == 0
                || apnTypesMatch(mReadOnlyApnTypes, mApnData.getString(TYPE_INDEX)))) {
            Log.d(TAG, "onCreate: apnTypesMatch; read-only APN");
            mReadOnlyApn = true;
            disableAllFields();
        } else if (!ArrayUtils.isEmpty(mReadOnlyApnFields)) {
            disableFields(mReadOnlyApnFields);
        }
        // Make sure that a user cannot break carrier id APN matching
        if (mIsCarrierIdApn) {
            disableFieldsForCarrieridApn();
        }

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            getPreferenceScreen().getPreference(i).setOnPreferenceChangeListener(this);
        }
    }

    /**
     * Enable ProxySubscriptionMgr with Lifecycle support for all controllers
     * live within this fragment
     */
    private void setLifecycleForAllControllers() {
        if (mProxySubscriptionMgr == null) {
            mProxySubscriptionMgr = ProxySubscriptionManager.getInstance(getContext());
        }
        mProxySubscriptionMgr.setLifecycle(getLifecycle());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        fillUI(savedInstanceState == null);
        setCarrierCustomizedConfigToUi();
    }

    @VisibleForTesting
    static String formatInteger(String value) {
        try {
            final int intValue = Integer.parseInt(value);
            return String.format(getCorrectDigitsFormat(value), intValue);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * Get the digits format so we preserve leading 0's.
     * MCCs are 3 digits and MNCs are either 2 or 3.
     */
    static String getCorrectDigitsFormat(String value) {
        if (value.length() == 2) return "%02d";
        else return "%03d";
    }


    /**
     * Check if passed in array of APN types indicates all APN types
     * @param apnTypes array of APN types. "*" indicates all types.
     * @return true if all apn types are included in the array, false otherwise
     */
    static boolean hasAllApns(String[] apnTypes) {
        if (ArrayUtils.isEmpty(apnTypes)) {
            return false;
        }

        final List apnList = Arrays.asList(apnTypes);
        if (apnList.contains(APN_TYPE_ALL)) {
            Log.d(TAG, "hasAllApns: true because apnList.contains(APN_TYPE_ALL)");
            return true;
        }
        for (String apn : APN_TYPES) {
            if (!apnList.contains(apn)) {
                return false;
            }
        }

        Log.d(TAG, "hasAllApns: true");
        return true;
    }

    /**
     * Check if APN types overlap.
     * @param apnTypesArray1 array of APNs. Empty array indicates no APN type; "*" indicates all
     *                       types
     * @param apnTypes2 comma separated string of APN types. Empty string represents all types.
     * @return if any apn type matches return true, otherwise return false
     */
    private boolean apnTypesMatch(String[] apnTypesArray1, String apnTypes2) {
        if (ArrayUtils.isEmpty(apnTypesArray1)) {
            return false;
        }

        final String[] apnTypesArray1LowerCase = new String[apnTypesArray1.length];
        for (int i = 0; i < apnTypesArray1.length; i++) {
            apnTypesArray1LowerCase[i] = apnTypesArray1[i].toLowerCase();
        }

        if (hasAllApns(apnTypesArray1LowerCase) || TextUtils.isEmpty(apnTypes2)) {
            return true;
        }

        final List apnTypesList1 = Arrays.asList(apnTypesArray1LowerCase);
        final String[] apnTypesArray2 = apnTypes2.split(",");

        for (String apn : apnTypesArray2) {
            if (apnTypesList1.contains(apn.trim().toLowerCase())) {
                Log.d(TAG, "apnTypesMatch: true because match found for " + apn.trim());
                return true;
            }
        }

        Log.d(TAG, "apnTypesMatch: false");
        return false;
    }

    /**
     * Function to get Preference obj corresponding to an apnField
     * @param apnField apn field name for which pref is needed
     * @return Preference obj corresponding to passed in apnField
     */
    private Preference getPreferenceFromFieldName(String apnField) {
        switch (apnField) {
            case Telephony.Carriers.NAME:
                return mName;
            case Telephony.Carriers.APN:
                return mApn;
            case Telephony.Carriers.PROXY:
                return mProxy;
            case Telephony.Carriers.PORT:
                return mPort;
            case Telephony.Carriers.USER:
                return mUser;
            case Telephony.Carriers.SERVER:
                return mServer;
            case Telephony.Carriers.PASSWORD:
                return mPassword;
            case Telephony.Carriers.MMSPROXY:
                return mMmsProxy;
            case Telephony.Carriers.MMSPORT:
                return mMmsPort;
            case Telephony.Carriers.MMSC:
                return mMmsc;
            case Telephony.Carriers.MCC:
                return mMcc;
            case Telephony.Carriers.MNC:
                return mMnc;
            case Telephony.Carriers.TYPE:
                return mApnType;
            case Telephony.Carriers.AUTH_TYPE:
                return mAuthType;
            case Telephony.Carriers.PROTOCOL:
                return mProtocol;
            case Telephony.Carriers.ROAMING_PROTOCOL:
                return mRoamingProtocol;
            case Telephony.Carriers.CARRIER_ENABLED:
                return mCarrierEnabled;
            case Telephony.Carriers.BEARER:
            case Telephony.Carriers.BEARER_BITMASK:
                return mBearerMulti;
            case Telephony.Carriers.MVNO_TYPE:
                return mMvnoType;
            case Telephony.Carriers.MVNO_MATCH_DATA:
                return mMvnoMatchData;
        }
        return null;
    }

    /**
     * Disables given fields so that user cannot modify them
     *
     * @param apnFields fields to be disabled
     */
    private void disableFields(String[] apnFields) {
        for (String apnField : apnFields) {
            final Preference preference = getPreferenceFromFieldName(apnField);
            if (preference != null) {
                preference.setEnabled(false);
            }
        }
    }

    /**
     * Disables all fields so that user cannot modify the APN
     */
    private void disableAllFields() {
        mName.setEnabled(false);
        mApn.setEnabled(false);
        mProxy.setEnabled(false);
        mPort.setEnabled(false);
        mUser.setEnabled(false);
        mServer.setEnabled(false);
        mPassword.setEnabled(false);
        mMmsProxy.setEnabled(false);
        mMmsPort.setEnabled(false);
        mMmsc.setEnabled(false);
        mMcc.setEnabled(false);
        mMnc.setEnabled(false);
        mApnType.setEnabled(false);
        mAuthType.setEnabled(false);
        mProtocol.setEnabled(false);
        mRoamingProtocol.setEnabled(false);
        mCarrierEnabled.setEnabled(false);
        mBearerMulti.setEnabled(false);
        mMvnoType.setEnabled(false);
        mMvnoMatchData.setEnabled(false);
    }

    /**
     * Disables fields for a carrier id APN to avoid breaking the match criteria
     */
    private void disableFieldsForCarrieridApn() {
        mMcc.setEnabled(false);
        mMnc.setEnabled(false);
        mMvnoType.setEnabled(false);
        mMvnoMatchData.setEnabled(false);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APN_EDITOR;
    }

    @VisibleForTesting
    void fillUI(boolean firstTime) {
        if (firstTime) {
            // Fill in all the values from the db in both text editor and summary
            mName.setText(mApnData.getString(NAME_INDEX));
            mApn.setText(mApnData.getString(APN_INDEX));
            mProxy.setText(mApnData.getString(PROXY_INDEX));
            mPort.setText(mApnData.getString(PORT_INDEX));
            mUser.setText(mApnData.getString(USER_INDEX));
            mServer.setText(mApnData.getString(SERVER_INDEX));
            mPassword.setText(mApnData.getString(PASSWORD_INDEX));
            mMmsProxy.setText(mApnData.getString(MMSPROXY_INDEX));
            mMmsPort.setText(mApnData.getString(MMSPORT_INDEX));
            mMmsc.setText(mApnData.getString(MMSC_INDEX));
            mMcc.setText(mApnData.getString(MCC_INDEX));
            mMnc.setText(mApnData.getString(MNC_INDEX));
            mApnType.setText(mApnData.getString(TYPE_INDEX));
            if (mNewApn) {
                final SubscriptionInfo subInfo =
                        mProxySubscriptionMgr.getAccessibleSubscriptionInfo(mSubId);

                // Country code
                final String mcc = (subInfo == null) ? null : subInfo.getMccString();
                // Network code
                final String mnc = (subInfo == null) ? null : subInfo.getMncString();

                if (!TextUtils.isEmpty(mcc)) {
                    // Auto populate MNC and MCC for new entries, based on what SIM reports
                    mMcc.setText(mcc);
                    mMnc.setText(mnc);
                    mCurMnc = mnc;
                    mCurMcc = mcc;
                }
            }
            final int authVal = mApnData.getInteger(AUTH_TYPE_INDEX, -1);
            if (authVal != -1) {
                mAuthType.setValueIndex(authVal);
            } else {
                mAuthType.setValue(null);
            }

            mProtocol.setValue(mApnData.getString(PROTOCOL_INDEX));
            mRoamingProtocol.setValue(mApnData.getString(ROAMING_PROTOCOL_INDEX));
            mCarrierEnabled.setChecked(mApnData.getInteger(CARRIER_ENABLED_INDEX, 1) == 1);
            mBearerInitialVal = mApnData.getInteger(BEARER_INDEX, 0);

            final HashSet<String> bearers = new HashSet<String>();
            int bearerBitmask = mApnData.getInteger(BEARER_BITMASK_INDEX, 0);
            if (bearerBitmask == 0) {
                if (mBearerInitialVal == 0) {
                    bearers.add("" + 0);
                }
            } else {
                int i = 1;
                while (bearerBitmask != 0) {
                    if ((bearerBitmask & 1) == 1) {
                        bearers.add("" + i);
                    }
                    bearerBitmask >>= 1;
                    i++;
                }
            }

            if (mBearerInitialVal != 0 && !bearers.contains("" + mBearerInitialVal)) {
                // add mBearerInitialVal to bearers
                bearers.add("" + mBearerInitialVal);
            }
            mBearerMulti.setValues(bearers);

            mMvnoType.setValue(mApnData.getString(MVNO_TYPE_INDEX));
            mMvnoMatchData.setEnabled(false);
            mMvnoMatchData.setText(mApnData.getString(MVNO_MATCH_DATA_INDEX));
            if (mNewApn && mMvnoTypeStr != null && mMvnoMatchDataStr != null) {
                mMvnoType.setValue(mMvnoTypeStr);
                mMvnoMatchData.setText(mMvnoMatchDataStr);
            }
        }

        mName.setSummary(checkNull(mName.getText()));
        mApn.setSummary(checkNull(mApn.getText()));
        mProxy.setSummary(checkNull(mProxy.getText()));
        mPort.setSummary(checkNull(mPort.getText()));
        mUser.setSummary(checkNull(mUser.getText()));
        mServer.setSummary(checkNull(mServer.getText()));
        mPassword.setSummary(starify(mPassword.getText()));
        mMmsProxy.setSummary(checkNull(mMmsProxy.getText()));
        mMmsPort.setSummary(checkNull(mMmsPort.getText()));
        mMmsc.setSummary(checkNull(mMmsc.getText()));
        mMcc.setSummary(formatInteger(checkNull(mMcc.getText())));
        mMnc.setSummary(formatInteger(checkNull(mMnc.getText())));
        mApnType.setSummary(checkNull(mApnType.getText()));

        final String authVal = mAuthType.getValue();
        if (authVal != null) {
            final int authValIndex = Integer.parseInt(authVal);
            mAuthType.setValueIndex(authValIndex);

            final String[] values = getResources().getStringArray(R.array.apn_auth_entries);
            mAuthType.setSummary(values[authValIndex]);
        } else {
            mAuthType.setSummary(sNotSet);
        }

        mProtocol.setSummary(checkNull(protocolDescription(mProtocol.getValue(), mProtocol)));
        mRoamingProtocol.setSummary(
                checkNull(protocolDescription(mRoamingProtocol.getValue(), mRoamingProtocol)));
        mBearerMulti.setSummary(
                checkNull(bearerMultiDescription(mBearerMulti.getValues())));
        mMvnoType.setSummary(
                checkNull(mvnoDescription(mMvnoType.getValue())));
        mMvnoMatchData.setSummary(checkNullforMvnoValue(mMvnoMatchData.getText()));
        // allow user to edit carrier_enabled for some APN
        final boolean ceEditable = getResources().getBoolean(
                R.bool.config_allow_edit_carrier_enabled);
        if (ceEditable) {
            mCarrierEnabled.setEnabled(true);
        } else {
            mCarrierEnabled.setEnabled(false);
        }
    }

    /**
     * Returns the UI choice (e.g., "IPv4/IPv6") corresponding to the given
     * raw value of the protocol preference (e.g., "IPV4V6"). If unknown,
     * return null.
     */
    private String protocolDescription(String raw, ListPreference protocol) {
        String uRaw = checkNull(raw).toUpperCase();
        uRaw = uRaw.equals("IPV4") ? "IP" : uRaw;
        final int protocolIndex = protocol.findIndexOfValue(uRaw);
        if (protocolIndex == -1) {
            return null;
        } else {
            final String[] values = getResources().getStringArray(R.array.apn_protocol_entries);
            try {
                return values[protocolIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    private String bearerMultiDescription(Set<String> raw) {
        final String[] values = getResources().getStringArray(R.array.bearer_entries);
        final StringBuilder retVal = new StringBuilder();
        boolean first = true;
        for (String bearer : raw) {
            int bearerIndex = mBearerMulti.findIndexOfValue(bearer);
            try {
                if (first) {
                    retVal.append(values[bearerIndex]);
                    first = false;
                } else {
                    retVal.append(", " + values[bearerIndex]);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // ignore
            }
        }
        final String val = retVal.toString();
        if (!TextUtils.isEmpty(val)) {
            return val;
        }
        return null;
    }

    private String mvnoDescription(String newValue) {
        final int mvnoIndex = mMvnoType.findIndexOfValue(newValue);
        final String oldValue = mMvnoType.getValue();

        if (mvnoIndex == -1) {
            return null;
        } else {
            final String[] values = getResources().getStringArray(R.array.mvno_type_entries);
            final boolean mvnoMatchDataUneditable =
                    mReadOnlyApn || (mReadOnlyApnFields != null
                            && Arrays.asList(mReadOnlyApnFields)
                            .contains(Telephony.Carriers.MVNO_MATCH_DATA));
            mMvnoMatchData.setEnabled(!mvnoMatchDataUneditable && mvnoIndex != 0);
            if (newValue != null && !newValue.equals(oldValue)) {
                if (values[mvnoIndex].equals("SPN")) {
                    TelephonyManager telephonyManager = (TelephonyManager)
                            getContext().getSystemService(TelephonyManager.class);
                    final TelephonyManager telephonyManagerForSubId =
                            telephonyManager.createForSubscriptionId(mSubId);
                    if (telephonyManagerForSubId != null) {
                        telephonyManager = telephonyManagerForSubId;
                    }
                    mMvnoMatchData.setText(telephonyManager.getSimOperatorName());
                } else if (values[mvnoIndex].equals("IMSI")) {
                    final SubscriptionInfo subInfo =
                            mProxySubscriptionMgr.getAccessibleSubscriptionInfo(mSubId);
                    final String mcc = (subInfo == null) ? "" :
                            Objects.toString(subInfo.getMccString(), "");
                    final String mnc = (subInfo == null) ? "" :
                            Objects.toString(subInfo.getMncString(), "");
                    mMvnoMatchData.setText(mcc + mnc + "x");
                } else if (values[mvnoIndex].equals("GID")) {
                    TelephonyManager telephonyManager = (TelephonyManager)
                            getContext().getSystemService(TelephonyManager.class);
                    final TelephonyManager telephonyManagerForSubId =
                            telephonyManager.createForSubscriptionId(mSubId);
                    if (telephonyManagerForSubId != null) {
                        telephonyManager = telephonyManagerForSubId;
                    }
                    mMvnoMatchData.setText(telephonyManager.getGroupIdLevel1());
                } else {
                    // mvno type 'none' case. At this time, mvnoIndex should be 0.
                    mMvnoMatchData.setText("");
                }
            }

            try {
                return values[mvnoIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
    }
    /**
     * Callback when preference status changed.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_AUTH_TYPE.equals(key)) {
            try {
                final int index = Integer.parseInt((String) newValue);
                mAuthType.setValueIndex(index);

                final String[] values = getResources().getStringArray(R.array.apn_auth_entries);
                mAuthType.setSummary(values[index]);
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (KEY_APN_TYPE.equals(key)) {
            String data = (TextUtils.isEmpty((String) newValue)
                    && !ArrayUtils.isEmpty(mDefaultApnTypes))
                    ? getEditableApnType(mDefaultApnTypes) : (String) newValue;
            if (!TextUtils.isEmpty(data)) {
                mApnType.setSummary(data);
            }
        } else if (KEY_PROTOCOL.equals(key)) {
            final String protocol = protocolDescription((String) newValue, mProtocol);
            if (protocol == null) {
                return false;
            }
            mProtocol.setSummary(protocol);
            mProtocol.setValue((String) newValue);
        } else if (KEY_ROAMING_PROTOCOL.equals(key)) {
            final String protocol = protocolDescription((String) newValue, mRoamingProtocol);
            if (protocol == null) {
                return false;
            }
            mRoamingProtocol.setSummary(protocol);
            mRoamingProtocol.setValue((String) newValue);
        } else if (KEY_BEARER_MULTI.equals(key)) {
            final String bearer = bearerMultiDescription((Set<String>) newValue);
            if (bearer == null) {
                return false;
            }
            mBearerMulti.setValues((Set<String>) newValue);
            mBearerMulti.setSummary(bearer);
        } else if (KEY_MVNO_TYPE.equals(key)) {
            final String mvno = mvnoDescription((String) newValue);
            if (mvno == null) {
                return false;
            }
            mMvnoType.setValue((String) newValue);
            mMvnoType.setSummary(mvno);
            mMvnoMatchData.setSummary(checkNullforMvnoValue(mMvnoMatchData.getText()));
        } else if (KEY_PASSWORD.equals(key)) {
            mPassword.setSummary(starify(newValue != null ? String.valueOf(newValue) : ""));
        } else if (KEY_CARRIER_ENABLED.equals(key)) {
            // do nothing
        } else {
            preference.setSummary(checkNull(newValue != null ? String.valueOf(newValue) : null));
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // If it's a new APN, then cancel will delete the new entry in onPause
        // If APN add is not allowed, delete might lead to issue regarding recovery
        if (!mNewApn && !mReadOnlyApn && mIsAddApnAllowed) {
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete)
                .setIcon(R.drawable.ic_delete);
        }
        if (!mReadOnlyApn) {
            menu.add(0, MENU_SAVE, 0, R.string.menu_save)
                .setIcon(android.R.drawable.ic_menu_save);
        }
        menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DELETE:
                deleteApn();
                finish();
                return true;
            case MENU_SAVE:
                if (validateAndSaveApnData()) {
                    finish();
                }
                return true;
            case MENU_CANCEL:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnKeyListener(this);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    /**
     * Try to save the apn data when pressed the back button. An error message will be displayed if
     * the apn data is invalid.
     *
     * TODO(b/77339593): Try to keep the same behavior between back button and up navigate button.
     * We will save the valid apn data to the database when pressed the back button, but discard all
     * user changed when pressed the up navigate button.
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (validateAndSaveApnData()) {
                    finish();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Add key, value to {@code cv} and compare the value against the value at index in
     * {@link #mApnData}.
     *
     * <p>
     * The key, value will not add to {@code cv} if value is null.
     *
     * @return true if values are different. {@code assumeDiff} indicates if values can be assumed
     * different in which case no comparison is needed.
     */
    boolean setStringValueAndCheckIfDiff(
            ContentValues cv, String key, String value, boolean assumeDiff, int index) {
        final String valueFromLocalCache = mApnData.getString(index);
        if (VDBG) {
            Log.d(TAG, "setStringValueAndCheckIfDiff: assumeDiff: " + assumeDiff
                    + " key: " + key
                    + " value: '" + value
                    + "' valueFromDb: '" + valueFromLocalCache + "'");
        }
        final boolean isDiff = assumeDiff
                || !((TextUtils.isEmpty(value) && TextUtils.isEmpty(valueFromLocalCache))
                || (value != null && value.equals(valueFromLocalCache)));

        if (isDiff && value != null) {
            cv.put(key, value);
        }
        return isDiff;
    }

    /**
     * Add key, value to {@code cv} and compare the value against the value at index in
     * {@link #mApnData}.
     *
     * @return true if values are different. {@code assumeDiff} indicates if values can be assumed
     * different in which case no comparison is needed.
     */
    boolean setIntValueAndCheckIfDiff(
            ContentValues cv, String key, int value, boolean assumeDiff, int index) {
        final Integer valueFromLocalCache = mApnData.getInteger(index);
        if (VDBG) {
            Log.d(TAG, "setIntValueAndCheckIfDiff: assumeDiff: " + assumeDiff
                    + " key: " + key
                    + " value: '" + value
                    + "' valueFromDb: '" + valueFromLocalCache + "'");
        }

        final boolean isDiff = assumeDiff || value != valueFromLocalCache;
        if (isDiff) {
            cv.put(key, value);
        }
        return isDiff;
    }

    /**
     * Validates the apn data and save it to the database if it's valid.
     *
     * <p>
     * A dialog with error message will be displayed if the APN data is invalid.
     *
     * @return true if there is no error
     */
    @VisibleForTesting
    boolean validateAndSaveApnData() {
        // Nothing to do if it's a read only APN
        if (mReadOnlyApn) {
            return true;
        }

        final String name = checkNotSet(mName.getText());
        final String apn = checkNotSet(mApn.getText());
        final String mcc = checkNotSet(mMcc.getText());
        final String mnc = checkNotSet(mMnc.getText());

        final String errorMsg = validateApnData();
        if (errorMsg != null) {
            showError();
            return false;
        }

        final ContentValues values = new ContentValues();
        // call update() if it's a new APN. If not, check if any field differs from the db value;
        // if any diff is found update() should be called
        boolean callUpdate = mNewApn;
        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.NAME,
                name,
                callUpdate,
                NAME_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.APN,
                apn,
                callUpdate,
                APN_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.PROXY,
                checkNotSet(mProxy.getText()),
                callUpdate,
                PROXY_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.PORT,
                checkNotSet(mPort.getText()),
                callUpdate,
                PORT_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.MMSPROXY,
                checkNotSet(mMmsProxy.getText()),
                callUpdate,
                MMSPROXY_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.MMSPORT,
                checkNotSet(mMmsPort.getText()),
                callUpdate,
                MMSPORT_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.USER,
                checkNotSet(mUser.getText()),
                callUpdate,
                USER_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.SERVER,
                checkNotSet(mServer.getText()),
                callUpdate,
                SERVER_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.PASSWORD,
                checkNotSet(mPassword.getText()),
                callUpdate,
                PASSWORD_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.MMSC,
                checkNotSet(mMmsc.getText()),
                callUpdate,
                MMSC_INDEX);

        final String authVal = mAuthType.getValue();
        if (authVal != null) {
            callUpdate = setIntValueAndCheckIfDiff(values,
                    Telephony.Carriers.AUTH_TYPE,
                    Integer.parseInt(authVal),
                    callUpdate,
                    AUTH_TYPE_INDEX);
        }

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.PROTOCOL,
                checkNotSet(mProtocol.getValue()),
                callUpdate,
                PROTOCOL_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.ROAMING_PROTOCOL,
                checkNotSet(mRoamingProtocol.getValue()),
                callUpdate,
                ROAMING_PROTOCOL_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.TYPE,
                checkNotSet(getUserEnteredApnType()),
                callUpdate,
                TYPE_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.MCC,
                mcc,
                callUpdate,
                MCC_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.MNC,
                mnc,
                callUpdate,
                MNC_INDEX);

        values.put(Telephony.Carriers.NUMERIC, mcc + mnc);

        if (mCurMnc != null && mCurMcc != null) {
            if (mCurMnc.equals(mnc) && mCurMcc.equals(mcc)) {
                values.put(Telephony.Carriers.CURRENT, 1);
            }
        }

        final Set<String> bearerSet = mBearerMulti.getValues();
        int bearerBitmask = 0;
        for (String bearer : bearerSet) {
            if (Integer.parseInt(bearer) == 0) {
                bearerBitmask = 0;
                break;
            } else {
                bearerBitmask |= getBitmaskForTech(Integer.parseInt(bearer));
            }
        }
        callUpdate = setIntValueAndCheckIfDiff(values,
                Telephony.Carriers.BEARER_BITMASK,
                bearerBitmask,
                callUpdate,
                BEARER_BITMASK_INDEX);

        int bearerVal;
        if (bearerBitmask == 0 || mBearerInitialVal == 0) {
            bearerVal = 0;
        } else if (bitmaskHasTech(bearerBitmask, mBearerInitialVal)) {
            bearerVal = mBearerInitialVal;
        } else {
            // bearer field was being used but bitmask has changed now and does not include the
            // initial bearer value -- setting bearer to 0 but maybe better behavior is to choose a
            // random tech from the new bitmask??
            bearerVal = 0;
        }
        callUpdate = setIntValueAndCheckIfDiff(values,
                Telephony.Carriers.BEARER,
                bearerVal,
                callUpdate,
                BEARER_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.MVNO_TYPE,
                checkNotSet(mMvnoType.getValue()),
                callUpdate,
                MVNO_TYPE_INDEX);

        callUpdate = setStringValueAndCheckIfDiff(values,
                Telephony.Carriers.MVNO_MATCH_DATA,
                checkNotSet(mMvnoMatchData.getText()),
                callUpdate,
                MVNO_MATCH_DATA_INDEX);

        callUpdate = setIntValueAndCheckIfDiff(values,
                Telephony.Carriers.CARRIER_ENABLED,
                mCarrierEnabled.isChecked() ? 1 : 0,
                callUpdate,
                CARRIER_ENABLED_INDEX);

        values.put(Telephony.Carriers.EDITED_STATUS, Telephony.Carriers.USER_EDITED);

        if (callUpdate) {
            final Uri uri = mApnData.getUri() == null ? mCarrierUri : mApnData.getUri();
            updateApnDataToDatabase(uri, values);
        } else {
            if (VDBG) Log.d(TAG, "validateAndSaveApnData: not calling update()");
        }

        return true;
    }

    private void updateApnDataToDatabase(Uri uri, ContentValues values) {
        ThreadUtils.postOnBackgroundThread(() -> {
            if (uri.equals(mCarrierUri)) {
                // Add a new apn to the database
                final Uri newUri = getContentResolver().insert(mCarrierUri, values);
                if (newUri == null) {
                    Log.e(TAG, "Can't add a new apn to database " + mCarrierUri);
                }
            } else {
                // Update the existing apn
                getContentResolver().update(
                        uri, values, null /* where */, null /* selection Args */);
            }
        });
    }

    /**
     * Validates whether the apn data is valid.
     *
     * @return An error message if the apn data is invalid, otherwise return null.
     */
    @VisibleForTesting
    String validateApnData() {
        String errorMsg = null;

        final String name = checkNotSet(mName.getText());
        final String apn = checkNotSet(mApn.getText());
        final String mcc = checkNotSet(mMcc.getText());
        final String mnc = checkNotSet(mMnc.getText());
        boolean doNotCheckMccMnc = mIsCarrierIdApn && TextUtils.isEmpty(mcc)
                && TextUtils.isEmpty(mnc);
        if (TextUtils.isEmpty(name)) {
            errorMsg = getResources().getString(R.string.error_name_empty);
        } else if (TextUtils.isEmpty(apn)) {
            errorMsg = getResources().getString(R.string.error_apn_empty);
        } else if (doNotCheckMccMnc) {
            Log.d(TAG, "validateApnData: carrier id APN does not have mcc/mnc defined");
            // no op, skip mcc mnc null check
        } else if (mcc == null || mcc.length() != 3) {
            errorMsg = getResources().getString(R.string.error_mcc_not3);
        } else if ((mnc == null || (mnc.length() & 0xFFFE) != 2)) {
            errorMsg = getResources().getString(R.string.error_mnc_not23);
        }

        if (errorMsg == null) {
            // if carrier does not allow editing certain apn types, make sure type does not include
            // those
            if (!ArrayUtils.isEmpty(mReadOnlyApnTypes)
                    && apnTypesMatch(mReadOnlyApnTypes, getUserEnteredApnType())) {
                final StringBuilder stringBuilder = new StringBuilder();
                for (String type : mReadOnlyApnTypes) {
                    stringBuilder.append(type).append(", ");
                    Log.d(TAG, "validateApnData: appending type: " + type);
                }
                // remove last ", "
                if (stringBuilder.length() >= 2) {
                    stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
                }
                errorMsg = String.format(getResources().getString(R.string.error_adding_apn_type),
                        stringBuilder);
            }
        }

        return errorMsg;
    }

    @VisibleForTesting
    void showError() {
        ErrorDialog.showError(this);
    }

    private void deleteApn() {
        if (mApnData.getUri() != null) {
            getContentResolver().delete(mApnData.getUri(), null, null);
            mApnData = new ApnData(sProjection.length);
        }
    }

    private String starify(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        } else {
            final char[] password = new char[value.length()];
            for (int i = 0; i < password.length; i++) {
                password[i] = '*';
            }
            return new String(password);
        }
    }

    /**
     * Returns {@link #sNotSet} if the given string {@code value} is null or empty. The string
     * {@link #sNotSet} typically used as the default display when an entry in the preference is
     * null or empty.
     */
    private String checkNull(String value) {
        return TextUtils.isEmpty(value) ? sNotSet : value;
    }

    /**
     * To make traslation be diversity, use another string id for MVNO value.
     */
    private String checkNullforMvnoValue(String value) {
        String notSetForMvnoValue = getResources().getString(R.string.apn_not_set_for_mvno);
        return TextUtils.isEmpty(value) ? notSetForMvnoValue : value;
    }

    /**
     * Returns null if the given string {@code value} equals to {@link #sNotSet}. This method
     * should be used when convert a string value from preference to database.
     */
    private String checkNotSet(String value) {
        return sNotSet.equals(value) ? null : value;
    }

    @VisibleForTesting
    String getUserEnteredApnType() {
        // if user has not specified a type, map it to "ALL APN TYPES THAT ARE NOT READ-ONLY"
        // but if user enter empty type, map it just for default
        String userEnteredApnType = mApnType.getText();
        if (userEnteredApnType != null) userEnteredApnType = userEnteredApnType.trim();
        if ((TextUtils.isEmpty(userEnteredApnType)
                || APN_TYPE_ALL.equals(userEnteredApnType))) {
            userEnteredApnType = getEditableApnType(APN_TYPES);
        }
        Log.d(TAG, "getUserEnteredApnType: changed apn type to editable apn types: "
                + userEnteredApnType);
        return userEnteredApnType;
    }

    private String getEditableApnType(String[] apnTypeList) {
        final StringBuilder editableApnTypes = new StringBuilder();
        final List<String> readOnlyApnTypes = Arrays.asList(mReadOnlyApnTypes);
        boolean first = true;
        for (String apnType : apnTypeList) {
            // add APN type if it is not read-only and is not wild-cardable
            if (!readOnlyApnTypes.contains(apnType)
                    && !apnType.equals(APN_TYPE_IA)
                    && !apnType.equals(APN_TYPE_EMERGENCY)
                    && !apnType.equals(APN_TYPE_MCX)
                    && !apnType.equals(APN_TYPE_IMS)) {
                if (first) {
                    first = false;
                } else {
                    editableApnTypes.append(",");
                }
                editableApnTypes.append(apnType);
            }
        }
        return editableApnTypes.toString();
    }

    private void initApnEditorUi() {
        addPreferencesFromResource(R.xml.apn_editor);

        sNotSet = getResources().getString(R.string.apn_not_set);
        mName = (EditTextPreference) findPreference("apn_name");
        mApn = (EditTextPreference) findPreference("apn_apn");
        mProxy = (EditTextPreference) findPreference("apn_http_proxy");
        mPort = (EditTextPreference) findPreference("apn_http_port");
        mUser = (EditTextPreference) findPreference("apn_user");
        mServer = (EditTextPreference) findPreference("apn_server");
        mPassword = (EditTextPreference) findPreference(KEY_PASSWORD);
        mMmsProxy = (EditTextPreference) findPreference("apn_mms_proxy");
        mMmsPort = (EditTextPreference) findPreference("apn_mms_port");
        mMmsc = (EditTextPreference) findPreference("apn_mmsc");
        mMcc = (EditTextPreference) findPreference("apn_mcc");
        mMnc = (EditTextPreference) findPreference("apn_mnc");
        mApnType = (EditTextPreference) findPreference("apn_type");
        mAuthType = (ListPreference) findPreference(KEY_AUTH_TYPE);
        mProtocol = (ListPreference) findPreference(KEY_PROTOCOL);
        mRoamingProtocol = (ListPreference) findPreference(KEY_ROAMING_PROTOCOL);
        mCarrierEnabled = (SwitchPreference) findPreference(KEY_CARRIER_ENABLED);
        mBearerMulti = (MultiSelectListPreference) findPreference(KEY_BEARER_MULTI);
        mMvnoType = (ListPreference) findPreference(KEY_MVNO_TYPE);
        mMvnoMatchData = (EditTextPreference) findPreference("mvno_match_data");
    }

    @VisibleForTesting
    protected void getCarrierCustomizedConfig(Context context) {
        mReadOnlyApn = false;
        mReadOnlyApnTypes = null;
        mReadOnlyApnFields = null;
        mIsAddApnAllowed = true;

        final CarrierConfigManager configManager = (CarrierConfigManager)
            context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            final PersistableBundle b = configManager.getConfigForSubId(mSubId);
            if (b != null) {
                mReadOnlyApnTypes = b.getStringArray(
                        CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY);
                if (!ArrayUtils.isEmpty(mReadOnlyApnTypes)) {
                    Log.d(TAG,
                            "onCreate: read only APN type: " + Arrays.toString(mReadOnlyApnTypes));
                }
                mReadOnlyApnFields = b.getStringArray(
                        CarrierConfigManager.KEY_READ_ONLY_APN_FIELDS_STRING_ARRAY);

                mDefaultApnTypes = b.getStringArray(
                        CarrierConfigManager.KEY_APN_SETTINGS_DEFAULT_APN_TYPES_STRING_ARRAY);

                if (!ArrayUtils.isEmpty(mDefaultApnTypes)) {
                    Log.d(TAG, "onCreate: default apn types: " + Arrays.toString(mDefaultApnTypes));
                }

                mDefaultApnProtocol = b.getString(
                        CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_PROTOCOL_STRING);
                if (!TextUtils.isEmpty(mDefaultApnProtocol)) {
                    Log.d(TAG, "onCreate: default apn protocol: " + mDefaultApnProtocol);
                }

                mDefaultApnRoamingProtocol = b.getString(
                        CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_ROAMING_PROTOCOL_STRING);
                if (!TextUtils.isEmpty(mDefaultApnRoamingProtocol)) {
                    Log.d(TAG, "onCreate: default apn roaming protocol: "
                            + mDefaultApnRoamingProtocol);
                }

                mIsAddApnAllowed = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);
                if (!mIsAddApnAllowed) {
                    Log.d(TAG, "onCreate: not allow to add new APN");
                }
            }
        }
    }

    private void setCarrierCustomizedConfigToUi() {
        if (TextUtils.isEmpty(mApnType.getText()) && !ArrayUtils.isEmpty(mDefaultApnTypes)) {
            String value = getEditableApnType(mDefaultApnTypes);
            mApnType.setText(value);
            mApnType.setSummary(value);
        }

        String protocol = protocolDescription(mDefaultApnProtocol, mProtocol);
        if (TextUtils.isEmpty(mProtocol.getValue()) && !TextUtils.isEmpty(protocol)) {
            mProtocol.setValue(mDefaultApnProtocol);
            mProtocol.setSummary(protocol);
        }

        String roamingProtocol = protocolDescription(mDefaultApnRoamingProtocol, mRoamingProtocol);
        if (TextUtils.isEmpty(mRoamingProtocol.getValue()) && !TextUtils.isEmpty(roamingProtocol)) {
            mRoamingProtocol.setValue(mDefaultApnRoamingProtocol);
            mRoamingProtocol.setSummary(roamingProtocol);
        }
    }

    /**
     * Dialog of error message.
     */
    public static class ErrorDialog extends InstrumentedDialogFragment {
        /**
         * Show error dialog.
         */
        public static void showError(ApnEditor editor) {
            final ErrorDialog dialog = new ErrorDialog();
            dialog.setTargetFragment(editor, 0);
            dialog.show(editor.getFragmentManager(), "error");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String msg = ((ApnEditor) getTargetFragment()).validateApnData();

            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(msg)
                    .create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_APN_EDITOR_ERROR;
        }
    }

    @VisibleForTesting
    ApnData getApnDataFromUri(Uri uri) {
        ApnData apnData = null;
        try (Cursor cursor = getContentResolver().query(
                uri,
                sProjection,
                null /* selection */,
                null /* selectionArgs */,
                null /* sortOrder */)) {
            if (cursor != null) {
                cursor.moveToFirst();
                apnData = new ApnData(uri, cursor);
            }
        }

        if (apnData == null) {
            Log.d(TAG, "Can't get apnData from Uri " + uri);
        }

        return apnData;
    }

    @VisibleForTesting
    boolean isUserRestricted() {
        UserManager userManager = getContext().getSystemService(UserManager.class);
        if (userManager == null) {
            return false;
        }
        if (!userManager.isAdminUser()) {
            Log.e(TAG, "User is not an admin");
            return true;
        }
        if (userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            Log.e(TAG, "User is not allowed to configure mobile network");
            return true;
        }
        return false;
    }

    @VisibleForTesting
    static class ApnData {
        /**
         * The uri correspond to a database row of the apn data. This should be null if the apn
         * is not in the database.
         */
        Uri mUri;

        /** Each element correspond to a column of the database row. */
        Object[] mData;

        ApnData(int numberOfField) {
            mData = new Object[numberOfField];
        }

        ApnData(Uri uri, Cursor cursor) {
            mUri = uri;
            mData = new Object[cursor.getColumnCount()];
            for (int i = 0; i < mData.length; i++) {
                switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_FLOAT:
                        mData[i] = cursor.getFloat(i);
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        mData[i] = cursor.getInt(i);
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        mData[i] = cursor.getString(i);
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        mData[i] = cursor.getBlob(i);
                        break;
                    default:
                        mData[i] = null;
                }
            }
        }

        Uri getUri() {
            return mUri;
        }

        void setUri(Uri uri) {
            mUri = uri;
        }

        Integer getInteger(int index) {
            return (Integer) mData[index];
        }

        Integer getInteger(int index, Integer defaultValue) {
            final Integer val = getInteger(index);
            return val == null ? defaultValue : val;
        }

        String getString(int index) {
            return (String) mData[index];
        }
    }

    private static int getBitmaskForTech(int radioTech) {
        if (radioTech >= 1) {
            return (1 << (radioTech - 1));
        }
        return 0;
    }

    private static boolean bitmaskHasTech(int bearerBitmask, int radioTech) {
        if (bearerBitmask == 0) {
            return true;
        } else if (radioTech >= 1) {
            return ((bearerBitmask & (1 << (radioTech - 1))) != 0);
        }
        return false;
    }
}
