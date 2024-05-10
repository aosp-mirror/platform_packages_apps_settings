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

package com.android.settings.development;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ForceEnableNotesRolePreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {


    @VisibleForTesting
    static final String OVERLAY_PACKAGE_NAME =
            "com.android.role.notes.enabled";

    private static final String NOTES_ROLE_ENABLED_KEY =
            "force_enable_notes_role";

    private final IOverlayManager mOverlayManager;
    private final UserManager mUserManager;

    public ForceEnableNotesRolePreferenceController(Context context) {
        super(context);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public String getPreferenceKey() {
        return NOTES_ROLE_ENABLED_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        setEnabled((boolean) newValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) mPreference).setChecked(isEnabled());
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        ((TwoStatePreference) mPreference).setChecked(false);
        setEnabled(false);
    }

    @VisibleForTesting
    protected boolean isEnabled() {
        return mContext.getResources().getBoolean(R.bool.config_enableDefaultNotes);
    }

    @VisibleForTesting
    protected void setEnabled(boolean enabled) {
        try {
            for (UserInfo user : mUserManager.getUsers()) {
                if (user.isFull() || user.isProfile()) {
                    mOverlayManager.setEnabled(OVERLAY_PACKAGE_NAME, enabled, user.id);
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
