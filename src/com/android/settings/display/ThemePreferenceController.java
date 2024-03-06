/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;


import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ThemePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_THEME = "theme";

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final IOverlayManager mOverlayService;
    private final PackageManager mPackageManager;

    public ThemePreferenceController(Context context) {
        this(context, IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE)));
    }

    @VisibleForTesting
    ThemePreferenceController(Context context, IOverlayManager overlayManager) {
        super(context);
        mOverlayService = overlayManager;
        mPackageManager = context.getPackageManager();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_THEME;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_THEME.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_THEME);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        ListPreference pref = (ListPreference) preference;
        String[] pkgs = getAvailableThemes(false /* currentThemeOnly */);
        CharSequence[] labels = new CharSequence[pkgs.length];
        for (int i = 0; i < pkgs.length; i++) {
            try {
                labels[i] = mPackageManager.getApplicationInfo(pkgs[i], 0)
                        .loadLabel(mPackageManager);
            } catch (NameNotFoundException e) {
                labels[i] = pkgs[i];
            }
        }
        pref.setEntries(labels);
        pref.setEntryValues(pkgs);
        String theme = getCurrentTheme();
        CharSequence themeLabel = null;

        for (int i = 0; i < pkgs.length; i++) {
            if (TextUtils.equals(pkgs[i], theme)) {
                themeLabel = labels[i];
                break;
            }
        }

        if (TextUtils.isEmpty(themeLabel)) {
            themeLabel = mContext.getString(R.string.default_theme);
        }

        pref.setSummary(themeLabel);
        pref.setValue(theme);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String current = getCurrentTheme();
        if (Objects.equals(newValue, current)) {
            return true;
        }
        try {
            mOverlayService.setEnabledExclusiveInCategory((String) newValue, UserHandle.myUserId());
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
        return true;
    }

    private boolean isTheme(OverlayInfo oi) {
        if (!OverlayInfo.CATEGORY_THEME.equals(oi.category)) {
            return false;
        }
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(oi.packageName, 0);
            return pi != null && !pi.isStaticOverlayPackage();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        if (mOverlayService == null) return false;
        String[] themes = getAvailableThemes(false /* currentThemeOnly */);
        return themes != null && themes.length > 1;
    }


    @VisibleForTesting
    String getCurrentTheme() {
        String[] themePackages = getAvailableThemes(true /* currentThemeOnly */);
        return themePackages.length < 1 ? null : themePackages[0];
    }

    @VisibleForTesting
    String[] getAvailableThemes(boolean currentThemeOnly) {
        List<OverlayInfo> infos;
        List<String> pkgs;
        try {
            infos = mOverlayService.getOverlayInfosForTarget("android", UserHandle.myUserId());
            pkgs = new ArrayList<>(infos.size());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (isTheme(infos.get(i))) {
                    if (infos.get(i).isEnabled() && currentThemeOnly) {
                        return new String[] {infos.get(i).packageName};
                    } else {
                        pkgs.add(infos.get(i).packageName);
                    }
                }
            }
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }

        // Current enabled theme is not found.
        if (currentThemeOnly) {
            return new String[0];
        }
        return pkgs.toArray(new String[pkgs.size()]);
    }
}
