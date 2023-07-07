/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.activeunlock;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settingslib.utils.ThreadUtils;

/** Listens to updates from the content provider and fetches the latest value. */
public class ActiveUnlockContentListener {

    /** Callback interface for updates to values from the ContentProvider. */
    public interface OnContentChangedListener {
        /**
         * Called when the content observer has updated.
         *
         * @param newValue the new value retrieved from the ContentProvider.
         **/
        void onContentChanged(@Nullable String newValue);
    }

    private static final String CONTENT_PROVIDER_PATH = "getSummary";

    private final Context mContext;
    private final OnContentChangedListener mContentChangedListener;
    @Nullable private final Uri mUri;
    private final String mLogTag;
    private final String mMethodName;
    private final String mContentKey;
    @Nullable private String mContent;
    private boolean mSubscribed = false;
    private ContentObserver mContentObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    getContentFromUri();
                }
            };

    ActiveUnlockContentListener(
            Context context,
            OnContentChangedListener listener,
            String logTag,
            String methodName,
            String contentKey) {
        mContext = context;
        mContentChangedListener = listener;
        mLogTag = logTag;
        mMethodName = methodName;
        mContentKey = contentKey;
        String authority = new ActiveUnlockStatusUtils(mContext).getAuthority();
        if (authority != null) {
            mUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(authority)
                    .appendPath(CONTENT_PROVIDER_PATH)
                    .build();
        } else {
            mUri = null;
        }

    }

    /** Returns true if start listening for updates from the ContentProvider, false otherwise. */
    public synchronized boolean subscribe() {
        if (mSubscribed || mUri == null) {
            return false;
        }
        mSubscribed = true;
        mContext.getContentResolver().registerContentObserver(
                mUri, true /* notifyForDescendants */, mContentObserver);
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    getContentFromUri();
                });
        return true;
    }

    /** Returns true if stops listening for updates from the ContentProvider, false otherewise. */
    public synchronized boolean unsubscribe() {
        if (!mSubscribed || mUri == null) {
            return false;
        }
        mSubscribed = false;
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        return true;
    }

    /** Retrieves the most recently fetched value from the ContentProvider. */
    @Nullable
    public String getContent() {
        return mContent;
    }

    private void getContentFromUri() {
        if (mUri == null) {
            Log.e(mLogTag, "Uri null when trying to fetch content");
            return;
        }
        ContentResolver contentResolver = mContext.getContentResolver();
        ContentProviderClient client = contentResolver.acquireContentProviderClient(mUri);
        Bundle bundle;
        try {
            bundle = client.call(mMethodName, null /* arg */, null /* extras */);
        } catch (RemoteException e) {
            Log.e(mLogTag, "Failed to call contentProvider", e);
            return;
        } finally {
            client.close();
        }
        if (bundle == null) {
            Log.e(mLogTag, "Null bundle returned from contentProvider");
            return;
        }
        String newValue = bundle.getString(mContentKey);
        if (!TextUtils.equals(mContent, newValue)) {
            mContent = newValue;
            mContentChangedListener.onContentChanged(mContent);
        }
    }
}
