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

package com.android.settings.applications.credentials;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialProviderInfo;
import android.graphics.drawable.Drawable;
import android.service.autofill.AutofillServiceInfo;
import android.text.TextUtils;
import android.util.IconDrawableFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds combined autofill and credential manager data grouped by package name. Contains backing
 * logic for each row in settings.
 */
public final class CombinedProviderInfo {
    private final List<CredentialProviderInfo> mCredentialProviderInfos;
    private final @Nullable AutofillServiceInfo mAutofillServiceInfo;
    private final boolean mIsDefaultAutofillProvider;
    private final boolean mIsPrimaryCredmanProvider;

    /** Constructs an information instance from both autofill and credential provider. */
    public CombinedProviderInfo(
            @Nullable List<CredentialProviderInfo> cpis,
            @Nullable AutofillServiceInfo asi,
            boolean isDefaultAutofillProvider,
            boolean isPrimaryCredmanProvider) {
        if (cpis == null) {
            mCredentialProviderInfos = new ArrayList<>();
        } else {
            mCredentialProviderInfos = new ArrayList<>(cpis);
        }
        mAutofillServiceInfo = asi;
        mIsDefaultAutofillProvider = isDefaultAutofillProvider;
        mIsPrimaryCredmanProvider = isPrimaryCredmanProvider;
    }

    /** Returns the credential provider info. */
    @NonNull
    public List<CredentialProviderInfo> getCredentialProviderInfos() {
        return mCredentialProviderInfos;
    }

    /** Returns the autofill provider info. */
    @Nullable
    public AutofillServiceInfo getAutofillServiceInfo() {
        return mAutofillServiceInfo;
    }

    /** Returns the application info. */
    public @Nullable ApplicationInfo getApplicationInfo() {
        if (!mCredentialProviderInfos.isEmpty()) {
            return mCredentialProviderInfos.get(0).getServiceInfo().applicationInfo;
        }
        return mAutofillServiceInfo.getServiceInfo().applicationInfo;
    }

    /** Returns the package name. */
    public @Nullable String getPackageName() {
        ApplicationInfo ai = getApplicationInfo();
        if (ai != null) {
            return ai.packageName;
        }

        return null;
    }

    /** Returns the settings activity. */
    public @Nullable String getSettingsActivity() {
        // This logic is not used by the top entry but rather what activity should
        // be launched from the settings screen.
        for (CredentialProviderInfo cpi : mCredentialProviderInfos) {
            final CharSequence settingsActivity = cpi.getSettingsActivity();
            if (!TextUtils.isEmpty(settingsActivity)) {
                return String.valueOf(settingsActivity);
            }
        }

        if (mAutofillServiceInfo != null) {
            final String settingsActivity = mAutofillServiceInfo.getSettingsActivity();
            if (!TextUtils.isEmpty(settingsActivity)) {
                return settingsActivity;
            }
        }

        return null;
    }

    /** Returns the app icon. */
    @Nullable
    public Drawable getAppIcon(@NonNull Context context, int userId) {
        final IconDrawableFactory factory = IconDrawableFactory.newInstance(context);
        final ServiceInfo brandingService = getBrandingService();
        final ApplicationInfo appInfo = getApplicationInfo();

        Drawable icon = null;
        if (brandingService != null && appInfo != null) {
            icon = factory.getBadgedIcon(brandingService, appInfo, userId);
        }

        // If the branding service gave us a icon then use that.
        if (icon != null) {
            return icon;
        }

        // Otherwise fallback to the app icon and then the package name.
        if (appInfo != null) {
            return factory.getBadgedIcon(appInfo, userId);
        }
        return null;
    }

    /** Returns the app name. */
    @Nullable
    public CharSequence getAppName(@NonNull Context context) {
        CharSequence name = "";
        ServiceInfo brandingService = getBrandingService();
        if (brandingService != null) {
            name = brandingService.loadLabel(context.getPackageManager());
        }

        // If the branding service gave us a name then use that.
        if (!TextUtils.isEmpty(name)) {
            return name;
        }

        // Otherwise fallback to the app label and then the package name.
        final ApplicationInfo appInfo = getApplicationInfo();
        if (appInfo != null) {
            name = appInfo.loadLabel(context.getPackageManager());
            if (TextUtils.isEmpty(name)) {
                return appInfo.packageName;
            }
        }
        return "";
    }

    /** Gets the service to use for branding (name, icons). */
    public @Nullable ServiceInfo getBrandingService() {
        // If the app has an autofill service then use that.
        if (mAutofillServiceInfo != null) {
            return mAutofillServiceInfo.getServiceInfo();
        }

        // If there are no credman providers then stop here.
        if (mCredentialProviderInfos.isEmpty()) {
            return null;
        }

        // Build a list of credential providers and sort them by component names
        // alphabetically to ensure we are deterministic when picking the provider.
        Map<String, ServiceInfo> flattenedNamesToServices = new HashMap<>();
        List<String> flattenedNames = new ArrayList<>();
        for (CredentialProviderInfo cpi : mCredentialProviderInfos) {
            final String flattenedName = cpi.getComponentName().flattenToString();
            flattenedNamesToServices.put(flattenedName, cpi.getServiceInfo());
            flattenedNames.add(flattenedName);
        }

        Collections.sort(flattenedNames);
        return flattenedNamesToServices.get(flattenedNames.get(0));
    }

