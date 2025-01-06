/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.satellite.SatelliteManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.FooterPreference;

import java.util.Set;

/** Handle Satellite Setting Preference Layout. */
public class SatelliteSetting extends RestrictedDashboardFragment {
    private static final String TAG = "SatelliteSetting";
    private static final String PREF_KEY_ABOUT_SATELLITE_MESSAGING =
            "key_about_satellite_messaging";
    private static final String PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN =
            "key_category_your_satellite_plan";
    private static final String PREF_KEY_YOUR_SATELLITE_PLAN = "key_your_satellite_plan";
    private static final String PREF_KEY_CATEGORY_HOW_IT_WORKS = "key_category_how_it_works";
    private static final String PREF_KEY_YOUR_SATELLITE_DATA_PLAN = "key_your_satellite_data_plan";
    private static final String PREF_KEY_CATEGORY_ABOUT_SATELLITE = "key_category_about_satellite";
    private static final String KEY_FOOTER_PREFERENCE = "satellite_setting_extra_info_footer_pref";

    static final String SUB_ID = "sub_id";
    static final String EXTRA_IS_SERVICE_DATA_TYPE = "is_service_data_type";
    static final String EXTRA_IS_SMS_AVAILABLE_FOR_MANUAL_TYPE = "is_sms_available";

    private Activity mActivity;
    private SatelliteManager mSatelliteManager;
    private PersistableBundle mConfigBundle;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private String mSimOperatorName = "";
    private boolean mIsServiceDataType = false;
    private boolean mIsSmsAvailableForManualType = false;

