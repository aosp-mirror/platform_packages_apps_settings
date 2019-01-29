/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BluetoothSliceBuilderTest {

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void getBluetoothSlice_correctSliceContent() {
        final Slice BluetoothSlice = BluetoothSliceBuilder.getSlice(mContext);

        final SliceMetadata metadata = SliceMetadata.from(mContext, BluetoothSlice);
        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.bluetooth_settings_title));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());
    }

    @Test
    public void handleUriChange_updatesBluetooth() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        final Intent intent = new Intent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        adapter.disable()/* enabled */;

        BluetoothSliceBuilder.handleUriChange(mContext, intent);

        assertThat(adapter.isEnabled()).isTrue();
    }
}
