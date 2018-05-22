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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;
import com.android.settings.testutils.shadow.ShadowLocalBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowLocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.graphics.drawable.IconCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowLocalBluetoothAdapter.class, ShadowLocalBluetoothProfileManager.class})
public class BluetoothSliceBuilderTest {

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        // Prevent crash in SliceMetadata.
        Resources resources = spy(mContext.getResources());
        doReturn(60).when(resources).getDimensionPixelSize(anyInt());
        doReturn(resources).when(mContext).getResources();

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void getBluetoothSlice_correctSliceContent() {
        final Slice BluetoothSlice = BluetoothSliceBuilder.getSlice(mContext);
        final SliceMetadata metadata = SliceMetadata.from(mContext, BluetoothSlice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_bluetooth);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());

        final List<SliceItem> sliceItems = BluetoothSlice.getItems();
        SliceTester.assertTitle(sliceItems, mContext.getString(R.string.bluetooth_settings_title));
    }

    @Test
    public void handleUriChange_updatesBluetooth() {
        final LocalBluetoothAdapter adapter = LocalBluetoothManager.getInstance(mContext,
                null /* callback */).getBluetoothAdapter();
        final Intent intent = new Intent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        adapter.setBluetoothEnabled(false /* enabled */);

        BluetoothSliceBuilder.handleUriChange(mContext, intent);

        assertThat(adapter.isEnabled()).isTrue();
    }
}