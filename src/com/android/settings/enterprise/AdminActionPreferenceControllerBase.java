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

package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyResources.Strings.Settings.ADMIN_ACTION_NONE;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.text.format.DateUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Date;

public abstract class AdminActionPreferenceControllerBase extends
        AbstractPreferenceController implements PreferenceControllerMixin {

    protected final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public AdminActionPreferenceControllerBase(Context context) {
        super(context);
        mFeatureProvider = FeatureFactory.getFeatureFactory()
                .getEnterprisePrivacyFeatureProvider();
    }

    protected abstract Date getAdminActionTimestamp();

    @Override
    public void updateState(Preference preference) {
        final Date timestamp = getAdminActionTimestamp();
        preference.setSummary(timestamp == null ?
                getEnterprisePrivacyNone() :
                DateUtils.formatDateTime(mContext, timestamp.getTime(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE));
    }

    private String getEnterprisePrivacyNone() {
        return ((DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .getResources()
                .getString(ADMIN_ACTION_NONE,
                        () -> mContext.getString(R.string.enterprise_privacy_none));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
