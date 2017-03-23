/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.defaultapps;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.SmsApplication;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultSmsPicker extends DefaultAppPickerFragment {

    private DefaultKeyUpdater mDefaultKeyUpdater = new DefaultKeyUpdater();

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_SMS_PICKER;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final Collection<SmsApplication.SmsApplicationData> smsApplications =
                SmsApplication.getApplicationCollection(getContext());
        final List<DefaultAppInfo> candidates = new ArrayList<>(smsApplications.size());

        for (SmsApplication.SmsApplicationData smsApplicationData : smsApplications) {
            try {
                candidates.add(new DefaultAppInfo(mPm,
                        mPm.getApplicationInfoAsUser(smsApplicationData.mPackageName, 0, mUserId)));
            } catch (PackageManager.NameNotFoundException e) {
                // Skip unknown packages.
            }
        }

        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        return mDefaultKeyUpdater.getDefaultApplication(getContext());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (!TextUtils.isEmpty(key) && !TextUtils.equals(key, getDefaultKey())) {
            mDefaultKeyUpdater.setDefaultApplication(getContext(), key);
            return true;
        }
        return false;
    }

    @Override
    protected String getConfirmationMessage(CandidateInfo info) {
        return Utils.isPackageDirectBootAware(getContext(), info.getKey()) ? null
                : getContext().getString(R.string.direct_boot_unaware_dialog_message);
    }

    /**
     * Wrapper class to handle default phone app update.
     */
    static class DefaultKeyUpdater {

        public String getDefaultApplication(Context context) {
            final ComponentName appName = SmsApplication.getDefaultSmsApplication(context, true);
            if (appName != null) {
                return appName.getPackageName();
            }
            return null;
        }

        public void setDefaultApplication(Context context, String key) {
            SmsApplication.setDefaultApplication(key, context);
        }
    }
}
