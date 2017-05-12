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

package com.android.settings.datausage;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.applications.AppHeaderController.ActionType;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.AppItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppDataUsageTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppHeaderController mHeaderController;
    private FakeFeatureFactory mFeatureFactory;
    private AppDataUsage mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
    }

    @Test
    public void bindAppHeader_allWorkApps_shouldNotShowAppInfoLink() {
        mFragment = spy(new AppDataUsage());

        doReturn(mock(PreferenceManager.class, RETURNS_DEEP_STUBS))
                .when(mFragment)
                .getPreferenceManager();
        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();
        ReflectionHelpers.setField(mFragment, "mAppItem", mock(AppItem.class));
        when(mFeatureFactory.applicationFeatureProvider.newAppHeaderController(mFragment, null))
                .thenReturn(mHeaderController);

        mFragment.onViewCreated(new View(RuntimeEnvironment.application), new Bundle());

        verify(mHeaderController).setButtonActions(ActionType.ACTION_NONE, ActionType.ACTION_NONE);
    }
}
