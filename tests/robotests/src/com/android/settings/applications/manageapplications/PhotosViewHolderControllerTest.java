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
 * limitations under the License.
 */

package com.android.settings.applications.manageapplications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;
import android.view.View;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.android.settingslib.applications.StorageStatsSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PhotosViewHolderControllerTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private StorageStatsSource mSource;

    private Context mContext;
    private PhotosViewHolderController mController;
    private ApplicationViewHolder mHolder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final String fsUuid = new VolumeInfo("id", 0, null, "id").fsUuid;
        mController = new PhotosViewHolderController(mContext, mSource, fsUuid, UserHandle.of(-1));

        final View view = ApplicationViewHolder.newView(new FrameLayout(mContext));
        mHolder = new ApplicationViewHolder(view);
    }

    @Test
    public void storageShouldBeZeroBytesIfQueriedBeforeStorageQueryFinishes() {
        mController.setupView(mHolder);

        assertThat(mHolder.mSummary.getText().toString())
                .isEqualTo(Formatter.formatFileSize(mContext, 0));
    }

    @Test
    public void storageShouldRepresentStorageStatsQuery() throws Exception {
        when(mSource.getExternalStorageStats(nullable(String.class), nullable(UserHandle.class)))
                .thenReturn(new StorageStatsSource.ExternalStorageStats(1, 0, 1, 10, 0));

        mController.queryStats();
        mController.setupView(mHolder);

        assertThat(mHolder.mSummary.getText().toString())
                .isEqualTo(Formatter.formatFileSize(mContext, 11));
    }

    @Test
    public void clickingShouldIntentIntoFilesApp() {
        mController.onClick(mFragment);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment).startActivity(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();

        assertThat(intent.getType()).isEqualTo("image/*");
        assertThat(intent.getAction()).isEqualTo(android.content.Intent.ACTION_VIEW);
        assertThat(intent.getBooleanExtra(Intent.EXTRA_FROM_STORAGE, false)).isTrue();
    }
}
