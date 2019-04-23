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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PanelFragmentTest {

    private Context mContext;
    private PanelFragment mPanelFragment;
    private FakeSettingsPanelActivity mActivity;
    private FakeFeatureFactory mFakeFeatureFactory;
    private PanelFeatureProvider mPanelFeatureProvider;
    private FakePanelContent mFakePanelContent;

    private final String FAKE_EXTRA = "fake_extra";

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mPanelFeatureProvider = spy(new PanelFeatureProviderImpl());
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.panelFeatureProvider = mPanelFeatureProvider;
        mFakePanelContent = new FakePanelContent();
        doReturn(mFakePanelContent).when(mPanelFeatureProvider).getPanel(any(), any(), any());

        mActivity = spy(Robolectric.buildActivity(FakeSettingsPanelActivity.class).setup().get());

        mPanelFragment =
                spy((PanelFragment)
                        mActivity.getSupportFragmentManager().findFragmentById(R.id.main_content));
        doReturn(mActivity).when(mPanelFragment).getActivity();

        final Bundle bundle = new Bundle();
        bundle.putString(SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT, FAKE_EXTRA);
        doReturn(bundle).when(mPanelFragment).getArguments();
    }

    @Test
    public void onCreateView_countdownLatch_setup() {
        mPanelFragment.onCreateView(LayoutInflater.from(mContext),
                new LinearLayout(mContext), null);
        PanelSlicesLoaderCountdownLatch countdownLatch =
                mPanelFragment.mPanelSlicesLoaderCountdownLatch;
        for (Uri sliecUri: mFakePanelContent.getSlices()) {
            countdownLatch.markSliceLoaded(sliecUri);
        }

        assertThat(countdownLatch.isPanelReadyToLoad()).isTrue();
    }

    @Test
    public void onCreate_logsOpenEvent() {
        verify(mFakeFeatureFactory.metricsFeatureProvider).action(
                0,
                SettingsEnums.PAGE_VISIBLE,
                mFakePanelContent.getMetricsCategory(),
                null,
                0);
    }

    @Test
    public void onDestroy_logCloseEvent() {
        mPanelFragment.onDestroyView();
        verify(mFakeFeatureFactory.metricsFeatureProvider).action(
                0,
                SettingsEnums.PAGE_HIDE,
                mFakePanelContent.getMetricsCategory(),
                PanelLoggingContract.PanelClosedKeys.KEY_OTHERS,
                0);
    }

    @Test
    public void panelSeeMoreClick_logsCloseEvent() {
        final View.OnClickListener listener = mPanelFragment.getSeeMoreListener();
        listener.onClick(null);
        verify(mActivity).finish();

        mPanelFragment.onDestroyView();
        verify(mFakeFeatureFactory.metricsFeatureProvider).action(
                0,
                SettingsEnums.PAGE_HIDE,
                mFakePanelContent.getMetricsCategory(),
                PanelLoggingContract.PanelClosedKeys.KEY_SEE_MORE,
                0
        );
    }

    @Test
    public void panelDoneClick_logsCloseEvent() {
        final View.OnClickListener listener = mPanelFragment.getCloseListener();
        listener.onClick(null);
        verify(mActivity).finish();

        mPanelFragment.onDestroyView();
        verify(mFakeFeatureFactory.metricsFeatureProvider).action(
                0,
                SettingsEnums.PAGE_HIDE,
                mFakePanelContent.getMetricsCategory(),
                PanelLoggingContract.PanelClosedKeys.KEY_DONE,
                0
        );
    }
}