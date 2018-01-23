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

import static android.os.UserHandle.USER_SYSTEM;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.List;

public class EmulateDisplayCutoutPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    public static final String EMULATION_OVERLAY_PREFIX =
            "com.android.internal.display.cutout.emulation.";
    private static final String KEY = "display_cutout_emulation";

    private final IOverlayManager mOverlayManager;
    private final boolean mAvailable;

    private ListPreference mPreference;
    private PackageManager mPackageManager;

    @VisibleForTesting
    EmulateDisplayCutoutPreferenceController(Context context, PackageManager packageManager,
            IOverlayManager overlayManager) {
        super(context);
        mOverlayManager = overlayManager;
        mPackageManager = packageManager;
        mAvailable = overlayManager != null && getOverlayInfos().length > 0;
    }

    public EmulateDisplayCutoutPreferenceController(Context context) {
        this(context, context.getPackageManager(), IOverlayManager.Stub.asInterface(
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
        setPreference((ListPreference) screen.findPreference(getPreferenceKey()));
    }

    @VisibleForTesting
    void setPreference(ListPreference preference) {
        mPreference = preference;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return setEmulationOverlay((String) newValue);
    }

    private boolean setEmulationOverlay(String packageName) {
        OverlayInfo[] overlays = getOverlayInfos();
        CharSequence currentPackageName = null;
        for (OverlayInfo o : overlays) {
            if (o.isEnabled()) {
                currentPackageName = o.packageName;
            }
        }

        if (TextUtils.isEmpty(packageName) && TextUtils.isEmpty(currentPackageName)
                || TextUtils.equals(packageName, currentPackageName)) {
            // Already set.
            return true;
        }

        for (OverlayInfo o : overlays) {
            boolean isEnabled = o.isEnabled();
            boolean shouldBeEnabled = TextUtils.equals(o.packageName, packageName);
            if (isEnabled != shouldBeEnabled) {
                try {
                    mOverlayManager.setEnabled(o.packageName, shouldBeEnabled, USER_SYSTEM);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        OverlayInfo[] overlays = getOverlayInfos();

        CharSequence[] pkgs = new CharSequence[overlays.length + 1];
        CharSequence[] labels = new CharSequence[pkgs.length];

        int current = 0;
        pkgs[0] = "";
        labels[0] = mContext.getString(R.string.display_cutout_emulation_none);

        for (int i = 0; i < overlays.length; i++) {
            OverlayInfo o = overlays[i];
            pkgs[i+1] = o.packageName;
            if (o.isEnabled()) {
                current = i+1;
            }
        }
        for (int i = 1; i < pkgs.length; i++) {
            try {
                labels[i] = mPackageManager.getApplicationInfo(pkgs[i].toString(), 0)
                        .loadLabel(mPackageManager);
            } catch (PackageManager.NameNotFoundException e) {
                labels[i] = pkgs[i];
            }
        }

        mPreference.setEntries(labels);
        mPreference.setEntryValues(pkgs);
        mPreference.setValueIndex(current);
        mPreference.setSummary(labels[current]);
    }

    private OverlayInfo[] getOverlayInfos() {
        try {
            @SuppressWarnings("unchecked") List<OverlayInfo> overlayInfos =
                    mOverlayManager.getOverlayInfosForTarget("android", USER_SYSTEM);
            for (int i = overlayInfos.size() - 1; i >= 0; i--) {
                if (!overlayInfos.get(i).packageName.startsWith(EMULATION_OVERLAY_PREFIX)) {
                    overlayInfos.remove(i);
                }
            }
            return overlayInfos.toArray(new OverlayInfo[overlayInfos.size()]);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        setEmulationOverlay("");
        updateState(mPreference);
        mPreference.setEnabled(false);
    }
}
