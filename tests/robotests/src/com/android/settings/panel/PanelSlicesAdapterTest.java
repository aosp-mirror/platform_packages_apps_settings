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
import static com.android.settings.panel.PanelSlicesAdapter.MAX_NUM_OF_SLICES;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_NOTIFICATION_URI;

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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.lifecycle.LiveData;
import androidx.slice.Slice;

import com.android.settings.R;
import com.android.settings.panel.PanelSlicesAdapter.SliceRowViewHolder;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

    /**
     * Edge case where fragment context is not available.
     */
    @Test
    public void withPanelFragmentContextNull_createAdapter_noExceptionThrown() {
        when(mPanelFragment.getContext()).thenReturn(null);

        final PanelSlicesAdapter adapter = spy(new PanelSlicesAdapter(mPanelFragment, mData, 0));

        Assert.assertNotNull(adapter);
    }

    /**
     * ViewHolder should load and set the action label correctly.
     */
    @Ignore("b/313576125")
    @Test
    public void setActionLabel_loadsActionLabel() {
        addTestLiveData(VOLUME_NOTIFICATION_URI);
        final PanelSlicesAdapter adapter = new PanelSlicesAdapter(mPanelFragment, mData, 0);
        final ViewGroup view = new FrameLayout(mContext);
        final SliceRowViewHolder viewHolder = adapter.onCreateViewHolder(view, VIEW_TYPE_SLIDER);

        // now let's see if setActionLabel can load and set the label correctly.
        LinearLayout llRow = new LinearLayout(mContext);
        viewHolder.setActionLabel(llRow);

        boolean isLabelSet = isActionLabelSet(llRow);
        Assert.assertTrue("Action label was not set correctly.", isLabelSet);
    }

    /**
     * @param rowView the view with id row_view
     * @return whether the accessibility action label is set
     */
    private boolean isActionLabelSet(View rowView) {
        View.AccessibilityDelegate delegate = rowView.getAccessibilityDelegate();
        if (delegate == null) {
            return false;
        }
        AccessibilityNodeInfo node = new AccessibilityNodeInfo(rowView);
        delegate.onInitializeAccessibilityNodeInfo(rowView, node);

        boolean foundLabel = false;
        final String expectedLabel =
                mContext.getString(R.string.accessibility_action_label_panel_slice);
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (action.equals(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
                    && TextUtils.equals(action.getLabel(), expectedLabel)) {
                foundLabel = true;
                break;
            }
        }
        return foundLabel;
    }

    @Ignore("b/313576125")
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

    @Ignore("b/313576125")
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

    @Ignore("b/313576125")
    @Test
    public void onBindViewHolder_viewTypeSlider_verifyActionLabelSet() {
        addTestLiveData(VOLUME_NOTIFICATION_URI);

        final PanelSlicesAdapter adapter =
                new PanelSlicesAdapter(mPanelFragment, mData, 0);
        final ViewGroup view = new FrameLayout(mContext);
        SliceRowViewHolder viewHolder = spy(adapter.onCreateViewHolder(view, 0 /* view type*/));
        adapter.onBindViewHolder(viewHolder, 0);

        verify(viewHolder).updateActionLabel();
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
