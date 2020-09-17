/*
 * Copyright (C) 2020 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting;

/**
 * {@link ContentObserver} to listen to Preferred Network Mode change
 */
public class PreferredNetworkModeContentObserver extends ContentObserver {
    @VisibleForTesting
    OnPreferredNetworkModeChangedListener mListener;

    public PreferredNetworkModeContentObserver(Handler handler) {
        super(handler);
    }

    public void setPreferredNetworkModeChangedListener(OnPreferredNetworkModeChangedListener lsn) {
        mListener = lsn;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        if (mListener != null) {
            mListener.onPreferredNetworkModeChanged();
        }
    }

    public void register(Context context, int subId) {
        final Uri uri = Settings.Global.getUriFor(
                Settings.Global.PREFERRED_NETWORK_MODE + subId);
        context.getContentResolver().registerContentObserver(uri, false, this);
    }

    public void unregister(Context context) {
        context.getContentResolver().unregisterContentObserver(this);
    }

    /**
     * Listener for update of Preferred Network Mode change
     */
    public interface OnPreferredNetworkModeChangedListener {
        void onPreferredNetworkModeChanged();
    }
}
