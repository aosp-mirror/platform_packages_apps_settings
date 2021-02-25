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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.os.Looper;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.UsageProgressBarPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class StorageUsageProgressBarPreferenceControllerTest {

    private static final String FAKE_UUID = "95D9-B3A4";
    private static final long WAIT_TIMEOUT = 10_000L;
    private static final long FREE_BYTES = 123L;
    private static final long TOTAL_BYTES = 456L;
    private static final long USAGE_BYTES = TOTAL_BYTES - FREE_BYTES;

    private Context mContext;
    private FakeStorageUsageProgressBarPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private StorageStatsManager mStorageStatsManager;

    @Before
    public void setUp() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(StorageStatsManager.class)).thenReturn(mStorageStatsManager);
        mController = new FakeStorageUsageProgressBarPreferenceController(mContext, "key");
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        final UsageProgressBarPreference usageProgressBarPreference =
                new UsageProgressBarPreference(mContext);
        usageProgressBarPreference.setKey(mController.getPreferenceKey());
        mPreferenceScreen.addPreference(usageProgressBarPreference);
    }

    @Test
    public void setSelectedStorageEntry_primaryStorage_getPrimaryStorageBytes() throws IOException {
        final StorageEntry defaultInternalStorageEntry =
                StorageEntry.getDefaultInternalStorageEntry(mContext);
        when(mStorageStatsManager.getTotalBytes(defaultInternalStorageEntry.getFsUuid()))
                .thenReturn(TOTAL_BYTES);
        when(mStorageStatsManager.getFreeBytes(defaultInternalStorageEntry.getFsUuid()))
                .thenReturn(FREE_BYTES);
        mController.displayPreference(mPreferenceScreen);

        synchronized (mController.mLock) {
            mController.setSelectedStorageEntry(defaultInternalStorageEntry);
            mController.waitUpdateState(WAIT_TIMEOUT);
        }

        assertThat(mController.mUsedBytes).isEqualTo(USAGE_BYTES);
        assertThat(mController.mTotalBytes).isEqualTo(TOTAL_BYTES);
    }

    private class FakeStorageUsageProgressBarPreferenceController
            extends StorageUsageProgressBarPreferenceController {
        private final Object mLock = new Object();

        FakeStorageUsageProgressBarPreferenceController(Context context, String key) {
            super(context, key);
        }

        @Override
        public void updateState(Preference preference) {
            super.updateState(preference);
            try {
                mLock.notifyAll();
            } catch (IllegalMonitorStateException e) {
                // Catch it for displayPreference to prevent exception by object not locked by
                // thread before notify. Do nothing.
            }
        }

        public void waitUpdateState(long timeout) {
            try {
                mLock.wait(timeout);
            } catch (InterruptedException e) {
                // Do nothing.
            }
        }
    }
}

