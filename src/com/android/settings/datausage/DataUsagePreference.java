/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.net.DataUsageController;

public class DataUsagePreference extends Preference implements TemplatePreference {

    private NetworkTemplate mTemplate;
    private int mSubId;
    private int mTitleRes;

    public DataUsagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, new int[] {com.android.internal.R.attr.title},
                TypedArrayUtils.getAttr(
                        context, androidx.preference.R.attr.preferenceStyle,
                        android.R.attr.preferenceStyle), 0);
        mTitleRes = a.getResourceId(0, 0);
        a.recycle();
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId, NetworkServices services) {
        mTemplate = template;
        mSubId = subId;
        final DataUsageController controller = getDataUsageController();
        if (mTemplate.isMatchRuleMobile()) {
            setTitle(R.string.app_cellular_data_usage);
        } else {
            final DataUsageController.DataUsageInfo usageInfo =
                    controller.getDataUsageInfo(mTemplate);
            setTitle(mTitleRes);
            setSummary(getContext().getString(R.string.data_usage_template,
                    DataUsageUtils.formatDataUsage(getContext(), usageInfo.usageLevel),
                    usageInfo.period));
        }
        final long usageLevel = controller.getHistoricalUsageLevel(template);
        if (usageLevel > 0L) {
            setIntent(getIntent());
        } else {
            setIntent(null);
            setEnabled(false);
        }
    }

    @Override
    public Intent getIntent() {
        final Bundle args = new Bundle();
        final SubSettingLauncher launcher;
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mTemplate);
        args.putInt(DataUsageList.EXTRA_SUB_ID, mSubId);
        args.putInt(DataUsageList.EXTRA_NETWORK_TYPE, mTemplate.isMatchRuleMobile()
            ? ConnectivityManager.TYPE_MOBILE : ConnectivityManager.TYPE_WIFI);
        launcher = new SubSettingLauncher(getContext())
            .setArguments(args)
            .setDestination(DataUsageList.class.getName())
            .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN);
        if (mTemplate.isMatchRuleMobile()) {
            launcher.setTitleRes(R.string.app_cellular_data_usage);
        } else {
            launcher.setTitleRes(mTitleRes);
        }
        return launcher.toIntent();
    }

    @VisibleForTesting
    DataUsageController getDataUsageController() {
        return new DataUsageController(getContext());
    }
}
