/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import static com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice.DEFERRED_TIME_DAYS;
import static com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice.PREF;
import static com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice.PREF_KEY_SETUP_TIME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ContextualAdaptiveSleepSliceTest {

    private static final String pkgName = "adaptive_sleep";
    private Context mContext;
    private ContextualAdaptiveSleepSlice mContextualAdaptiveSleepSlice;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private SharedPreferences mSharedPreferences;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mContext = spy(RuntimeEnvironment.application);
        mContextualAdaptiveSleepSlice = spy(new ContextualAdaptiveSleepSlice(mContext));

        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mSharedPreferences).when(mContext).getSharedPreferences(eq(PREF), anyInt());
        doReturn(true).when(mContextualAdaptiveSleepSlice).isSettingsAvailable();
        doReturn(pkgName).when(mPackageManager).getAttentionServicePackageName();
        doReturn(-DEFERRED_TIME_DAYS).when(mSharedPreferences).getLong(eq(PREF_KEY_SETUP_TIME),
                anyLong());
    }

    @Test
    public void getUri_shouldReturnContextualAdaptiveSleepSliceUri() {
        final Uri uri = mContextualAdaptiveSleepSlice.getUri();

        assertThat(uri).isEqualTo(CustomSliceRegistry.CONTEXTUAL_ADAPTIVE_SLEEP_URI);
    }

    @Test
    public void getSlice_ShowIfFeatureIsAvailable() {
        final Slice slice = mContextualAdaptiveSleepSlice.getSlice();

        assertThat(slice).isNotNull();
    }

    @Test
    public void getSlice_DoNotShowIfFeatureIsUnavailable() {
        doReturn(false).when(mContextualAdaptiveSleepSlice).isSettingsAvailable();

        final Slice slice = mContextualAdaptiveSleepSlice.getSlice();

        assertThat(slice).isNull();
    }

    @Test
    public void getSlice_ShowIfNotRecentlySetup() {
        final Slice slice = mContextualAdaptiveSleepSlice.getSlice();

        assertThat(slice).isNotNull();
    }

    @Test
    public void getSlice_DoNotShowIfRecentlySetup() {
        doReturn(System.currentTimeMillis()).when(mSharedPreferences).getLong(
                eq(PREF_KEY_SETUP_TIME), anyLong());

        final Slice slice = mContextualAdaptiveSleepSlice.getSlice();

        assertThat(slice).isNull();
    }
}
