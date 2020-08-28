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
package com.android.settings.system;

import android.content.Context;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.NetworkResetPreferenceController;

public class ResetPreferenceController extends BasePreferenceController {

    private final UserManager mUm;
    private final NetworkResetPreferenceController mNetworkReset;
    private final FactoryResetPreferenceController mFactpruReset;

    public ResetPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mNetworkReset = new NetworkResetPreferenceController(context);
        mFactpruReset = new FactoryResetPreferenceController(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_reset_dashboard)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (!mNetworkReset.isAvailable() && !mFactpruReset.isAvailable()) {
            return mContext.getText(R.string.reset_dashboard_summary_onlyApps);
        }

        return mContext.getText(R.string.reset_dashboard_summary);
    }
}
