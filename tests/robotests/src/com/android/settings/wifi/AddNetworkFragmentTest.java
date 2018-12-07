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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.view.View;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class AddNetworkFragmentTest {

    private AddNetworkFragment mAddNetworkFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAddNetworkFragment = spy(new AddNetworkFragment());
        FragmentController.setupFragment(mAddNetworkFragment);
    }

    @Test
    public void getMetricsCategory_shouldReturnAddNetwork() {
        assertThat(mAddNetworkFragment.getMetricsCategory()).isEqualTo(
                MetricsEvent.SETTINGS_WIFI_ADD_NETWORK);
    }

    @Test
    public void getMode_shouldBeModeConnected() {
        assertThat(mAddNetworkFragment.getMode()).isEqualTo(WifiConfigUiBase.MODE_CONNECT);
    }

    @Test
    public void launchFragment_shouldShowSubmitButton() {
        assertThat(mAddNetworkFragment.getSubmitButton()).isNotNull();
    }

    @Test
    public void launchFragment_shouldShowCancelButton() {
        assertThat(mAddNetworkFragment.getCancelButton()).isNotNull();
    }

    @Test
    public void onClickSubmitButton_shouldHandleSubmitAction() {
        View submitButton = mAddNetworkFragment.getView().findViewById(
                AddNetworkFragment.SUBMIT_BUTTON_ID);

        mAddNetworkFragment.onClick(submitButton);

        verify(mAddNetworkFragment).handleSubmitAction();
    }

    @Test
    public void onClickCancelButton_shouldHandleCancelAction() {
        View cancelButton = mAddNetworkFragment.getView().findViewById(
                AddNetworkFragment.CANCEL_BUTTON_ID);

        mAddNetworkFragment.onClick(cancelButton);

        verify(mAddNetworkFragment).handleCancelAction();
    }

    @Test
    public void dispatchSubmit_shouldHandleSubmitAction() {
        mAddNetworkFragment.dispatchSubmit();

        verify(mAddNetworkFragment).handleSubmitAction();
    }
}
