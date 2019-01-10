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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Mobile data"
 */
public class MobileDataPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String DIALOG_TAG = "MobileDataDialog";

    private SwitchPreference mPreference;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private DataContentObserver mDataContentObserver;
    private FragmentManager mFragmentManager;
    private int mSubId;
    @VisibleForTesting
    int mDialogType;
    @VisibleForTesting
    boolean mNeedDialog;

    public MobileDataPreferenceController(Context context, String key) {
        super(context, key);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mDataContentObserver = new DataContentObserver(new Handler(Looper.getMainLooper()));
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @Override
    public int getAvailabilityStatus() {
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mDataContentObserver.register(mContext, mSubId);
        }
    }

    @Override
    public void onStop() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mDataContentObserver.unRegister(mContext);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            if (mNeedDialog) {
                showDialog(mDialogType);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mNeedDialog = isDialogNeeded();

        if (!mNeedDialog) {
            // Update data directly if we don't need dialog
            MobileNetworkUtils.setMobileDataEnabled(mContext, mSubId, isChecked, false);
            return true;
        }

        return false;
    }

    @Override
    public boolean isChecked() {
        return mTelephonyManager.isDataEnabled()
                && mSubId == SubscriptionManager.getDefaultDataSubscriptionId();
    }

    public static Uri getObservableUri(int subId) {
        Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
        if (TelephonyManager.getDefault().getSimCount() != 1) {
            uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId);
        }
        return uri;
    }

    public void init(FragmentManager fragmentManager, int subId) {
        mFragmentManager = fragmentManager;
        mSubId = subId;
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);
    }

    @VisibleForTesting
    boolean isDialogNeeded() {
        final boolean enableData = !isChecked();
        final boolean isMultiSim = (mTelephonyManager.getSimCount() > 1);
        final int defaultSubId = mSubscriptionManager.getDefaultDataSubscriptionId();
        final boolean needToDisableOthers = mSubscriptionManager
                .isActiveSubscriptionId(defaultSubId) && defaultSubId != mSubId;
        if (enableData) {
            if (isMultiSim && needToDisableOthers) {
                mDialogType = MobileDataDialogFragment.TYPE_MULTI_SIM_DIALOG;
                return true;
            }
        } else {
            if (!isMultiSim) {
                mDialogType = MobileDataDialogFragment.TYPE_DISABLE_DIALOG;
                return true;
            }
        }

        return false;
    }

    private void showDialog(int type) {
        final MobileDataDialogFragment dialogFragment = MobileDataDialogFragment.newInstance(type,
                mSubId);
        dialogFragment.show(mFragmentManager, DIALOG_TAG);
    }

    /**
     * Listener that listens mobile data state change.
     */
    public class DataContentObserver extends ContentObserver {

        public DataContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            final Uri uri = getObservableUri(subId);
            context.getContentResolver().registerContentObserver(uri, false, this);

        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }
    }
}
