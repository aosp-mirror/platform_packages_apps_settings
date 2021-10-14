/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.Looper;
import android.os.storage.StorageManager;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.SettingsSpinnerPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class StorageSelectionPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    private StorageManager mStorageManager;
    private StorageSelectionPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = ApplicationProvider.getApplicationContext();
        mStorageManager = mContext.getSystemService(StorageManager.class);
        mController = new StorageSelectionPreferenceController(mContext, PREFERENCE_KEY);

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen preferenceScreen =
                preferenceManager.createPreferenceScreen(mContext);
        final SettingsSpinnerPreference spinnerPreference = new SettingsSpinnerPreference(mContext);
        spinnerPreference.setKey(PREFERENCE_KEY);
        preferenceScreen.addPreference(spinnerPreference);
        mController.displayPreference(preferenceScreen);
    }

    @Test
    public void setStorageEntries_fromStorageManager_correctAdapterItems() {
        final List<StorageEntry> storageEntries = mStorageManager.getVolumes().stream()
                .map(volumeInfo -> new StorageEntry(mContext, volumeInfo))
                .collect(Collectors.toList());

        mController.setStorageEntries(storageEntries);

        final int adapterItemCount = mController.mStorageAdapter.getCount();
        assertThat(adapterItemCount).isEqualTo(storageEntries.size());
        for (int i = 0; i < adapterItemCount; i++) {
            assertThat(storageEntries.get(i).getDescription())
                    .isEqualTo(mController.mStorageAdapter.getItem(i).getDescription());
        }
    }

    @Test
    public void setSelectedStorageEntry_primaryStorage_correctSelectedAdapterItem() {
        final StorageEntry primaryStorageEntry =
                StorageEntry.getDefaultInternalStorageEntry(mContext);
        mController.setStorageEntries(mStorageManager.getVolumes().stream()
                .map(volumeInfo -> new StorageEntry(mContext, volumeInfo))
                .collect(Collectors.toList()));

        mController.setSelectedStorageEntry(primaryStorageEntry);

        assertThat((StorageEntry) mController.mSpinnerPreference.getSelectedItem())
                .isEqualTo(primaryStorageEntry);
    }

    @Test
    public void setStorageEntries_1StorageEntry_preferenceInvisible() {
        final List<StorageEntry> storageEntries = new ArrayList<>();
        storageEntries.add(mock(StorageEntry.class));

        mController.setStorageEntries(storageEntries);

        assertThat(mController.mSpinnerPreference.isVisible()).isFalse();
    }

    @Test
    public void setStorageEntries_2StorageEntries_preferenceVisible() {
        final List<StorageEntry> storageEntries = new ArrayList<>();
        storageEntries.add(mock(StorageEntry.class));
        storageEntries.add(mock(StorageEntry.class));

        mController.setStorageEntries(storageEntries);

        assertThat(mController.mSpinnerPreference.isVisible()).isTrue();
    }
}

