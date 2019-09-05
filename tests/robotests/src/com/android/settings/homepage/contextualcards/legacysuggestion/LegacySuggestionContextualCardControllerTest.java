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

package com.android.settings.homepage.contextualcards.legacysuggestion;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.homepage.contextualcards.ContextualCardUpdateListener;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.suggestions.SuggestionController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowThreadUtils.class)
public class LegacySuggestionContextualCardControllerTest {

    @Mock
    private SuggestionController mSuggestionController;
    @Mock
    private ContextualCardUpdateListener mCardUpdateListener;

    private Context mContext;
    private LegacySuggestionContextualCardController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mContext = RuntimeEnvironment.application;
        mController = new LegacySuggestionContextualCardController(mContext);
    }

    @Test
    public void init_configOn_shouldCreateSuggestionController() {
        final LegacySuggestionContextualCardController controller =
                new LegacySuggestionContextualCardController(mContext);
        assertThat(controller.mSuggestionController).isNotNull();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void init_configOff_shouldNotCreateSuggestionController() {
        final LegacySuggestionContextualCardController controller =
                new LegacySuggestionContextualCardController(mContext);

        assertThat(controller.mSuggestionController).isNull();
    }

    @Test
    public void goThroughLifecycle_hasSuggestionController_shouldStartStopController() {
        mController.mSuggestionController = mSuggestionController;
        mController.onStart();
        verify(mSuggestionController).start();

        mController.onStop();
        verify(mSuggestionController).stop();
    }

    @Test
    public void onServiceConnected_shouldLoadSuggestion() {
        when(mSuggestionController.getSuggestions()).thenReturn(null);
        mController.mSuggestionController = mSuggestionController;
        mController.setCardUpdateListener(mCardUpdateListener);

        mController.onServiceConnected();

        verify(mSuggestionController).getSuggestions();
    }
}
