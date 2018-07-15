/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.password;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

/**
 * An invisible retained fragment to track lock check result.
 */
public class CredentialCheckResultTracker extends Fragment {

    private Listener mListener;
    private boolean mHasResult = false;

    private boolean mResultMatched;
    private Intent mResultData;
    private int mResultTimeoutMs;
    private int mResultEffectiveUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setListener(Listener listener) {
        if (mListener == listener) {
            return;
        }

        mListener = listener;
        if (mListener != null && mHasResult) {
            mListener.onCredentialChecked(mResultMatched, mResultData, mResultTimeoutMs,
                    mResultEffectiveUserId, false /* newResult */);
        }
    }

    public void setResult(boolean matched, Intent intent, int timeoutMs, int effectiveUserId) {
        mResultMatched = matched;
        mResultData = intent;
        mResultTimeoutMs = timeoutMs;
        mResultEffectiveUserId = effectiveUserId;

        mHasResult = true;
        if (mListener != null) {
            mListener.onCredentialChecked(mResultMatched, mResultData, mResultTimeoutMs,
                    mResultEffectiveUserId, true /* newResult */);
            mHasResult = false;
        }
    }

    public void clearResult() {
        mHasResult = false;
        mResultMatched = false;
        mResultData = null;
        mResultTimeoutMs = 0;
        mResultEffectiveUserId = 0;
    }

    interface Listener {
        public void onCredentialChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId, boolean newResult);
    }
}
