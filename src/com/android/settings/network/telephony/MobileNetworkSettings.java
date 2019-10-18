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
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.datausage.BillingCyclePreferenceController;
import com.android.settings.datausage.DataUsageSummaryPreferenceController;
import com.android.settings.development.featureflags.FeatureFlagPersistent;
import com.android.settings.network.telephony.cdma.CdmaSubscriptionPreferenceController;
import com.android.settings.network.telephony.cdma.CdmaSystemSelectPreferenceController;
import com.android.settings.network.telephony.gsm.AutoSelectPreferenceController;
import com.android.settings.network.telephony.gsm.OpenNetworkSelectPagePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MobileNetworkSettings extends RestrictedDashboardFragment {

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
                        new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
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

        if (FeatureFlagPersistent.isEnabled(getContext(), FeatureFlags.NETWORK_INTERNET_V2) &&
            mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return Arrays.asList(
                    new DataUsageSummaryPreferenceController(getActivity(), getSettingsLifecycle(),
                            this, mSubId));
        }
        return Arrays.asList();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (FeatureFlagPersistent.isEnabled(getContext(), FeatureFlags.NETWORK_INTERNET_V2)) {
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
        }
        use(MobileDataPreferenceController.class).init(getFragmentManager(), mSubId);
        use(RoamingPreferenceController.class).init(getFragmentManager(), mSubId);
        use(ApnPreferenceController.class).init(mSubId);
        use(CarrierPreferenceController.class).init(mSubId);
        use(DataUsagePreferenceController.class).init(mSubId);
        use(PreferredNetworkModePreferenceController.class).init(mSubId);
        use(EnabledNetworkModePreferenceController.class).init(getLifecycle(), mSubId);
        use(DataServiceSetupPreferenceController.class).init(mSubId);
        if (!FeatureFlagPersistent.isEnabled(getContext(), FeatureFlags.NETWORK_INTERNET_V2)) {
            use(EuiccPreferenceController.class).init(mSubId);
        }
        use(WifiCallingPreferenceController.class).init(mSubId);

        final OpenNetworkSelectPagePreferenceController openNetworkSelectPagePreferenceController =
                use(OpenNetworkSelectPagePreferenceController.class).init(mSubId);
        final AutoSelectPreferenceController autoSelectPreferenceController =
                use(AutoSelectPreferenceController.class)
                        .init(mSubId)
                        .addListener(openNetworkSelectPagePreferenceController);
        use(PreferenceCategoryController.class).setChildren(
                Arrays.asList(autoSelectPreferenceController));

        mCdmaSystemSelectPreferenceController = use(CdmaSystemSelectPreferenceController.class);
        mCdmaSystemSelectPreferenceController.init(getPreferenceManager(), mSubId);
        mCdmaSubscriptionPreferenceController = use(CdmaSubscriptionPreferenceController.class);
        mCdmaSubscriptionPreferenceController.init(getPreferenceManager(), mSubId);

        final VideoCallingPreferenceController videoCallingPreferenceController =
                use(VideoCallingPreferenceController.class).init(mSubId);
        use(Enhanced4gLtePreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate:+");
        super.onCreate(icicle);
        final Context context = getContext();

        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mTelephonyManager = TelephonyManager.from(context).createForSubscriptionId(mSubId);

        onRestoreInstance(icicle);
    }

    @VisibleForTesting
    void onRestoreInstance(Bundle icicle) {
        if (icicle != null) {
            mClickedPrefKey = icicle.getString(KEY_CLICKED_PREF);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        if (FeatureFlagPersistent.isEnabled(getContext(), FeatureFlags.NETWORK_INTERNET_V2)) {
            return R.xml.mobile_network_settings_v2;
        } else {
            return R.xml.mobile_network_settings;
        }
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
                final Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    activity.finish();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (FeatureFlagPersistent.isEnabled(getContext(), FeatureFlags.NETWORK_INTERNET_V2) &&
                mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            final MenuItem item = menu.add(Menu.NONE, R.id.edit_sim_name, Menu.NONE,
                    R.string.mobile_network_sim_name);
            item.setIcon(com.android.internal.R.drawable.ic_mode_edit);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (FeatureFlagPersistent.isEnabled(getContext(), FeatureFlags.NETWORK_INTERNET_V2) &&
                mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (menuItem.getItemId() == R.id.edit_sim_name) {
                RenameMobileNetworkDialogFragment.newInstance(mSubId).show(
                        getFragmentManager(), RenameMobileNetworkDialogFragment.TAG);
                return true;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.mobile_network_settings;
                    result.add(sir);
                    return result;
                }

                /** suppress full page if user is not admin */
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return context.getSystemService(UserManager.class).isAdminUser();
                }
            };
}
