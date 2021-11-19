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

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsRcsManager;
import android.text.TextUtils;
import android.util.Log;


import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.network.SubscriptionUtil;

/**
 * Activity for displaying MobileNetworkSettings.
 *
 * @Deprecated The MobileNetworkActivity should be removed in Android U. Instead of using the
 * singleTask activity which will cause an additional window transition when users launch the SIMs
 * page, using the {@link com.android.settings.Settings.SubscriptionSettingsActivity} which can be
 * managed by {@link SettingsActivity} and be migrated into the Settings architecture.
 */
@Deprecated
public class MobileNetworkActivity extends Activity {

    private static final String TAG = "MobileNetworkActivity";
    public static final String SHOW_CAPABILITY_DISCOVERY_OPT_IN =
            "show_capability_discovery_opt_in";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final UserManager userManager = this.getSystemService(UserManager.class);
        if (!userManager.isAdminUser()) {
            this.finish();
            return;
        }

        // TODO: Move these intent's extra into SubscriptionSettingsActivity if the
        //  MobileNetworkActivity is removed in Android U.
        Intent intent = getIntent();
        if (intent == null) {
            Log.d(TAG, "onCreate(), intent = null");
            this.finish();
            return;
        }

        Intent trampolineIntent;
        final Intent subscriptionSettingsIntent = new Intent(this,
                com.android.settings.Settings.SubscriptionSettingsActivity.class);
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this) || !isTaskRoot()) {
            trampolineIntent = subscriptionSettingsIntent;
        } else {
            trampolineIntent = new Intent(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)
                    .setPackage(Utils.SETTINGS_PACKAGE_NAME);
            trampolineIntent.putExtra(
                    android.provider.Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI,
                    subscriptionSettingsIntent.toUri(Intent.URI_INTENT_SCHEME));
        }

        int subId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        SubscriptionInfo subInfo = SubscriptionUtil.getSubscriptionOrDefault(this, subId);
        CharSequence title = SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, this);
        trampolineIntent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE, title);
        trampolineIntent.putExtra(Settings.EXTRA_SUB_ID, subId);
        if (Settings.ACTION_MMS_MESSAGE_SETTING.equals(intent.getAction())) {
            // highlight "mms_message" preference.
            trampolineIntent.putExtra(EXTRA_FRAGMENT_ARG_KEY, "mms_message");
        }

        if (doesIntentContainOptInAction(intent)) {
            trampolineIntent.putExtra(SHOW_CAPABILITY_DISCOVERY_OPT_IN,
                    maybeShowContactDiscoveryDialog(subId));
        }

        startActivity(trampolineIntent);
        if (isTaskRoot()) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    private boolean maybeShowContactDiscoveryDialog(int subId) {
        // If this activity was launched using ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN, show the
        // associated dialog only if the opt-in has not been granted yet.
        return MobileNetworkUtils.isContactDiscoveryVisible(this, subId)
                // has the user already enabled this configuration?
                && !MobileNetworkUtils.isContactDiscoveryEnabled(this, subId);
    }

    public static boolean doesIntentContainOptInAction(Intent intent) {
        String intentAction = (intent != null ? intent.getAction() : null);
        return TextUtils.equals(intentAction,
                ImsRcsManager.ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN);
    }
}
