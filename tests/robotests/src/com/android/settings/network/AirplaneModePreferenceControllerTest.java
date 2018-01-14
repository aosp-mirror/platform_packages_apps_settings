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

package com.android.settings.network;

import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AirplaneModePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Mock
    private Resources mResources;

    @Mock
    private PreferenceScreen mScreen;

    @Mock
    private PackageManager mPackageManager;

    private AirplaneModePreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private FakeFeatureFactory mFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = FakeFeatureFactory.setupForTest();
        doReturn(mResources).when(mContext).getResources();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mController = spy(new AirplaneModePreferenceController(mContext, null));
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void airplaneModePreference_shouldNotBeAvailable_ifHasLeanbackFeature() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true);
        assertFalse(mController.isAvailable());

        mController.displayPreference(mScreen);

        // This should not crash
        mController.onResume();
        mController.onPause();
    }
}
