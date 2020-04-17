/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import static android.content.pm.ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA;
import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.GrantedUriPermission;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.util.MutableInt;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.StorageWizardMoveConfirm;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;
import com.android.settingslib.widget.ActionButtonsPreference;
import com.android.settingslib.widget.LayoutPreference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class AppStorageSettings extends AppInfoWithHeader
        implements OnClickListener, Callbacks, DialogInterface.OnClickListener,
        LoaderManager.LoaderCallbacks<AppStorageStats> {
    private static final String TAG = AppStorageSettings.class.getSimpleName();

    //internal constants used in Handler
    private static final int OP_SUCCESSFUL = 1;
    private static final int OP_FAILED = 2;
    private static final int MSG_CLEAR_USER_DATA = 1;
    private static final int MSG_CLEAR_CACHE = 3;

    // invalid size value used initially and also when size retrieval through PackageManager
    // fails for whatever reason
    private static final int SIZE_INVALID = -1;

    // Result code identifiers
    public static final int REQUEST_MANAGE_SPACE = 2;

    private static final int DLG_CLEAR_DATA = DLG_BASE + 1;
    private static final int DLG_CANNOT_CLEAR_DATA = DLG_BASE + 2;

    private static final String KEY_STORAGE_USED = "storage_used";
    private static final String KEY_CHANGE_STORAGE = "change_storage_button";
    private static final String KEY_STORAGE_SPACE = "storage_space";
    private static final String KEY_STORAGE_CATEGORY = "storage_category";

    private static final String KEY_TOTAL_SIZE = "total_size";
    private static final String KEY_APP_SIZE = "app_size";
    private static final String KEY_DATA_SIZE = "data_size";
    private static final String KEY_CACHE_SIZE = "cache_size";

    private static final String KEY_HEADER_BUTTONS = "header_view";

    private static final String KEY_URI_CATEGORY = "uri_category";
    private static final String KEY_CLEAR_URI = "clear_uri_button";

    private static final String KEY_CACHE_CLEARED = "cache_cleared";
    private static final String KEY_DATA_CLEARED = "data_cleared";

    // Views related to cache info
    @VisibleForTesting
    ActionButtonsPreference mButtonsPref;

    private Preference mStorageUsed;
    private Button mChangeStorageButton;

    // Views related to URI permissions
    private Button mClearUriButton;
    private LayoutPreference mClearUri;
    private PreferenceCategory mUri;

    private boolean mCanClearData = true;
    private boolean mCacheCleared;
    private boolean mDataCleared;

    @VisibleForTesting
    AppStorageSizesController mSizeController;

    private ClearCacheObserver mClearCacheObserver;
    private ClearUserDataObserver mClearDataObserver;

    private VolumeInfo[] mCandidates;
    private AlertDialog.Builder mDialogBuilder;
    private ApplicationInfo mInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mCacheCleared = savedInstanceState.getBoolean(KEY_CACHE_CLEARED, false);
            mDataCleared = savedInstanceState.getBoolean(KEY_DATA_CLEARED, false);
            mCacheCleared = mCacheCleared || mDataCleared;
        }

        addPreferencesFromResource(R.xml.app_storage_settings);
        setupViews();
        initMoveDialog();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSize();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CACHE_CLEARED, mCacheCleared);
        outState.putBoolean(KEY_DATA_CLEARED, mDataCleared);
    }

    private void setupViews() {
        // Set default values on sizes
        mSizeController = new AppStorageSizesController.Builder()
                .setTotalSizePreference(findPreference(KEY_TOTAL_SIZE))
                .setAppSizePreference(findPreference(KEY_APP_SIZE))
                .setDataSizePreference(findPreference(KEY_DATA_SIZE))
                .setCacheSizePreference(findPreference(KEY_CACHE_SIZE))
                .setComputingString(R.string.computing_size)
                .setErrorString(R.string.invalid_size_value)
                .build();
        mButtonsPref = ((ActionButtonsPreference) findPreference(KEY_HEADER_BUTTONS));
        mStorageUsed = findPreference(KEY_STORAGE_USED);
        mChangeStorageButton = (Button) ((LayoutPreference) findPreference(KEY_CHANGE_STORAGE))
                .findViewById(R.id.button);
        mChangeStorageButton.setText(R.string.change);
        mChangeStorageButton.setOnClickListener(this);

        // Cache section
        mButtonsPref
                .setButton2Text(R.string.clear_cache_btn_text)
                .setButton2Icon(R.drawable.ic_settings_delete);

        // URI permissions section
        mUri = (PreferenceCategory) findPreference(KEY_URI_CATEGORY);
        mClearUri = (LayoutPreference) mUri.findPreference(KEY_CLEAR_URI);
        mClearUriButton = (Button) mClearUri.findViewById(R.id.button);
        mClearUriButton.setText(R.string.clear_uri_btn_text);
        mClearUriButton.setOnClickListener(this);
    }

    @VisibleForTesting
    void handleClearCacheClick() {
        if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                    getActivity(), mAppsControlDisallowedAdmin);
            return;
        } else if (mClearCacheObserver == null) { // Lazy initialization of observer
            mClearCacheObserver = new ClearCacheObserver();
        }
        mMetricsFeatureProvider.action(getContext(),
                SettingsEnums.ACTION_SETTINGS_CLEAR_APP_CACHE);
        mPm.deleteApplicationCacheFiles(mPackageName, mClearCacheObserver);
    }

    @VisibleForTesting
    void handleClearDataClick() {
        if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                    getActivity(), mAppsControlDisallowedAdmin);
        } else if (mAppEntry.info.manageSpaceActivityName != null) {
            if (!Utils.isMonkeyRunning()) {
                Intent intent = new Intent(Intent.ACTION_DEFAULT);
                intent.setClassName(mAppEntry.info.packageName,
                        mAppEntry.info.manageSpaceActivityName);
                startActivityForResult(intent, REQUEST_MANAGE_SPACE);
            }
        } else {
            showDialogInner(DLG_CLEAR_DATA, 0);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mChangeStorageButton && mDialogBuilder != null && !isMoveInProgress()) {
            mDialogBuilder.show();
        } else if (v == mClearUriButton) {
            if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                        getActivity(), mAppsControlDisallowedAdmin);
            } else {
                clearUriPermissions();
            }
        }
    }

    private boolean isMoveInProgress() {
        try {
            // TODO: define a cleaner API for this
            AppGlobals.getPackageManager().checkPackageStartable(mPackageName,
                    UserHandle.myUserId());
            return false;
        } catch (RemoteException | SecurityException e) {
            return true;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Context context = getActivity();

        // If not current volume, kick off move wizard
        final VolumeInfo targetVol = mCandidates[which];
        final VolumeInfo currentVol = context.getPackageManager().getPackageCurrentVolume(
                mAppEntry.info);
        if (!Objects.equals(targetVol, currentVol)) {
            final Intent intent = new Intent(context, StorageWizardMoveConfirm.class);
            intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, targetVol.getId());
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mAppEntry.info.packageName);
            startActivity(intent);
        }
        dialog.dismiss();
    }

    @Override
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (mAppEntry == null) {
            return false;
        }
        updateUiWithSize(mSizeController.getLastResult());
        refreshGrantedUriPermissions();

        final VolumeInfo currentVol = getActivity().getPackageManager()
                .getPackageCurrentVolume(mAppEntry.info);
        final StorageManager storage = getContext().getSystemService(StorageManager.class);
        mStorageUsed.setSummary(storage.getBestVolumeDescription(currentVol));

        refreshButtons();

        return true;
    }

    private void refreshButtons() {
        initMoveDialog();
        initDataButtons();
    }

    private void initDataButtons() {
        final boolean appHasSpaceManagementUI = mAppEntry.info.manageSpaceActivityName != null;
        final boolean appHasActiveAdmins = mDpm.packageHasActiveAdmins(mPackageName);
        // Check that SYSTEM_APP flag is set, and ALLOW_CLEAR_USER_DATA is not set.
        final boolean isNonClearableSystemApp =
                (mAppEntry.info.flags & (FLAG_SYSTEM | FLAG_ALLOW_CLEAR_USER_DATA)) == FLAG_SYSTEM;
        final boolean appRestrictsClearingData = isNonClearableSystemApp || appHasActiveAdmins;

        final Intent intent = new Intent(Intent.ACTION_DEFAULT);
        if (appHasSpaceManagementUI) {
            intent.setClassName(mAppEntry.info.packageName, mAppEntry.info.manageSpaceActivityName);
        }
        final boolean isManageSpaceActivityAvailable =
                getPackageManager().resolveActivity(intent, 0) != null;

        if ((!appHasSpaceManagementUI && appRestrictsClearingData)
                || !isManageSpaceActivityAvailable) {
            mButtonsPref
                    .setButton1Text(R.string.clear_user_data_text)
                    .setButton1Icon(R.drawable.ic_settings_delete)
                    .setButton1Enabled(false);
            mCanClearData = false;
        } else {
            if (appHasSpaceManagementUI) {
                mButtonsPref.setButton1Text(R.string.manage_space_text);
            } else {
                mButtonsPref
                        .setButton1Text(R.string.clear_user_data_text)
                        .setButton1Icon(R.drawable.ic_settings_delete);
            }
            mButtonsPref.setButton1OnClickListener(v -> handleClearDataClick());
        }

        if (mAppsControlDisallowedBySystem) {
            mButtonsPref.setButton1Enabled(false);
        }
    }

    private void initMoveDialog() {
        final Context context = getActivity();
        final StorageManager storage = context.getSystemService(StorageManager.class);

        final List<VolumeInfo> candidates = context.getPackageManager()
                .getPackageCandidateVolumes(mAppEntry.info);
        if (candidates.size() > 1) {
            Collections.sort(candidates, VolumeInfo.getDescriptionComparator());

            CharSequence[] labels = new CharSequence[candidates.size()];
            int current = -1;
            for (int i = 0; i < candidates.size(); i++) {
                final String volDescrip = storage.getBestVolumeDescription(candidates.get(i));
                if (Objects.equals(volDescrip, mStorageUsed.getSummary())) {
                    current = i;
                }
                labels[i] = volDescrip;
            }
            mCandidates = candidates.toArray(new VolumeInfo[candidates.size()]);
            mDialogBuilder = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.change_storage)
                    .setSingleChoiceItems(labels, current, this)
                    .setNegativeButton(R.string.cancel, null);
        } else {
            removePreference(KEY_STORAGE_USED);
            removePreference(KEY_CHANGE_STORAGE);
            removePreference(KEY_STORAGE_SPACE);
        }
    }

    /*
     * Private method to initiate clearing user data when the user clicks the clear data
     * button for a system package
     */
    private void initiateClearUserData() {
        mMetricsFeatureProvider.action(getContext(), SettingsEnums.ACTION_SETTINGS_CLEAR_APP_DATA);
        mButtonsPref.setButton1Enabled(false);
        // Invoke uninstall or clear user data based on sysPackage
        String packageName = mAppEntry.info.packageName;
        Log.i(TAG, "Clearing user data for package : " + packageName);
        if (mClearDataObserver == null) {
            mClearDataObserver = new ClearUserDataObserver();
        }
        ActivityManager am = (ActivityManager)
                getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        boolean res = am.clearApplicationUserData(packageName, mClearDataObserver);
        if (!res) {
            // Clearing data failed for some obscure reason. Just log error for now
            Log.i(TAG, "Couldn't clear application user data for package:" + packageName);
            showDialogInner(DLG_CANNOT_CLEAR_DATA, 0);
        } else {
            mButtonsPref.setButton1Text(R.string.recompute_size);
        }
    }

    /*
     * Private method to handle clear message notification from observer when
     * the async operation from PackageManager is complete
     */
    private void processClearMsg(Message msg) {
        int result = msg.arg1;
        String packageName = mAppEntry.info.packageName;
        mButtonsPref
                .setButton1Text(R.string.clear_user_data_text)
                .setButton1Icon(R.drawable.ic_settings_delete);
        if (result == OP_SUCCESSFUL) {
            Log.i(TAG, "Cleared user data for package : " + packageName);
            updateSize();
        } else {
            mButtonsPref.setButton1Enabled(true);
        }
    }

    private void refreshGrantedUriPermissions() {
        // Clear UI first (in case the activity has been resumed)
        removeUriPermissionsFromUi();

        // Gets all URI permissions from am.
        ActivityManager am = (ActivityManager) getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        List<GrantedUriPermission> perms =
                am.getGrantedUriPermissions(mAppEntry.info.packageName).getList();

        if (perms.isEmpty()) {
            mClearUriButton.setVisibility(View.GONE);
            return;
        }

        PackageManager pm = getActivity().getPackageManager();

        // Group number of URIs by app.
        Map<CharSequence, MutableInt> uriCounters = new TreeMap<>();
        for (GrantedUriPermission perm : perms) {
            String authority = perm.uri.getAuthority();
            ProviderInfo provider = pm.resolveContentProvider(authority, 0);
            if (provider == null) {
                continue;
            }

            CharSequence app = provider.applicationInfo.loadLabel(pm);
            MutableInt count = uriCounters.get(app);
            if (count == null) {
                uriCounters.put(app, new MutableInt(1));
            } else {
                count.value++;
            }
        }

        // Dynamically add the preferences, one per app.
        int order = 0;
        for (Map.Entry<CharSequence, MutableInt> entry : uriCounters.entrySet()) {
            int numberResources = entry.getValue().value;
            Preference pref = new Preference(getPrefContext());
            pref.setTitle(entry.getKey());
            pref.setSummary(getPrefContext().getResources()
                    .getQuantityString(R.plurals.uri_permissions_text, numberResources,
                            numberResources));
            pref.setSelectable(false);
            pref.setLayoutResource(R.layout.horizontal_preference);
            pref.setOrder(order);
            Log.v(TAG, "Adding preference '" + pref + "' at order " + order);
            mUri.addPreference(pref);
        }

        if (mAppsControlDisallowedBySystem) {
            mClearUriButton.setEnabled(false);
        }

        mClearUri.setOrder(order);
        mClearUriButton.setVisibility(View.VISIBLE);

    }

    private void clearUriPermissions() {
        final Context context = getActivity();
        final String packageName = mAppEntry.info.packageName;
        // Synchronously revoke the permissions.
        final ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        am.clearGrantedUriPermissions(packageName);

        // Update UI
        refreshGrantedUriPermissions();
    }

    private void removeUriPermissionsFromUi() {
        // Remove all preferences but the clear button.
        int count = mUri.getPreferenceCount();
        for (int i = count - 1; i >= 0; i--) {
            Preference pref = mUri.getPreference(i);
            if (pref != mClearUri) {
                mUri.removePreference(pref);
            }
        }
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        switch (id) {
            case DLG_CLEAR_DATA:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getText(R.string.clear_data_dlg_title))
                        .setMessage(getActivity().getText(R.string.clear_data_dlg_text))
                        .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Clear user data here
                                initiateClearUserData();
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            case DLG_CANNOT_CLEAR_DATA:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getText(R.string.clear_user_data_text))
                        .setMessage(getActivity().getText(R.string.clear_failed_dlg_text))
                        .setNeutralButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mButtonsPref.setButton1Enabled(false);
                                //force to recompute changed value
                                setIntentAndFinish(false  /* appChanged */);
                            }
                        })
                        .create();
        }
        return null;
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public Loader<AppStorageStats> onCreateLoader(int id, Bundle args) {
        Context context = getContext();
        return new FetchPackageStorageAsyncLoader(
                context, new StorageStatsSource(context), mInfo, UserHandle.of(mUserId));
    }

    @Override
    public void onLoadFinished(Loader<AppStorageStats> loader, AppStorageStats result) {
        mSizeController.setResult(result);
        updateUiWithSize(result);
    }

    @Override
    public void onLoaderReset(Loader<AppStorageStats> loader) {
    }

    private void updateSize() {
        PackageManager packageManager = getPackageManager();
        try {
            mInfo = packageManager.getApplicationInfo(mPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find package", e);
        }

        if (mInfo == null) {
            return;
        }

        getLoaderManager().restartLoader(1, Bundle.EMPTY, this);
    }

    @VisibleForTesting
    void updateUiWithSize(AppStorageStats result) {
        if (mCacheCleared) {
            mSizeController.setCacheCleared(true);
        }
        if (mDataCleared) {
            mSizeController.setDataCleared(true);
        }

        mSizeController.updateUi(getContext());

        if (result == null) {
            mButtonsPref.setButton1Enabled(false).setButton2Enabled(false);
        } else {
            long cacheSize = result.getCacheBytes();
            long dataSize = result.getDataBytes() - cacheSize;

            if (dataSize <= 0 || !mCanClearData || mDataCleared) {
                mButtonsPref.setButton1Enabled(false);
            } else {
                mButtonsPref.setButton1Enabled(true)
                        .setButton1OnClickListener(v -> handleClearDataClick());
            }
            if (cacheSize <= 0 || mCacheCleared) {
                mButtonsPref.setButton2Enabled(false);
            } else {
                mButtonsPref.setButton2Enabled(true)
                        .setButton2OnClickListener(v -> handleClearCacheClick());
            }
        }
        if (mAppsControlDisallowedBySystem) {
            mButtonsPref.setButton1Enabled(false).setButton2Enabled(false);
        }
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (getView() == null) {
                return;
            }
            switch (msg.what) {
                case MSG_CLEAR_USER_DATA:
                    mDataCleared = true;
                    mCacheCleared = true;
                    processClearMsg(msg);
                    break;
                case MSG_CLEAR_CACHE:
                    mCacheCleared = true;
                    // Refresh size info
                    updateSize();
                    break;
            }
        }
    };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_APP_STORAGE;
    }

    class ClearCacheObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            final Message msg = mHandler.obtainMessage(MSG_CLEAR_CACHE);
            msg.arg1 = succeeded ? OP_SUCCESSFUL : OP_FAILED;
            mHandler.sendMessage(msg);
        }
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            final Message msg = mHandler.obtainMessage(MSG_CLEAR_USER_DATA);
            msg.arg1 = succeeded ? OP_SUCCESSFUL : OP_FAILED;
            mHandler.sendMessage(msg);
        }
    }
}
