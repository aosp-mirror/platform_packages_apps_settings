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

import static com.android.settings.panel.PanelContent.VIEW_TYPE_SLIDER;
import static com.android.settings.panel.PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON;
import static com.android.settings.panel.PanelSlicesAdapter.MAX_NUM_OF_SLICES;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_GROUP_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_MEDIA_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.lifecycle.LiveData;
import androidx.slice.Slice;

import com.android.settings.R;
import com.android.settings.panel.PanelSlicesAdapter.SliceRowViewHolder;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = PanelSlicesAdapterTest.ShadowLayoutInflater.class)
public class PanelSlicesAdapterTest {

    private static LayoutInflater sLayoutInflater;

    private Context mContext;
    private PanelFragment mPanelFragment;
    private PanelFeatureProvider mPanelFeatureProvider;
    private FakeFeatureFactory mFakeFeatureFactory;
    private FakePanelContent mFakePanelContent;
    private Map<Uri, LiveData<Slice>> mData = new LinkedHashMap<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mPanelFeatureProvider = spy(new PanelFeatureProviderImpl());
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.panelFeatureProvider = mPanelFeatureProvider;
        mFakePanelContent = new FakePanelContent();
        doReturn(mFakePanelContent).when(mPanelFeatureProvider).getPanel(any(), any());

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

    private void addTestLiveData(Uri uri) {
        // Create a slice to return for the LiveData
        final Slice slice = spy(new Slice());
        doReturn(uri).when(slice).getUri();
        final LiveData<Slice> liveData = mock(LiveData.class);
        when(liveData.getValue()).thenReturn(slice);
        mData.put(uri, liveData);
    }

    @Test
    public void sizeOfAdapter_shouldNotExceedMaxNum() {
        for (int i = 0; i < MAX_NUM_OF_SLICES + 2; i++) {
            addTestLiveData(Uri.parse("uri" + i));
        }

        assertThat(mData.size()).isEqualTo(MAX_NUM_OF_SLICES + 2);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, 0);

        assertThat(adapter.getItemCount()).isEqualTo(MAX_NUM_OF_SLICES);
        assertThat(adapter.getData().size()).isEqualTo(MAX_NUM_OF_SLICES);
    }

    @Test
    public void mediaOutputIndicatorSlice_shouldNotAllowDividerAbove() {
        addTestLiveData(MEDIA_OUTPUT_INDICATOR_SLICE_URI);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, 0 /* view type*/);

        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.isDividerAllowedAbove()).isFalse();
    }

    @Test
    public void sliderLargeIconPanel_shouldNotAllowDividerBelow() {
        addTestLiveData(MEDIA_OUTPUT_SLICE_URI);
        mFakePanelContent.setViewType(PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON);
        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.isDividerAllowedBelow()).isFalse();
    }

    @Test
    public void sliderPanelType_shouldAllowDividerBelow() {
        addTestLiveData(VOLUME_MEDIA_URI);
        mFakePanelContent.setViewType(PanelContent.VIEW_TYPE_SLIDER);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, PanelContent.VIEW_TYPE_SLIDER);
        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.isDividerAllowedBelow()).isTrue();
    }

    @Test
    public void defaultPanelType_shouldAllowDividerBelow() {
        addTestLiveData(VOLUME_MEDIA_URI);
        mFakePanelContent.setViewType(0 /* viewType */);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder = adapter.onCreateViewHolder(view, 0/* viewType */);
        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.isDividerAllowedBelow()).isTrue();
    }

    @Test
    public void outputSwitcherSlice_shouldAddFirstItemPadding() {
        addTestLiveData(MEDIA_OUTPUT_SLICE_URI);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON);

        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.mSliceSliderLayout.getPaddingTop()).isEqualTo(
                mPanelFragment.getResources().getDimensionPixelSize(
                        R.dimen.output_switcher_slice_padding_top));
    }

    @Test
    public void outputSwitcherGroupSlice_shouldAddFirstItemPadding() {
        addTestLiveData(MEDIA_OUTPUT_GROUP_SLICE_URI);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON);

        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.mSliceSliderLayout.getPaddingTop()).isEqualTo(
                mPanelFragment.getResources().getDimensionPixelSize(
                        R.dimen.output_switcher_slice_padding_top));
    }

    @Test
    public void mediaOutputIndicatorSlice_notSliderPanel_noSliderLayout() {
        addTestLiveData(MEDIA_OUTPUT_INDICATOR_SLICE_URI);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0 /* metrics category */);
        final int position = 0;
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder =
                adapter.onCreateViewHolder(view, 0 /* view type*/);

        adapter.onBindViewHolder(viewHolder, position);

        assertThat(viewHolder.mSliceSliderLayout).isNull();
    }

    @Test
    public void onCreateViewHolder_viewTypeSlider_verifyLayout() {
        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0);
        final ViewGroup view = new FrameLayout(mContext);
        final ArgumentCaptor<Integer> intArgumentCaptor = ArgumentCaptor.forClass(Integer.class);

        adapter.onCreateViewHolder(view, VIEW_TYPE_SLIDER);

        verify(sLayoutInflater).inflate(intArgumentCaptor.capture(), eq(view), eq(false));
        assertThat(intArgumentCaptor.getValue()).isEqualTo(R.layout.panel_slice_slider_row);
    }

    @Test
    public void onCreateViewHolder_viewTypeSliderLargeIcon_verifyLayout() {
        final PanelSlicesAdapter adapter = new PanelSlicesAdapter(mPanelFragment, mData, 0);
        final ViewGroup view = new FrameLayout(mContext);
        final ArgumentCaptor<Integer> intArgumentCaptor = ArgumentCaptor.forClass(Integer.class);

        adapter.onCreateViewHolder(view, VIEW_TYPE_SLIDER_LARGE_ICON);

        verify(sLayoutInflater).inflate(intArgumentCaptor.capture(), eq(view), eq(false));
        assertThat(intArgumentCaptor.getValue()).isEqualTo(
                R.layout.panel_slice_slider_row_large_icon);
    }

    @Test
    public void onCreateViewHolder_viewTypeDefault_verifyLayout() {
        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0);
        final ViewGroup view = new FrameLayout(mContext);
        final ArgumentCaptor<Integer> intArgumentCaptor = ArgumentCaptor.forClass(Integer.class);

        adapter.onCreateViewHolder(view, 0);

        verify(sLayoutInflater).inflate(intArgumentCaptor.capture(), eq(view), eq(false));
        assertThat(intArgumentCaptor.getValue()).isEqualTo(R.layout.panel_slice_row);
    }

    @Implements(LayoutInflater.class)
    public static class ShadowLayoutInflater {

        @Implementation
        public static LayoutInflater from(Context context) {
            final LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater == null) {
                throw new AssertionError("LayoutInflater not found.");
            }
            sLayoutInflater = spy(inflater);
            return sLayoutInflater;
        }
    }
}
