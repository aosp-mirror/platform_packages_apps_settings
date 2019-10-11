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
 * limitations under the License
 */

package com.android.settings.applications;

import android.content.Context;
import android.text.format.Formatter;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;

import com.android.internal.util.Preconditions;
import com.android.settingslib.applications.StorageStatsSource;

/**
 * Handles setting the sizes for the app info screen.
 */
public class AppStorageSizesController {
    private final Preference mTotalSize;
    private final Preference mAppSize;
    private final Preference mDataSize;
    private final Preference mCacheSize;
    private final @StringRes int mComputing;
    private final @StringRes int mError;

    @Nullable
    private StorageStatsSource.AppStorageStats mLastResult;
    private boolean mLastResultFailed;
    private boolean mCachedCleared;
    private boolean mDataCleared;
    private long mLastCodeSize = -1;
    private long mLastDataSize = -1;
    private long mLastCacheSize = -1;
    private long mLastTotalSize = -1;

    private AppStorageSizesController(Preference total, Preference app,
            Preference data, Preference cache, @StringRes int computing, @StringRes int error) {
        mTotalSize = total;
        mAppSize = app;
        mDataSize = data;
        mCacheSize = cache;
        mComputing = computing;
        mError = error;
    }

    /**
     * Updates the UI using storage stats.
     * @param context Context to use to fetch strings
     */
    public void updateUi(Context context) {
        if (mLastResult == null) {
            int errorRes = mLastResultFailed ? mError : mComputing;

            mAppSize.setSummary(errorRes);
            mDataSize.setSummary(errorRes);
            mCacheSize.setSummary(errorRes);
            mTotalSize.setSummary(errorRes);
        } else {
            long codeSize = mLastResult.getCodeBytes();
            long dataSize =
                    mDataCleared ? 0 : mLastResult.getDataBytes() - mLastResult.getCacheBytes();
            if (mLastCodeSize != codeSize) {
                mLastCodeSize = codeSize;
                mAppSize.setSummary(getSizeStr(context, codeSize));
            }
            if (mLastDataSize != dataSize) {
                mLastDataSize = dataSize;
                mDataSize.setSummary(getSizeStr(context, dataSize));
            }
            long cacheSize = (mDataCleared || mCachedCleared) ? 0 : mLastResult.getCacheBytes();
            if (mLastCacheSize != cacheSize) {
                mLastCacheSize = cacheSize;
                mCacheSize.setSummary(getSizeStr(context, cacheSize));
            }

            long totalSize = codeSize + dataSize + cacheSize;
            if (mLastTotalSize != totalSize) {
                mLastTotalSize = totalSize;
                mTotalSize.setSummary(getSizeStr(context, totalSize));
            }
        }
    }

    /**
     * Sets a result for the controller to use to update the UI.
     * @param result A result for the UI. If null, count as a failed calculation.
     */
    public void setResult(StorageStatsSource.AppStorageStats result) {
        mLastResult = result;
        mLastResultFailed = result == null;
    }

    /**
     * Sets if we have cleared the cache and should zero the cache bytes.
     * When the cache is cleared, the cache directories are recreated. These directories have
     * some size, but are empty. We zero this out to best match user expectations.
     */
    public void setCacheCleared(boolean isCleared) {
        mCachedCleared = isCleared;
    }

    /**
     * Sets if we have cleared data and should zero the data bytes.
     * When the data is cleared, the directory are recreated. Directories have some size, but are
     * empty. We zero this out to best match user expectations.
     */
    public void setDataCleared(boolean isCleared) {
        mDataCleared = isCleared;
    }

    /**
     * Returns the last result calculated, if it exists. If it does not, returns null.
     */
    public StorageStatsSource.AppStorageStats getLastResult() {
        return mLastResult;
    }

    private String getSizeStr(Context context, long size) {
        return Formatter.formatFileSize(context, size);
    }

    public static class Builder {
        private Preference mTotalSize;
        private Preference mAppSize;
        private Preference mDataSize;
        private Preference mCacheSize;
        private @StringRes int mComputing;
        private @StringRes int mError;

        public Builder setAppSizePreference(Preference preference) {
            mAppSize = preference;
            return this;
        }

        public Builder setDataSizePreference(Preference preference) {
            mDataSize = preference;
            return this;
        }

        public Builder setCacheSizePreference(Preference preference) {
            mCacheSize = preference;
            return this;
        }

        public Builder setTotalSizePreference(Preference preference) {
            mTotalSize = preference;
            return this;
        }

        public Builder setComputingString(@StringRes int sequence) {
            mComputing = sequence;
            return this;
        }

        public Builder setErrorString(@StringRes int sequence) {
            mError = sequence;
            return this;
        }

        public AppStorageSizesController build() {
            return new AppStorageSizesController(
                    Preconditions.checkNotNull(mTotalSize),
                    Preconditions.checkNotNull(mAppSize),
                    Preconditions.checkNotNull(mDataSize),
                    Preconditions.checkNotNull(mCacheSize),
                    mComputing,
                    mError);
        }
    }
}
