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

import static com.android.settings.network.MobileNetworkListFragment.collectAirplaneModeAndFinishIfOn;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Settings.MobileNetworkActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.datausage.BillingCyclePreferenceController;
import com.android.settings.datausage.DataUsageSummaryPreferenceController;
import com.android.settings.network.CarrierWifiTogglePreferenceController;
import com.android.settings.network.MobileNetworkRepository;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.cdma.CdmaSubscriptionPreferenceController;
import com.android.settings.network.telephony.cdma.CdmaSystemSelectPreferenceController;
import com.android.settings.network.telephony.gsm.AutoSelectPreferenceController;
import com.android.settings.network.telephony.gsm.OpenNetworkSelectPagePreferenceController;
import com.android.settings.network.telephony.wificalling.CrossSimCallingViewModel;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MobileNetworkSettings extends AbstractMobileNetworkSettings implements
        MobileNetworkRepository.MobileNetworkCallback {

    private static final String LOG_TAG = "NetworkSettings";
    public static final int REQUEST_CODE_EXIT_ECM = 17;
    public static final int REQUEST_CODE_DELETE_SUBSCRIPTION = 18;
    @VisibleForTesting
    static final String KEY_CLICKED_PREF = "key_clicked_pref";

    private static final String KEY_CALLS_PREF = "calls_preference";
    private static final String KEY_SMS_PREF = "sms_preference";
    private static final String KEY_MOBILE_DATA_PREF = "mobile_data_enable";
    private static final String KEY_CONVERT_TO_ESIM_PREF = "convert_to_esim";
    private static final String KEY_EID_KEY = "network_mode_eid_info";

    //String keys for preference lookup
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private TelephonyManager mTelephonyManager;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private CdmaSystemSelectPreferenceController mCdmaSystemSelectPreferenceController;
    private CdmaSubscriptionPreferenceController mCdmaSubscriptionPreferenceController;

    private UserManager mUserManager;
    private String mClickedPrefKey;

    private MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubInfoEntityList = new ArrayList<>();
    @Nullable
    private SubscriptionInfoEntity mSubscriptionInfoEntity;
    private MobileNetworkInfoEntity mMobileNetworkInfoEntity;

    private BroadcastReceiver mBrocastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                redrawPreferenceControllers();
            }
        }
    };

    public MobileNetworkSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK;
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
        final String key = preference.getKey();

        if (TextUtils.equals(key, BUTTON_CDMA_SYSTEM_SELECT_KEY)
                || TextUtils.equals(key, BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            if (mTelephonyManager.getEmergencyCallbackMode()) {
                startActivityForResult(
                        new Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null)
                                .setPackage(Utils.PHONE_PACKAGE_NAME),
                        REQUEST_CODE_EXIT_ECM);
                mClickedPrefKey = key;
            }
            return true;
        }

        return false;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        if (!SubscriptionUtil.isSimHardwareVisible(context)) {
            finish();
            return Arrays.asList();
        }
        if (getArguments() == null) {
            Intent intent = getIntent();
            if (intent != null) {
                mSubId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                        MobileNetworkUtils.getSearchableSubscriptionId(context));
                Log.d(LOG_TAG, "display subId from intent: " + mSubId);
            } else {
                Log.d(LOG_TAG, "intent is null, can not get subId " + mSubId + " from intent.");
            }
        } else {
            mSubId = getArguments().getInt(Settings.EXTRA_SUB_ID,
                    MobileNetworkUtils.getSearchableSubscriptionId(context));
            Log.d(LOG_TAG, "display subId from getArguments(): " + mSubId);
        }
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mExecutor.execute(() -> {
            mSubscriptionInfoEntity = mMobileNetworkRepository.getSubInfoById(
                    String.valueOf(mSubId));
            mMobileNetworkInfoEntity =
                    mMobileNetworkRepository.queryMobileNetworkInfoBySubId(
                            String.valueOf(mSubId));
        });

        MobileNetworkEidPreferenceController eid = new MobileNetworkEidPreferenceController(context,
                KEY_EID_KEY);
        eid.init(this, mSubId);

        return Arrays.asList(
                new DataUsageSummaryPreferenceController(context, mSubId),
                new CallsDefaultSubscriptionController(context, KEY_CALLS_PREF,
                        getSettingsLifecycle(), this),
                new SmsDefaultSubscriptionController(context, KEY_SMS_PREF, getSettingsLifecycle(),
                        this),
                new MobileDataPreferenceController(context, KEY_MOBILE_DATA_PREF,
                        getSettingsLifecycle(), this, mSubId),
                new ConvertToEsimPreferenceController(context, KEY_CONVERT_TO_ESIM_PREF,
                        getSettingsLifecycle(), this, mSubId), eid);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(LOG_TAG, "Invalid subId, get the default subscription to show.");
            SubscriptionInfo info = SubscriptionUtil.getSubscriptionOrDefault(context, mSubId);
            if (info == null) {
                Log.d(LOG_TAG, "Invalid subId request " + mSubId);
                return;
            }
            mSubId = info.getSubscriptionId();
            Log.d(LOG_TAG, "Show NetworkSettings fragment for subId" + mSubId);
        }

        Intent intent = getIntent();
        if (intent != null) {
            int updateSubscriptionIndex = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);

            if (updateSubscriptionIndex != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                int oldSubId = mSubId;
                mSubId = updateSubscriptionIndex;
                // If the subscription has changed or the new intent does not contain the opt in
                // action,
                // remove the old discovery dialog. If the activity is being recreated, we will see
                // onCreate -> onNewIntent, so the dialog will first be recreated for the old
                // subscription
                // and then removed.
                if (updateSubscriptionIndex != oldSubId
                        || !MobileNetworkActivity.doesIntentContainOptInAction(intent)) {
                    removeContactDiscoveryDialog(oldSubId);
                }

                // evaluate showing the new discovery dialog if this intent contains an action to
                // show the
                // opt-in.
                if (MobileNetworkActivity.doesIntentContainOptInAction(intent)) {
                    showContactDiscoveryDialog();
                }
            }

        }

        use(MobileNetworkSwitchController.class).init(mSubId);
        use(CarrierSettingsVersionPreferenceController.class).init(mSubId);
        use(BillingCyclePreferenceController.class).init(mSubId);
        use(MmsMessagePreferenceController.class).init(mSubId);
        // CrossSimCallingViewModel is responsible for maintaining the correct cross sim calling
        // settings (backup calling).
        new ViewModelProvider(this).get(CrossSimCallingViewModel.class);
        use(AutoDataSwitchPreferenceController.class).init(mSubId);
        use(DisabledSubscriptionController.class).init(mSubId);
        use(DeleteSimProfilePreferenceController.class).init(mSubId);
        use(DisableSimFooterPreferenceController.class).init(mSubId);
        use(NrDisabledInDsdsFooterPreferenceController.class).init(mSubId);

        use(MobileNetworkSpnPreferenceController.class).init(this, mSubId);
        use(MobileNetworkPhoneNumberPreferenceController.class).init(mSubId);
        use(MobileNetworkImeiPreferenceController.class).init(this, mSubId);

        final MobileDataPreferenceController mobileDataPreferenceController =
                use(MobileDataPreferenceController.class);
        if (mobileDataPreferenceController != null) {
            mobileDataPreferenceController.init(getFragmentManager(), mSubId,
                    mSubscriptionInfoEntity, mMobileNetworkInfoEntity);
            mobileDataPreferenceController.setWifiPickerTrackerHelper(
                    new WifiPickerTrackerHelper(getSettingsLifecycle(), context,
                            null /* WifiPickerTrackerCallback */));
        }

        final RoamingPreferenceController roamingPreferenceController =
                use(RoamingPreferenceController.class);
        if (roamingPreferenceController != null) {
            roamingPreferenceController.init(getParentFragmentManager(), mSubId);
        }
        final SatelliteSettingPreferenceController satelliteSettingPreferenceController = use(
                SatelliteSettingPreferenceController.class);
        if (satelliteSettingPreferenceController != null) {
            satelliteSettingPreferenceController.init(mSubId);
        }
        use(ApnPreferenceController.class).init(mSubId);
        use(CarrierPreferenceController.class).init(mSubId);
        use(DataUsagePreferenceController.class).init(mSubId);
        use(PreferredNetworkModePreferenceController.class).init(mSubId);
        use(EnabledNetworkModePreferenceController.class).init(mSubId, getParentFragmentManager());
        use(DataServiceSetupPreferenceController.class).init(mSubId);
        use(Enable2gPreferenceController.class).init(mSubId);
        use(CarrierWifiTogglePreferenceController.class).init(getLifecycle(), mSubId);

        final CallingPreferenceCategoryController callingPreferenceCategoryController =
                use(CallingPreferenceCategoryController.class);
        use(WifiCallingPreferenceController.class)
                .init(mSubId, callingPreferenceCategoryController);

        final OpenNetworkSelectPagePreferenceController openNetworkSelectPagePreferenceController =
                use(OpenNetworkSelectPagePreferenceController.class).init(mSubId);
        final AutoSelectPreferenceController autoSelectPreferenceController =
                use(AutoSelectPreferenceController.class)
                        .init(mSubId)
                        .addListener(openNetworkSelectPagePreferenceController);
        use(NetworkPreferenceCategoryController.class).init(mSubId)
                .setChildren(Arrays.asList(autoSelectPreferenceController));
        mCdmaSystemSelectPreferenceController = use(CdmaSystemSelectPreferenceController.class);
        mCdmaSystemSelectPreferenceController.init(getPreferenceManager(), mSubId);
        mCdmaSubscriptionPreferenceController = use(CdmaSubscriptionPreferenceController.class);
        mCdmaSubscriptionPreferenceController.init(getPreferenceManager(), mSubId);

        final VideoCallingPreferenceController videoCallingPreferenceController =
                use(VideoCallingPreferenceController.class)
                        .init(mSubId, callingPreferenceCategoryController);
        use(Enhanced4gLtePreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(Enhanced4gCallingPreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(Enhanced4gAdvancedCallingPreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(ContactDiscoveryPreferenceController.class).init(getParentFragmentManager(), mSubId);
        use(NrAdvancedCallingPreferenceController.class).init(mSubId);
        use(TransferEsimPreferenceController.class).init(mSubId, mSubscriptionInfoEntity);
        final ConvertToEsimPreferenceController convertToEsimPreferenceController =
                use(ConvertToEsimPreferenceController.class);
        if (convertToEsimPreferenceController != null) {
            convertToEsimPreferenceController.init(mSubId, mSubscriptionInfoEntity);
        }

        List<AbstractSubscriptionPreferenceController> subscriptionPreferenceControllers =
                useAll(AbstractSubscriptionPreferenceController.class);
        for (AbstractSubscriptionPreferenceController controller :
                subscriptionPreferenceControllers) {
            controller.init(mSubId);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate:+");

        final TelephonyStatusControlSession session =
                setTelephonyAvailabilityStatus(getPreferenceControllersAsList());

        super.onCreate(icicle);
        final Context context = getContext();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mTelephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);

        session.close();

        onRestoreInstance(icicle);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        collectAirplaneModeAndFinishIfOn(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMobileNetworkRepository.addRegister(this, this, mSubId);
        mMobileNetworkRepository.updateEntity();
        // TODO: remove log after fixing b/182326102
        Log.d(LOG_TAG, "onResume() subId=" + mSubId);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        getContext().registerReceiver(mBrocastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }

    private void onSubscriptionDetailChanged() {
        final SubscriptionInfoEntity subscriptionInfoEntity = mSubscriptionInfoEntity;
        if (subscriptionInfoEntity == null) {
            return;
        }
        ThreadUtils.postOnMainThread(() -> {
            if (getActivity() instanceof SettingsActivity activity && !activity.isFinishing()) {
                // Update the title when SIM stats got changed
                activity.setTitle(subscriptionInfoEntity.uniqueName);
            }
            redrawPreferenceControllers();
        });
    }

    @Override
    public void onPause() {
        mMobileNetworkRepository.removeRegister(this);
        getContext().unregisterReceiver(mBrocastReceiver);
        super.onPause();
    }

    @VisibleForTesting
    void onRestoreInstance(Bundle icicle) {
        if (icicle != null) {
            mClickedPrefKey = icicle.getString(KEY_CLICKED_PREF);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.mobile_network_settings;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CLICKED_PREF, mClickedPrefKey);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_EXIT_ECM:
                if (resultCode != Activity.RESULT_CANCELED) {
                    // If the phone exits from ECM mode, show the CDMA
                    final Preference preference = getPreferenceScreen()
                            .findPreference(mClickedPrefKey);
                    if (preference != null) {
                        preference.performClick();
                    }
                }
                break;

            case REQUEST_CODE_DELETE_SUBSCRIPTION:
                if (resultCode != Activity.RESULT_CANCELED) {
                    final Activity activity = getActivity();
                    if (activity != null && !activity.isFinishing()) {
                        activity.finish();
                    }
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            final MenuItem item = menu.add(Menu.NONE, R.id.edit_sim_name, Menu.NONE,
                    R.string.mobile_network_sim_name);
            item.setIcon(com.android.internal.R.drawable.ic_mode_edit);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (menuItem.getItemId() == R.id.edit_sim_name) {
                RenameMobileNetworkDialogFragment.newInstance(mSubId).show(
                        getFragmentManager(), RenameMobileNetworkDialogFragment.TAG);
                return true;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mobile_network_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return MobileNetworkSettingsSearchIndex
                            .isMobileNetworkSettingsSearchable(context);
                }
            };

    private ContactDiscoveryDialogFragment getContactDiscoveryFragment(int subId) {
        // In the case that we are rebuilding this activity after it has been destroyed and
        // recreated, look up the dialog in the fragment manager.
        return (ContactDiscoveryDialogFragment) getChildFragmentManager()
                .findFragmentByTag(ContactDiscoveryDialogFragment.getFragmentTag(subId));
    }


    private void removeContactDiscoveryDialog(int subId) {
        ContactDiscoveryDialogFragment fragment = getContactDiscoveryFragment(subId);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showContactDiscoveryDialog() {
        ContactDiscoveryDialogFragment fragment = getContactDiscoveryFragment(mSubId);

        if (mSubscriptionInfoEntity == null) {
            Log.d(LOG_TAG, "showContactDiscoveryDialog, Invalid subId request " + mSubId);
            onDestroy();
            return;
        }

        if (fragment == null) {
            fragment = ContactDiscoveryDialogFragment.newInstance(mSubId,
                    mSubscriptionInfoEntity.uniqueName);
        }
        // Only try to show the dialog if it has not already been added, otherwise we may
        // accidentally add it multiple times, causing multiple dialogs.
        if (!fragment.isAdded()) {
            fragment.show(getChildFragmentManager(),
                    ContactDiscoveryDialogFragment.getFragmentTag(mSubId));
        }
    }

    @Override
    public void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        mSubInfoEntityList = subInfoEntityList;
        SubscriptionInfoEntity[] entityArray = mSubInfoEntityList.toArray(
                new SubscriptionInfoEntity[0]);
        mSubscriptionInfoEntity = null;
        for (SubscriptionInfoEntity entity : entityArray) {
            int subId = Integer.parseInt(entity.subId);
            if (subId == mSubId) {
                mSubscriptionInfoEntity = entity;
                Log.d(LOG_TAG, "Set subInfo for subId " + mSubId);
                break;
            } else if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                    && entity.isDefaultSubscriptionSelection) {
                mSubscriptionInfoEntity = entity;
                Log.d(LOG_TAG, "Set subInfo to default subInfo.");
            }
        }
        if (mSubscriptionInfoEntity == null && getActivity() != null) {
            // If the current subId is not existed, finish it.
            finishFragment();
            return;
        }
        onSubscriptionDetailChanged();
    }
}
