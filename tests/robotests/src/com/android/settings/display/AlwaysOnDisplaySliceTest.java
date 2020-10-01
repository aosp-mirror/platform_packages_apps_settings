/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.display;

import static android.provider.Settings.Secure.DOZE_ALWAYS_ON;
import static android.provider.Settings.Secure.DOZE_WAKE_DISPLAY_GESTURE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.AmbientDisplayConfiguration;
import android.net.Uri;
import android.provider.Settings;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.aware.AwareFeatureProvider;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class AlwaysOnDisplaySliceTest {

    private Context mContext;
    private AlwaysOnDisplaySlice mSlice;
    private FakeFeatureFactory mFeatureFactory;
    private AwareFeatureProvider mFeatureProvider;

    @Mock
    private AmbientDisplayConfiguration mConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFeatureProvider = mFeatureFactory.getAwareFeatureProvider();

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mSlice = new AlwaysOnDisplaySlice(mContext);
        ReflectionHelpers.setField(mSlice, "mConfig", mConfig);
    }

    @Test
    public void getUri_shouldReturnCorrectSliceUri() {
        final Uri uri = mSlice.getUri();

        assertThat(uri).isEqualTo(CustomSliceRegistry.ALWAYS_ON_SLICE_URI);
    }

    @Test
    public void getSlice_alwaysOnNotSupported_returnNull() {
        when(mConfig.alwaysOnAvailableForUser(anyInt())).thenReturn(false);

        final Slice slice = mSlice.getSlice();

        assertThat(slice).isNull();
    }

    @Test
    public void getSlice_alwaysOnSupported_showTitleSubtitle() {
        when(mConfig.alwaysOnAvailableForUser(anyInt())).thenReturn(true);

        final Slice slice = mSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);

        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.doze_always_on_title));
        assertThat(metadata.getSubtitle()).isEqualTo(
                mContext.getString(R.string.doze_always_on_summary));
    }

    @Test
    public void onNotifyChange_toggleOff_disableAoD() {
        final Intent intent = new Intent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, false);

        mSlice.onNotifyChange(intent);

        final ContentResolver resolver = mContext.getContentResolver();
        assertThat(Settings.Secure.getInt(resolver, DOZE_ALWAYS_ON, 0)).isEqualTo(0);
        assertThat(Settings.Secure.getInt(resolver, DOZE_WAKE_DISPLAY_GESTURE, 0)).isEqualTo(0);
    }

    @Test
    public void onNotifyChange_toggleOn_awareNotSupported_enableAoD() {
        final Intent intent = new Intent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        when(mFeatureProvider.isEnabled(mContext)).thenReturn(false);
        when(mFeatureProvider.isSupported(mContext)).thenReturn(false);

        mSlice.onNotifyChange(intent);

        final ContentResolver resolver = mContext.getContentResolver();
        assertThat(Settings.Secure.getInt(resolver, DOZE_ALWAYS_ON, 0)).isEqualTo(1);
        assertThat(Settings.Secure.getInt(resolver, DOZE_WAKE_DISPLAY_GESTURE, 0)).isEqualTo(0);
    }

    @Test
    public void onNotifyChange_toggleOn_awareDisabled_enableAoD() {
        final Intent intent = new Intent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        when(mFeatureProvider.isEnabled(mContext)).thenReturn(false);
        when(mFeatureProvider.isSupported(mContext)).thenReturn(true);

        mSlice.onNotifyChange(intent);

        final ContentResolver resolver = mContext.getContentResolver();
        assertThat(Settings.Secure.getInt(resolver, DOZE_ALWAYS_ON, 0)).isEqualTo(1);
        assertThat(Settings.Secure.getInt(resolver, DOZE_WAKE_DISPLAY_GESTURE, 0)).isEqualTo(0);
    }

    @Test
    public void onNotifyChange_toggleOn_awareSupported_enableAoD() {
        final Intent intent = new Intent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        when(mFeatureProvider.isEnabled(mContext)).thenReturn(true);
        when(mFeatureProvider.isSupported(mContext)).thenReturn(true);

        mSlice.onNotifyChange(intent);

        final ContentResolver resolver = mContext.getContentResolver();
        assertThat(Settings.Secure.getInt(resolver, DOZE_ALWAYS_ON, 0)).isEqualTo(1);
        assertThat(Settings.Secure.getInt(resolver, DOZE_WAKE_DISPLAY_GESTURE, 0)).isEqualTo(1);
    }
}
