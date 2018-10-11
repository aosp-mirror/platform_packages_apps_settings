/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static android.provider.Telephony.Carriers.ENFORCE_MANAGED_URI;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.telephony.cdma.CdmaSubscriptionPreferenceController;
import com.android.settings.network.telephony.cdma.CdmaSystemSelectPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MobileNetworkFragment extends DashboardFragment implements
        Preference.OnPreferenceChangeListener, RoamingDialogFragment.RoamingDialogListener {

    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    // Number of active Subscriptions to show tabs
    private static final int TAB_THRESHOLD = 2;

    // Number of last phone number digits shown in Euicc Setting tab
    private static final int NUM_LAST_PHONE_DIGITS = 4;

    // fragment tag for roaming data dialog
    private static final String ROAMING_TAG = "RoamingDialogFragment";

    //String keys for preference lookup
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
    private static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";
    private static final String BUTTON_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
    private static final String BUTTON_CARRIER_SETTINGS_EUICC_KEY =
            "carrier_settings_euicc_key";
    private static final String BUTTON_WIFI_CALLING_KEY = "wifi_calling_key";
    private static final String BUTTON_VIDEO_CALLING_KEY = "video_calling_key";
    private static final String BUTTON_MOBILE_DATA_ENABLE_KEY = "mobile_data_enable";
    private static final String BUTTON_DATA_USAGE_KEY = "data_usage_summary";
    private static final String BUTTON_ADVANCED_OPTIONS_KEY = "advanced_options";
    private static final String CATEGORY_CALLING_KEY = "calling";
    private static final String CATEGORY_GSM_APN_EXPAND_KEY = "category_gsm_apn_key";
    private static final String CATEGORY_CDMA_APN_EXPAND_KEY = "category_cdma_apn_key";
    private static final String BUTTON_GSM_APN_EXPAND_KEY = "button_gsm_apn_key";
    private static final String BUTTON_CDMA_APN_EXPAND_KEY = "button_cdma_apn_key";

    private static final String EXTRA_EXIT_ECM_RESULT = "exit_ecm_result";
    private static final String LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT =
            "android.telecom.action.CONNECTION_SERVICE_CONFIGURE";

    private final BroadcastReceiver
            mPhoneChangeReceiver = new PhoneChangeReceiver();
    private final ContentObserver
            mDpcEnforcedContentObserver = new DpcApnEnforcedObserver();

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    //Information about logical "up" Activity
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String UP_ACTIVITY_CLASS =
            "com.android.settings.Settings$WirelessSettingsActivity";

    //Information that needs to save into Bundle.
    private static final String EXPAND_ADVANCED_FIELDS = "expand_advanced_fields";
    //Intent extra to indicate expand all fields.
    private static final String EXPAND_EXTRA = "expandable";

    private enum TabState {
        NO_TABS, UPDATE, DO_NOTHING
    }

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private CarrierConfigManager mCarrierConfigManager;
    private int mSubId;

    //UI objects
    private ListPreference mButtonPreferredNetworkMode;
    private ListPreference mButtonEnabledNetworks;
    private RestrictedSwitchPreference mButtonDataRoam;
    private SwitchPreference mButton4glte;
    private Preference mLteDataServicePref;
    private Preference mEuiccSettingsPref;
    private PreferenceCategory mCallingCategory;
    private Preference mWiFiCallingPref;
    private SwitchPreference mVideoCallingPref;
    private NetworkSelectListPreference mButtonNetworkSelect;
    private DataUsagePreference mDataUsagePref;

    private CdmaSystemSelectPreferenceController mCdmaSystemSelectPreferenceController;
    private CdmaSubscriptionPreferenceController mCdmaSubscriptionPreferenceController;

    private static final String iface = "rmnet0"; //TODO: this will go away
    private List<SubscriptionInfo> mActiveSubInfos;

    private UserManager mUm;
    private ImsManager mImsMgr;
    private MyHandler mHandler;
    private boolean mOkClicked;

    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;
    private boolean mShow4GForLTE;
    private boolean mIsGlobalCdma;
    private boolean mOnlyAutoSelectInHomeNW;
    private boolean mUnavailable;

    private class PhoneCallStateListener extends PhoneStateListener {
        /*
         * Enable/disable the 'Enhanced 4G LTE Mode' when in/out of a call
         * and depending on TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);

            updateEnhanced4gLteState();
            updateWiFiCallState();
            updateVideoCallState();
            updatePreferredNetworkType();
        }

        /**
         * Listen to different subId if it's changed.
         */
        protected void updateSubscriptionId(Integer subId) {
            if (subId.equals(PhoneCallStateListener.this.mSubId)) {
                return;
            }

            PhoneCallStateListener.this.mSubId = subId;

            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);

            // Now, listen to new subId if it's valid.
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    private final PhoneCallStateListener
            mPhoneStateListener = new PhoneCallStateListener();

    @Override
    public int getMetricsCategory() {
        //TODO(b/114749736): add metrics id for it
        return 0;
    }

    @Override
    public void onPositiveButtonClick(androidx.fragment.app.DialogFragment dialog) {
        mTelephonyManager.setDataRoamingEnabled(true);
        mButtonDataRoam.setChecked(true);
        MetricsLogger.action(getContext(),
                getMetricsEventCategory(getPreferenceScreen(), mButtonDataRoam),
                true);
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (super.onPreferenceTreeClick(preference)) {
            return true;
        }
        sendMetricsEventPreferenceClicked(getPreferenceScreen(), preference);

        /** TODO: Refactor and get rid of the if's using subclasses */
        if (preference.getKey().equals(BUTTON_4G_LTE_KEY)) {
            return true;
        } else if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
            return true;
        } else if (mCdmaOptions != null &&
                mCdmaOptions.preferenceTreeClick(preference) == true) {
            if (mTelephonyManager.getEmergencyCallbackMode()) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                        new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mLteDataServicePref) {
            String tmpl = android.provider.Settings.Global.getString(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
            if (!TextUtils.isEmpty(tmpl)) {
                String imsi = mTelephonyManager.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.isEmpty(tmpl) ? null
                        : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            }
            return true;
        }  else if (preference == mButtonEnabledNetworks) {
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    preferredNetworkMode);
            mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mButtonDataRoam) {
            // Do not disable the preference screen if the user clicks Data roaming.
            return true;
        } else if (preference == mEuiccSettingsPref) {
            Intent intent = new Intent(EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS);
            startActivity(intent);
            return true;
        } else if (preference == mWiFiCallingPref || preference == mVideoCallingPref
                || preference == mDataUsagePref) {
            return false;
        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            getPreferenceScreen().setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener
            mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged:");
            initializeSubscriptions();
        }
    };

    private int getSlotIdFromIntent(Intent intent) {
        Bundle data = intent.getExtras();
        int subId = -1;
        if (data != null) {
            subId = data.getInt(Settings.EXTRA_SUB_ID, -1);
        }
        return SubscriptionManager.getSlotIndex(subId);
    }

    private void initializeSubscriptions() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            // Process preferences in activity only if its not destroyed
            return;
        }
        updatePhone();
        updateBody();
        if (DBG) log("initializeSubscriptions:-");
    }

    private TabState isUpdateTabsNeeded(List<SubscriptionInfo> newSil) {
        TabState state = TabState.DO_NOTHING;
        if (newSil == null) {
            if (mActiveSubInfos.size() >= TAB_THRESHOLD) {
                if (DBG) log("isUpdateTabsNeeded: NO_TABS, size unknown and was tabbed");
                state = TabState.NO_TABS;
            }
        } else if (newSil.size() < TAB_THRESHOLD && mActiveSubInfos.size() >= TAB_THRESHOLD) {
            if (DBG) log("isUpdateTabsNeeded: NO_TABS, size went to small");
            state = TabState.NO_TABS;
        } else if (newSil.size() >= TAB_THRESHOLD && mActiveSubInfos.size() < TAB_THRESHOLD) {
            if (DBG) log("isUpdateTabsNeeded: UPDATE, size changed");
            state = TabState.UPDATE;
        } else if (newSil.size() >= TAB_THRESHOLD) {
            Iterator<SubscriptionInfo> siIterator = mActiveSubInfos.iterator();
            for(SubscriptionInfo newSi : newSil) {
                SubscriptionInfo curSi = siIterator.next();
                if (!newSi.getDisplayName().equals(curSi.getDisplayName())) {
                    if (DBG) log("isUpdateTabsNeeded: UPDATE, new name="
                            + newSi.getDisplayName());
                    state = TabState.UPDATE;
                    break;
                }
            }
        }
        if (DBG) {
            Log.i(LOG_TAG, "isUpdateTabsNeeded:- " + state
                    + " newSil.size()=" + ((newSil != null) ? newSil.size() : 0)
                    + " mActiveSubInfos.size()=" + mActiveSubInfos.size());
        }
        return state;
    }

    private void updatePhone() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mImsMgr = ImsManager.getInstance(getContext(),
                    SubscriptionManager.getPhoneId(mSubId));
            mTelephonyManager = new TelephonyManager(getContext(), mSubId);
        }

        mPhoneStateListener.updateSubscriptionId(mSubId);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSubId = getArguments().getInt(MobileSettingsActivity.KEY_SUBSCRIPTION_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        use(MobileDataPreferenceController.class).init(getFragmentManager(), mSubId);

        mCdmaSystemSelectPreferenceController = use(CdmaSystemSelectPreferenceController.class);
        mCdmaSystemSelectPreferenceController.init(getPreferenceManager(), mSubId);
        mCdmaSubscriptionPreferenceController = use(CdmaSubscriptionPreferenceController.class);
        mCdmaSubscriptionPreferenceController.init(getPreferenceManager(), mSubId);
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate:+");
        super.onCreate(icicle);
        final Context context = getContext();

        if (context == null) {
            Log.e(LOG_TAG, "onCreate:- with no valid activity.");
            return;
        }

        mHandler = new MyHandler();
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(context);
        mTelephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        mCarrierConfigManager = new CarrierConfigManager(getContext());

        mButton4glte = (SwitchPreference)findPreference(BUTTON_4G_LTE_KEY);
        mButton4glte.setOnPreferenceChangeListener(this);

        mCallingCategory = (PreferenceCategory) findPreference(CATEGORY_CALLING_KEY);
        mWiFiCallingPref = findPreference(BUTTON_WIFI_CALLING_KEY);
        mVideoCallingPref = (SwitchPreference) findPreference(BUTTON_VIDEO_CALLING_KEY);
        mDataUsagePref = (DataUsagePreference) findPreference(BUTTON_DATA_USAGE_KEY);

        try {
            Context con = context.createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            mShow4GForLTE = con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "NameNotFoundException for show4GFotLTE");
            mShow4GForLTE = false;
        }

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mButtonDataRoam = (RestrictedSwitchPreference) prefSet.findPreference(
                BUTTON_ROAMING_KEY);
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonEnabledNetworks = (ListPreference) prefSet.findPreference(
                BUTTON_ENABLED_NETWORKS_KEY);
        mButtonDataRoam.setOnPreferenceChangeListener(this);

        mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);

        mEuiccSettingsPref = prefSet.findPreference(BUTTON_CARRIER_SETTINGS_EUICC_KEY);
        mEuiccSettingsPref.setOnPreferenceChangeListener(this);

        // Initialize mActiveSubInfo
        int max = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        mActiveSubInfos = mSubscriptionManager.getActiveSubscriptionInfoList();

        updatePhone();
        if (hasActiveSubscriptions()) {
            updateEnabledNetworksEntries();
        }
        Log.i(LOG_TAG, "onCreate:-");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
                || !mUm.isSystemUser()) {
            mUnavailable = true;
            //TODO(b/114749736): migrate telephony_disallowed_preference_screen.xml
        } else {
            initializeSubscriptions();
        }
    }

    private class PhoneChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "onReceive:");
            if (getActivity() == null || getContext() == null) {
                // Received broadcast and activity is in the process of being torn down.
                return;
            }
            // When the radio changes (ex: CDMA->GSM), refresh all options.
            updateBody();
        }
    }

    private class DpcApnEnforcedObserver extends ContentObserver {
        DpcApnEnforcedObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.i(LOG_TAG, "DPC enforced onChange:");
            if (getActivity() == null || getContext() == null) {
                // Received content change and activity is in the process of being torn down.
                return;
            }
            updateBody();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume:+");

        if (mUnavailable) {
            Log.i(LOG_TAG, "onResume:- ignore mUnavailable == false");
            return;
        }

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        getPreferenceScreen().setEnabled(true);

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mTelephonyManager.isDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null
                || getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null)  {
            updatePreferredNetworkUIFromDb();
        }

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
        updateEnhanced4gLteState();

        // Video calling and WiFi calling state might have changed.
        updateCallingCategory();

        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        final Context context = getContext();
        IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        context.registerReceiver(mPhoneChangeReceiver, intentFilter);
        context.getContentResolver().registerContentObserver(ENFORCE_MANAGED_URI, false,
                mDpcEnforcedContentObserver);

        Log.i(LOG_TAG, "onResume:-");

    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_setting_fragment;
    }

    @Override
    protected String getLogTag() {
        return null;
    }

    private boolean hasActiveSubscriptions() {
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    private void updateBodyBasicFields(FragmentActivity activity, PreferenceScreen prefSet,
            int phoneSubId, boolean hasActiveSubscriptions) {
        Context context = getContext();

        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        prefSet.addPreference(mButtonDataRoam);
        prefSet.addPreference(mDataUsagePref);

        mButtonDataRoam.setEnabled(hasActiveSubscriptions);
        mDataUsagePref.setEnabled(hasActiveSubscriptions);

        if (hasActiveSubscriptions) {
            // Customized preferences needs to be initialized with subId.
            mDataUsagePref.initialize(phoneSubId);

            // Initialize states of mButtonDataRoam.
            mButtonDataRoam.setChecked(mTelephonyManager.isDataRoamingEnabled());
            if (mButtonDataRoam.isEnabled()) {
                if (RestrictedLockUtilsInternal.hasBaseUserRestriction(context,
                        UserManager.DISALLOW_DATA_ROAMING, UserHandle.myUserId())) {
                    mButtonDataRoam.setEnabled(false);
                } else {
                    mButtonDataRoam.checkRestrictionAndSetDisabled(
                            UserManager.DISALLOW_DATA_ROAMING);
                }
            }
        }
    }

    private void updateBody() {
        final FragmentActivity activity = getActivity();
        final PreferenceScreen prefSet = getPreferenceScreen();
        final boolean hasActiveSubscriptions = hasActiveSubscriptions();

        if (activity == null) {
            Log.e(LOG_TAG, "updateBody with no valid activity.");
            return;
        }

        if (prefSet == null) {
            Log.e(LOG_TAG, "updateBody with no null prefSet.");
            return;
        }

        updateBodyBasicFields(activity, prefSet, mSubId, hasActiveSubscriptions);

        if (hasActiveSubscriptions) {
            updateBodyAdvancedFields(activity, prefSet, mSubId, hasActiveSubscriptions);
        } else {
            // Shows the "Carrier" preference that allows user to add a e-sim profile.
            if (MobileNetworkUtils.showEuiccSettings(getContext())) {
                mEuiccSettingsPref.setSummary(null /* summary */);
                prefSet.addPreference(mEuiccSettingsPref);
            }
        }
    }

    private void updateBodyAdvancedFields(FragmentActivity activity, PreferenceScreen prefSet,
            int phoneSubId, boolean hasActiveSubscriptions) {
        boolean isLteOnCdma = mTelephonyManager.getLteOnCdmaMode()
                == PhoneConstants.LTE_ON_CDMA_TRUE;

        if (DBG) {
            log("updateBody: isLteOnCdma=" + isLteOnCdma + " phoneSubId=" + phoneSubId);
        }

        prefSet.addPreference(mButtonPreferredNetworkMode);
        prefSet.addPreference(mButtonEnabledNetworks);
        prefSet.addPreference(mButton4glte);

        if (MobileNetworkUtils.showEuiccSettings(getContext())) {
            prefSet.addPreference(mEuiccSettingsPref);
            String spn = mTelephonyManager.getSimOperatorName();
            if (TextUtils.isEmpty(spn)) {
                mEuiccSettingsPref.setSummary(null);
            } else {
                mEuiccSettingsPref.setSummary(spn);
            }
        }

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                preferredNetworkMode);

        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        mIsGlobalCdma = isLteOnCdma
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
        if (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            prefSet.removePreference(mButtonEnabledNetworks);
            prefSet.removePreference(mLteDataServicePref);
        } else if (carrierConfig.getBoolean(CarrierConfigManager
                .KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)
                && !mTelephonyManager.getServiceState().getRoaming()
                && mTelephonyManager.getServiceState().getDataRegState()
                == ServiceState.STATE_IN_SERVICE) {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            prefSet.removePreference(mButtonEnabledNetworks);

            final int phoneType = mTelephonyManager.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                updateCdmaOptions(this, prefSet, mSubId);
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                updateGsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            // Since pref is being hidden from user, set network mode to default
            // in case it is currently something else. That is possible if user
            // changed the setting while roaming and is now back to home network.
            settingsNetworkMode = preferredNetworkMode;
        } else if (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_WORLD_PHONE_BOOL) == true) {
            prefSet.removePreference(mButtonEnabledNetworks);
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            updateCdmaOptions(this, prefSet, mSubId);
            updateGsmUmtsOptions(this, prefSet, phoneSubId);
        } else {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            updateEnabledNetworksEntries();
            mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
            if (DBG) log("settingsNetworkMode: " + settingsNetworkMode);
        }

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Global.getString(activity.getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(mLteDataServicePref);
        } else {
            android.util.Log.d(LOG_TAG, "keep ltePref");
        }

        updateEnhanced4gLteState();
        updatePreferredNetworkType();
        updateCallingCategory();

        // Enable link to CMAS app settings depending on the value in config.xml.
        final boolean isCellBroadcastAppLinkEnabled = activity.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        if (!mUm.isAdminUser() || !isCellBroadcastAppLinkEnabled
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
            if (ps != null) {
                root.removePreference(ps);
            }
        }

        // Get the networkMode from Settings.System and displays it
        mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
        mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        // Display preferred network type based on what modem returns b/18676277
        new SetPreferredNetworkAsyncTask(
                mTelephonyManager,
                mSubId,
                settingsNetworkMode,
                mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE))
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        /**
         * Enable/disable depending upon if there are any active subscriptions.
         *
         * I've decided to put this enable/disable code at the bottom as the
         * code above works even when there are no active subscriptions, thus
         * putting it afterwards is a smaller change. This can be refined later,
         * but you do need to remember that this all needs to work when subscriptions
         * change dynamically such as when hot swapping sims.
         */
        boolean useVariant4glteTitle = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_BOOL);
        int enhanced4glteModeTitleId = useVariant4glteTitle ?
                R.string.enhanced_4g_lte_mode_title_variant :
                R.string.enhanced_4g_lte_mode_title;

        mOnlyAutoSelectInHomeNW = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL);
        mButton4glte.setTitle(enhanced4glteModeTitleId);
        mLteDataServicePref.setEnabled(hasActiveSubscriptions);
        Preference ps;
        ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(CATEGORY_GSM_APN_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(CATEGORY_CDMA_APN_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(CATEGORY_CALLING_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(NetworkOperators.BUTTON_AUTO_SELECT_KEY);
        if (ps != null) {
            ps.setSummary(null);
            if (mTelephonyManager.getServiceState().getRoaming()) {
                ps.setEnabled(true);
            } else {
                ps.setEnabled(!mOnlyAutoSelectInHomeNW);
                if (mOnlyAutoSelectInHomeNW) {
                    ps.setSummary(getResources().getString(
                            R.string.manual_mode_disallowed_summary,
                            mTelephonyManager.getSimOperatorName()));
                }
            }
        }
    }

    // Requires that mSubId is up to date
    void updateEnabledNetworksEntries() {
        final int phoneType = mTelephonyManager.getPhoneType();
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            final int lteForced = android.provider.Settings.Global.getInt(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.LTE_SERVICE_FORCED + mSubId,
                    0);
            final boolean isLteOnCdma = mTelephonyManager.getLteOnCdmaMode()
                    == PhoneConstants.LTE_ON_CDMA_TRUE;
            final int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    preferredNetworkMode);
            if (isLteOnCdma) {
                if (lteForced == 0) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_cdma_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_cdma_values);
                } else {
                    switch (settingsNetworkMode) {
                        case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
                        case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                        case TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA:
                            mButtonEnabledNetworks.setEntries(
                                    R.array.enabled_networks_cdma_no_lte_choices);
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_cdma_no_lte_values);
                            break;
                        case TelephonyManager.NETWORK_MODE_GLOBAL:
                        case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                        case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                        case TelephonyManager.NETWORK_MODE_LTE_ONLY:
                            mButtonEnabledNetworks.setEntries(
                                    R.array.enabled_networks_cdma_only_lte_choices);
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_cdma_only_lte_values);
                            break;
                        default:
                            mButtonEnabledNetworks.setEntries(
                                    R.array.enabled_networks_cdma_choices);
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_cdma_values);
                            break;
                    }
                }
            }
            updateCdmaOptions(this, getPreferenceScreen(), mSubId);

        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            if (isSupportTdscdma()) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_tdscdma_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_tdscdma_values);
            } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                    && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_except_gsm_lte_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_except_gsm_lte_values);
            } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
                int select = mShow4GForLTE
                        ? R.array.enabled_networks_except_gsm_4g_choices
                        : R.array.enabled_networks_except_gsm_choices;
                mButtonEnabledNetworks.setEntries(select);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_except_gsm_values);
            } else if (!getResources().getBoolean(R.bool.config_enabled_lte)) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_except_lte_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_except_lte_values);
            } else if (mIsGlobalCdma) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_cdma_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_cdma_values);
            } else {
                int select = mShow4GForLTE ? R.array.enabled_networks_4g_choices
                        : R.array.enabled_networks_choices;
                mButtonEnabledNetworks.setEntries(select);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_values);
            }
            updateGsmUmtsOptions(this, getPreferenceScreen(), mSubId);
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }
        if (isWorldMode()) {
            mButtonEnabledNetworks.setEntries(
                    R.array.preferred_network_mode_choices_world_mode);
            mButtonEnabledNetworks.setEntryValues(
                    R.array.preferred_network_mode_values_world_mode);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DBG) log("onPause:+");

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        mSubscriptionManager
                .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        final Context context = getActivity();
        context.unregisterReceiver(mPhoneChangeReceiver);
        context.getContentResolver().unregisterContentObserver(mDpcEnforcedContentObserver);
        if (DBG) log("onPause:-");
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        sendMetricsEventPreferenceChanged(getPreferenceScreen(), preference, objValue);

        final int phoneSubId = mSubId;
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.parseInt((String) objValue);
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case TelephonyManager.NETWORK_MODE_WCDMA_PREF:
                    case TelephonyManager.NETWORK_MODE_GSM_ONLY:
                    case TelephonyManager.NETWORK_MODE_WCDMA_ONLY:
                    case TelephonyManager.NETWORK_MODE_GSM_UMTS:
                    case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
                    case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                    case TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA:
                    case TelephonyManager.NETWORK_MODE_GLOBAL:
                    case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                    case TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_ONLY:
                    case TelephonyManager.NETWORK_MODE_LTE_WCDMA:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" +buttonNetworkMode+ ") chosen. Ignore.");
                        return true;
                }

                android.provider.Settings.Global.putInt(
                        getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        buttonNetworkMode );
                //Set the modem network mode
                new SetPreferredNetworkAsyncTask(
                        mTelephonyManager,
                        mSubId,
                        modemNetworkMode,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else if (preference == mButtonEnabledNetworks) {
            mButtonEnabledNetworks.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.parseInt((String) objValue);
            if (DBG) log("buttonNetworkMode: " + buttonNetworkMode);
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case TelephonyManager.NETWORK_MODE_WCDMA_PREF:
                    case TelephonyManager.NETWORK_MODE_GSM_ONLY:
                    case TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
                    case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                    case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" +buttonNetworkMode+ ") chosen. Ignore.");
                        return true;
                }

                UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);

                android.provider.Settings.Global.putInt(
                        getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        buttonNetworkMode );
                //Set the modem network mode
                new SetPreferredNetworkAsyncTask(
                        mTelephonyManager,
                        mSubId,
                        modemNetworkMode,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else if (preference == mButton4glte) {
            boolean enhanced4gMode = !mButton4glte.isChecked();
            mButton4glte.setChecked(enhanced4gMode);
            mImsMgr.setEnhanced4gLteModeSetting(mButton4glte.isChecked());
        } else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (!mButtonDataRoam.isChecked()) {
                PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                        mSubId);
                if (carrierConfig != null && carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL)) {
                    mTelephonyManager.setDataRoamingEnabled(true);
                    MetricsLogger.action(getContext(),
                            getMetricsEventCategory(getPreferenceScreen(), mButtonDataRoam),
                            true);
                } else {
                    // MetricsEvent with no value update.
                    MetricsLogger.action(getContext(),
                            getMetricsEventCategory(getPreferenceScreen(), mButtonDataRoam));
                    // First confirm with a warning dialog about charges
                    mOkClicked = false;
                    RoamingDialogFragment
                            fragment = new RoamingDialogFragment();
                    Bundle b = new Bundle();
                    b.putInt(RoamingDialogFragment.SUB_ID_KEY, mSubId);
                    fragment.setArguments(b);
                    fragment.setTargetFragment(this, 0 /* requestCode */);
                    fragment.show(getFragmentManager(), ROAMING_TAG);
                    // Don't update the toggle unless the confirm button is actually pressed.
                    return false;
                }
            } else {
                mTelephonyManager.setDataRoamingEnabled(false);
                MetricsLogger.action(getContext(),
                        getMetricsEventCategory(getPreferenceScreen(), mButtonDataRoam),
                        false);
                return true;
            }
        } else if (preference == mVideoCallingPref) {
            // If mButton4glte is not checked, mVideoCallingPref should be disabled.
            // So it only makes sense to call phoneMgr.enableVideoCalling if it's checked.
            if (mButton4glte.isChecked()) {
                mImsMgr.setVtSetting((boolean) objValue);
                return true;
            } else {
                loge("mVideoCallingPref should be disabled if mButton4glte is not checked.");
                mVideoCallingPref.setEnabled(false);
                return false;
            }
        }

        updateBody();
        // always let the preference setting proceed.
        return true;
    }

    private boolean is4gLtePrefEnabled(PersistableBundle carrierConfig) {
        return (mTelephonyManager.getCallState(mSubId)
                == TelephonyManager.CALL_STATE_IDLE)
                && mImsMgr != null
                && mImsMgr.isNonTtyOrTtyOnVolteEnabled()
                && carrierConfig.getBoolean(
                CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL);
    }

    private class MyHandler extends Handler {

        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            final FragmentActivity activity = getActivity();
            if (activity == null || activity.isDestroyed()) {
                // Access preferences of activity only if it is not destroyed
                // or if fragment is not attached to an activity.
                return;
            }

            boolean success = (boolean) msg.obj;

            if (success) {
                int networkMode;
                if (getPreferenceScreen().findPreference(
                        BUTTON_PREFERED_NETWORK_MODE) != null)  {
                    networkMode =  Integer.parseInt(mButtonPreferredNetworkMode.getValue());
                    android.provider.Settings.Global.putInt(
                            getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                                    + mSubId,
                            networkMode );
                }
                if (getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                    networkMode = Integer.parseInt(mButtonEnabledNetworks.getValue());
                    android.provider.Settings.Global.putInt(
                            getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                                    + mSubId,
                            networkMode );
                }
            } else {
                Log.i(LOG_TAG, "handleSetPreferredNetworkTypeResponse:" +
                        "exception in setting network mode.");
                updatePreferredNetworkUIFromDb();
            }
        }
    }

    private void updatePreferredNetworkUIFromDb() {
        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                preferredNetworkMode);

        if (DBG) {
            log("updatePreferredNetworkUIFromDb: settingsNetworkMode = " +
                    settingsNetworkMode);
        }

        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        // changes the mButtonPreferredNetworkMode accordingly to settingsNetworkMode
        mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch(NetworkMode) {
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_tdscdma_gsm_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_tdscdma_gsm_summary);
                break;
            case TelephonyManager.NETWORK_MODE_WCDMA_PREF:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_perf_summary);
                break;
            case TelephonyManager.NETWORK_MODE_GSM_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_only_summary);
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_tdscdma_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_WCDMA_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_only_summary);
                break;
            case TelephonyManager.NETWORK_MODE_GSM_UMTS:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
                switch (mTelephonyManager.getLteOnCdmaMode()) {
                    case PhoneConstants.LTE_ON_CDMA_TRUE:
                        mButtonPreferredNetworkMode.setSummary(
                                R.string.preferred_network_mode_cdma_summary);
                        break;
                    case PhoneConstants.LTE_ON_CDMA_FALSE:
                    default:
                        mButtonPreferredNetworkMode.setSummary(
                                R.string.preferred_network_mode_cdma_evdo_summary);
                        break;
                }
                break;
            case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_only_summary);
                break;
            case TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_evdo_only_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_tdscdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_tdscdma_gsm_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_tdscdma_gsm_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_cdma_evdo_summary);
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_tdscdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_tdscdma_cdma_evdo_gsm_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (mTelephonyManager.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                        || mIsGlobalCdma
                        || isWorldMode()) {
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_global_summary);
                } else {
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_summary);
                }
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_tdscdma_cdma_evdo_gsm_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_GLOBAL:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_tdscdma_wcdma_summary);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_wcdma_summary);
                break;
            default:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_global_summary);
        }
    }

    private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
        switch (NetworkMode) {
            case TelephonyManager.NETWORK_MODE_TDSCDMA_WCDMA:
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA));
                mButtonEnabledNetworks.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_WCDMA_ONLY:
            case TelephonyManager.NETWORK_MODE_GSM_UMTS:
            case TelephonyManager.NETWORK_MODE_WCDMA_PREF:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager.NETWORK_MODE_WCDMA_PREF));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case TelephonyManager.NETWORK_MODE_GSM_ONLY:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager.NETWORK_MODE_GSM_ONLY));
                    mButtonEnabledNetworks.setSummary(R.string.network_2G);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA:
                if (isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(
                            R.string.preferred_network_mode_lte_gsm_umts_summary);
                    controlCdmaOptions(false);
                    controlGsmOptions(true);
                    break;
                }
            case TelephonyManager.NETWORK_MODE_LTE_ONLY:
            case TelephonyManager.NETWORK_MODE_LTE_WCDMA:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                            ? R.string.network_4G : R.string.network_lte);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                if (isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(
                            R.string.preferred_network_mode_lte_cdma_summary);
                    controlCdmaOptions(true);
                    controlGsmOptions(false);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO));
                    mButtonEnabledNetworks.setSummary(R.string.network_lte);
                }
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(TelephonyManager
                                .NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                mButtonEnabledNetworks.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
            case TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA:
            case TelephonyManager.NETWORK_MODE_GLOBAL:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_CDMA_EVDO));
                mButtonEnabledNetworks.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO));
                mButtonEnabledNetworks.setSummary(R.string.network_1x);
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY));
                mButtonEnabledNetworks.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (isSupportTdscdma()) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_lte);
                } else {
                    if (isWorldMode()) {
                        controlCdmaOptions(true);
                        controlGsmOptions(false);
                    }
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    if (mTelephonyManager.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                            || mIsGlobalCdma
                            || isWorldMode()) {
                        mButtonEnabledNetworks.setSummary(R.string.network_global);
                    } else {
                        mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                                ? R.string.network_4G : R.string.network_lte);
                    }
                }
                break;
            default:
                String errMsg = "Invalid Network Mode (" + NetworkMode + "). Ignore.";
                loge(errMsg);
                mButtonEnabledNetworks.setSummary(errMsg);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_EXIT_ECM:
                Boolean isChoiceYes = data.getBooleanExtra(
                        EXTRA_EXIT_ECM_RESULT, false);
                if (isChoiceYes) {
                    // If the phone exits from ECM mode, show the CDMA Options
                    final String key = mClickedPreference.getKey();
                    if (TextUtils.equals(key,
                            mCdmaSystemSelectPreferenceController.getPreferenceKey())) {
                        mCdmaSystemSelectPreferenceController.showDialog();
                    } else if (TextUtils.equals(key,
                            mCdmaSubscriptionPreferenceController.getPreferenceKey())) {
                        mCdmaSubscriptionPreferenceController.showDialog();
                    }
                } else {
                    // do nothing
                }
                break;

            default:
                break;
        }
    }

    private void updateWiFiCallState() {
        if (mWiFiCallingPref == null || mCallingCategory == null) {
            return;
        }

        // Removes the preference if the wifi calling is disabled.
        if (!MobileNetworkUtils.isWifiCallingEnabled(getContext(),
                SubscriptionManager.getPhoneId(mSubId))) {
            mCallingCategory.removePreference(mWiFiCallingPref);
            return;
        }

        final PhoneAccountHandle simCallManager =
                TelecomManager.from(getContext()).getSimCallManager();

        if (simCallManager != null) {
            Intent intent = buildPhoneAccountConfigureIntent(getContext(), simCallManager);
            PackageManager pm = getContext().getPackageManager();
            List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
            mWiFiCallingPref.setTitle(resolutions.get(0).loadLabel(pm));
            mWiFiCallingPref.setSummary(null);
            mWiFiCallingPref.setIntent(intent);
        } else {
            int resId = com.android.internal.R.string.wifi_calling_off_summary;
            if (mImsMgr.isWfcEnabledByUser()) {
                boolean isRoaming = mTelephonyManager.isNetworkRoaming();
                int wfcMode = mImsMgr.getWfcMode(isRoaming);

                switch (wfcMode) {
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                        resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                        resId = com.android.internal.R.string
                                .wfc_mode_cellular_preferred_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                        break;
                    default:
                        if (DBG) log("Unexpected WFC mode value: " + wfcMode);
                }
            }
            mWiFiCallingPref.setSummary(resId);
        }

        mCallingCategory.addPreference(mWiFiCallingPref);
        mWiFiCallingPref.setEnabled(mTelephonyManager.getCallState(mSubId)
                == TelephonyManager.CALL_STATE_IDLE && hasActiveSubscriptions());
    }

    private void updateEnhanced4gLteState() {
        if (mButton4glte == null) {
            return;
        }

        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);

        if ((mImsMgr == null
                || !mImsMgr.isVolteEnabledByPlatform()
                || !mImsMgr.isVolteProvisionedOnDevice()
                || !MobileNetworkUtils.isImsServiceStateReady(mImsMgr)
                || carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL))) {
            getPreferenceScreen().removePreference(mButton4glte);
        } else {
            mButton4glte.setEnabled(is4gLtePrefEnabled(carrierConfig)
                    && hasActiveSubscriptions());
            boolean enh4glteMode = mImsMgr.isEnhanced4gLteModeSettingEnabledByUser()
                    && mImsMgr.isNonTtyOrTtyOnVolteEnabled();
            mButton4glte.setChecked(enh4glteMode);
        }
    }

    private void updateVideoCallState() {
        if (mVideoCallingPref == null || mCallingCategory == null) {
            return;
        }

        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);

        if (mImsMgr != null
                && mImsMgr.isVtEnabledByPlatform()
                && mImsMgr.isVtProvisionedOnDevice()
                && MobileNetworkUtils.isImsServiceStateReady(mImsMgr)
                && (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS)
                || mTelephonyManager.isDataEnabled())) {
            mCallingCategory.addPreference(mVideoCallingPref);
            if (!mButton4glte.isChecked()) {
                mVideoCallingPref.setEnabled(false);
                mVideoCallingPref.setChecked(false);
            } else {
                mVideoCallingPref.setEnabled(mTelephonyManager.getCallState(mSubId)
                        == TelephonyManager.CALL_STATE_IDLE && hasActiveSubscriptions());
                mVideoCallingPref.setChecked(mImsMgr.isVtEnabledByUser());
                mVideoCallingPref.setOnPreferenceChangeListener(this);
            }
        } else {
            mCallingCategory.removePreference(mVideoCallingPref);
        }
    }

    private void updatePreferredNetworkType() {
        boolean enabled = mTelephonyManager.getCallState(
                mSubId) == TelephonyManager.CALL_STATE_IDLE
                && hasActiveSubscriptions();
        Log.i(LOG_TAG, "updatePreferredNetworkType: " + enabled);
        // TODO: Disentangle enabled networks vs preferred network mode, it looks like
        // both buttons are shown to the user as "Preferred network type" and the options change
        // based on what looks like World mode.
        if (mButtonEnabledNetworks != null) {
            mButtonEnabledNetworks.setEnabled(enabled);
        }
        if (mButtonPreferredNetworkMode != null) {
            mButtonPreferredNetworkMode.setEnabled(enabled);
        }
    }

    private void updateCallingCategory() {
        if (mCallingCategory == null) {
            return;
        }

        updateWiFiCallState();
        updateVideoCallState();

        // If all items in calling category is removed, we remove it from
        // the screen. Otherwise we'll see title of the category but nothing
        // is in there.
        if (mCallingCategory.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(mCallingCategory);
        } else {
            getPreferenceScreen().addPreference(mCallingCategory);
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isWorldMode() {
        boolean worldModeOn = false;
        final String configString = getResources().getString(R.string.config_world_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            // Check if we have World mode configuration set to True only or config is set to True
            // and SIM GID value is also set and matches to the current SIM GID.
            if (configArray != null &&
                    ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true"))
                            || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1])
                            && mTelephonyManager != null
                            && configArray[1].equalsIgnoreCase(
                            mTelephonyManager.getGroupIdLevel1())))) {
                worldModeOn = true;
            }
        }

        Log.d(LOG_TAG, "isWorldMode=" + worldModeOn);

        return worldModeOn;
    }

    private void controlGsmOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }

        updateGsmUmtsOptions(this, prefSet, mSubId);

        PreferenceCategory networkOperatorCategory =
                (PreferenceCategory) prefSet.findPreference(
                        NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
        Preference carrierSettings = prefSet.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (networkOperatorCategory != null) {
            if (enable) {
                networkOperatorCategory.setEnabled(true);
            } else {
                prefSet.removePreference(networkOperatorCategory);
            }
        }
        if (carrierSettings != null) {
            prefSet.removePreference(carrierSettings);
        }
    }

    private void controlCdmaOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }
        updateCdmaOptions(this, prefSet, mSubId);
    }

    private boolean isSupportTdscdma() {
        if (getResources().getBoolean(R.bool.config_support_tdscdma)) {
            return true;
        }

        String operatorNumeric = mTelephonyManager.getServiceState().getOperatorNumeric();
        String[] numericArray = getResources().getStringArray(
                R.array.config_support_tdscdma_roaming_on_networks);
        if (numericArray.length == 0 || operatorNumeric == null) {
            return false;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.equals(numeric)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Metrics events related methods. it takes care of all preferences possible in this
     * fragment(except a few that log on their own). It doesn't only include preferences in
     * network_setting_fragment.xml, but also those defined in GsmUmtsOptions and CdmaOptions.
     */
    private void sendMetricsEventPreferenceClicked(
            PreferenceScreen preferenceScreen, Preference preference) {
        final int category = getMetricsEventCategory(preferenceScreen, preference);
        if (category == MetricsProto.MetricsEvent.VIEW_UNKNOWN) {
            return;
        }

        // Send MetricsEvent on click. It includes preferences other than SwitchPreferences,
        // which send MetricsEvent in onPreferenceChange.
        // For ListPreferences, we log it here without a value, only indicating it's clicked to
        // open the list dialog. When a value is chosen, another MetricsEvent is logged with
        // new value in onPreferenceChange.
        if (preference == mLteDataServicePref || preference == mDataUsagePref
                || preference == mEuiccSettingsPref
                || preference == mWiFiCallingPref || preference == mButtonPreferredNetworkMode
                || preference == mButtonEnabledNetworks
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_GSM_APN_EXPAND_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_APN_EXPAND_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY)) {
            MetricsLogger.action(getContext(), category);
        }
    }

    private void sendMetricsEventPreferenceChanged(
            PreferenceScreen preferenceScreen, Preference preference, Object newValue) {
        final int category = getMetricsEventCategory(preferenceScreen, preference);
        if (category == MetricsProto.MetricsEvent.VIEW_UNKNOWN) {
            return;
        }

        // MetricsEvent logging with new value, for SwitchPreferences and ListPreferences.
        if (preference == mButton4glte || preference == mVideoCallingPref) {
            MetricsLogger.action(getContext(), category, (Boolean) newValue);
        } else if (preference == mButtonPreferredNetworkMode
                || preference == mButtonEnabledNetworks
                || preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY)
                || preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            // Network select preference sends metrics event in its own listener.
            MetricsLogger.action(getContext(), category, Integer.valueOf((String) newValue));
        }
    }

    private int getMetricsEventCategory(
            PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == null) {
            return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
        } else if (preference == mButtonDataRoam) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_DATA_ROAMING_TOGGLE;
        } else if (preference == mDataUsagePref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_DATA_USAGE;
        } else if (preference == mLteDataServicePref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_SET_UP_DATA_SERVICE;
        } else if (preference == mButton4glte) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_ENHANCED_4G_LTE_MODE_TOGGLE;
        } else if (preference == mButtonPreferredNetworkMode) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_SELECT_PREFERRED_NETWORK;
        } else if (preference == mButtonEnabledNetworks) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_SELECT_ENABLED_NETWORK;
        } else if (preference == mEuiccSettingsPref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_EUICC_SETTING;
        } else if (preference == mWiFiCallingPref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_WIFI_CALLING;
        } else if (preference == mVideoCallingPref) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_VIDEO_CALLING_TOGGLE;
        } else if (preference == preferenceScreen
                .findPreference(NetworkOperators.BUTTON_AUTO_SELECT_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_AUTO_SELECT_NETWORK_TOGGLE;
        } else if (preference == preferenceScreen
                .findPreference(NetworkOperators.BUTTON_NETWORK_SELECT_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_MANUAL_SELECT_NETWORK;
        } else if (preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_CDMA_SYSTEM_SELECT;
        } else if (preference == preferenceScreen
                .findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_CDMA_SUBSCRIPTION_SELECT;
        } else if (preference == preferenceScreen.findPreference(BUTTON_GSM_APN_EXPAND_KEY)
                || preference == preferenceScreen.findPreference(BUTTON_CDMA_APN_EXPAND_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_APN_SETTINGS;
        } else if (preference == preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY)) {
            return MetricsProto.MetricsEvent.ACTION_MOBILE_NETWORK_CARRIER_SETTINGS;
        } else {
            return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
        }
    }

    private void updateGsmUmtsOptions(PreferenceFragmentCompat prefFragment,
            PreferenceScreen prefScreen, final int subId) {
        // We don't want to re-create GsmUmtsOptions if already exists. Otherwise, the
        // preferences inside it will also be re-created which causes unexpected behavior.
        // For example, the open dialog gets dismissed or detached after pause / resume.
        if (mGsmUmtsOptions == null) {
            mGsmUmtsOptions = new GsmUmtsOptions(prefFragment, prefScreen, subId);
        } else {
            mGsmUmtsOptions.update(subId);
        }
    }

    private void updateCdmaOptions(PreferenceFragmentCompat prefFragment, PreferenceScreen prefScreen,
            int subId) {
        // We don't want to re-create CdmaOptions if already exists. Otherwise, the preferences
        // inside it will also be re-created which causes unexpected behavior. For example,
        // the open dialog gets dismissed or detached after pause / resume.
        if (mCdmaOptions == null) {
            mCdmaOptions = new CdmaOptions(prefFragment, prefScreen, subId);
        } else {
            mCdmaOptions.updateSubscriptionId(subId);
        }
    }

    private static Intent buildPhoneAccountConfigureIntent(
            Context context, PhoneAccountHandle accountHandle) {
        Intent intent = buildConfigureIntent(
                context, accountHandle, TelecomManager.ACTION_CONFIGURE_PHONE_ACCOUNT);

        if (intent == null) {
            // If the new configuration didn't work, try the old configuration intent.
            intent = buildConfigureIntent(
                    context, accountHandle, LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT);
            if (intent != null) {
                Log.w(MobileNetworkFragment.LOG_TAG,
                        "Phone account using old configuration intent: " + accountHandle);
            }
        }
        return intent;
    }

    private static Intent buildConfigureIntent(
            Context context, PhoneAccountHandle accountHandle, String actionStr) {
        if (accountHandle == null || accountHandle.getComponentName() == null
                || TextUtils.isEmpty(accountHandle.getComponentName().getPackageName())) {
            return null;
        }

        // Build the settings intent.
        Intent intent = new Intent(actionStr);
        intent.setPackage(accountHandle.getComponentName().getPackageName());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);

        // Check to see that the phone account package can handle the setting intent.
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
        if (resolutions.size() == 0) {
            intent = null;  // set no intent if the package cannot handle it.
        }

        return intent;
    }

    //TODO(b/114749736): update search provider
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_setting_fragment;
                    result.add(sir);
                    return result;
                }
            };

    private static final class SetPreferredNetworkAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final TelephonyManager mTelephonyManager;
        private final int mSubId;
        private final int mNetworkType;
        private final Message mCallback;

        SetPreferredNetworkAsyncTask(
                TelephonyManager tm, int subId, int networkType, Message callback) {
            mTelephonyManager = tm;
            mSubId = subId;
            mNetworkType = networkType;
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return mTelephonyManager.setPreferredNetworkType(mSubId, mNetworkType);
        }

        @Override
        protected void onPostExecute(Boolean isSuccessed) {
            mCallback.obj = isSuccessed;
            mCallback.sendToTarget();
        }
    }
}
