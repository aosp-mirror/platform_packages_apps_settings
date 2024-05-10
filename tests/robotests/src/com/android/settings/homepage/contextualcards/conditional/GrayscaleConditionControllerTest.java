/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.ColorDisplayManager;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class GrayscaleConditionControllerTest {

    @Mock
    private ConditionManager mConditionManager;

    private ColorDisplayManager mColorDisplayManager;
    private Context mContext;
    private GrayscaleConditionController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mColorDisplayManager = spy(mContext.getSystemService(ColorDisplayManager.class));
        doReturn(mColorDisplayManager).when(mContext).getSystemService(ColorDisplayManager.class);
        mController = new GrayscaleConditionController(mContext, mConditionManager);
    }

    @Ignore("b/313597163")
    @Test
    public void isDisplayable_noIntent_shouldReturnFalse() {
        assertThat(mController.isDisplayable()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isDisplayable_validIntentAndGrayscaleOn_shouldReturnTrue() {
        doReturn(true).when(mColorDisplayManager).isSaturationActivated();

        assertThat(mController.isDisplayable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isDisplayable_validIntentAndGrayscaleOff_shouldReturnFalse() {
        doReturn(false).when(mColorDisplayManager).isSaturationActivated();

        assertThat(mController.isDisplayable()).isFalse();
    }

    @Test
    public void onActionClick_shouldRefreshCondition() {
        mController.onActionClick();

        verify(mConditionManager).onConditionChanged();
    }

    @Test
    public void onActionClick_shouldSendBroadcast() {
        mController.onActionClick();

        verify(mContext).sendBroadcastAsUser(any(Intent.class), any(UserHandle.class),
                any(String.class));
    }
}
