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
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Preference controller to allow users to choose an overlay from a list for a given category.
 * The chosen overlay is enabled exclusively within its category. A default option is also
 * exposed that disables all overlays in the given category.
 */
public class OverlayCategoryPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String TAG = "OverlayCategoryPC";
    @VisibleForTesting
    static final String PACKAGE_DEVICE_DEFAULT = "package_device_default";
    private static final String OVERLAY_TARGET_PACKAGE = "android";
    private static final Comparator<OverlayInfo> OVERLAY_INFO_COMPARATOR =
            Comparator.comparingInt(a -> a.priority);
    private final IOverlayManager mOverlayManager;
    private final boolean mAvailable;
    private final String mCategory;
    private final PackageManager mPackageManager;

    private ListPreference mPreference;

    @VisibleForTesting
    OverlayCategoryPreferenceController(Context context, PackageManager packageManager,
            IOverlayManager overlayManager, String category) {
        super(context);
        mOverlayManager = overlayManager;
        mPackageManager = packageManager;
        mCategory = category;
        mAvailable = overlayManager != null && !getOverlayInfos().isEmpty();
    }

    public OverlayCategoryPreferenceController(Context context, String category) {
        this(context, context.getPackageManager(), IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE)), category);
    }

    @Override
    public boolean isAvailable() {
        return mAvailable;
    }

    @Override
    public String getPreferenceKey() {
        return mCategory;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        setPreference(screen.findPreference(getPreferenceKey()));
    }

    @VisibleForTesting
    void setPreference(ListPreference preference) {
        mPreference = preference;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return setOverlay((String) newValue);
    }

    private boolean setOverlay(String packageName) {
        final String currentPackageName = getOverlayInfos().stream()
                .filter(info -> info.isEnabled())
                .map(info -> info.packageName)
                .findFirst()
                .orElse(null);

        if (PACKAGE_DEVICE_DEFAULT.equals(packageName) && TextUtils.isEmpty(currentPackageName)
                || TextUtils.equals(packageName, currentPackageName)) {
            // Already set.
            return true;
        }

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    if (PACKAGE_DEVICE_DEFAULT.equals(packageName)) {
                        return mOverlayManager.setEnabled(currentPackageName, false, USER_SYSTEM);
                    } else {
                        return mOverlayManager.setEnabledExclusiveInCategory(packageName,
                                USER_SYSTEM);
                    }
                } catch (RemoteException re) {
                    Log.w(TAG, "Error enabling overlay.", re);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                updateState(mPreference);
                if (!success) {
                    Toast.makeText(
                            mContext, R.string.overlay_toast_failed_to_apply, Toast.LENGTH_LONG)
                            .show();
                }
            }
        }.execute();

        return true; // Assume success; toast on failure.
    }

    @Override
    public void updateState(Preference preference) {
        final List<String> pkgs = new ArrayList<>();
        final List<String> labels = new ArrayList<>();

        String selectedPkg = PACKAGE_DEVICE_DEFAULT;
        String selectedLabel = mContext.getString(R.string.overlay_option_device_default);

        // Add the default package / label before all of the overlays
        pkgs.add(selectedPkg);
        labels.add(selectedLabel);

        for (OverlayInfo overlayInfo : getOverlayInfos()) {
            pkgs.add(overlayInfo.packageName);
            try {
                labels.add(mPackageManager.getApplicationInfo(overlayInfo.packageName, 0)
                        .loadLabel(mPackageManager).toString());
            } catch (PackageManager.NameNotFoundException e) {
                labels.add(overlayInfo.packageName);
            }
            if (overlayInfo.isEnabled()) {
                selectedPkg = pkgs.get(pkgs.size() - 1);
                selectedLabel = labels.get(labels.size() - 1);
            }
        }

        mPreference.setEntries(labels.toArray(new String[labels.size()]));
        mPreference.setEntryValues(pkgs.toArray(new String[pkgs.size()]));
        mPreference.setValue(selectedPkg);
        mPreference.setSummary(selectedLabel);
    }

    private List<OverlayInfo> getOverlayInfos() {
        final List<OverlayInfo> filteredInfos = new ArrayList<>();
        try {
            List<OverlayInfo> overlayInfos = mOverlayManager
                    .getOverlayInfosForTarget(OVERLAY_TARGET_PACKAGE, USER_SYSTEM);
            for (OverlayInfo overlayInfo : overlayInfos) {
                if (mCategory.equals(overlayInfo.category)) {
                    filteredInfos.add(overlayInfo);
                }
            }
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
        filteredInfos.sort(OVERLAY_INFO_COMPARATOR);
        return filteredInfos;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        // TODO b/133222035: remove these developer settings when the
        // Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES setting is used
        setOverlay(PACKAGE_DEVICE_DEFAULT);
        updateState(mPreference);
    }

}
