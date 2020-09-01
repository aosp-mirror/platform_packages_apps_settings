/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
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

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller that update the battery header view
 */
public class BatteryAppListPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnDestroy {
    @VisibleForTesting
    static final boolean USE_FAKE_DATA = false;
    private static final int MAX_ITEMS_TO_LIST = USE_FAKE_DATA ? 30 : 20;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final int STATS_TYPE = BatteryStats.STATS_SINCE_CHARGED;

    private final String mPreferenceKey;
    @VisibleForTesting
    PreferenceGroup mAppListGroup;
    private BatteryStatsHelper mBatteryStatsHelper;
    private ArrayMap<String, Preference> mPreferenceCache;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private UserManager mUserManager;
    private SettingsActivity mActivity;
    private InstrumentedPreferenceFragment mFragment;
    private Context mPrefContext;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp =
                            (PowerGaugePreference) mAppListGroup.findPreference(
                                    Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        final int userId = UserHandle.getUserId(entry.sipper.getUid());
                        final UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(mUserManager.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                        if (entry.sipper.drainType == DrainType.APP) {
                            pgp.setContentDescription(entry.name);
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
        mActivity = activity;
        mFragment = fragment;
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
            AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity, mBatteryUtils,
                    mFragment, mBatteryStatsHelper, STATS_TYPE, entry, pgp.getPercent());
            return true;
        }
        return false;
    }

