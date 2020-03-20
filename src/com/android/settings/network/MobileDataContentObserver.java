/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;

/**
 * {@link ContentObserver} to listen to update of mobile data change
 */
public class MobileDataContentObserver extends ContentObserver {
    private OnMobileDataChangedListener mListener;

    public MobileDataContentObserver(Handler handler) {
        super(handler);
    }

    /**
     * Return a URI of mobile data(ON vs OFF)
     */
    public static Uri getObservableUri(Context context, int subId) {
        Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        if (telephonyManager.getActiveModemCount() != 1) {
            uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId);
        }
        return uri;
    }

    public void setOnMobileDataChangedListener(OnMobileDataChangedListener lsn) {
        mListener = lsn;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        if (mListener != null) {
            mListener.onMobileDataChanged();
        }
    }

    public void register(Context context, int subId) {
        final Uri uri = getObservableUri(context, subId);
        context.getContentResolver().registerContentObserver(uri, false, this);

    }

    public void unRegister(Context context) {
        context.getContentResolver().unregisterContentObserver(this);
    }

    /**
     * Listener for update of mobile data(ON vs OFF)
     */
    public interface OnMobileDataChangedListener {
        void onMobileDataChanged();
    }
}
