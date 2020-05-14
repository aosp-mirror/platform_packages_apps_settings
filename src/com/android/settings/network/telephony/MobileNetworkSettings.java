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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.datausage.BillingCyclePreferenceController;
import com.android.settings.datausage.DataUsageSummaryPreferenceController;
import com.android.settings.network.ActiveSubsciptionsListener;
import com.android.settings.network.telephony.cdma.CdmaSubscriptionPreferenceController;
import com.android.settings.network.telephony.cdma.CdmaSystemSelectPreferenceController;
import com.android.settings.network.telephony.gsm.AutoSelectPreferenceController;
import com.android.settings.network.telephony.gsm.OpenNetworkSelectPagePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Arrays;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MobileNetworkSettings extends AbstractMobileNetworkSettings {

    private static final String LOG_TAG = "NetworkSettings";
    public static final int REQUEST_CODE_EXIT_ECM = 17;
    public static final int REQUEST_CODE_DELETE_SUBSCRIPTION = 18;
    @VisibleForTesting
    static final String KEY_CLICKED_PREF = "key_clicked_pref";

    //String keys for preference lookup
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";

    private TelephonyManager mTelephonyManager;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private CdmaSystemSelectPreferenceController mCdmaSystemSelectPreferenceController;
    private CdmaSubscriptionPreferenceController mCdmaSubscriptionPreferenceController;

    private UserManager mUserManager;
    private String mClickedPrefKey;

    private ActiveSubsciptionsListener mActiveSubsciptionsListener;
    private boolean mDropFirstSubscriptionChangeNotify;
    private int mActiveSubsciptionsListenerCount;

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
                        new Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_EXIT_ECM);
                mClickedPrefKey = key;
            }
            return true;
        }

        return false;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mSubId = getArguments().getInt(Settings.EXTRA_SUB_ID,
                MobileNetworkUtils.getSearchableSubscriptionId(context));
        Log.i(LOG_TAG, "display subId: " + mSubId);

        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return Arrays.asList();
        }
        return Arrays.asList(
                new DataUsageSummaryPreferenceController(getActivity(), getSettingsLifecycle(),
                        this, mSubId));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final DataUsageSummaryPreferenceController dataUsageSummaryPreferenceController =
                use(DataUsageSummaryPreferenceController.class);
        if (dataUsageSummaryPreferenceController != null) {
            dataUsageSummaryPreferenceController.init(mSubId);
        }
        use(CallsDefaultSubscriptionController.class).init(getLifecycle());
        use(SmsDefaultSubscriptionController.class).init(getLifecycle());
        use(MobileNetworkSwitchController.class).init(getLifecycle(), mSubId);
        use(CarrierSettingsVersionPreferenceController.class).init(mSubId);
        use(BillingCyclePreferenceController.class).init(mSubId);
        use(MmsMessagePreferenceController.class).init(mSubId);
        use(DataDuringCallsPreferenceController.class).init(getLifecycle(), mSubId);
        use(DisabledSubscriptionController.class).init(getLifecycle(), mSubId);
        use(DeleteSimProfilePreferenceController.class).init(mSubId, this,
                REQUEST_CODE_DELETE_SUBSCRIPTION);
        use(DisableSimFooterPreferenceController.class).init(mSubId);
        use(NrDisabledInDsdsFooterPreferenceController.class).init(mSubId);
        use(MobileDataPreferenceController.class).init(getFragmentManager(), mSubId);
        use(RoamingPreferenceController.class).init(getFragmentManager(), mSubId);
        use(ApnPreferenceController.class).init(mSubId);
        use(CarrierPreferenceController.class).init(mSubId);
        use(DataUsagePreferenceController.class).init(mSubId);
        use(PreferredNetworkModePreferenceController.class).init(mSubId);
        use(EnabledNetworkModePreferenceController.class).init(getLifecycle(), mSubId);
        use(DataServiceSetupPreferenceController.class).init(mSubId);

        final WifiCallingPreferenceController wifiCallingPreferenceController =
                use(WifiCallingPreferenceController.class).init(mSubId);

        final OpenNetworkSelectPagePreferenceController openNetworkSelectPagePreferenceController =
                use(OpenNetworkSelectPagePreferenceController.class).init(getLifecycle(), mSubId);
        final AutoSelectPreferenceController autoSelectPreferenceController =
                use(AutoSelectPreferenceController.class)
                        .init(getLifecycle(), mSubId)
                        .addListener(openNetworkSelectPagePreferenceController);
        use(NetworkPreferenceCategoryController.class).init(getLifecycle(), mSubId)
                .setChildren(Arrays.asList(autoSelectPreferenceController));
        mCdmaSystemSelectPreferenceController = use(CdmaSystemSelectPreferenceController.class);
        mCdmaSystemSelectPreferenceController.init(getPreferenceManager(), mSubId);
        mCdmaSubscriptionPreferenceController = use(CdmaSubscriptionPreferenceController.class);
        mCdmaSubscriptionPreferenceController.init(getPreferenceManager(), mSubId);

        final VideoCallingPreferenceController videoCallingPreferenceController =
                use(VideoCallingPreferenceController.class).init(mSubId);
        use(CallingPreferenceCategoryController.class).setChildren(
                Arrays.asList(wifiCallingPreferenceController, videoCallingPreferenceController));
        use(Enhanced4gLtePreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(Enhanced4gCallingPreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(Enhanced4gAdvancedCallingPreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(ContactDiscoveryPreferenceController.class).init(getParentFragmentManager(), mSubId,
                getLifecycle());
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
    public void onResume() {
        super.onResume();
        if (mActiveSubsciptionsListener == null) {
            mActiveSubsciptionsListener = new ActiveSubsciptionsListener(
                    getContext().getMainLooper(), getContext(), mSubId) {
                public void onChanged() {
                    onSubscriptionDetailChanged();
                }
            };
            mDropFirstSubscriptionChangeNotify = true;
        }
        mActiveSubsciptionsListener.start();
    }

    private void onSubscriptionDetailChanged() {
        if (mDropFirstSubscriptionChangeNotify) {
            mDropFirstSubscriptionChangeNotify = false;
            Log.d(LOG_TAG, "Callback during onResume()");
            return;
        }
        mActiveSubsciptionsListenerCount++;
        if (mActiveSubsciptionsListenerCount != 1) {
            return;
        }

        ThreadUtils.postOnMainThread(() -> {
            mActiveSubsciptionsListenerCount = 0;
            redrawPreferenceControllers();
        });
    }

    @Override
    public void onDestroy() {
        if (mActiveSubsciptionsListener != null) {
            mActiveSubsciptionsListener.stop();
        }
        super.onDestroy();
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
        if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
            final MenuItem item = menu.add(Menu.NONE, R.id.edit_sim_name, Menu.NONE,
                    R.string.mobile_network_sim_name);
            item.setIcon(com.android.internal.R.drawable.ic_mode_edit);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
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

                /** suppress full page if user is not admin */
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return context.getSystemService(UserManager.class).isAdminUser();
                }
            };
}
