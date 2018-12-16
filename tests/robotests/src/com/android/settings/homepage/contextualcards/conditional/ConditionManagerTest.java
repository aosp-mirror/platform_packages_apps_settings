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

package com.android.settings.homepage.contextualcards.conditional;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConditionManagerTest {

    private static final long ID = 123L;

    @Mock
    private ConditionalCardController mController;
    @Mock
    private ConditionListener mConditionListener;

    private Context mContext;
    private ConditionManager mManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mManager = spy(new ConditionManager(mContext, mConditionListener));

        when(mController.getId()).thenReturn(ID);
        when(mController.buildContextualCard()).thenReturn(
                new ConditionalContextualCard.Builder()
                        .build());
        mManager.mCardControllers.clear();
        mManager.mCardControllers.add(mController);
    }

    @Test
    public void getDisplayableCards_nothingDisplayable() {
        assertThat(mManager.getDisplayableCards()).isEmpty();
    }

    @Test
    public void getDisplayableCards_hasDisplayable() {
        when(mController.isDisplayable()).thenReturn(true);

        assertThat(mManager.getDisplayableCards()).hasSize(1);
    }

    @Test
    public void onPrimaryClick_shouldRelayToController() {
        mManager.onPrimaryClick(mContext, ID);

        verify(mController).onPrimaryClick(mContext);
    }

    @Test
    public void onActionClick_shouldRelayToController() {
        mManager.onActionClick(ID);

        verify(mController).onActionClick();
    }

    @Test
    public void startMonitoringStateChange_multipleTimes_shouldRegisterOnce() {
        final int loopCount = 10;
        for (int i = 0; i < loopCount; i++) {
            mManager.startMonitoringStateChange();
        }

        verify(mController).startMonitoringStateChange();
        verify(mManager, times(loopCount)).onConditionChanged();
    }

    @Test
    public void stopMonitoringStateChange_beforeStart_shouldDoNothing() {
        mManager.stopMonitoringStateChange();
        mManager.stopMonitoringStateChange();
        mManager.stopMonitoringStateChange();

        verify(mController, never()).startMonitoringStateChange();
        verify(mController, never()).stopMonitoringStateChange();
    }

    @Test
    public void stopMonitoringStateChange_multipleTimes_shouldUnregisterOnce() {
        mManager.startMonitoringStateChange();

        mManager.stopMonitoringStateChange();
        mManager.stopMonitoringStateChange();
        mManager.stopMonitoringStateChange();

        verify(mController).startMonitoringStateChange();
        verify(mController).stopMonitoringStateChange();
    }

    @Test
    public void onConditionChanged_shouldNotifyListener() {
        mManager.onConditionChanged();

        verify(mConditionListener).onConditionsChanged();
    }
}
