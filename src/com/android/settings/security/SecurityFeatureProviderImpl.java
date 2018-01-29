/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.security.trustagent.TrustAgentManager;

/** Implementation for {@code SecurityFeatureProvider}. */
public class SecurityFeatureProviderImpl implements SecurityFeatureProvider {

    private TrustAgentManager mTrustAgentManager;
    private LockPatternUtils mLockPatternUtils;

    @Override
    public TrustAgentManager getTrustAgentManager() {
        if (mTrustAgentManager == null) {
            mTrustAgentManager = new TrustAgentManager();
        }
        return mTrustAgentManager;
    }

    @Override
    public LockPatternUtils getLockPatternUtils(Context context) {
        if (mLockPatternUtils == null) {
            mLockPatternUtils = new LockPatternUtils(context.getApplicationContext());
        }
        return mLockPatternUtils;
    }
}
