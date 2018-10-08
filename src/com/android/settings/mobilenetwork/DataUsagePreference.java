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

package com.android.settings.mobilenetwork;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import androidx.preference.Preference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.AttributeSet;

import com.android.settings.R;
import com.android.settingslib.net.DataUsageController;

/**
 * The preference that shows mobile data usage summary and
 * leads to mobile data usage list page.
 */
public class DataUsagePreference extends Preference {

    private NetworkTemplate mTemplate;
    private int mSubId;

    public DataUsagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * After creating this preference, this functions needs to be called to
     * initialize which subID it connects to.
     */
    public void initialize(int subId) {
        final Context context = getContext();
        mSubId = subId;
        mTemplate = getNetworkTemplate(context, subId);

        DataUsageController controller = new DataUsageController(context);

        DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);
        setSummary(context.getString(R.string.data_usage_template,
                Formatter.formatFileSize(context, usageInfo.usageLevel), usageInfo.period));
        setIntent(getIntent());
    }

    @Override
    public Intent getIntent() {
        Intent intent = new Intent(Settings.ACTION_MOBILE_DATA_USAGE);

        intent.putExtra(Settings.EXTRA_NETWORK_TEMPLATE, mTemplate);
        intent.putExtra(Settings.EXTRA_SUB_ID, mSubId);

        return intent;
    }

    private NetworkTemplate getNetworkTemplate(Context context, int subId) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                tm.getSubscriberId(subId));
        return NetworkTemplate.normalize(mobileAll,
                tm.getMergedSubscriberIds());
    }
}
