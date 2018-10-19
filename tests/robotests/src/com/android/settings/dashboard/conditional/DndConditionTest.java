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
 * limitations under the License
 */
package com.android.settings.dashboard.conditional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class DndConditionTest {

    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        when(mConditionManager.getContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void constructor_shouldNotDisableReceiver() {
        new DndCondition(mConditionManager);
        verify(mPackageManager, never()).setComponentEnabledSetting(any(ComponentName.class),
            eq(PackageManager.COMPONENT_ENABLED_STATE_DISABLED), eq(PackageManager.DONT_KILL_APP));
    }

    @Test
    public void constructor_shouldRegisterReceiver() {
        new DndCondition(mConditionManager);
        verify(mContext).registerReceiver(any(DndCondition.Receiver.class),
            eq(DndCondition.DND_FILTER));
    }

    @Test
    public void silence_shouldNotDisableReceiver() {
        new DndCondition(mConditionManager).silence();

        verify(mPackageManager, never()).setComponentEnabledSetting(any(ComponentName.class),
            eq(PackageManager.COMPONENT_ENABLED_STATE_DISABLED), eq(PackageManager.DONT_KILL_APP));
    }

    @Test
    public void onResume_shouldRegisterReceiver() {
        DndCondition condition = new DndCondition(mConditionManager);
        condition.onPause();
        condition.onResume();

        // one from constructor, one from onResume()
        verify(mContext, times(2)).registerReceiver(any(DndCondition.Receiver.class),
            eq(DndCondition.DND_FILTER));
    }

    @Test
    public void onPause_shouldUnregisterReceiver() {
        new DndCondition(mConditionManager).onPause();

        verify(mContext).unregisterReceiver(any(DndCondition.Receiver.class));
    }

    @Test
    public void onPause_noReceiverRegistered_shouldNotUnregisterReceiver() {
        DndCondition condition = new DndCondition(mConditionManager);
        condition.onPause();
        reset(mContext);

        condition.onPause();

        verify(mContext, never()).unregisterReceiver(any(DndCondition.Receiver.class));
    }

    @Test
    public void nullZenConfig_noCrash() {
        DndCondition condition = new DndCondition(mConditionManager);
        assertThat(condition.mConfig).isNull();

        // should not crash, instead summary is null
        assertThat(condition.getSummary()).isNull();
    }
}
