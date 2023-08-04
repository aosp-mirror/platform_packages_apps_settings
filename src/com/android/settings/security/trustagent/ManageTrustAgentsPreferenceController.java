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

package com.android.settings.security.trustagent;

import android.content.Context;
import android.os.UserHandle;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settingslib.utils.StringUtil;

public class ManageTrustAgentsPreferenceController extends BasePreferenceController {

    private static final int MY_USER_ID = UserHandle.myUserId();

    private final LockPatternUtils mLockPatternUtils;
    private TrustAgentManager mTrustAgentManager;

    public ManageTrustAgentsPreferenceController(Context context, String key) {
        super(context, key);
        final SecurityFeatureProvider securityFeatureProvider = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider();
        mLockPatternUtils = securityFeatureProvider.getLockPatternUtils(context);
        mTrustAgentManager = securityFeatureProvider.getTrustAgentManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_manage_trust_agents)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        final int numberOfTrustAgent = getTrustAgentCount();
        if (!mLockPatternUtils.isSecure(MY_USER_ID)) {
            preference.setEnabled(false);
            preference.setSummary(R.string.disabled_because_no_backup_security);
        } else if (numberOfTrustAgent > 0) {
            preference.setEnabled(true);
            preference.setSummary(StringUtil.getIcuPluralsString(mContext, numberOfTrustAgent,
                    R.string.manage_trust_agents_summary_on));
        } else {
            preference.setEnabled(true);
            preference.setSummary(R.string.manage_trust_agents_summary);
        }
    }

    private int getTrustAgentCount() {
        return mTrustAgentManager.getActiveTrustAgents(mContext, mLockPatternUtils, false).size();
    }
}