    public SatelliteSetting() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SATELLITE_SETTING;
    }

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case carrier roaming satellite is not supported, do nothing.
        if (!Flags.carrierEnabledSatelliteFlag()) {
            Log.d(TAG, "SatelliteSettings: satellite feature is not supported, do nothing.");
            finish();
            return;
        }

        mActivity = getActivity();

        mSatelliteManager = mActivity.getSystemService(SatelliteManager.class);
        if (mSatelliteManager == null) {
            Log.d(TAG, "SatelliteManager is null, do nothing.");
            finish();
            return;
        }

        mSubId = mActivity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mConfigBundle = fetchCarrierConfigData(mSubId);

        if (!isSatelliteAttachSupported(mSubId)) {
            Log.d(TAG, "SatelliteSettings: KEY_SATELLITE_ATTACH_SUPPORTED_BOOL is false, "
                    + "do nothing.");
            finish();
            return;
        }

        mIsServiceDataType = getIntent().getBooleanExtra(EXTRA_IS_SERVICE_DATA_TYPE, false);
        mIsSmsAvailableForManualType = getIntent().getBooleanExtra(
                EXTRA_IS_SMS_AVAILABLE_FOR_MANUAL_TYPE, false);
        mSimOperatorName = getSystemService(TelephonyManager.class).getSimOperatorName(mSubId);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean isSatelliteEligible = isSatelliteEligible();
        updateTitle();
        updateAboutSatelliteContent();
        updateMobilePlan(isSatelliteEligible);
        updateHowItWorksContent(isSatelliteEligible);
        updateFooterContent();
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.satellite_setting;
    }

    private void updateTitle() {
        getActivity().setTitle(getSubjectString());
    }

    // About satellite content
    private void updateAboutSatelliteContent() {
        Preference categoryTitle = findPreference(PREF_KEY_CATEGORY_ABOUT_SATELLITE);
        categoryTitle.setTitle(
                getString(R.string.category_name_about_satellite_messaging,
                        getDescriptionString()));

        Preference preference = findPreference(PREF_KEY_ABOUT_SATELLITE_MESSAGING);
        preference.setTitle(
                getResources().getString(R.string.title_about_satellite_setting, mSimOperatorName));
    }

    private void updateMobilePlan(boolean isSatelliteEligible) {
        // Your mobile plan
        PreferenceCategory prefCategory = findPreference(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        prefCategory.setTitle(getResources().getString(R.string.category_title_your_satellite_plan,
                mSimOperatorName));
        Preference messagingPreference = findPreference(PREF_KEY_YOUR_SATELLITE_PLAN);

        Drawable icon = getContext().getDrawable(R.drawable.ic_check_circle_24px);
        if (isSatelliteEligible) {
            /* In case satellite is allowed by carrier's entitlement server, the page will show
               the check icon with guidance that satellite is included in user's mobile plan */
            messagingPreference.setTitle(R.string.title_have_satellite_plan);
            if (com.android.settings.flags.Flags.satelliteOemSettingsUxMigration()) {
                if (mIsServiceDataType) {
                    Preference connectivityPreference = findPreference(
                            PREF_KEY_YOUR_SATELLITE_DATA_PLAN);
                    connectivityPreference.setTitle(R.string.title_have_satellite_data_plan);
                    connectivityPreference.setIcon(icon);
                    connectivityPreference.setVisible(true);
                }
            }
        } else {
            /* Or, it will show the blocked icon with the guidance that satellite is not included
               in user's mobile plan */
            messagingPreference.setTitle(R.string.title_no_satellite_plan);
            /* And, the link url provides more information via web page will be shown */
            SpannableString spannable = new SpannableString(
                    getResources().getString(R.string.summary_add_satellite_setting));
            spannable.setSpan(new UnderlineSpan(), 0, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            messagingPreference.setSummary(spannable);
            /* The link will lead users to a guide page */
            messagingPreference.setOnPreferenceClickListener(pref -> {
                String url = readSatelliteMoreInfoString();
                if (!url.isEmpty()) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
                return true;
            });
            icon = getResources().getDrawable(R.drawable.ic_block_24px, null);
        }
        icon.setTintList(Utils.getColorAttr(getContext(), android.R.attr.textColorPrimary));
        messagingPreference.setIcon(icon);
    }

    private void updateHowItWorksContent(boolean isSatelliteEligible) {
        /* Composes "How it works" section, which guides how users can use satellite messaging, when
           satellite messaging is included in user's mobile plan, or it'll will be grey out. */
        if (!isSatelliteEligible) {
            PreferenceCategory category = findPreference(PREF_KEY_CATEGORY_HOW_IT_WORKS);
            category.setEnabled(false);
            category.setShouldDisableView(true);
        }
    }

    private void updateFooterContent() {
        // More about satellite messaging
        FooterPreference footerPreference = findPreference(KEY_FOOTER_PREFERENCE);
        if (footerPreference != null) {
            footerPreference.setSummary(
                    getResources().getString(R.string.satellite_setting_summary_more_information,
                            getSubjectString(), mSimOperatorName));

            final String[] link = new String[1];
            link[0] = readSatelliteMoreInfoString();
            if (link[0] != null && !link[0].isEmpty()) {
                footerPreference.setLearnMoreAction(view -> {
                    if (!link[0].isEmpty()) {
                        Intent helpIntent = HelpUtils.getHelpIntent(mActivity, link[0],
                                this.getClass().getName());
                        if (helpIntent != null) {
                            mActivity.startActivityForResult(helpIntent, /*requestCode=*/ 0);
                        }
                    }
                });

                footerPreference.setLearnMoreText(
                        getString(R.string.more_about_satellite_messaging, getDescriptionString()));
            }
        }
    }

    private boolean isSatelliteEligible() {
        if (isCarrierRoamingNtnConnectedTypeManual()) {
            return mIsSmsAvailableForManualType;
        }
        try {
            Set<Integer> restrictionReason =
                    mSatelliteManager.getAttachRestrictionReasonsForCarrier(mSubId);
            return !restrictionReason.contains(
                    SatelliteManager.SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT);
        } catch (SecurityException | IllegalStateException | IllegalArgumentException ex) {
            loge(ex.toString());
            return false;
        }
    }

    private PersistableBundle fetchCarrierConfigData(int subId) {
        CarrierConfigManager carrierConfigManager = mActivity.getSystemService(
                CarrierConfigManager.class);
        PersistableBundle bundle = CarrierConfigManager.getDefaultConfig();
        try {
            bundle = carrierConfigManager.getConfigForSubId(subId,
                    KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                    KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING,
                    KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT);
            if (bundle.isEmpty()) {
                Log.d(TAG, "SatelliteSettings: getDefaultConfig");
                bundle = CarrierConfigManager.getDefaultConfig();
            }
        } catch (IllegalStateException exception) {
            Log.d(TAG, "SatelliteSettings exception : " + exception);
        }
        return bundle;
    }

    private String readSatelliteMoreInfoString() {
        return mConfigBundle.getString(KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING, "");
    }

    private boolean isCarrierRoamingNtnConnectedTypeManual() {
        return CARRIER_ROAMING_NTN_CONNECT_MANUAL == mConfigBundle.getInt(
                KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT, CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
    }

    private boolean isSatelliteAttachSupported(int subId) {

        return mConfigBundle.getBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, false);
    }

    // This is for a word which first letter is uppercase. e.g. Satellite messaging.
    private String getSubjectString() {
        int result;
        if (com.android.settings.flags.Flags.satelliteOemSettingsUxMigration()) {
            result = mIsServiceDataType
                    ? R.string.title_satellite_setting_connectivity
                    : R.string.satellite_setting_title;
        } else {
            result = R.string.satellite_setting_title;
        }
        return getString(result);
    }

    // This is for a word without uppercase letter. e.g. satellite messaging.
    private String getDescriptionString() {
        int result;
        if (com.android.settings.flags.Flags.satelliteOemSettingsUxMigration()) {
            result = mIsServiceDataType
                    ? R.string.description_satellite_setting_connectivity
                    : R.string.description_satellite_setting_messaging;
        } else {
            result = R.string.satellite_setting_title;
        }
        return getString(result);
    }

    private static void loge(String message) {
        Log.e(TAG, message);
    }
}
