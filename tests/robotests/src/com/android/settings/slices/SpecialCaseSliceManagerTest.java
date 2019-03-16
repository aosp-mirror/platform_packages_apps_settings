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

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

import androidx.slice.Slice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SpecialCaseSliceManagerTest {

    private final String FAKE_PARAMETER_KEY = "fake_parameter_key";
    private final String FAKE_PARAMETER_VALUE = "fake_value";

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        CustomSliceRegistry.sUriToSlice.clear();
        CustomSliceRegistry.sUriToSlice.put(FakeSliceable.URI, FakeSliceable.class);
    }

    @Test
    public void getSliceableFromUri_returnsCorrectObject() {
        final CustomSliceable sliceable = CustomSliceable.createInstance(
                mContext, CustomSliceRegistry.getSliceClassByUri(FakeSliceable.URI));

        assertThat(sliceable).isInstanceOf(FakeSliceable.class);
    }

    @Test
    public void getSliceableFromUriWithParameter_returnsCorrectObject() {
        final Uri parameterUri = FakeSliceable.URI
                .buildUpon()
                .clearQuery()
                .appendQueryParameter(FAKE_PARAMETER_KEY, FAKE_PARAMETER_VALUE)
                .build();

        final CustomSliceable sliceable = CustomSliceable.createInstance(
                mContext, CustomSliceRegistry.getSliceClassByUri(parameterUri));

        assertThat(sliceable).isInstanceOf(FakeSliceable.class);
    }

    @Test
    public void isValidUri_validUri_returnsTrue() {
        final boolean isValidUri = CustomSliceRegistry.isValidUri(FakeSliceable.URI);

        assertThat(isValidUri).isTrue();
    }

    @Test
    public void isValidUri_invalidUri_returnsFalse() {
        final boolean isValidUri = CustomSliceRegistry.isValidUri(null);

        assertThat(isValidUri).isFalse();
    }

    @Test
    public void isValidAction_validActions_returnsTrue() {
        final boolean isValidAction =
                CustomSliceRegistry.isValidAction(FakeSliceable.URI.toString());

        assertThat(isValidAction).isTrue();
    }

    @Test
    public void isValidAction_invalidAction_returnsFalse() {
        final boolean isValidAction = CustomSliceRegistry.isValidAction("action");

        assertThat(isValidAction).isFalse();
    }

    static class FakeSliceable implements CustomSliceable {

        static final String KEY = "magic key of khazad dum";

        static final Uri URI = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(KEY)
                .build();

        static final Slice SLICE = new Slice.Builder(URI).build();

        static boolean backingData = false;

        final Context mContext;

        public FakeSliceable(Context context) {
            mContext = context;
        }

        @Override
        public Slice getSlice() {
            return SLICE;
        }

        @Override
        public Uri getUri() {
            return URI;
        }

        @Override
        public void onNotifyChange(Intent intent) {
            backingData = !backingData;
        }

        @Override
        public IntentFilter getIntentFilter() {
            return new IntentFilter();
        }

        @Override
        public Intent getIntent() {
            return null;
        }
    }
}
