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

package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Icon;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Settings;

import java.util.List;

public class WorkModeCondition extends Condition {

    private UserManager mUm;
    private UserHandle mUserHandle;

    public WorkModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
        mUm = (UserManager) mManager.getContext().getSystemService(Context.USER_SERVICE);
    }

    private void updateUserHandle() {
        List<UserInfo> profiles = mUm.getProfiles(UserHandle.myUserId());
        final int profilesCount = profiles.size();
        mUserHandle = null;
        for (int i = 0; i < profilesCount; i++) {
            UserInfo userInfo = profiles.get(i);
            if (userInfo.isManagedProfile()) {
                // We assume there's only one managed profile, otherwise UI needs to change.
                mUserHandle = userInfo.getUserHandle();
                break;
            }
        }
    }

    @Override
    public void refreshState() {
        updateUserHandle();
        setActive(mUserHandle != null && mUm.isQuietModeEnabled(mUserHandle));
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(mManager.getContext(),
                R.drawable.ic_signal_workmode_enable);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_work_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_work_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] {
                mManager.getContext().getString(R.string.condition_turn_on)
        };
    }

    @Override
    public void onPrimaryClick() {
        mManager.getContext().startActivity(new Intent(mManager.getContext(),
                Settings.AccountSettingsActivity.class));
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            if (mUserHandle != null) {
                mUm.trySetQuietModeDisabled(mUserHandle.getIdentifier(), null);
            }
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_WORK_MODE;
    }
}
