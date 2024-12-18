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

package com.android.settings.notification.modes;

import android.app.Flags;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.service.notification.ConditionProviderService;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.Utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

class ZenModesListAddModePreferenceController extends BasePreferenceController {
    private final ZenServiceListing mServiceListing;
    private final OnAddModeListener mOnAddModeListener;

    private final ConfigurationActivityHelper mConfigurationActivityHelper;
    private final NotificationManager mNotificationManager;
    private final PackageManager mPackageManager;
    private final Function<ApplicationInfo, Drawable> mAppIconRetriever;
    private final ListeningExecutorService mBackgroundExecutor;
    private final Executor mUiThreadExecutor;

    record ModeType(String name, Drawable icon, @Nullable String summary,
                    @Nullable Intent creationActivityIntent) { }

    interface OnAddModeListener {
        void onAvailableModeTypesForAdd(List<ModeType> types);
    }

    ZenModesListAddModePreferenceController(Context context, String key,
            OnAddModeListener onAddModeListener) {
        this(context, key, onAddModeListener, new ZenServiceListing(context),
                new ConfigurationActivityHelper(context.getPackageManager()),
                context.getSystemService(NotificationManager.class), context.getPackageManager(),
                applicationInfo -> Utils.getBadgedIcon(context, applicationInfo),
                Executors.newCachedThreadPool(), context.getMainExecutor());
    }

    @VisibleForTesting
    ZenModesListAddModePreferenceController(Context context, String key,
            OnAddModeListener onAddModeListener, ZenServiceListing serviceListing,
            ConfigurationActivityHelper configurationActivityHelper,
            NotificationManager notificationManager, PackageManager packageManager,
            Function<ApplicationInfo, Drawable> appIconRetriever,
            ExecutorService backgroundExecutor, Executor uiThreadExecutor) {
        super(context, key);
        mOnAddModeListener = onAddModeListener;
        mServiceListing = serviceListing;
        mConfigurationActivityHelper = configurationActivityHelper;
        mNotificationManager = notificationManager;
        mPackageManager = packageManager;
        mAppIconRetriever = appIconRetriever;
        mBackgroundExecutor = MoreExecutors.listeningDecorator(backgroundExecutor);
        mUiThreadExecutor = uiThreadExecutor;
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.modesUi() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setOnPreferenceClickListener(pref -> {
            onClickAddMode();
            return true;
        });
    }

    @VisibleForTesting
    void onClickAddMode() {
        FutureUtil.whenDone(
                mBackgroundExecutor.submit(this::getModeProviders),
                mOnAddModeListener::onAvailableModeTypesForAdd,
                mUiThreadExecutor);
    }

    @WorkerThread
    private ImmutableList<ModeType> getModeProviders() {
        ImmutableSet<ComponentInfo> approvedComponents = mServiceListing.loadApprovedComponents();

        ArrayList<ModeType> appProvidedModes = new ArrayList<>();
        for (ComponentInfo ci: approvedComponents) {
            ModeType modeType = getValidNewModeTypeFromComponent(ci);
            if (modeType != null) {
                appProvidedModes.add(modeType);
            }
        }

        return ImmutableList.<ModeType>builder()
                .add(new ModeType(
                        mContext.getString(R.string.zen_mode_new_option_custom),
                        mContext.getDrawable(R.drawable.ic_zen_mode_new_option_custom),
                        null, null))
                .addAll(appProvidedModes.stream()
                        .sorted(Comparator.comparing(ModeType::name))
                        .toList())
                .build();
    }

    /**
     * Returns a {@link ModeType} object corresponding to the approved {@link ComponentInfo} that
     * specifies a creatable rule, if such a mode can actually be created (has an associated and
     * enabled configuration activity, has not exceeded the rule instance limit, etc). Otherwise,
     * returns {@code null}.
     */
    @WorkerThread
    @Nullable
    private ModeType getValidNewModeTypeFromComponent(ComponentInfo ci) {
        if (ci.metaData == null) {
            return null;
        }

        String ruleType = (ci instanceof ServiceInfo)
                ? ci.metaData.getString(ConditionProviderService.META_DATA_RULE_TYPE)
                : ci.metaData.getString(NotificationManager.META_DATA_AUTOMATIC_RULE_TYPE);
        if (ruleType == null || ruleType.trim().isEmpty()) {
            return null;
        }

        int ruleInstanceLimit = (ci instanceof ServiceInfo)
                ? ci.metaData.getInt(ConditionProviderService.META_DATA_RULE_INSTANCE_LIMIT, -1)
                : ci.metaData.getInt(NotificationManager.META_DATA_RULE_INSTANCE_LIMIT, -1);
        if (ruleInstanceLimit > 0 && mNotificationManager.getRuleInstanceCount(
                ci.getComponentName()) >= ruleInstanceLimit) {
            return null; // Would exceed instance limit.
        }

        ComponentName configurationActivity =
                mConfigurationActivityHelper.getConfigurationActivityFromApprovedComponent(ci);
        if (configurationActivity == null) {
            return null;
        }

        String appName = ci.applicationInfo.loadLabel(mPackageManager).toString();
        Drawable appIcon = mAppIconRetriever.apply(ci.applicationInfo);
        Intent configActivityIntent = new Intent().setComponent(configurationActivity);
        return new ModeType(ruleType, appIcon, appName, configActivityIntent);
    }
}
