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
 */

package com.android.settings.panel;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.lifecycle.LiveData;
import androidx.slice.Slice;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PanelSlicesAdapterTest {

    private static final Uri DATA_URI = CustomSliceRegistry.DATA_USAGE_SLICE_URI;

    private Context mContext;
    private PanelFragment mPanelFragment;
    private PanelFeatureProvider mPanelFeatureProvider;
    private FakeFeatureFactory mFakeFeatureFactory;
    private FakePanelContent mFakePanelContent;
    private List<LiveData<Slice>> mData = new ArrayList<>();

    @Mock
    private LiveData<Slice> mLiveData;

    private Slice mSlice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mPanelFeatureProvider = spy(new PanelFeatureProviderImpl());
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.panelFeatureProvider = mPanelFeatureProvider;
        mFakePanelContent = new FakePanelContent();
        doReturn(mFakePanelContent).when(mPanelFeatureProvider).getPanel(any(), any(), any());

        ActivityController<FakeSettingsPanelActivity> activityController =
                Robolectric.buildActivity(FakeSettingsPanelActivity.class);
        activityController.setup();

        mPanelFragment =
                spy((PanelFragment)
                        activityController
                                .get()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.main_content));

    }

    private void constructTestLiveData(Uri uri) {
        // Create a slice to return for the LiveData
        mSlice = spy(new Slice());
        doReturn(uri).when(mSlice).getUri();
        when(mLiveData.getValue()).thenReturn(mSlice);
        mData.add(mLiveData);
    }

    @Test
    public void onCreateViewHolder_returnsSliceRowViewHolder() {
        constructTestLiveData(DATA_URI);
        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final ViewGroup view = new FrameLayout(mContext);
        final PanelSlicesAdapter.SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, 0);

        assertThat(viewHolder.sliceView).isNotNull();
    }

    @Test
    public void nonMediaOutputIndicatorSlice_shouldAllowDividerAboveAndBelow() {
        constructTestLiveData(DATA_URI);
        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final PanelSlicesAdapter.SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, 0 /* view type*/);

        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.isDividerAllowedAbove()).isTrue();
        assertThat(viewHolder.isDividerAllowedBelow()).isTrue();
    }

    @Test
    public void mediaOutputIndicatorSlice_shouldNotAllowDividerAbove() {
        constructTestLiveData(MEDIA_OUTPUT_INDICATOR_SLICE_URI);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final PanelSlicesAdapter.SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, 0 /* view type*/);

        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.isDividerAllowedAbove()).isFalse();
    }
}