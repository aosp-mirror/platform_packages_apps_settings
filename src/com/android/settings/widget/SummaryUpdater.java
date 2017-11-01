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

package com.android.settings.widget;

import android.content.Context;
import android.text.TextUtils;

/**
 * Helper class that listens to settings changes and notifies client when there is update in
 * corresponding summary info.
 */
public abstract class SummaryUpdater {

    protected final Context mContext;

    private final OnSummaryChangeListener mListener;
    private String mSummary;

    /**
     * Interface definition for a callback to be invoked when the summary has been changed.
     */
    public interface OnSummaryChangeListener {
        /**
         * Called when summary has changed.
         *
         * @param summary The new summary .
         */
        void onSummaryChanged(String summary);
    }

    /**
     * Constructor
     *
     * @param context The Context the updater is running in, through which it can register broadcast
     * receiver etc.
     * @param listener The listener that would like to receive summary change notification.
     *
     */
    public SummaryUpdater(Context context, OnSummaryChangeListener listener) {
        mContext = context;
        mListener = listener;
    }

    /**
     * Notifies the listener when there is update in summary
     */
    protected void notifyChangeIfNeeded() {
        String summary = getSummary();
        if (!TextUtils.equals(mSummary, summary)) {
            mSummary = summary;
            if (mListener != null) {
                mListener.onSummaryChanged(summary);
            }
        }
    }

    /**
     * Starts/stops receiving updates on the summary.
     *
     * @param register true if we want to receive updates, false otherwise
     */
    public abstract void register(boolean register);

    /**
     * Gets the summary. Subclass should checks latest conditions and update the summary
     * accordingly.
     *
     * @return  the latest summary text
     */
    protected abstract String getSummary();

}
