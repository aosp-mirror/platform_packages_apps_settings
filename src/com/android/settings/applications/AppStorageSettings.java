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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.format.Formatter;
import android.util.Log;
import android.util.MutableInt;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.StorageWizardMoveConfirm;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static android.content.pm.ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA;
import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;

public class AppStorageSettings extends AppInfoWithHeader
        implements OnClickListener, Callbacks, DialogInterface.OnClickListener {
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
    private static final String KEY_EXTERNAL_CODE_SIZE = "external_code_size";
    private static final String KEY_DATA_SIZE = "data_size";
    private static final String KEY_EXTERNAL_DATA_SIZE = "external_data_size";
    private static final String KEY_CACHE_SIZE = "cache_size";

    private static final String KEY_CLEAR_DATA = "clear_data_button";
    private static final String KEY_CLEAR_CACHE = "clear_cache_button";

    private static final String KEY_URI_CATEGORY = "uri_category";
    private static final String KEY_CLEAR_URI = "clear_uri_button";

    private Preference mTotalSize;
    private Preference mAppSize;
    private Preference mDataSize;
    private Preference mExternalCodeSize;
    private Preference mExternalDataSize;

    // Views related to cache info
    private Preference mCacheSize;
    private Button mClearDataButton;
    private Button mClearCacheButton;

    private Preference mStorageUsed;
    private Button mChangeStorageButton;

    // Views related to URI permissions
    private Button mClearUriButton;
    private LayoutPreference mClearUri;
    private PreferenceCategory mUri;

    private boolean mCanClearData = true;
    private boolean mHaveSizes = false;

    private long mLastCodeSize = -1;
    private long mLastDataSize = -1;
    private long mLastExternalCodeSize = -1;
    private long mLastExternalDataSize = -1;
    private long mLastCacheSize = -1;
    private long mLastTotalSize = -1;

    private ClearCacheObserver mClearCacheObserver;
    private ClearUserDataObserver mClearDataObserver;

    // Resource strings
    private CharSequence mInvalidSizeStr;
    private CharSequence mComputingStr;

    private VolumeInfo[] mCandidates;
    private AlertDialog.Builder mDialogBuilder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_storage_settings);
        setupViews();
        initMoveDialog();
    }

    @Override
    public void onResume() {
        super.onResume();
        mState.requestSize(mPackageName, mUserId);
    }

    private void setupViews() {
        mComputingStr = getActivity().getText(R.string.computing_size);
        mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);

        // Set default values on sizes
        mTotalSize = findPreference(KEY_TOTAL_SIZE);
        mAppSize =  findPreference(KEY_APP_SIZE);
        mDataSize =  findPreference(KEY_DATA_SIZE);
        mExternalCodeSize = findPreference(KEY_EXTERNAL_CODE_SIZE);
        mExternalDataSize = findPreference(KEY_EXTERNAL_DATA_SIZE);

        if (Environment.isExternalStorageEmulated()) {
            PreferenceCategory category = (PreferenceCategory) findPreference(KEY_STORAGE_CATEGORY);
            category.removePreference(mExternalCodeSize);
            category.removePreference(mExternalDataSize);
        }
        mClearDataButton = (Button) ((LayoutPreference) findPreference(KEY_CLEAR_DATA))
                .findViewById(R.id.button);

        mStorageUsed = findPreference(KEY_STORAGE_USED);
        mChangeStorageButton = (Button) ((LayoutPreference) findPreference(KEY_CHANGE_STORAGE))
                .findViewById(R.id.button);
        mChangeStorageButton.setText(R.string.change);
        mChangeStorageButton.setOnClickListener(this);

        // Cache section
        mCacheSize = findPreference(KEY_CACHE_SIZE);
        mClearCacheButton = (Button) ((LayoutPreference) findPreference(KEY_CLEAR_CACHE))
                .findViewById(R.id.button);
        mClearCacheButton.setText(R.string.clear_cache_btn_text);

        // URI permissions section
        mUri = (PreferenceCategory) findPreference(KEY_URI_CATEGORY);
        mClearUri = (LayoutPreference) mUri.findPreference(KEY_CLEAR_URI);
        mClearUriButton = (Button) mClearUri.findViewById(R.id.button);
        mClearUriButton.setText(R.string.clear_uri_btn_text);
        mClearUriButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mClearCacheButton) {
            if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                        getActivity(), mAppsControlDisallowedAdmin);
                return;
            } else if (mClearCacheObserver == null) { // Lazy initialization of observer
                mClearCacheObserver = new ClearCacheObserver();
            }
            mPm.deleteApplicationCacheFiles(mPackageName, mClearCacheObserver);
        } else if (v == mClearDataButton) {
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
        } else if (v == mChangeStorageButton && mDialogBuilder != null && !isMoveInProgress()) {
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

    private String getSizeStr(long size) {
        if (size == SIZE_INVALID) {
            return mInvalidSizeStr.toString();
        }
        return Formatter.formatFileSize(getActivity(), size);
    }

    private void refreshSizeInfo() {
        if (mAppEntry.size == ApplicationsState.SIZE_INVALID
                || mAppEntry.size == ApplicationsState.SIZE_UNKNOWN) {
            mLastCodeSize = mLastDataSize = mLastCacheSize = mLastTotalSize = -1;
            if (!mHaveSizes) {
                mAppSize.setSummary(mComputingStr);
                mDataSize.setSummary(mComputingStr);
                mCacheSize.setSummary(mComputingStr);
                mTotalSize.setSummary(mComputingStr);
            }
            mClearDataButton.setEnabled(false);
            mClearCacheButton.setEnabled(false);
        } else {
            mHaveSizes = true;
            long codeSize = mAppEntry.codeSize;
            long dataSize = mAppEntry.dataSize;
            if (Environment.isExternalStorageEmulated()) {
                codeSize += mAppEntry.externalCodeSize;
                dataSize +=  mAppEntry.externalDataSize;
            } else {
                if (mLastExternalCodeSize != mAppEntry.externalCodeSize) {
                    mLastExternalCodeSize = mAppEntry.externalCodeSize;
                    mExternalCodeSize.setSummary(getSizeStr(mAppEntry.externalCodeSize));
                }
                if (mLastExternalDataSize !=  mAppEntry.externalDataSize) {
                    mLastExternalDataSize =  mAppEntry.externalDataSize;
                    mExternalDataSize.setSummary(getSizeStr( mAppEntry.externalDataSize));
                }
            }
            if (mLastCodeSize != codeSize) {
                mLastCodeSize = codeSize;
                mAppSize.setSummary(getSizeStr(codeSize));
            }
            if (mLastDataSize != dataSize) {
                mLastDataSize = dataSize;
                mDataSize.setSummary(getSizeStr(dataSize));
            }
            long cacheSize = mAppEntry.cacheSize + mAppEntry.externalCacheSize;
            if (mLastCacheSize != cacheSize) {
                mLastCacheSize = cacheSize;
                mCacheSize.setSummary(getSizeStr(cacheSize));
            }
            if (mLastTotalSize != mAppEntry.size) {
                mLastTotalSize = mAppEntry.size;
                mTotalSize.setSummary(getSizeStr(mAppEntry.size));
            }

            if ((mAppEntry.dataSize+ mAppEntry.externalDataSize) <= 0 || !mCanClearData) {
                mClearDataButton.setEnabled(false);
            } else {
                mClearDataButton.setEnabled(true);
                mClearDataButton.setOnClickListener(this);
            }
            if (cacheSize <= 0) {
                mClearCacheButton.setEnabled(false);
            } else {
                mClearCacheButton.setEnabled(true);
                mClearCacheButton.setOnClickListener(this);
            }
        }
        if (mAppsControlDisallowedBySystem) {
            mClearCacheButton.setEnabled(false);
            mClearDataButton.setEnabled(false);
        }
    }

    @Override
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (mAppEntry == null) {
            return false;
        }
        refreshSizeInfo();
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
            mClearDataButton.setText(R.string.clear_user_data_text);
            mClearDataButton.setEnabled(false);
            mCanClearData = false;
        } else {
            if (appHasSpaceManagementUI) {
                mClearDataButton.setText(R.string.manage_space_text);
            } else {
                mClearDataButton.setText(R.string.clear_user_data_text);
            }
            mClearDataButton.setOnClickListener(this);
        }

        if (mAppsControlDisallowedBySystem) {
            mClearDataButton.setEnabled(false);
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
        mClearDataButton.setEnabled(false);
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
            Log.i(TAG, "Couldnt clear application user data for package:"+packageName);
            showDialogInner(DLG_CANNOT_CLEAR_DATA, 0);
        } else {
            mClearDataButton.setText(R.string.recompute_size);
        }
    }

    /*
     * Private method to handle clear message notification from observer when
     * the async operation from PackageManager is complete
     */
    private void processClearMsg(Message msg) {
        int result = msg.arg1;
        String packageName = mAppEntry.info.packageName;
        mClearDataButton.setText(R.string.clear_user_data_text);
        if (result == OP_SUCCESSFUL) {
            Log.i(TAG, "Cleared user data for package : "+packageName);
            mState.requestSize(mPackageName, mUserId);
        } else {
            mClearDataButton.setEnabled(true);
        }
    }

    private void refreshGrantedUriPermissions() {
        // Clear UI first (in case the activity has been resumed)
        removeUriPermissionsFromUi();

        // Gets all URI permissions from am.
        ActivityManager am = (ActivityManager) getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        List<UriPermission> perms =
                am.getGrantedUriPermissions(mAppEntry.info.packageName).getList();

        if (perms.isEmpty()) {
            mClearUriButton.setVisibility(View.GONE);
            return;
        }

        PackageManager pm = getActivity().getPackageManager();

        // Group number of URIs by app.
        Map<CharSequence, MutableInt> uriCounters = new TreeMap<>();
        for (UriPermission perm : perms) {
            String authority = perm.getUri().getAuthority();
            ProviderInfo provider = pm.resolveContentProvider(authority, 0);
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
        // Synchronously revoke the permissions.
        final ActivityManager am = (ActivityManager) getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        am.clearGrantedUriPermissions(mAppEntry.info.packageName);

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
                        .setTitle(getActivity().getText(R.string.clear_failed_dlg_title))
                        .setMessage(getActivity().getText(R.string.clear_failed_dlg_text))
                        .setNeutralButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mClearDataButton.setEnabled(false);
                                //force to recompute changed value
                                setIntentAndFinish(false, false);
                            }
                        })
                        .create();
        }
        return null;
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        if (packageName.equals(mAppEntry.info.packageName)) {
            refreshSizeInfo();
        }
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (getView() == null) {
                return;
            }
            switch (msg.what) {
                case MSG_CLEAR_USER_DATA:
                    processClearMsg(msg);
                    break;
                case MSG_CLEAR_CACHE:
                    // Refresh size info
                    mState.requestSize(mPackageName, mUserId);
                    break;
            }
        }
    };

    public static CharSequence getSummary(AppEntry appEntry, Context context) {
        if (appEntry.size == ApplicationsState.SIZE_INVALID
                || appEntry.size == ApplicationsState.SIZE_UNKNOWN) {
            return context.getText(R.string.computing_size);
        } else {
            CharSequence storageType = context.getString(
                    (appEntry.info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0
                    ? R.string.storage_type_external
                    : R.string.storage_type_internal);
            return context.getString(R.string.storage_summary_format,
                    getSize(appEntry, context), storageType);
        }
    }

    private static CharSequence getSize(AppEntry appEntry, Context context) {
        long size = appEntry.size;
        if (size == SIZE_INVALID) {
            return context.getText(R.string.invalid_size_value);
        }
        return Formatter.formatFileSize(context, size);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_APP_STORAGE;
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