    /** Returns whether the provider is the default autofill provider. */
    public boolean isDefaultAutofillProvider() {
        return mIsDefaultAutofillProvider;
    }

    /** Returns whether the provider is the default credman provider. */
    public boolean isPrimaryCredmanProvider() {
        return mIsPrimaryCredmanProvider;
    }

    /** Returns the settings subtitle. */
    @Nullable
    public String getSettingsSubtitle() {
        List<String> subtitles = new ArrayList<>();
        for (CredentialProviderInfo cpi : mCredentialProviderInfos) {
            // Convert from a CharSequence.
            String subtitle = String.valueOf(cpi.getSettingsSubtitle());
            if (subtitle != null && !TextUtils.isEmpty(subtitle) && !subtitle.equals("null")) {
                subtitles.add(subtitle);
            }
        }

        if (subtitles.size() == 0) {
            return "";
        }

        return String.join(", ", subtitles);
    }

    /** Returns the autofill component name string. */
    @Nullable
    public String getAutofillServiceString() {
        if (mAutofillServiceInfo != null) {
            return mAutofillServiceInfo.getServiceInfo().getComponentName().flattenToString();
        }
        return null;
    }

    /** Returns the provider that gets the top spot. */
    public static @Nullable CombinedProviderInfo getTopProvider(
            List<CombinedProviderInfo> providers) {
        // If there is an autofill provider then it should be the
        // top app provider.
        for (CombinedProviderInfo cpi : providers) {
            if (cpi.isDefaultAutofillProvider()) {
                return cpi;
            }
        }

        // If there is a primary cred man provider then return that.
        for (CombinedProviderInfo cpi : providers) {
            if (cpi.isPrimaryCredmanProvider()) {
                return cpi;
            }
        }

        return null;
    }

    public static List<CombinedProviderInfo> buildMergedList(
            List<AutofillServiceInfo> asiList,
            List<CredentialProviderInfo> cpiList,
            @Nullable String defaultAutofillProvider) {
        ComponentName defaultAutofillProviderComponent =
                (defaultAutofillProvider == null)
                        ? null
                        : ComponentName.unflattenFromString(defaultAutofillProvider);

        // Index the autofill providers by package name.
        Set<String> packageNames = new HashSet<>();
        Map<String, List<AutofillServiceInfo>> autofillServices = new HashMap<>();
        for (AutofillServiceInfo asi : asiList) {
            final String packageName = asi.getServiceInfo().packageName;
            if (!autofillServices.containsKey(packageName)) {
                autofillServices.put(packageName, new ArrayList<>());
            }

            autofillServices.get(packageName).add(asi);
            packageNames.add(packageName);
        }

        // Index the credman providers by package name.
        Map<String, List<CredentialProviderInfo>> credmanServices = new HashMap<>();
        for (CredentialProviderInfo cpi : cpiList) {
            String packageName = cpi.getServiceInfo().packageName;
            if (!credmanServices.containsKey(packageName)) {
                credmanServices.put(packageName, new ArrayList<>());
            }

            credmanServices.get(packageName).add(cpi);
            packageNames.add(packageName);
        }

        // Now go through and build the joint datasets.
        List<CombinedProviderInfo> cmpi = new ArrayList<>();
        for (String packageName : packageNames) {
            List<AutofillServiceInfo> asi =
                    autofillServices.getOrDefault(packageName, new ArrayList<>());
            List<CredentialProviderInfo> cpi =
                    credmanServices.getOrDefault(packageName, new ArrayList<>());

            // If there are multiple autofill services then pick the first one.
            AutofillServiceInfo selectedAsi = null;
            if (asi != null && !asi.isEmpty()) {
                selectedAsi = asi.get(0);
            }

            // Check if we are the default autofill provider.
            boolean isDefaultAutofillProvider = false;
            if (defaultAutofillProviderComponent != null
                    && defaultAutofillProviderComponent.getPackageName().equals(packageName)) {
                isDefaultAutofillProvider = true;
            }

            // Check if we have any enabled cred man services.
            boolean isPrimaryCredmanProvider = false;
            if (cpi != null && !cpi.isEmpty()) {
                isPrimaryCredmanProvider = cpi.get(0).isPrimary();
            }

            cmpi.add(
                    new CombinedProviderInfo(
                            cpi, selectedAsi, isDefaultAutofillProvider, isPrimaryCredmanProvider));
        }

        return cmpi;
    }
}
