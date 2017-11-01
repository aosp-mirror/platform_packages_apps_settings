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

import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.DefaultDialerManager;
import android.telecom.TelecomManager;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;

import java.util.ArrayList;
import java.util.List;

public class DefaultPhonePicker extends DefaultAppPickerFragment {

    private DefaultKeyUpdater mDefaultKeyUpdater;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_PHONE_PICKER;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDefaultKeyUpdater = new DefaultKeyUpdater(
                (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE));
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final List<String> dialerPackages =
                DefaultDialerManager.getInstalledDialerApplications(getContext(), mUserId);
        for (String packageName : dialerPackages) {
            try {
                candidates.add(new DefaultAppInfo(mPm,
                        mPm.getApplicationInfoAsUser(packageName, 0, mUserId)));
            } catch (PackageManager.NameNotFoundException e) {
                // Skip unknown packages.
            }
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        return mDefaultKeyUpdater.getDefaultDialerApplication(getContext(), mUserId);
    }

    @Override
    protected String getSystemDefaultKey() {
        return mDefaultKeyUpdater.getSystemDialerPackage();
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (!TextUtils.isEmpty(key) && !TextUtils.equals(key, getDefaultKey())) {
            return mDefaultKeyUpdater.setDefaultDialerApplication(getContext(), key, mUserId);
        }
        return false;
    }

    /**
     * Wrapper class to handle default phone app update.
     */
    static class DefaultKeyUpdater {
        private final TelecomManager mTelecomManager;

        public DefaultKeyUpdater(TelecomManager telecomManager) {
            mTelecomManager = telecomManager;
        }

        public String getSystemDialerPackage() {
            return mTelecomManager.getSystemDialerPackage();
        }

        public String getDefaultDialerApplication(Context context, int uid) {
            return DefaultDialerManager.getDefaultDialerApplication(context, uid);
        }

        public boolean setDefaultDialerApplication(Context context, String key, int uid) {
            return DefaultDialerManager.setDefaultDialerApplication(context, key, uid);
        }
    }
}
