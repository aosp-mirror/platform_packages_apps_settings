/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deletionhelper;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.settings.deletionhelper.DownloadsDeletionPreference;
import com.android.settings.CollapsibleCheckboxPreferenceGroup;
import com.android.settings.PhotosDeletionPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.overlay.DeletionHelperFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.Session;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Settings screen for the deletion helper, which manually removes data which is not recently used.
 */
public class DeletionHelperFragment extends SettingsPreferenceFragment implements
        ApplicationsState.Callbacks, AppStateBaseBridge.Callback,
        Preference.OnPreferenceChangeListener, DeletionType.FreeableChangedListener,
        View.OnClickListener {
    public static final int CLEAR_DATA_RESULT = 1;
    public static final String FREED_BYTES_KEY = "freed";

    private static final String TAG = "DeletionHelperFragment";

    private static final String EXTRA_HAS_BRIDGE = "hasBridge";
    private static final String EXTRA_HAS_SIZES = "hasSizes";
    private static final String EXTRA_CHECKED_SET = "checkedSet";

    private static final String KEY_APPS_GROUP = "apps_group";
    private static final String KEY_PHOTOS_VIDEOS_PREFERENCE = "delete_photos";
    private static final String KEY_DOWNLOADS_PREFERENCE = "delete_downloads";

    private static final int DOWNLOADS_LOADER_ID = 1;

    private Button mCancel, mFree;
    private CollapsibleCheckboxPreferenceGroup mApps;
    private PhotosDeletionPreference mPhotoPreference;
    private DownloadsDeletionPreference mDownloadsPreference;

    private ApplicationsState mState;
    private Session mSession;
    private HashSet<String> mCheckedApplications;
    private AppStateUsageStatsBridge mDataUsageBridge;
    private ArrayList<AppEntry> mAppEntries;
    private boolean mHasReceivedAppEntries, mHasReceivedBridgeCallback, mFinishedLoading;
    private DeletionHelperFeatureProvider mProvider;
    private DeletionType mPhotoVideoDeletion;
    private DownloadsDeletionType mDownloadsDeletion;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAnimationAllowed(true);
        Application app = getActivity().getApplication();
        mState = ApplicationsState.getInstance(app);
        mSession = mState.newSession(this);
        mCheckedApplications = new HashSet<>();
        mDataUsageBridge = new AppStateUsageStatsBridge(getActivity(), mState, this);

        addPreferencesFromResource(R.xml.deletion_helper_list);
        mApps = (CollapsibleCheckboxPreferenceGroup) findPreference(KEY_APPS_GROUP);
        mPhotoPreference = (PhotosDeletionPreference) findPreference(KEY_PHOTOS_VIDEOS_PREFERENCE);
        mDownloadsPreference =
                (DownloadsDeletionPreference) findPreference(KEY_DOWNLOADS_PREFERENCE);
        mProvider =
                FeatureFactory.getFactory(app).getDeletionHelperFeatureProvider();
        if (mProvider != null) {
            mPhotoVideoDeletion = mProvider.createPhotoVideoDeletionType(getContext());
        }
        mDownloadsDeletion = new DownloadsDeletionType(getActivity());

        if (savedInstanceState != null) {
            mHasReceivedAppEntries =
                    savedInstanceState.getBoolean(EXTRA_HAS_SIZES, false);
            mHasReceivedBridgeCallback =
                    savedInstanceState.getBoolean(EXTRA_HAS_BRIDGE, false);
            mCheckedApplications =
                    (HashSet<String>) savedInstanceState.getSerializable(EXTRA_CHECKED_SET);
        }
    }

    private void initializeButtons(View v) {
        mCancel = (Button) v.findViewById(R.id.skip_button);
        mCancel.setText(R.string.cancel);
        mCancel.setOnClickListener(this);
        mCancel.setVisibility(View.VISIBLE);

        mFree = (Button) v.findViewById(R.id.next_button);
        mFree.setText(R.string.storage_menu_free);
        mFree.setOnClickListener(this);

        Button back = (Button) v.findViewById(R.id.back_button);
        back.setVisibility(View.GONE);
    }

    private void initializeDeletionPreferences() {
        if (mProvider == null) {
            getPreferenceScreen().removePreference(mPhotoPreference);
            mPhotoPreference = null;
        } else {
            mPhotoPreference.registerFreeableChangedListener(this);
            mPhotoPreference.registerDeletionService(mPhotoVideoDeletion);
        }

        mDownloadsPreference.registerFreeableChangedListener(this);
        mDownloadsPreference.registerDeletionService(mDownloadsDeletion);
        mApps.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        initializeButtons(v);
        initializeDeletionPreferences();
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
        mDataUsageBridge.resume();

        if (mPhotoVideoDeletion != null) {
            mPhotoVideoDeletion.onResume();
        }
        if (mDownloadsDeletion != null) {
            mDownloadsDeletion.onResume();
            getLoaderManager().initLoader(DOWNLOADS_LOADER_ID, new Bundle(), mDownloadsDeletion);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_HAS_SIZES, mHasReceivedAppEntries);
        outState.putBoolean(EXTRA_HAS_BRIDGE, mHasReceivedBridgeCallback);
        outState.putSerializable(EXTRA_CHECKED_SET, mCheckedApplications);
    }


    @Override
    public void onPause() {
        super.onPause();
        mDataUsageBridge.pause();
        mSession.pause();

        if (mPhotoVideoDeletion != null) {
            mPhotoVideoDeletion.onPause();
        }
        if (mDownloadsDeletion != null) {
            mDownloadsDeletion.onPause();
        }
    }

    private void rebuild() {
        // Only rebuild if we have the packages and their usage stats.
        if (!mHasReceivedBridgeCallback || !mHasReceivedAppEntries) {
            return;
        }

        final ArrayList<AppEntry> apps =
                mSession.rebuild(AppStateUsageStatsBridge.FILTER_USAGE_STATS,
                        ApplicationsState.SIZE_COMPARATOR);
        if (apps == null) return;
        mAppEntries = apps;
        refreshAppGroup(apps);

        // All applications should be filled in if we've received the sizes.
        // setLoading being called multiple times causes flickering, so we only do it once.
        if (mHasReceivedAppEntries && !mFinishedLoading) {
            mFinishedLoading = true;
            setLoading(false, true);
            getButtonBar().setVisibility(View.VISIBLE);
        }
        updateFreeButtonText();
    }

    private void updateFreeButtonText() {
        mFree.setText(String.format(getActivity().getString(R.string.deletion_helper_free_button),
                Formatter.formatFileSize(getActivity(), getTotalFreeableSpace())));
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        // No-op.
    }

    @Override
    public void onPackageListChanged() {
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        rebuild();
    }

    @Override
    public void onAllSizesComputed() {
        rebuild();
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
        mHasReceivedAppEntries = true;
        rebuild();
    }

    @Override
    public void onExtraInfoUpdated() {
        mHasReceivedBridgeCallback = true;
        rebuild();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STORAGE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean checked = (boolean) newValue;
        if (preference.getKey().equals(mApps.getKey())) {
            return toggleAllApps(checked);
        }

        String packageName = ((AppDeletionPreference) preference).getPackageName();
        if (checked) {
            mCheckedApplications.add(packageName);
        } else {
            mCheckedApplications.remove(packageName);

            // We remove the preference change listener to avoid toggling every app on and off.
            mApps.setOnPreferenceChangeListener(null);
            mApps.setChecked(false);
            mApps.setOnPreferenceChangeListener(this);
        }
        updateFreeButtonText();
        return true;
    }

    @Override
    public void onFreeableChanged(int numItems, long freeableBytes) {
        updateFreeButtonText();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mFree.getId()) {
            ConfirmDeletionDialog dialog =
                    ConfirmDeletionDialog.newInstance(getTotalFreeableSpace());
            // The 0 is a placeholder for an optional result code.
            dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), ConfirmDeletionDialog.TAG);
        } else {
            finishFragment();
        }
    }

    /**
     * Clears out the selected apps and data from the device and closes the fragment.
     */
    protected void clearData() {
        // This should be fine as long as there is only one extra deletion feature.
        // In the future, this should be done in an async queue in order to not
        // interfere with the simultaneous PackageDeletionTask.
        if (mPhotoPreference != null && mPhotoPreference.isChecked()) {
            mPhotoVideoDeletion.clearFreeableData();
        }

        ArraySet<String> apps = new ArraySet<>();
        for (AppEntry entry : mAppEntries) {
            if (mCheckedApplications.contains(entry.label)) {
                synchronized (entry) {
                    apps.add(entry.info.packageName);
                }
            }
        }
        // TODO: If needed, add an action on the callback.
        PackageDeletionTask task = new PackageDeletionTask(getActivity().getPackageManager(), apps,
                new PackageDeletionTask.Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                        Log.e(TAG, "An error occurred while uninstalling packages.");
                    }
                });
        Intent data = new Intent();
        data.putExtra(FREED_BYTES_KEY, getTotalFreeableSpace());
        getActivity().setResult(CLEAR_DATA_RESULT, data);

        task.run();
        finishFragment();
    }

    private long getTotalFreeableSpace() {
        long freeableSpace = 0;
        freeableSpace += getTotalAppsFreeableSpace(false);
        if (mPhotoPreference != null) {
            freeableSpace += mPhotoPreference.getFreeableBytes();
        }
        if (mDownloadsPreference != null) {
            freeableSpace += mDownloadsPreference.getFreeableBytes();
        }
        return freeableSpace;
    }

    private void refreshAppGroup(ArrayList<AppEntry> apps) {
        int entryCount = apps.size();
        cacheRemoveAllPrefs(mApps);
        for (int i = 0; i < entryCount; i++) {
            AppEntry entry = apps.get(i);
            final String packageName = entry.label;
            AppDeletionPreference preference =
                    (AppDeletionPreference) getCachedPreference(entry.label);
            if (preference == null) {
                preference = new AppDeletionPreference(getActivity(), entry, mState);
                preference.setKey(packageName);
                preference.setOnPreferenceChangeListener(this);
                mApps.addPreference(preference);
            }
            preference.setChecked(mCheckedApplications.contains(packageName));
            preference.setOrder(i);
        }
        removeCachedPrefs(mApps);
        updateAppsGroupText();
    }

    private long getTotalAppsFreeableSpace(boolean countUnchecked) {
        long freeableSpace = 0;
        if (mAppEntries != null) {
            for (int i = 0; i < mAppEntries.size(); i++) {
                final AppEntry entry = mAppEntries.get(i);
                long entrySize = mAppEntries.get(i).size;
                // If the entrySize is negative, it is either an unknown size or an error occurred.
                if ((countUnchecked ||
                        mCheckedApplications.contains(entry.label)) && entrySize > 0) {
                    freeableSpace += entrySize;
                }
            }
        }

        return freeableSpace;
    }

    private void updateAppsGroupText() {
        if (mAppEntries != null) {
            Activity app = getActivity();
            mApps.setTitle(app.getString(R.string.deletion_helper_apps_group_title,
                    mAppEntries.size()));
            mApps.setSummary(app.getString(R.string.deletion_helper_apps_group_summary,
                    Formatter.formatFileSize(app,
                            getTotalAppsFreeableSpace(true))));
        }
    }

    private boolean toggleAllApps(boolean checked) {
        for (AppEntry entry : mAppEntries) {
            final String packageName = entry.label;
            if (checked) {
                mCheckedApplications.add(packageName);
            } else {
                mCheckedApplications.remove(packageName);
            }
        }
        refreshAppGroup(mAppEntries);
        updateFreeButtonText();
        return true;
    }
}