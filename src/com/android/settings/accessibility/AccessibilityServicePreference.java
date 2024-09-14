/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Preference item for showing an accessibility service in a preference list
 */
public class AccessibilityServicePreference extends RestrictedPreference {
    private static final String LOG_TAG = AccessibilityServicePreference.class.getSimpleName();
    // Index of the first preference in a preference category.
    private static final int FIRST_PREFERENCE_IN_CATEGORY_INDEX = -1;
    private final PackageManager mPm;
    private final AccessibilityServiceInfo mA11yServiceInfo;
    private final ComponentName mComponentName;
    private final boolean mServiceEnabled;
    private final ListenableFuture mExtraArgumentsFuture;

    public AccessibilityServicePreference(Context context, String packageName, int uid,
            AccessibilityServiceInfo a11yServiceInfo, boolean serviceEnabled) {
        super(context, packageName, uid);
        mPm = context.getPackageManager();
        mA11yServiceInfo = a11yServiceInfo;
        mServiceEnabled = serviceEnabled;
        mComponentName = new ComponentName(packageName,
                mA11yServiceInfo.getResolveInfo().serviceInfo.name);
        // setup basic info for a preference
        setKey(mComponentName.flattenToString());
        setTitle(mA11yServiceInfo.getResolveInfo().loadLabel(mPm));
        setSummary(AccessibilitySettings.getServiceSummary(
                getContext(), mA11yServiceInfo, mServiceEnabled));
        setFragment(RestrictedPreferenceHelper.getAccessibilityServiceFragmentTypeName(
                mA11yServiceInfo));
        setIconSize(ICON_SIZE_MEDIUM);
        setIconSpaceReserved(true);
        setPersistent(false); // Disable SharedPreferences.
        setOrder(FIRST_PREFERENCE_IN_CATEGORY_INDEX);

        // kick off image loading tasks
        ThreadUtils.postOnBackgroundThread(() -> {
            final Drawable icon = getA11yServiceIcon();
            ThreadUtils.getUiThreadHandler().post(() -> this.setIcon(icon));
        });

        final Bundle extras = getExtras();
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, mComponentName);

        mExtraArgumentsFuture = ThreadUtils.postOnBackgroundThread(this::setupDataForOpenFragment);
    }

    @Override
    public void performClick() {
        try {
            mExtraArgumentsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(LOG_TAG,
                    "Unable to finish grabbing necessary arguments to open the fragment: "
                            + "componentName: " + mComponentName);
        }
        super.performClick();
    }

    @NonNull
    public ComponentName getComponentName() {
        return mComponentName;
    }

    private Drawable getA11yServiceIcon() {
        ResolveInfo resolveInfo = mA11yServiceInfo.getResolveInfo();
        Drawable serviceIcon;
        if (resolveInfo.getIconResource() == 0) {
            serviceIcon = ContextCompat.getDrawable(getContext(),
                    R.drawable.ic_accessibility_generic);
        } else {
            serviceIcon = resolveInfo.loadIcon(mPm);
        }
        return Utils.getAdaptiveIcon(getContext(), serviceIcon, Color.WHITE);
    }

    private void setupDataForOpenFragment() {
        final String prefKey = getKey();
        final int imageRes = mA11yServiceInfo.getAnimatedImageRes();
        final CharSequence intro = mA11yServiceInfo.loadIntro(mPm);
        final CharSequence description = AccessibilitySettings.getServiceDescription(
                getContext(), mA11yServiceInfo, mServiceEnabled);
        final String htmlDescription = mA11yServiceInfo.loadHtmlDescription(mPm);
        final String settingsClassName = mA11yServiceInfo.getSettingsActivityName();
        final String tileServiceClassName = mA11yServiceInfo.getTileServiceName();
        final ResolveInfo resolveInfo = mA11yServiceInfo.getResolveInfo();
        final int metricsCategory = FeatureFactory.getFeatureFactory()
                .getAccessibilityMetricsFeatureProvider()
                .getDownloadedFeatureMetricsCategory(mComponentName);
        ThreadUtils.getUiThreadHandler().post(() -> {
            RestrictedPreferenceHelper.putBasicExtras(
                    this, prefKey, getTitle(), intro, description, imageRes,
                    htmlDescription, mComponentName, metricsCategory);
            RestrictedPreferenceHelper.putServiceExtras(this, resolveInfo, mServiceEnabled);
            RestrictedPreferenceHelper.putSettingsExtras(this, getPackageName(), settingsClassName);
            RestrictedPreferenceHelper.putTileServiceExtras(
                    this, getPackageName(), tileServiceClassName);
        });
    }
}
