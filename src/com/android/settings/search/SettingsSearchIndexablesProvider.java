/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_OFF;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_USER_ID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesProvider;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.settings.DateTimeSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.LegalSettings;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment;
import com.android.settings.accessibility.MagnificationPreferenceFragment;
import com.android.settings.accounts.UserAndAccountDashboardFragment;
import com.android.settings.applications.AppAndNotificationDashboardFragment;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.applications.SpecialAccessSettings;
import com.android.settings.applications.assist.ManageAssist;
import com.android.settings.backup.BackupSettingsActivity;
import com.android.settings.backup.BackupSettingsFragment;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.datausage.DataUsageMeteredSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;
import com.android.settings.development.DevelopmentSettingsDashboardFragment;
import com.android.settings.deviceinfo.Status;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.AmbientDisplaySettings;
import com.android.settings.display.ScreenZoomSettings;
import com.android.settings.dream.DreamSettings;
import com.android.settings.enterprise.EnterprisePrivacySettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageAdvanced;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.AssistGestureSettings;
import com.android.settings.gestures.DoubleTapPowerSettings;
import com.android.settings.gestures.DoubleTapScreenSettings;
import com.android.settings.gestures.DoubleTwistGestureSettings;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.gestures.PickupGestureSettings;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.VirtualKeyboardFragment;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModeBehaviorSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.security.EncryptionAndCredential;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.security.SecuritySettings;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.sim.SimSettings;
import com.android.settings.support.SupportDashboardActivity;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.tts.TtsEnginePreferenceFragment;
import com.android.settings.users.UserSettings;
import com.android.settings.wallpaper.WallpaperTypeSettings;
import com.android.settings.wifi.ConfigureWifiSettings;
import com.android.settings.wifi.WifiSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsSearchIndexablesProvider extends SearchIndexablesProvider {
    public static final boolean DEBUG = false;
    private static final String TAG = "SettingsSearchProvider";

    public static final Set<Class> INDEXABLES = new HashSet<>();
    private static final Collection<String> INVALID_KEYS;

    @VisibleForTesting
    static void addIndex(Class indexClass) {
        INDEXABLES.add(indexClass);
    }

    static {
        addIndex(WifiSettings.class);
        addIndex(NetworkDashboardFragment.class);
        addIndex(ConfigureWifiSettings.class);
        addIndex(BluetoothSettings.class);
        addIndex(SimSettings.class);
        addIndex(DataUsageSummary.class);
        addIndex(DataUsageMeteredSettings.class);
        addIndex(ScreenZoomSettings.class);
        addIndex(DisplaySettings.class);
        addIndex(AmbientDisplaySettings.class);
        addIndex(WallpaperTypeSettings.class);
        addIndex(AppAndNotificationDashboardFragment.class);
        addIndex(SoundSettings.class);
        addIndex(ZenModeSettings.class);
        addIndex(StorageSettings.class);
        addIndex(PowerUsageAdvanced.class);
        addIndex(DefaultAppSettings.class);
        addIndex(ManageAssist.class);
        addIndex(SpecialAccessSettings.class);
        addIndex(UserSettings.class);
        addIndex(AssistGestureSettings.class);
        addIndex(PickupGestureSettings.class);
        addIndex(DoubleTapScreenSettings.class);
        addIndex(DoubleTapPowerSettings.class);
        addIndex(DoubleTwistGestureSettings.class);
        addIndex(SwipeToNotificationSettings.class);
        addIndex(GestureSettings.class);
        addIndex(LanguageAndInputSettings.class);
        addIndex(LocationSettings.class);
        addIndex(ScanningSettings.class);
        addIndex(SecuritySettings.class);
        addIndex(ScreenLockSettings.class);
        addIndex(EncryptionAndCredential.class);
        addIndex(ScreenPinningSettings.class);
        addIndex(UserAndAccountDashboardFragment.class);
        addIndex(VirtualKeyboardFragment.class);
        addIndex(AvailableVirtualKeyboardFragment.class);
        addIndex(PhysicalKeyboardFragment.class);
        addIndex(BackupSettingsActivity.class);
        addIndex(BackupSettingsFragment.class);
        addIndex(DateTimeSettings.class);
        addIndex(AccessibilitySettings.class);
        addIndex(PrintSettingsFragment.class);
        addIndex(DevelopmentSettingsDashboardFragment.class);
        addIndex(DeviceInfoSettings.class);
        addIndex(Status.class);
        addIndex(LegalSettings.class);
        addIndex(SystemDashboardFragment.class);
        addIndex(ResetDashboardFragment.class);
        addIndex(StorageDashboardFragment.class);
        addIndex(ConnectedDeviceDashboardFragment.class);
        addIndex(EnterprisePrivacySettings.class);
        addIndex(PaymentSettings.class);
        addIndex(TextToSpeechSettings.class);
        addIndex(TtsEnginePreferenceFragment.class);
        addIndex(MagnificationPreferenceFragment.class);
        addIndex(AccessibilityShortcutPreferenceFragment.class);
        addIndex(DreamSettings.class);
        addIndex(SupportDashboardActivity.class);
        addIndex(AutomaticStorageManagerSettings.class);
        addIndex(ConfigureNotificationSettings.class);
        addIndex(PowerUsageSummary.class);
        addIndex(BatterySaverSettings.class);
        addIndex(LockscreenDashboardFragment.class);
        addIndex(ZenModeBehaviorSettings.class);
        addIndex(ZenModeAutomationSettings.class);

        INVALID_KEYS = new ArraySet<>();
        INVALID_KEYS.add(null);
        INVALID_KEYS.add("");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        final List<SearchIndexableResource> resources =
                getSearchIndexableResourcesFromProvider(getContext());
        for (SearchIndexableResource val : resources) {
            Object[] ref = new Object[INDEXABLES_XML_RES_COLUMNS.length];
            ref[COLUMN_INDEX_XML_RES_RANK] = val.rank;
            ref[COLUMN_INDEX_XML_RES_RESID] = val.xmlResId;
            ref[COLUMN_INDEX_XML_RES_CLASS_NAME] = val.className;
            ref[COLUMN_INDEX_XML_RES_ICON_RESID] = val.iconResId;
            ref[COLUMN_INDEX_XML_RES_INTENT_ACTION] = val.intentAction;
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] = val.intentTargetPackage;
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = null; // intent target class
            cursor.addRow(ref);
        }

        return cursor;
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        final List<SearchIndexableRaw> raws = getSearchIndexableRawFromProvider(getContext());
        for (SearchIndexableRaw val : raws) {
            Object[] ref = new Object[INDEXABLES_RAW_COLUMNS.length];
            ref[COLUMN_INDEX_RAW_TITLE] = val.title;
            ref[COLUMN_INDEX_RAW_SUMMARY_ON] = val.summaryOn;
            ref[COLUMN_INDEX_RAW_SUMMARY_OFF] = val.summaryOff;
            ref[COLUMN_INDEX_RAW_ENTRIES] = val.entries;
            ref[COLUMN_INDEX_RAW_KEYWORDS] = val.keywords;
            ref[COLUMN_INDEX_RAW_SCREEN_TITLE] = val.screenTitle;
            ref[COLUMN_INDEX_RAW_CLASS_NAME] = val.className;
            ref[COLUMN_INDEX_RAW_ICON_RESID] = val.iconResId;
            ref[COLUMN_INDEX_RAW_INTENT_ACTION] = val.intentAction;
            ref[COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE] = val.intentTargetPackage;
            ref[COLUMN_INDEX_RAW_INTENT_TARGET_CLASS] = val.intentTargetClass;
            ref[COLUMN_INDEX_RAW_KEY] = val.key;
            ref[COLUMN_INDEX_RAW_USER_ID] = val.userId;
            cursor.addRow(ref);
        }

        return cursor;
    }

    /**
     * Gets a combined list non-indexable keys that come from providers inside of settings.
     * The non-indexable keys are used in Settings search at both index and update time to verify
     * the validity of results in the database.
     */
    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);
        final List<String> nonIndexableKeys = getNonIndexableKeysFromProvider(getContext());
        for (String nik : nonIndexableKeys) {
            final Object[] ref = new Object[NON_INDEXABLES_KEYS_COLUMNS.length];
            ref[COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE] = nik;
            cursor.addRow(ref);
        }

        return cursor;
    }

    private List<String> getNonIndexableKeysFromProvider(Context context) {
        final List<String> nonIndexableKeys = new ArrayList<>();

        for (Class clazz : INDEXABLES) {
            final long startTime = System.currentTimeMillis();
            Indexable.SearchIndexProvider provider = DatabaseIndexingUtils.getSearchIndexProvider(
                    clazz);
            List<String> providerNonIndexableKeys = provider.getNonIndexableKeys(context);

            if (providerNonIndexableKeys == null || providerNonIndexableKeys.isEmpty()) {
                if (DEBUG) {
                    final long totalTime = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "No indexable, total time " + totalTime);
                }
                continue;
            }

            if (providerNonIndexableKeys.removeAll(INVALID_KEYS)) {
                Log.v(TAG, provider + " tried to add an empty non-indexable key");
            }

            if (DEBUG) {
                final long totalTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Non-indexables " + providerNonIndexableKeys.size() + ", total time "
                        + totalTime);
            }

            nonIndexableKeys.addAll(providerNonIndexableKeys);
        }

        return nonIndexableKeys;
    }

    private List<SearchIndexableResource> getSearchIndexableResourcesFromProvider(Context context) {
        List<SearchIndexableResource> resourceList = new ArrayList<>();

        for (Class clazz : INDEXABLES) {
            Indexable.SearchIndexProvider provider = DatabaseIndexingUtils.getSearchIndexProvider(
                    clazz);

            final List<SearchIndexableResource> resList =
                    provider.getXmlResourcesToIndex(context, true);

            if (resList == null) {
                continue;
            }

            for (SearchIndexableResource item : resList) {
                item.className = TextUtils.isEmpty(item.className)
                        ? clazz.getName()
                        : item.className;
            }

            resourceList.addAll(resList);
        }

        return resourceList;
    }

    private List<SearchIndexableRaw> getSearchIndexableRawFromProvider(Context context) {
        final List<SearchIndexableRaw> rawList = new ArrayList<>();

        for (Class clazz : INDEXABLES) {
            Indexable.SearchIndexProvider provider = DatabaseIndexingUtils.getSearchIndexProvider(
                    clazz);
            final List<SearchIndexableRaw> providerRaws = provider.getRawDataToIndex(context,
                    true /* enabled */);

            if (providerRaws == null) {
                continue;
            }

            for (SearchIndexableRaw raw : providerRaws) {
                // The classname and intent information comes from the PreIndexData
                // This will be more clear when provider conversion is done at PreIndex time.
                raw.className = clazz.getName();

            }
            rawList.addAll(providerRaws);
        }

        return rawList;
    }
}
