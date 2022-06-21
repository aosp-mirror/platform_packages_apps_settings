/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AggregateBatteryConsumer;
import android.os.BatteryConsumer;
import android.os.BatteryUsageStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Controller that update the battery header view
 */
public class BatteryAppListPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnDestroy {
    private static final String TAG = "BatteryAppListPreferenceController";
    @VisibleForTesting
    static final boolean USE_FAKE_DATA = false;
    private static final int MAX_ITEMS_TO_LIST = USE_FAKE_DATA ? 30 : 20;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final String MEDIASERVER_PACKAGE_NAME = "mediaserver";
    private static final String NOT_AVAILABLE = "not_available";

    @VisibleForTesting
    PreferenceGroup mAppListGroup;
    private BatteryUsageStats mBatteryUsageStats;
    private ArrayMap<String, Preference> mPreferenceCache;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private final UserManager mUserManager;
    private final PackageManager mPackageManager;
    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private final Set<CharSequence> mNotAllowShowSummaryPackages;
    private final String mPreferenceKey;

    private Context mPrefContext;

    /**
     * Battery attribution list configuration.
     */
    public interface Config {
        /**
         * Returns true if the attribution list should be shown.
         */
        boolean shouldShowBatteryAttributionList(Context context);
    }

    @VisibleForTesting
    static Config sConfig = new Config() {
        @Override
        public boolean shouldShowBatteryAttributionList(Context context) {
            if (USE_FAKE_DATA) {
                return true;
            }

            PowerProfile powerProfile = new PowerProfile(context);
            // Cheap hack to try to figure out if the power_profile.xml was populated.
            final double averagePowerForOrdinal = powerProfile.getAveragePowerForOrdinal(
                    PowerProfile.POWER_GROUP_DISPLAY_SCREEN_FULL, 0);
            final boolean shouldShowBatteryAttributionList =
                    averagePowerForOrdinal >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP;
            if (!shouldShowBatteryAttributionList) {
                Log.w(TAG, "shouldShowBatteryAttributionList(): " + averagePowerForOrdinal);
            }
            return shouldShowBatteryAttributionList;
        }
    };

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp = mAppListGroup.findPreference(entry.getKey());
                    if (pgp != null) {
                        final int userId = UserHandle.getUserId(entry.getUid());
                        final UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(mUserManager.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.mName);
                        if (entry.isAppEntry()) {
                            pgp.setContentDescription(entry.mName);
                        }
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = mActivity;
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public BatteryAppListPreferenceController(Context context, String preferenceKey,
            Lifecycle lifecycle, SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }

        mPreferenceKey = preferenceKey;
        mBatteryUtils = BatteryUtils.getInstance(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPackageManager = context.getPackageManager();
        mActivity = activity;
        mFragment = fragment;
        mNotAllowShowSummaryPackages = Set.of(
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideApplicationSummary(context));
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
    }

    @Override
    public void onDestroy() {
        if (mActivity.isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mAppListGroup = screen.findPreference(mPreferenceKey);
        mAppListGroup.setTitle(mPrefContext.getString(R.string.power_usage_list_summary));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference instanceof PowerGaugePreference) {
            PowerGaugePreference pgp = (PowerGaugePreference) preference;
            BatteryEntry entry = pgp.getInfo();
            AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity,
                    mFragment, entry, pgp.getPercent(), /*isValidToShowSummary=*/ true);
            return true;
        }
        return false;
    }

