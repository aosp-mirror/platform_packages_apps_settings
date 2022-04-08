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
 * limitations under the License
 */
package com.android.settings.deviceinfo;

import static com.android.settings.utils.FileSizeFormatter.MEGABYTE_IN_BYTES;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ProgressBar;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class StorageItemPreferenceTest {

    private Context mContext;
    private StorageItemPreference mPreference;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mPreference = new StorageItemPreference(mContext);
    }

    @Test
    public void testBeforeLoad() {
        assertThat(mPreference.getSummary())
            .isEqualTo(mContext.getString(R.string.memory_calculating_size));
    }

    @Test
    public void testAfterLoad() {
        mPreference.setStorageSize(MEGABYTE_IN_BYTES * 10, MEGABYTE_IN_BYTES * 100);
        assertThat(mPreference.getSummary()).isEqualTo("0.01 GB");
    }

    @Test
    public void testProgressBarPercentageSet() {
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(R.layout.storage_item, null));
        final ProgressBar progressBar = holder.itemView.findViewById(android.R.id.progress);

        mPreference.onBindViewHolder(holder);
        mPreference.setStorageSize(MEGABYTE_IN_BYTES, MEGABYTE_IN_BYTES * 10);

        assertThat(progressBar).isNotNull();
        assertThat(progressBar.getProgress()).isEqualTo(10);
    }
}
