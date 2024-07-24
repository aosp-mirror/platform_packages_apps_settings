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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
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

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.FooterPreference;

import java.util.Set;

/** Handle Satellite Setting Preference Layout. */
public class SatelliteSetting extends RestrictedDashboardFragment {
    private static final String TAG = "SatelliteSetting";
    public static final String PREF_KEY_ABOUT_SATELLITE_MESSAGING = "key_about_satellite_messaging";
    public static final String PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN =
            "key_category_your_satellite_plan";
    public static final String PREF_KEY_YOUR_SATELLITE_PLAN = "key_your_satellite_plan";
    public static final String PREF_KEY_CATEGORY_HOW_IT_WORKS = "key_category_how_it_works";
    private static final String KEY_FOOTER_PREFERENCE = "satellite_setting_extra_info_footer_pref";
    public static final String SUB_ID = "sub_id";

    private Activity mActivity;
    private TelephonyManager mTelephonymanager;
    private SatelliteManager mSatelliteManager;
    private int mSubId;

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

        mActivity = getActivity();
        mTelephonymanager = mActivity.getSystemService(TelephonyManager.class);
        mSatelliteManager = mActivity.getSystemService(SatelliteManager.class);
        mSubId = mActivity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateDynamicPreferenceViews();
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.satellite_setting;
    }

    private void updateDynamicPreferenceViews() {
        String operatorName = mTelephonymanager.getSimOperatorName(mSubId);
        boolean isSatelliteEligible = isSatelliteEligible();

        // About satellite messaging
        Preference preference = findPreference(PREF_KEY_ABOUT_SATELLITE_MESSAGING);
        preference.setTitle(
                getResources().getString(R.string.title_about_satellite_setting, operatorName));

        // Your mobile plan
        PreferenceCategory prefCategory = findPreference(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        prefCategory.setTitle(getResources().getString(R.string.category_title_your_satellite_plan,
                operatorName));

        preference = findPreference(PREF_KEY_YOUR_SATELLITE_PLAN);
        Drawable icon;
        if (isSatelliteEligible) {
            /* In case satellite is allowed by carrier's entitlement server, the page will show
               the check icon with guidance that satellite is included in user's mobile plan */
            preference.setTitle(R.string.title_have_satellite_plan);
            icon = getResources().getDrawable(R.drawable.ic_check_circle_24px);
        } else {
            /* Or, it will show the blocked icon with the guidance that satellite is not included
               in user's mobile plan */
            preference.setTitle(R.string.title_no_satellite_plan);
            /* And, the link url provides more information via web page will be shown */
            SpannableString spannable = new SpannableString(
                    getResources().getString(R.string.summary_add_satellite_setting));
            spannable.setSpan(new UnderlineSpan(), 0, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            preference.setSummary(spannable);
            /* The link will lead users to a guide page */
            preference.setOnPreferenceClickListener(pref -> {
                String url = getResources().getString(R.string.more_info_satellite_messaging_link);
                if (!url.isEmpty()) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
                return true;
            });
            icon = getResources().getDrawable(R.drawable.ic_block_24px);
        }
        icon.setTintList(Utils.getColorAttr(getContext(), android.R.attr.textColorPrimary));
        preference.setIcon(icon);

        /* Composes "How it works" section, which guides how users can use satellite messaging, when
           satellite messaging is included in user's mobile plan, or it'll will be grey out. */
        if (!isSatelliteEligible) {
            PreferenceCategory category = findPreference(PREF_KEY_CATEGORY_HOW_IT_WORKS);
            category.setEnabled(false);
            category.setShouldDisableView(true);
        }

        // More about satellite messaging
        FooterPreference footerPreference = findPreference(KEY_FOOTER_PREFERENCE);
        if (footerPreference != null) {
            footerPreference.setSummary(
                    getResources().getString(R.string.satellite_setting_summary_more_information,
                            operatorName));

            final String[] link = new String[1];
            link[0] = getResources().getString(R.string.more_info_satellite_messaging_link);
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
                    getResources().getString(R.string.more_about_satellite_messaging));

            // TODO : b/320467418 add rounded rectangle border line to footer preference.
        }
    }

    private boolean isSatelliteEligible() {
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

    private static void loge(String message) {
        Log.e(TAG, message);
    }
}