    /**
     * Refreshes the list of battery consumers using the supplied BatteryUsageStats.
     */
    public void refreshAppListGroup(BatteryUsageStats batteryUsageStats, boolean showAllApps) {
        if (!isAvailable()) {
            return;
        }

        mBatteryUsageStats = USE_FAKE_DATA ? getFakeStats() : batteryUsageStats;
        mAppListGroup.setTitle(R.string.power_usage_list_summary);

        boolean addedSome = false;

        cacheRemoveAllPrefs(mAppListGroup);
        mAppListGroup.setOrderingAsAdded(false);

        if (sConfig.shouldShowBatteryAttributionList(mContext)) {
            final int dischargePercentage = getDischargePercentage(batteryUsageStats);
            final List<BatteryEntry> usageList =
                    getCoalescedUsageList(showAllApps, /*loadDataInBackground=*/ true);
            final double totalPower = batteryUsageStats.getConsumedPower();
            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatteryEntry entry = usageList.get(i);

                final double percentOfTotal = mBatteryUtils.calculateBatteryPercent(
                        entry.getConsumedPower(), totalPower, dischargePercentage);

                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }

                final int uid = entry.getUid();
                final UserHandle userHandle = new UserHandle(UserHandle.getUserId(uid));
                final Drawable badgedIcon = mUserManager.getBadgedIconForUser(entry.getIcon(),
                        userHandle);
                final CharSequence contentDescription = mUserManager.getBadgedLabelForUser(
                        entry.getLabel(), userHandle);

                final String key = entry.getKey();
                PowerGaugePreference pref = (PowerGaugePreference) getCachedPreference(key);
                if (pref == null) {
                    pref = new PowerGaugePreference(mPrefContext, badgedIcon,
                            contentDescription, entry);
                    pref.setKey(key);
                }
                entry.mPercent = percentOfTotal;
                pref.setTitle(entry.getLabel());
                pref.setOrder(i + 1);
                pref.setPercent(percentOfTotal);
                pref.shouldShowAnomalyIcon(false);
                pref.setEnabled(uid != BatteryUtils.UID_TETHERING
                        && uid != BatteryUtils.UID_REMOVED_APPS);
                setUsageSummary(pref, entry);
                addedSome = true;
                mAppListGroup.addPreference(pref);
                if (mAppListGroup.getPreferenceCount() - getCachedCount()
                        > (MAX_ITEMS_TO_LIST + 1)) {
                    break;
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }
        removeCachedPrefs(mAppListGroup);

        BatteryEntry.startRequestQueue();
    }

    /**
     * Gets the BatteryEntry list by using the supplied BatteryUsageStats.
     */
    public List<BatteryEntry> getBatteryEntryList(
            BatteryUsageStats batteryUsageStats, boolean showAllApps) {
        mBatteryUsageStats = USE_FAKE_DATA ? getFakeStats() : batteryUsageStats;
        if (!sConfig.shouldShowBatteryAttributionList(mContext)) {
            return null;
        }
        final int dischargePercentage = getDischargePercentage(batteryUsageStats);
        final List<BatteryEntry> usageList =
                getCoalescedUsageList(showAllApps, /*loadDataInBackground=*/ false);
        final double totalPower = batteryUsageStats.getConsumedPower();
        for (int i = 0; i < usageList.size(); i++) {
            final BatteryEntry entry = usageList.get(i);
            final double percentOfTotal = mBatteryUtils.calculateBatteryPercent(
                    entry.getConsumedPower(), totalPower, dischargePercentage);
            entry.mPercent = percentOfTotal;
        }
        return usageList;
    }

