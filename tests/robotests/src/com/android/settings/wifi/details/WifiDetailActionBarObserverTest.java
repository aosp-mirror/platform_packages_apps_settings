/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.wifi.details;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiDetailActionBarObserverTest {

    @Mock private Bundle mockBundle;
    @Mock private Activity mockActivity;
    @Mock private ActionBar mockActionBar;
    @Mock private WifiNetworkDetailsFragment mockFragment;

    private Context mContext = RuntimeEnvironment.application;
    private Lifecycle mLifecycle;
    private WifiDetailActionBarObserver mObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mLifecycle = new Lifecycle();

        when(mockFragment.getActivity()).thenReturn(mockActivity);
        when(mockActivity.getActionBar()).thenReturn(mockActionBar);

        mObserver = new WifiDetailActionBarObserver(mContext, mockFragment);
        mLifecycle.addObserver(mObserver);
    }

    @Test
    public void actionBarIsSetToNetworkInfo() {
        mLifecycle.onCreate(mockBundle);

        verify(mockActionBar).setTitle(mContext.getString(R.string.wifi_details_title));
    }
}
