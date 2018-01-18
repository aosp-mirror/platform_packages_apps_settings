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

package com.android.settings.development;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class EmulateDisplayCutoutPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String EMULATION_OVERLAY = "com.android.internal.display.cutout.emulation";
    private static final String KEY = "display_cutout_emulation";

    private final IOverlayManager mOverlayManager;
    private final boolean mAvailable;

    private TwoStatePreference mPreference;

    @VisibleForTesting
    EmulateDisplayCutoutPreferenceController(Context context, IOverlayManager overlayManager) {
        super(context);
        mOverlayManager = overlayManager;
        mAvailable = overlayManager != null && getEmulationOverlayInfo() != null;
    }

    public EmulateDisplayCutoutPreferenceController(Context context) {
        this(context, IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE)));
    }

    @Override
    public boolean isAvailable() {
        return mAvailable;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        setPreference((TwoStatePreference) screen.findPreference(getPreferenceKey()));
    }

    @VisibleForTesting
    void setPreference(TwoStatePreference preference) {
        mPreference = preference;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return writeEnabled((boolean) newValue);
    }

    private boolean writeEnabled(boolean newValue) {
        OverlayInfo current = getEmulationOverlayInfo();
        if (current == null || current.isEnabled() == newValue) {
            return false;
        }
        try {
            return mOverlayManager.setEnabled(EMULATION_OVERLAY, newValue, UserHandle.USER_SYSTEM);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void updateState(Preference preference) {
        OverlayInfo overlayInfo = getEmulationOverlayInfo();
        mPreference.setChecked(overlayInfo != null && overlayInfo.isEnabled());
    }

    private OverlayInfo getEmulationOverlayInfo() {
        OverlayInfo overlayInfo = null;
        try {
            overlayInfo = mOverlayManager.getOverlayInfo(EMULATION_OVERLAY, UserHandle.USER_SYSTEM);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        return overlayInfo;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        writeEnabled(false);
        mPreference.setChecked(false);
        mPreference.setEnabled(false);
    }
}
