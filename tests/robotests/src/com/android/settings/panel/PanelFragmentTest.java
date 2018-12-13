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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class PanelFragmentTest {

    private Context mContext;
    private PanelFragment mPanelFragment;
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

    @Test
    public void onCreateView_adapterGetsDataset() {
        final Bundle bundle = new Bundle();
        bundle.putString(SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT, FAKE_EXTRA);
        doReturn(bundle).when(mPanelFragment).getArguments();
        mPanelFragment.onCreateView(LayoutInflater.from(mContext),
                new LinearLayout(mContext), null);
        PanelSlicesAdapter adapter = mPanelFragment.mAdapter;

        assertThat(adapter.getData()).containsAllIn(mFakePanelContent.getSlices());
    }

}