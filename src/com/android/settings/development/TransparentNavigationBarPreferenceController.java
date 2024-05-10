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

import static android.os.UserHandle.USER_CURRENT;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.os.RemoteException;
import android.os.ServiceManager;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class TransparentNavigationBarPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TRANSPARENT_NAVIGATION_BAR_KEY =
            "transparent_navigation_bar";

    private static final String OVERLAY_PACKAGE_NAME =
            "com.android.internal.systemui.navbar.transparent";

    private final IOverlayManager mOverlayManager;

    public TransparentNavigationBarPreferenceController(Context context) {
        super(context);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
    }

    @Override
    public String getPreferenceKey() {
        return TRANSPARENT_NAVIGATION_BAR_KEY;
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
        final boolean enabled = isEnabled();
        if (!enabled) {
            setEnabled(false);
        }
    }

    @VisibleForTesting
    protected boolean isEnabled() {
        return mContext.getResources().getBoolean(R.bool.config_navBarDefaultTransparent);
    }

    @VisibleForTesting
    protected void setEnabled(boolean enabled) {
        try {
            mOverlayManager.setEnabled(OVERLAY_PACKAGE_NAME, enabled, USER_CURRENT);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
