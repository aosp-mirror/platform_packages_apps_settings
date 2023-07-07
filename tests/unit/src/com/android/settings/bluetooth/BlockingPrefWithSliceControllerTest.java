/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.slice.Slice;
import androidx.slice.SliceViewManager;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.bluetooth.BlockingPrefWithSliceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class BlockingPrefWithSliceControllerTest {
    private static final String KEY = "bt_device_slice_category";
    private static final String TEST_URI_AUTHORITY = "com.android.authority.test";
    private static final String TEST_EXTRA_INTENT = "EXTRA_INTENT";
    private static final String TEST_EXTRA_PENDING_INTENT = "EXTRA_PENDING_INTENT";
    private static final String TEST_INTENT_ACTION = "test";
    private static final String TEST_PENDING_INTENT_ACTION = "test";
    private static final String TEST_SLICE_TITLE = "Test Title";
    private static final String TEST_SLICE_SUBTITLE = "Test Subtitle";
    private static final String FAKE_ACTION = "fake_action";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private LiveData<Slice> mLiveData;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    private Context mContext;
    private BlockingPrefWithSliceController mController;
    private Uri mUri;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = spy(new BlockingPrefWithSliceController(mContext, KEY));
        mController.mLiveData = mLiveData;
        mController.mExtraIntent = TEST_EXTRA_INTENT;
        mController.mExtraPendingIntent = TEST_EXTRA_PENDING_INTENT;
        mController.mSliceIntentAction = TEST_INTENT_ACTION;
        mController.mSlicePendingIntentAction = TEST_PENDING_INTENT_ACTION;
        mController.mPreferenceCategory = mPreferenceCategory;
        mUri = Uri.EMPTY;
    }

    @Test
    public void isAvailable_uriNull_returnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void isAvailable_uriNotNull_returnTrue() {
        mController.setSliceUri(mUri);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onStart_registerObserver() {
        mController.onStart();

        verify(mLiveData).observeForever(mController);
    }

    @Test
    public void onStop_unregisterObserver() {
        mController.onStop();

        verify(mLiveData).removeObserver(mController);
    }

    @Test
    public void onChanged_nullSlice_updateSlice() {
        mController.onChanged(null);

        verify(mController).updatePreferenceFromSlice(null);
    }

    @Test
    public void onChanged_testSlice_updateSlice() {
        mController.onChanged(buildTestSlice());

        verify(mController.mPreferenceCategory).addPreference(any());
    }

    private Slice buildTestSlice() {
        Uri uri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(TEST_URI_AUTHORITY)
                        .build();
        SliceViewManager.getInstance(mContext).pinSlice(uri);
        ListBuilder listBuilder = new ListBuilder(mContext, uri, ListBuilder.INFINITY);
        IconCompat icon = mock(IconCompat.class);
        listBuilder.addRow(
                new RowBuilder()
                        .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                        .setTitle(TEST_SLICE_TITLE)
                        .setSubtitle(TEST_SLICE_SUBTITLE)
                        .setPrimaryAction(
                                SliceAction.create(
                                        PendingIntent.getActivity(
                                                mContext,
                                                /*requestCode= */ 0,
                                                new Intent(FAKE_ACTION),
                                                PendingIntent.FLAG_UPDATE_CURRENT
                                                        | PendingIntent.FLAG_IMMUTABLE),
                                        icon,
                                        ListBuilder.ICON_IMAGE,
                                        "")));
        return listBuilder.build();
    }
}
