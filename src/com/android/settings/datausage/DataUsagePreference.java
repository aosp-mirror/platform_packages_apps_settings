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
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.net.DataUsageController;

public class DataUsagePreference extends Preference implements TemplatePreference {

    private NetworkTemplate mTemplate;
    private int mSubId;

    public DataUsagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId,
            NetworkServices services) {
        mTemplate = template;
        mSubId = subId;
        DataUsageController controller = new DataUsageController(getContext());
        DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(mTemplate);
        setSummary(getContext().getString(R.string.data_usage_template,
                Formatter.formatFileSize(getContext(), usageInfo.usageLevel), usageInfo.period));
        setIntent(getIntent());
    }

    @Override
    public Intent getIntent() {
        Bundle args = new Bundle();
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mTemplate);
        args.putInt(DataUsageList.EXTRA_SUB_ID, mSubId);
        return Utils.onBuildStartFragmentIntent(getContext(), DataUsageList.class.getName(), args,
                getContext().getPackageName(), 0, getTitle(), false,
                MetricsProto.MetricsEvent.VIEW_UNKNOWN);
    }
}
