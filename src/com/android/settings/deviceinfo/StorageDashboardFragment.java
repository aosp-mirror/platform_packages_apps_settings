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

package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.SearchIndexableResource;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.deviceinfo.storage.AutomaticStorageManagementSwitchPreferenceController;
import com.android.settings.deviceinfo.storage.CachedStorageValuesHelper;
import com.android.settings.deviceinfo.storage.SecondaryUserController;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settings.deviceinfo.storage.VolumeSizesLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class StorageDashboardFragment extends DashboardFragment
        implements
        LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.AppsStorageResult>> {
    private static final String TAG = "StorageDashboardFrag";
    private static final String SUMMARY_PREF_KEY = "storage_summary";
    private static final int STORAGE_JOB_ID = 0;
    private static final int ICON_JOB_ID = 1;
    private static final int VOLUME_SIZE_JOB_ID = 2;

    private VolumeInfo mVolume;
    private PrivateStorageInfo mStorageInfo;
    private SparseArray<StorageAsyncLoader.AppsStorageResult> mAppsResult;
    private CachedStorageValuesHelper mCachedStorageValuesHelper;

    private StorageItemPreferenceController mPreferenceController;
    private PrivateVolumeOptionMenuController mOptionMenuController;
    private List<AbstractPreferenceController> mSecondaryUsers;
    private boolean mPersonalOnly;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Initialize the storage sizes that we can quickly calc.
        final Activity activity = getActivity();
        StorageManager sm = activity.getSystemService(StorageManager.class);
        mVolume = Utils.maybeInitializeVolume(sm, getArguments());
        mPersonalOnly = getArguments().getInt(ProfileSelectFragment.EXTRA_PROFILE)
                == ProfileSelectFragment.ProfileType.PERSONAL;
        if (mVolume == null) {
            activity.finish();
            return;
        }
        initializeOptionsMenu(activity);
        if (mPersonalOnly) {
            final Preference summary = getPreferenceScreen().findPreference(SUMMARY_PREF_KEY);
            if (summary != null) {
                summary.setVisible(false);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AutomaticStorageManagementSwitchPreferenceController.class).setFragmentManager(
                getFragmentManager());
    }

    @VisibleForTesting
    void initializeOptionsMenu(Activity activity) {
        mOptionMenuController = new PrivateVolumeOptionMenuController(
                activity, mVolume, activity.getPackageManager());
        getSettingsLifecycle().addObserver(mOptionMenuController);
        setHasOptionsMenu(true);
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        initializeCacheProvider();
        maybeSetLoading(isQuotaSupported());

        final Activity activity = getActivity();
        EntityHeaderController.newInstance(activity, this /*fragment*/,
                null /* header view */)
                .setRecyclerView(getListView(), getSettingsLifecycle())
                .styleActionBar(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(STORAGE_JOB_ID, Bundle.EMPTY, this);
        getLoaderManager()
                .restartLoader(VOLUME_SIZE_JOB_ID, Bundle.EMPTY, new VolumeSizeCallbacks());
        getLoaderManager().restartLoader(ICON_JOB_ID, Bundle.EMPTY, new IconLoaderCallbacks());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_storage_dashboard;
    }

    private void onReceivedSizes() {
        boolean stopLoading = false;
        if (mStorageInfo != null) {
            long privateUsedBytes = mStorageInfo.totalBytes - mStorageInfo.freeBytes;
            mPreferenceController.setVolume(mVolume);
            mPreferenceController.setUsedSize(privateUsedBytes);
            mPreferenceController.setTotalSize(mStorageInfo.totalBytes);
            for (int i = 0, size = mSecondaryUsers.size(); i < size; i++) {
                AbstractPreferenceController controller = mSecondaryUsers.get(i);
                if (controller instanceof SecondaryUserController) {
                    SecondaryUserController userController = (SecondaryUserController) controller;
                    userController.setTotalSize(mStorageInfo.totalBytes);
                }
            }
            stopLoading = true;

        }

        if (mAppsResult != null) {
            mPreferenceController.onLoadFinished(mAppsResult, UserHandle.myUserId());
            updateSecondaryUserControllers(mSecondaryUsers, mAppsResult);
            stopLoading = true;
        }

        // setLoading always causes a flicker, so let's avoid doing it.
        if (stopLoading) {
            if (getView().findViewById(R.id.loading_container).getVisibility() == View.VISIBLE) {
                setLoading(false, true);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_STORAGE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_dashboard_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        StorageManager sm = context.getSystemService(StorageManager.class);
        mPreferenceController = new StorageItemPreferenceController(context, this,
                mVolume, new StorageManagerVolumeProvider(sm));
        controllers.add(mPreferenceController);

        final UserManager userManager = context.getSystemService(UserManager.class);
        mSecondaryUsers = SecondaryUserController.getSecondaryUserControllers(context, userManager);
        controllers.addAll(mSecondaryUsers);

        return controllers;
    }

    @VisibleForTesting
    protected void setVolume(VolumeInfo info) {
        mVolume = info;
    }

    /**
     * Updates the secondary user controller sizes.
     */
    private void updateSecondaryUserControllers(List<AbstractPreferenceController> controllers,
            SparseArray<StorageAsyncLoader.AppsStorageResult> stats) {
        for (int i = 0, size = controllers.size(); i < size; i++) {
            AbstractPreferenceController controller = controllers.get(i);
            if (controller instanceof StorageAsyncLoader.ResultHandler) {
                StorageAsyncLoader.ResultHandler userController =
                        (StorageAsyncLoader.ResultHandler) controller;
                userController.handleResult(stats);
            }
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.storage_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    final StorageManager sm = context.getSystemService(StorageManager.class);
                    final UserManager userManager = context.getSystemService(UserManager.class);
                    final List<AbstractPreferenceController> controllers = new ArrayList<>();
                    controllers.add(new StorageItemPreferenceController(context, null /* host */,
                            null /* volume */, new StorageManagerVolumeProvider(sm)));
                    controllers.addAll(SecondaryUserController.getSecondaryUserControllers(
                            context, userManager));
                    return controllers;
                }

            };

    @Override
    public Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> onCreateLoader(int id,
            Bundle args) {
        final Context context = getContext();
        return new StorageAsyncLoader(context, context.getSystemService(UserManager.class),
                mVolume.fsUuid,
                new StorageStatsSource(context),
                context.getPackageManager());
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader,
            SparseArray<StorageAsyncLoader.AppsStorageResult> data) {
        mAppsResult = data;
        maybeCacheFreshValues();
        onReceivedSizes();
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader) {
    }

    @VisibleForTesting
    public void setCachedStorageValuesHelper(CachedStorageValuesHelper helper) {
        mCachedStorageValuesHelper = helper;
    }

    @VisibleForTesting
    public PrivateStorageInfo getPrivateStorageInfo() {
        return mStorageInfo;
    }

    @VisibleForTesting
    public void setPrivateStorageInfo(PrivateStorageInfo info) {
        mStorageInfo = info;
    }

    @VisibleForTesting
    public SparseArray<StorageAsyncLoader.AppsStorageResult> getAppsStorageResult() {
        return mAppsResult;
    }

    @VisibleForTesting
    public void setAppsStorageResult(SparseArray<StorageAsyncLoader.AppsStorageResult> info) {
        mAppsResult = info;
    }

    @VisibleForTesting
    public void initializeCachedValues() {
        PrivateStorageInfo info = mCachedStorageValuesHelper.getCachedPrivateStorageInfo();
        SparseArray<StorageAsyncLoader.AppsStorageResult> loaderResult =
                mCachedStorageValuesHelper.getCachedAppsStorageResult();
        if (info == null || loaderResult == null) {
            return;
        }

        mStorageInfo = info;
        mAppsResult = loaderResult;
    }

    @VisibleForTesting
    public void maybeSetLoading(boolean isQuotaSupported) {
        // If we have fast stats, we load until both have loaded.
        // If we have slow stats, we load when we get the total volume sizes.
        if ((isQuotaSupported && (mStorageInfo == null || mAppsResult == null)) ||
                (!isQuotaSupported && mStorageInfo == null)) {
            setLoading(true /* loading */, false /* animate */);
        }
    }

    private void initializeCacheProvider() {
        mCachedStorageValuesHelper =
                new CachedStorageValuesHelper(getContext(), UserHandle.myUserId());
        initializeCachedValues();
        onReceivedSizes();
    }

    private void maybeCacheFreshValues() {
        if (mStorageInfo != null && mAppsResult != null) {
            mCachedStorageValuesHelper.cacheResult(
                    mStorageInfo, mAppsResult.get(UserHandle.myUserId()));
        }
    }

    private boolean isQuotaSupported() {
        final StorageStatsManager stats = getActivity().getSystemService(StorageStatsManager.class);
        return stats.isQuotaSupported(mVolume.fsUuid);
    }

    /**
     * IconLoaderCallbacks exists because StorageDashboardFragment already implements
     * LoaderCallbacks for a different type.
     */
    public final class IconLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<SparseArray<Drawable>> {
        @Override
        public Loader<SparseArray<Drawable>> onCreateLoader(int id, Bundle args) {
            return new UserIconLoader(
                    getContext(),
                    () -> UserIconLoader.loadUserIconsWithContext(getContext()));
        }

        @Override
        public void onLoadFinished(
                Loader<SparseArray<Drawable>> loader, SparseArray<Drawable> data) {
            mSecondaryUsers
                    .stream()
                    .filter(controller -> controller instanceof UserIconLoader.UserIconHandler)
                    .forEach(
                            controller ->
                                    ((UserIconLoader.UserIconHandler) controller)
                                            .handleUserIcons(data));
        }

        @Override
        public void onLoaderReset(Loader<SparseArray<Drawable>> loader) {
        }
    }

    public final class VolumeSizeCallbacks
            implements LoaderManager.LoaderCallbacks<PrivateStorageInfo> {
        @Override
        public Loader<PrivateStorageInfo> onCreateLoader(int id, Bundle args) {
            Context context = getContext();
            StorageManager sm = context.getSystemService(StorageManager.class);
            StorageManagerVolumeProvider smvp = new StorageManagerVolumeProvider(sm);
            final StorageStatsManager stats = context.getSystemService(StorageStatsManager.class);
            return new VolumeSizesLoader(context, smvp, stats, mVolume);
        }

        @Override
        public void onLoaderReset(Loader<PrivateStorageInfo> loader) {
        }

        @Override
        public void onLoadFinished(
                Loader<PrivateStorageInfo> loader, PrivateStorageInfo privateStorageInfo) {
            if (privateStorageInfo == null) {
                getActivity().finish();
                return;
            }

            mStorageInfo = privateStorageInfo;
            maybeCacheFreshValues();
            onReceivedSizes();
        }
    }
}
