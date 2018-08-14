/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;

import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceViewManager;
import androidx.slice.SliceMetadata;

@RunWith(SettingsRobolectricTestRunner.class)
public class DeviceIndexUpdateJobServiceTest {
        private static final Uri BASE_URI = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .build();

    private Activity mActivity;
    private DeviceIndexUpdateJobService mJob;
    private SliceViewManager mSliceManager;

    @Before
    public void setup() {
        FakeFeatureFactory.setupForTest();
        mActivity = spy(Robolectric.buildActivity(Activity.class).create().visible().get());
        mJob = spy(new DeviceIndexUpdateJobService());
        mSliceManager = mock(SliceViewManager.class);

        doReturn(mActivity.getPackageName()).when(mJob).getPackageName();
        doReturn(mSliceManager).when(mJob).getSliceViewManager();
        doNothing().when(mJob).jobFinished(null, false);
    }

    @Test
    public void testGetsSlices() {
        setSlices();

        mJob.updateIndex(null);
        verify(mSliceManager).getSliceDescendants(eq(BASE_URI));
    }

    @Test
    public void testIndexesSlices() {
        setSlices(genSlice("path1"), genSlice("path2"));

        mJob.mRunningJob = true;
        mJob.updateIndex(null);
        verify(mSliceManager).getSliceDescendants(eq(BASE_URI));

        DeviceIndexFeatureProvider indexFeatureProvider = FakeFeatureFactory.getFactory(mActivity)
                .getDeviceIndexFeatureProvider();
        verify(indexFeatureProvider, times(2)).index(any(), any(), any(), any(), any());
    }

    @Test
    public void testDoNotIndexWithoutTitle() {
        Slice testSlice = genSlice("path2");
        setSlices(genSlice("path1"), testSlice);
        doReturn(null).when(mJob).findTitle(testSlice, mJob.getMetadata(testSlice));

        mJob.mRunningJob = true;
        mJob.updateIndex(null);
        verify(mSliceManager).getSliceDescendants(eq(BASE_URI));

        DeviceIndexFeatureProvider indexFeatureProvider = FakeFeatureFactory.getFactory(mActivity)
                .getDeviceIndexFeatureProvider();
        verify(indexFeatureProvider, times(1)).index(any(), any(), any(), any(), any());
    }

    @Test
    public void testStopIndexing() {
        Slice testSlice = genSlice("path1");
        setSlices(testSlice, genSlice("path2"));
        mJob.mRunningJob = true;

        doAnswer(invocation -> {
            // Stop running after the first iteration
            mJob.mRunningJob = false;
            return testSlice;
        }).when(mJob).bindSliceSynchronous(mSliceManager, testSlice.getUri());

        mJob.updateIndex(null);
        verify(mSliceManager).getSliceDescendants(eq(BASE_URI));

        DeviceIndexFeatureProvider indexFeatureProvider = FakeFeatureFactory.getFactory(mActivity)
                .getDeviceIndexFeatureProvider();
        verify(indexFeatureProvider).clearIndex(any());
        verify(indexFeatureProvider, times(1)).index(any(), any(), any(), any(), any());
    }

    private Slice genSlice(String path) {
        return new Slice.Builder(BASE_URI.buildUpon().path(path).build()).build();
    }

    private void setSlices(Slice... slice) {
        List<Uri> mUris = new ArrayList<>();
        for (Slice slouse : slice) {
            SliceMetadata m = mock(SliceMetadata.class);
            mUris.add(slouse.getUri());
            doReturn(slouse).when(mJob).bindSliceSynchronous(mSliceManager, slouse.getUri());
            doReturn(m).when(mJob).getMetadata(slouse);
            doReturn(slouse.getUri().getPath()).when(mJob).findTitle(slouse, m);
        }
        when(mSliceManager.getSliceDescendants(BASE_URI)).thenReturn(mUris);
    }

}