    public void refreshAppListGroup(BatteryStatsHelper statsHelper, boolean showAllApps) {
        if (!isAvailable()) {
            return;
        }

        mBatteryStatsHelper = statsHelper;
        mAppListGroup.setTitle(R.string.power_usage_list_summary);

        final PowerProfile powerProfile = statsHelper.getPowerProfile();
        final BatteryStats stats = statsHelper.getStats();
        final double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        boolean addedSome = false;
        final int dischargeAmount = USE_FAKE_DATA ? 5000
                : stats != null ? stats.getDischargeAmount(STATS_TYPE) : 0;

        cacheRemoveAllPrefs(mAppListGroup);
        mAppListGroup.setOrderingAsAdded(false);

        if (averagePower >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP || USE_FAKE_DATA) {
            final List<BatterySipper> usageList = getCoalescedUsageList(
                    USE_FAKE_DATA ? getFakeStats() : statsHelper.getUsageList());
            double hiddenPowerMah = showAllApps ? 0 :
                    mBatteryUtils.removeHiddenBatterySippers(usageList);
            mBatteryUtils.sortUsageList(usageList);

            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatterySipper sipper = usageList.get(i);
                double totalPower = USE_FAKE_DATA ? 4000 : statsHelper.getTotalPower();

                final double percentOfTotal = mBatteryUtils.calculateBatteryPercent(
                        sipper.totalPowerMah, totalPower, hiddenPowerMah, dischargeAmount);

                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }
                if (shouldHideSipper(sipper)) {
                    continue;
                }
                final UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
                final BatteryEntry entry = new BatteryEntry(mActivity, mHandler, mUserManager,
                        sipper);
                final Drawable badgedIcon = mUserManager.getBadgedIconForUser(entry.getIcon(),
                        userHandle);
                final CharSequence contentDescription = mUserManager.getBadgedLabelForUser(
                        entry.getLabel(),
                        userHandle);

                final String key = extractKeyFromSipper(sipper);
                PowerGaugePreference pref = (PowerGaugePreference) getCachedPreference(key);
                if (pref == null) {
                    pref = new PowerGaugePreference(mPrefContext, badgedIcon,
                            contentDescription, entry);
                    pref.setKey(key);
                }
                sipper.percent = percentOfTotal;
                pref.setTitle(entry.getLabel());
                pref.setOrder(i + 1);
                pref.setPercent(percentOfTotal);
                pref.shouldShowAnomalyIcon(false);
                if (sipper.usageTimeMs == 0 && sipper.drainType == DrainType.APP) {
                    sipper.usageTimeMs = mBatteryUtils.getProcessTimeMs(
                            BatteryUtils.StatusType.FOREGROUND, sipper.uidObj, STATS_TYPE);
                }
                setUsageSummary(pref, sipper);
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
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     *
     * @return A sorted list of apps using power.
     */
    private List<BatterySipper> getCoalescedUsageList(final List<BatterySipper> sippers) {
        final SparseArray<BatterySipper> uidList = new SparseArray<>();

        final ArrayList<BatterySipper> results = new ArrayList<>();
        final int numSippers = sippers.size();
        for (int i = 0; i < numSippers; i++) {
            BatterySipper sipper = sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();

                // Check if this UID is a shared GID. If so, we combine it with the OWNER's
                // actual app UID.
                if (isSharedGid(sipper.getUid())) {
                    realUid = UserHandle.getUid(UserHandle.USER_SYSTEM,
                            UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }

                // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
                if (isSystemUid(realUid)
                        && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    // Use the system UID for all UIDs running in their own sandbox that
                    // are not apps. We exclude mediaserver because we already are expected to
                    // report that as a separate item.
                    realUid = Process.SYSTEM_UID;
                }

                if (realUid != sipper.getUid()) {
                    // Replace the BatterySipper with a new one with the real UID set.
                    BatterySipper newSipper = new BatterySipper(sipper.drainType,
                            new FakeUid(realUid), 0.0);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }

                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    // New entry.
                    uidList.put(realUid, sipper);
                } else {
                    // Combine BatterySippers if we already have one with this UID.
                    final BatterySipper existingSipper = uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null
                            && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }

                    final int existingPackageLen = existingSipper.mPackages != null ?
                            existingSipper.mPackages.length : 0;
                    final int newPackageLen = sipper.mPackages != null ?
                            sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[existingPackageLen + newPackageLen];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0,
                                    existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen,
                                newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }

        final int numUidSippers = uidList.size();
        for (int i = 0; i < numUidSippers; i++) {
            results.add(uidList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        mBatteryUtils.sortUsageList(results);
        return results;
    }

    @VisibleForTesting
    void setUsageSummary(Preference preference, BatterySipper sipper) {
        // Only show summary when usage time is longer than one minute
        final long usageTimeMs = sipper.usageTimeMs;
        if (shouldShowSummary(sipper) && usageTimeMs >= DateUtils.MINUTE_IN_MILLIS) {
            final CharSequence timeSequence =
                    StringUtil.formatElapsedTime(mContext, usageTimeMs, false);
            preference.setSummary(
                    (sipper.drainType != DrainType.APP || mBatteryUtils.shouldHideSipper(sipper))
                            ? timeSequence
                            : TextUtils.expandTemplate(mContext.getText(R.string.battery_used_for),
                                    timeSequence));
        }
    }

    @VisibleForTesting
    boolean shouldHideSipper(BatterySipper sipper) {
        // Don't show over-counted, unaccounted and hidden system module in any condition
        return sipper.drainType == BatterySipper.DrainType.OVERCOUNTED
                || sipper.drainType == BatterySipper.DrainType.UNACCOUNTED
                || mBatteryUtils.isHiddenSystemModule(sipper) || sipper.getUid() < 0;
    }

    @VisibleForTesting
    String extractKeyFromSipper(BatterySipper sipper) {
        if (sipper.uidObj != null) {
            return extractKeyFromUid(sipper.getUid());
        } else if (sipper.drainType == DrainType.USER) {
            return sipper.drainType.toString() + sipper.userId;
        } else if (sipper.drainType != DrainType.APP) {
            return sipper.drainType.toString();
        } else if (sipper.getPackages() != null) {
            return TextUtils.concat(sipper.getPackages()).toString();
        } else {
            Log.w(TAG, "Inappropriate BatterySipper without uid and package names: " + sipper);
            return "-1";
        }
    }

    @VisibleForTesting
    String extractKeyFromUid(int uid) {
        return Integer.toString(uid);
    }

    private void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<>();
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    private boolean shouldShowSummary(BatterySipper sipper) {
        final CharSequence[] whitelistPackages = mContext.getResources()
                .getTextArray(R.array.whitelist_hide_summary_in_battery_usage);
        final String target = sipper.packageWithHighestDrain;

        for (CharSequence packageName: whitelistPackages) {
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

    private static List<BatterySipper> getFakeStats() {
        ArrayList<BatterySipper> stats = new ArrayList<>();
        float use = 5;
        for (DrainType type : DrainType.values()) {
            if (type == DrainType.APP) {
                continue;
            }
            stats.add(new BatterySipper(type, null, use));
            use += 5;
        }
        for (int i = 0; i < 100; i++) {
            stats.add(new BatterySipper(DrainType.APP,
                    new FakeUid(Process.FIRST_APPLICATION_UID + i), use));
        }
        stats.add(new BatterySipper(DrainType.APP,
                new FakeUid(0), use));

        // Simulate dex2oat process.
        BatterySipper sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID + 1)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.LOG_UID)), 9.0f);
        stats.add(sipper);

        return stats;
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
        final String NOT_AVAILABLE = "not_available";
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
