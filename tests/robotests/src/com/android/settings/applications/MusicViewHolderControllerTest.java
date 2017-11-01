/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MusicViewHolderControllerTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private StorageVolumeProvider mSvp;
    @Mock
    private StorageStatsSource mSource;

    private Context mContext;
    private MusicViewHolderController mController;
    private VolumeInfo mVolume;
    private AppViewHolder mHolder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mVolume = new VolumeInfo("id", 0, null, "id");
        mController = new MusicViewHolderController(mContext, mSource, mVolume.fsUuid,
                new UserHandle(0));

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mHolder = AppViewHolder.createOrRecycle(inflater, null);
    }

    @Test
    public void storageShouldBeZeroBytesIfQueriedBeforeStorageQueryFinishes() {
        mController.setupView(mHolder);

        assertThat(mHolder.summary.getText().toString()).isEqualTo("0.00B");
    }

    @Test
    public void storageShouldRepresentStorageStatsQuery() throws Exception {
        when(mSource.getExternalStorageStats(nullable(String.class), nullable(UserHandle.class))).thenReturn(
                new StorageStatsSource.ExternalStorageStats(1, 1, 0, 0, 0));

        mController.queryStats();
        mController.setupView(mHolder);

        assertThat(mHolder.summary.getText().toString()).isEqualTo("1.00B");
    }

    @Test
    public void clickingShouldIntentIntoFilesApp() {
        mController.onClick(mFragment);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment).startActivity(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(DocumentsContract.buildRootUri(
                "com.android.providers.media.documents",
                "audio_root"));
        assertThat(intent.getType()).isEqualTo(DocumentsContract.Root.MIME_TYPE_ITEM);
    }
}
