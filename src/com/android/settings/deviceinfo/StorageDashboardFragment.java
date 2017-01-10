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

import android.content.Context;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.SearchIndexableResource;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.StorageSummaryDonutPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.FooterPreference;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageDashboardFragment extends DashboardFragment {
    private static final String TAG = "StorageDashboardFrag";

    private VolumeInfo mVolume;
    private long mTotalSize;

    private StorageSummaryDonutPreferenceController mSummaryController;

    private boolean isVolumeValid() {
        return (mVolume != null) && (mVolume.getType() == VolumeInfo.TYPE_PRIVATE)
                && mVolume.isMountedReadable();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getActivity();

        // Initialize the storage sizes that we can quickly calc.
        StorageManager sm = context.getSystemService(StorageManager.class);
        String volumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
        mVolume = sm.findVolumeById(volumeId);
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }

        final long sharedDataSize = mVolume.getPath().getTotalSpace();
        mTotalSize = sm.getPrimaryStorageSize();
        long systemSize = mTotalSize - sharedDataSize;

        if (mTotalSize <= 0) {
            mTotalSize = sharedDataSize;
            systemSize = 0;
        }

        final long usedBytes = mTotalSize - mVolume.getPath().getFreeSpace();
        mSummaryController.updateBytes(usedBytes, mTotalSize);

        // Initialize the footer preference to go to the smart storage management.
        final FooterPreference pref = mFooterPreferenceMixin.createFooterPreference();
        pref.setTitle(R.string.storage_menu_manage);
        pref.setFragment("com.android.settings.deletionhelper.AutomaticStorageManagerSettings");
        pref.setIcon(R.drawable.ic_settings_storage);
        pref.setEnabled(true);


    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_STORAGE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_STORAGE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_dashboard_fragment;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        mSummaryController = new StorageSummaryDonutPreferenceController(context);
        controllers.add(mSummaryController);
        controllers.add(new ManageStoragePreferenceController(context));
        controllers.add(new StorageItemPreferenceController(context, "pref_photos_videos"));
        controllers.add(new StorageItemPreferenceController(context, "pref_music_audio"));
        controllers.add(new StorageItemPreferenceController(context, "pref_games"));
        controllers.add(new StorageItemPreferenceController(context, "pref_other_apps"));
        controllers.add(new StorageItemPreferenceController(context, "pref_system"));
        controllers.add(new StorageItemPreferenceController(context, "pref_files"));
        return controllers;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        // TODO: Add loader to load the storage sizes for the StorageItemPreferenceControllers.
        return root;
    }
    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.storage_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final ManageStoragePreferenceController controller =
                            new ManageStoragePreferenceController(context);
                    final List<String> keys = new ArrayList<>();
                    controller.updateNonIndexableKeys(keys);
                    return keys;
                }
            };
}
