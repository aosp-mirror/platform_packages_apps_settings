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

package com.android.settings.deviceinfo.imei;

import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;

import android.app.Fragment;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.deviceinfo.AbstractSimStatusImeiInfoPreferenceController;

/**
 * Controller that manages preference for single and dual sim devices.
 */
public abstract class AbstractImeiInfoPreferenceController extends
        AbstractSimStatusImeiInfoPreferenceController implements PreferenceControllerMixin {

    protected final boolean mIsMultiSim;
    protected final TelephonyManager mTelephonyManager;

    private Preference mPreference;
    private Fragment mFragment;

    public AbstractImeiInfoPreferenceController(Context context, Fragment fragment) {
        super(context);

        mFragment = fragment;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mIsMultiSim = mTelephonyManager.getPhoneCount() > 1;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference == null) {
            return;
        }
        final int phoneType = mTelephonyManager.getPhoneType();
        if (phoneType == PHONE_TYPE_CDMA) {
            mPreference.setTitle(getTitleForCdmaPhone());
            mPreference.setSummary(getMeid());
        } else {
            // GSM phone
            mPreference.setTitle(getTitleForGsmPhone());
            mPreference.setSummary(mTelephonyManager.getImei(getSimSlot()));
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        ImeiInfoDialogFragment.show(mFragment, getSimSlot(), mPreference.getTitle().toString());
        return true;
    }

    /**
     * @return The preference title for phones based on CDMA technology.
     */
    protected abstract String getTitleForCdmaPhone();

    /**
     * @return The preference title for phones based on GSM technology.
     */
    protected abstract String getTitleForGsmPhone();

    /**
     * @return The sim slot to retrieve IMEI/CDMA information about.
     */
    protected abstract int getSimSlot();

    @VisibleForTesting
    String getMeid() {
        return mTelephonyManager.getMeid(getSimSlot());
    }
}
