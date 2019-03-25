/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Preference controller for location footer preference category
 */
public class LocationFooterPreferenceController extends LocationBasePreferenceController
        implements LifecycleObserver, OnPause {
    private static final String TAG = "LocationFooter";
    private static final String KEY_LOCATION_FOOTER = "location_footer";
    private static final Intent INJECT_INTENT =
            new Intent(LocationManager.SETTINGS_FOOTER_DISPLAYED_ACTION);
    private final Context mContext;
    private final PackageManager mPackageManager;
    private Collection<ComponentName> mFooterInjectors;

    public LocationFooterPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        mFooterInjectors = new ArrayList<>();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCATION_FOOTER;
    }

    /**
     * Insert footer preferences. Send a {@link LocationManager#SETTINGS_FOOTER_DISPLAYED_ACTION}
     * broadcast to receivers who have injected a footer
     */
    @Override
    public void updateState(Preference preference) {
        PreferenceCategory category = (PreferenceCategory) preference;
        category.removeAll();
        mFooterInjectors.clear();
        Collection<FooterData> footerData = getFooterData();
        for (FooterData data : footerData) {
            // Generate a footer preference with the given text
            FooterPreference footerPreference = new FooterPreference(preference.getContext());
            String footerString;
            try {
                footerString =
                        mPackageManager
                                .getResourcesForApplication(data.applicationInfo)
                                .getString(data.footerStringRes);
            } catch (NameNotFoundException exception) {
                Log.w(
                        TAG,
                        "Resources not found for application "
                                + data.applicationInfo.packageName);
                continue;
            }
            footerPreference.setTitle(footerString);
            // Inject the footer
            category.addPreference(footerPreference);
            // Send broadcast to the injector announcing a footer has been injected
            sendBroadcastFooterDisplayed(data.componentName);
            mFooterInjectors.add(data.componentName);
        }
    }

    /**
     * Do nothing on location mode changes.
     */
    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {}

    /**
     * Location footer preference group should be displayed if there is at least one footer to
     * inject.
     */
    @Override
    public boolean isAvailable() {
        return !getFooterData().isEmpty();
    }

    /**
     * Send a {@link LocationManager#SETTINGS_FOOTER_REMOVED_ACTION} broadcast to footer injectors
     * when LocationFragment is on pause
     */
    @Override
    public void onPause() {
        // Send broadcast to the footer injectors. Notify them the footer is not visible.
        for (ComponentName componentName : mFooterInjectors) {
            final Intent intent = new Intent(LocationManager.SETTINGS_FOOTER_REMOVED_ACTION);
            intent.setComponent(componentName);
            mContext.sendBroadcast(intent);
        }
    }

    /**
     * Send a {@link LocationManager#SETTINGS_FOOTER_DISPLAYED_ACTION} broadcast to a footer
     * injector.
     */
    @VisibleForTesting
    void sendBroadcastFooterDisplayed(ComponentName componentName) {
        Intent intent = new Intent(LocationManager.SETTINGS_FOOTER_DISPLAYED_ACTION);
        intent.setComponent(componentName);
        mContext.sendBroadcast(intent);
    }

    /**
     * Return a list of strings with text provided by ACTION_INJECT_FOOTER broadcast receivers.
     */
    private Collection<FooterData> getFooterData() {
        // Fetch footer text from system apps
        final List<ResolveInfo> resolveInfos =
                mPackageManager.queryBroadcastReceivers(
                        INJECT_INTENT, PackageManager.GET_META_DATA);
        if (resolveInfos == null) {
            Log.e(TAG, "Unable to resolve intent " + INJECT_INTENT);
            return Collections.emptyList();
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Found broadcast receivers: " + resolveInfos);
        }

        final Collection<FooterData> footerDataList = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            final ApplicationInfo appInfo = activityInfo.applicationInfo;

            // If a non-system app tries to inject footer, ignore it
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                Log.w(TAG, "Ignoring attempt to inject footer from app not in system image: "
                        + resolveInfo);
                continue;
            }

            // Get the footer text resource id from broadcast receiver's metadata
            if (activityInfo.metaData == null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "No METADATA in broadcast receiver " + activityInfo.name);
                }
                continue;
            }

            final int footerTextRes =
                    activityInfo.metaData.getInt(LocationManager.METADATA_SETTINGS_FOOTER_STRING);
            if (footerTextRes == 0) {
                Log.w(
                        TAG,
                        "No mapping of integer exists for "
                                + LocationManager.METADATA_SETTINGS_FOOTER_STRING);
                continue;
            }
            footerDataList.add(
                    new FooterData(
                            footerTextRes,
                            appInfo,
                            new ComponentName(activityInfo.packageName, activityInfo.name)));
        }
        return footerDataList;
    }

    /**
     * Contains information related to a footer.
     */
    private static class FooterData {

        // The string resource of the footer
        final int footerStringRes;

        // Application info of receiver injecting this footer
        final ApplicationInfo applicationInfo;

        // The component that injected the footer. It must be a receiver of broadcast
        // LocationManager.SETTINGS_FOOTER_DISPLAYED_ACTION
        final ComponentName componentName;

        FooterData(int footerRes, ApplicationInfo appInfo, ComponentName componentName) {
            this.footerStringRes = footerRes;
            this.applicationInfo = appInfo;
            this.componentName = componentName;
        }
    }
}
