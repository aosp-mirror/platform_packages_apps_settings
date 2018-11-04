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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardUpdateListener;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class ConditionContextualCardControllerTest {

    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private ContextualCardUpdateListener mListener;
    private Context mContext;
    private ConditionContextualCardController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = spy(new ConditionContextualCardController(mContext));
        ReflectionHelpers.setField(mController, "mConditionManager", mConditionManager);
    }

    @Test
    public void onStart_shouldStartMonitoring() {
        mController.onStart();

        verify(mConditionManager).startMonitoringStateChange();
    }

    @Test
    public void onStop_shouldStopMonitoring() {
        mController.onStop();

        verify(mConditionManager).stopMonitoringStateChange();
    }

    @Test
    public void onConditionsChanged_listenerIsSet_shouldUpdateData() {
        final ContextualCard fakeConditionalCard = new ConditionalContextualCard.Builder().build();
        final List<ContextualCard> conditionalCards = new ArrayList<>();
        conditionalCards.add(fakeConditionalCard);
        when(mConditionManager.getDisplayableCards()).thenReturn(conditionalCards);
        mController.setCardUpdateListener(mListener);

        mController.onConditionsChanged();

        verify(mListener).onContextualCardUpdated(any());
    }

    @Test
    public void onConditionsChanged_listenerNotSet_shouldNotUpdateData() {
        final ContextualCard fakeConditionalCard = new ConditionalContextualCard.Builder().build();
        final List<ContextualCard> conditionalCards = new ArrayList<>();
        conditionalCards.add(fakeConditionalCard);
        when(mConditionManager.getDisplayableCards()).thenReturn(conditionalCards);

        mController.onConditionsChanged();

        verify(mListener, never()).onContextualCardUpdated(any());
    }
}
