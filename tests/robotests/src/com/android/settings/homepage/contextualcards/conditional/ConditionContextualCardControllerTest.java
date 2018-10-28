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
import android.graphics.drawable.Drawable;

import com.android.settings.R;
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
        final FakeConditionalCard fakeConditionalCard = new FakeConditionalCard(mContext);
        final List<ConditionalCard> conditionalCards = new ArrayList<>();
        conditionalCards.add(fakeConditionalCard);
        when(mConditionManager.getDisplayableCards()).thenReturn(conditionalCards);
        mController.setCardUpdateListener(mListener);

        mController.onConditionsChanged();

        verify(mListener).onContextualCardUpdated(any());
    }

    @Test
    public void onConditionsChanged_listenerNotSet_shouldNotUpdateData() {
        final FakeConditionalCard fakeConditionalCard = new FakeConditionalCard(mContext);
        final List<ConditionalCard> conditionalCards = new ArrayList<>();
        conditionalCards.add(fakeConditionalCard);
        when(mConditionManager.getDisplayableCards()).thenReturn(conditionalCards);

        mController.onConditionsChanged();

        verify(mListener, never()).onContextualCardUpdated(any());
    }

    private class FakeConditionalCard implements ConditionalCard {

        private final Context mContext;

        public FakeConditionalCard(Context context) {
            mContext = context;
        }

        @Override
        public long getId() {
            return 100;
        }

        @Override
        public CharSequence getActionText() {
            return "action_text_test";
        }

        @Override
        public int getMetricsConstant() {
            return 1;
        }

        @Override
        public Drawable getIcon() {
            return mContext.getDrawable(R.drawable.ic_do_not_disturb_on_24dp);
        }

        @Override
        public CharSequence getTitle() {
            return "title_text_test";
        }

        @Override
        public CharSequence getSummary() {
            return "summary_text_test";
        }
    }
}
