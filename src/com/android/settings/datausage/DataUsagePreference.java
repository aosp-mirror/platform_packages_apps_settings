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

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.FeatureFlagUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
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
                        context, android.support.v7.preference.R.attr.preferenceStyle,
                        android.R.attr.preferenceStyle), 0);
        mTitleRes = a.getResourceId(0, 0);
        a.recycle();
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId,
            NetworkServices services) {
        mTemplate = template;
        mSubId = subId;
        DataUsageController controller = new DataUsageController(getContext());
        DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);
        if (FeatureFlagUtils.isEnabled(getContext(), FeatureFlags.DATA_USAGE_SETTINGS_V2)) {
            if (mTemplate.isMatchRuleMobile()) {
                setTitle(R.string.app_cellular_data_usage);
            } else {
                setTitle(mTitleRes);
                setSummary(getContext().getString(R.string.data_usage_template,
                        DataUsageUtils.formatDataUsage(getContext(), usageInfo.usageLevel),
                                usageInfo.period));
            }
        } else {
            setTitle(mTitleRes);
            setSummary(getContext().getString(R.string.data_usage_template,
                    DataUsageUtils.formatDataUsage(getContext(), usageInfo.usageLevel),
                            usageInfo.period));
        }
        setIntent(getIntent());
    }

    @Override
    public Intent getIntent() {
        final Bundle args = new Bundle();
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mTemplate);
        args.putInt(DataUsageList.EXTRA_SUB_ID, mSubId);
        final SubSettingLauncher launcher = new SubSettingLauncher(getContext())
                .setArguments(args)
                .setDestination(DataUsageList.class.getName())
                .setSourceMetricsCategory(MetricsProto.MetricsEvent.VIEW_UNKNOWN);
        if (FeatureFlagUtils.isEnabled(getContext(), FeatureFlags.DATA_USAGE_SETTINGS_V2)) {
            if (mTemplate.isMatchRuleMobile()) {
                launcher.setTitle(R.string.app_cellular_data_usage);
            } else {
                launcher.setTitle(mTitleRes);
            }
        } else {
            if (mTitleRes > 0) {
                launcher.setTitle(mTitleRes);
            } else {
                launcher.setTitle(getTitle());
            }
        }
        return launcher.toIntent();
    }
}