    private int getDischargePercentage(BatteryUsageStats batteryUsageStats) {
        int dischargePercentage = batteryUsageStats.getDischargePercentage();
        if (dischargePercentage < 0) {
            dischargePercentage = 0;
        }
        return dischargePercentage;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     *
     * @return A sorted list of apps using power.
     */
    private List<BatteryEntry> getCoalescedUsageList(
            boolean showAllApps, boolean loadDataInBackground) {
        final SparseArray<BatteryEntry> batteryEntryList = new SparseArray<>();

        final ArrayList<BatteryEntry> results = new ArrayList<>();
        final List<UidBatteryConsumer> uidBatteryConsumers =
                mBatteryUsageStats.getUidBatteryConsumers();

        // Sort to have all apps with "real" UIDs first, followed by apps that are supposed
        // to be combined with the real ones.
        uidBatteryConsumers.sort(Comparator.comparingInt(
                consumer -> consumer.getUid() == getRealUid(consumer) ? 0 : 1));

        for (int i = 0, size = uidBatteryConsumers.size(); i < size; i++) {
            final UidBatteryConsumer consumer = uidBatteryConsumers.get(i);
            final int uid = getRealUid(consumer);

            final String[] packages = mPackageManager.getPackagesForUid(uid);
            if (mBatteryUtils.shouldHideUidBatteryConsumerUnconditionally(consumer, packages)) {
                continue;
            }

            final boolean isHidden = mBatteryUtils.shouldHideUidBatteryConsumer(consumer, packages);
            if (isHidden && !showAllApps) {
                continue;
            }

            final int index = batteryEntryList.indexOfKey(uid);
            if (index < 0) {
                // New entry.
                batteryEntryList.put(uid, new BatteryEntry(mContext, mHandler, mUserManager,
                        consumer, isHidden, uid, packages, null, loadDataInBackground));
            } else {
                // Combine BatterySippers if we already have one with this UID.
                final BatteryEntry existingSipper = batteryEntryList.valueAt(index);
                existingSipper.add(consumer);
            }
        }

        final BatteryConsumer deviceConsumer = mBatteryUsageStats.getAggregateBatteryConsumer(
                BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_DEVICE);
        final BatteryConsumer appsConsumer = mBatteryUsageStats.getAggregateBatteryConsumer(
                BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_ALL_APPS);

        for (int componentId = 0; componentId < BatteryConsumer.POWER_COMPONENT_COUNT;
                componentId++) {
            if (!showAllApps
                    && mBatteryUtils.shouldHideDevicePowerComponent(deviceConsumer, componentId)) {
                continue;
            }

            results.add(new BatteryEntry(mContext, componentId,
                    deviceConsumer.getConsumedPower(componentId),
                    appsConsumer.getConsumedPower(componentId),
                    deviceConsumer.getUsageDurationMillis(componentId)));
        }

        for (int componentId = BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID;
                componentId < BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID
                        + deviceConsumer.getCustomPowerComponentCount();
                componentId++) {
            if (!showAllApps) {
                continue;
            }

            results.add(new BatteryEntry(mContext, componentId,
                    deviceConsumer.getCustomPowerComponentName(componentId),
                    deviceConsumer.getConsumedPowerForCustomComponent(componentId),
                    appsConsumer.getConsumedPowerForCustomComponent(componentId)));
        }

        if (showAllApps) {
            final List<UserBatteryConsumer> userBatteryConsumers =
                    mBatteryUsageStats.getUserBatteryConsumers();
            for (int i = 0, size = userBatteryConsumers.size(); i < size; i++) {
                final UserBatteryConsumer consumer = userBatteryConsumers.get(i);
                results.add(new BatteryEntry(mContext, mHandler, mUserManager,
                        consumer, /* isHidden */ true, Process.INVALID_UID, null, null,
                        loadDataInBackground));
            }
        }

        final int numUidSippers = batteryEntryList.size();

        for (int i = 0; i < numUidSippers; i++) {
            results.add(batteryEntryList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        results.sort(BatteryEntry.COMPARATOR);
        return results;
    }

    private int getRealUid(UidBatteryConsumer consumer) {
        int realUid = consumer.getUid();

        // Check if this UID is a shared GID. If so, we combine it with the OWNER's
        // actual app UID.
        if (isSharedGid(consumer.getUid())) {
            realUid = UserHandle.getUid(UserHandle.USER_SYSTEM,
                    UserHandle.getAppIdFromSharedAppGid(consumer.getUid()));
        }

        // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
        if (isSystemUid(realUid)
                && !MEDIASERVER_PACKAGE_NAME.equals(consumer.getPackageWithHighestDrain())) {
            // Use the system UID for all UIDs running in their own sandbox that
            // are not apps. We exclude mediaserver because we already are expected to
            // report that as a separate item.
            realUid = Process.SYSTEM_UID;
        }
        return realUid;
    }

    @VisibleForTesting
    void setUsageSummary(Preference preference, BatteryEntry entry) {
        if (BatteryEntry.isSystemUid(entry.getUid())) {
            return;
        }
        String packageName = entry.getDefaultPackageName();
        if (packageName != null
                && mNotAllowShowSummaryPackages != null
                && mNotAllowShowSummaryPackages.contains(packageName)) {
            return;
        }
        // Only show summary when usage time is longer than one minute
        final long usageTimeMs = entry.getTimeInForegroundMs();
        if (shouldShowSummary(entry) && usageTimeMs >= DateUtils.MINUTE_IN_MILLIS) {
            final CharSequence timeSequence =
                    StringUtil.formatElapsedTime(mContext, usageTimeMs, false, false);
            preference.setSummary(
                    entry.isHidden()
                            ? timeSequence
                            : TextUtils.expandTemplate(mContext.getText(R.string.battery_used_for),
                                    timeSequence));
        }
    }

    private void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<>();
        final int n = group.getPreferenceCount();
        for (int i = 0; i < n; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    private boolean shouldShowSummary(BatteryEntry entry) {
        final CharSequence[] allowlistPackages =
                FeatureFactory.getFactory(mContext)
                        .getPowerUsageFeatureProvider(mContext)
                        .getHideApplicationSummary(mContext);
        final String target = entry.getDefaultPackageName();

        for (CharSequence packageName : allowlistPackages) {
            if (TextUtils.equals(target, packageName)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        final int appUid = UserHandle.getAppId(uid);
        return appUid >= Process.SYSTEM_UID && appUid < Process.FIRST_APPLICATION_UID;
    }

    private BatteryUsageStats getFakeStats() {
        BatteryUsageStats.Builder builder = new BatteryUsageStats.Builder(new String[0])
                .setDischargePercentage(100);

        float use = 500;
        final AggregateBatteryConsumer.Builder appsBatteryConsumerBuilder =
                builder.getAggregateBatteryConsumerBuilder(
                        BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_ALL_APPS);
        final AggregateBatteryConsumer.Builder deviceBatteryConsumerBuilder =
                builder.getAggregateBatteryConsumerBuilder(
                        BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_DEVICE);
        for (@BatteryConsumer.PowerComponent int componentId : new int[]{
                BatteryConsumer.POWER_COMPONENT_AMBIENT_DISPLAY,
                BatteryConsumer.POWER_COMPONENT_BLUETOOTH,
                BatteryConsumer.POWER_COMPONENT_CAMERA,
                BatteryConsumer.POWER_COMPONENT_FLASHLIGHT,
                BatteryConsumer.POWER_COMPONENT_IDLE,
                BatteryConsumer.POWER_COMPONENT_MEMORY,
                BatteryConsumer.POWER_COMPONENT_MOBILE_RADIO,
                BatteryConsumer.POWER_COMPONENT_PHONE,
                BatteryConsumer.POWER_COMPONENT_SCREEN,
                BatteryConsumer.POWER_COMPONENT_WIFI,
        }) {
            appsBatteryConsumerBuilder.setConsumedPower(componentId, use);
            deviceBatteryConsumerBuilder.setConsumedPower(componentId, use * 2);
            use += 5;
        }

        use = 450;
        for (int i = 0; i < 100; i++) {
            builder.getOrCreateUidBatteryConsumerBuilder(Process.FIRST_APPLICATION_UID + i)
                    .setTimeInStateMs(UidBatteryConsumer.STATE_FOREGROUND, 10000 + i * 1000)
                    .setTimeInStateMs(UidBatteryConsumer.STATE_BACKGROUND, 20000 + i * 2000)
                    .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, use);
            use += 1;
        }

        // Simulate dex2oat process.
        builder.getOrCreateUidBatteryConsumerBuilder(Process.FIRST_APPLICATION_UID)
                .setUsageDurationMillis(BatteryConsumer.POWER_COMPONENT_CPU, 100000)
                .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, 1000.0)
                .setPackageWithHighestDrain("dex2oat");

        builder.getOrCreateUidBatteryConsumerBuilder(Process.FIRST_APPLICATION_UID + 1)
                .setUsageDurationMillis(BatteryConsumer.POWER_COMPONENT_CPU, 100000)
                .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, 1000.0)
                .setPackageWithHighestDrain("dex2oat");

        builder.getOrCreateUidBatteryConsumerBuilder(UserHandle.getSharedAppGid(Process.LOG_UID))
                .setUsageDurationMillis(BatteryConsumer.POWER_COMPONENT_CPU, 100000)
                .setConsumedPower(BatteryConsumer.POWER_COMPONENT_CPU, 900.0);

        return builder.build();
    }

    private Preference getCachedPreference(String key) {
        return mPreferenceCache != null ? mPreferenceCache.remove(key) : null;
    }

    private void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
        mPreferenceCache = null;
    }

    private int getCachedCount() {
        return mPreferenceCache != null ? mPreferenceCache.size() : 0;
    }

    private void addNotAvailableMessage() {
        Preference notAvailable = getCachedPreference(NOT_AVAILABLE);
        if (notAvailable == null) {
            notAvailable = new Preference(mPrefContext);
            notAvailable.setKey(NOT_AVAILABLE);
            notAvailable.setTitle(R.string.power_usage_not_available);
            notAvailable.setSelectable(false);
            mAppListGroup.addPreference(notAvailable);
        }
    }
}
