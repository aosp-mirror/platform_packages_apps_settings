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

import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class RingerVibrateConditionCardTest {

    private Context mContext;
    private RingerVibrateConditionCard mCard;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mCard = new RingerVibrateConditionCard(mContext);
    }

    @Test
    public void verifyText() {
        assertThat(mCard.getTitle()).isEqualTo(
                mContext.getText(R.string.condition_device_vibrate_title));
        assertThat(mCard.getSummary()).isEqualTo(
                mContext.getText(R.string.condition_device_vibrate_summary));
        assertThat(mCard.getActionText()).isEqualTo(
                mContext.getText(R.string.condition_device_muted_action_turn_on_sound));
    }
}
