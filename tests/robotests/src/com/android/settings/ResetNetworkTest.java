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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.CheckBox;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ResetNetworkTest {
    private Activity mActivity;
    private ResetNetwork mResetNetwork;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
        mResetNetwork = spy(new ResetNetwork());
        when(mResetNetwork.getContext()).thenReturn(mActivity);
        mResetNetwork.mEsimContainer = new View(mActivity);
        mResetNetwork.mEsimCheckbox = new CheckBox(mActivity);
    }

    @Test
    @Ignore
    public void showFinalConfirmation_checkboxVisible_eraseEsimChecked() {
        mResetNetwork.mEsimContainer.setVisibility(View.VISIBLE);
        mResetNetwork.mEsimCheckbox.setChecked(true);

        mResetNetwork.showFinalConfirmation();

        Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getStringExtra(ResetNetworkRequest.KEY_ESIM_PACKAGE))
                .isNotNull();
    }

    @Test
    public void showFinalConfirmation_checkboxVisible_eraseEsimUnchecked() {
        mResetNetwork.mEsimContainer.setVisibility(View.VISIBLE);
        mResetNetwork.mEsimCheckbox.setChecked(false);

        mResetNetwork.showFinalConfirmation();

        Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getStringExtra(ResetNetworkRequest.KEY_ESIM_PACKAGE))
                .isNull();
    }

    @Test
    public void showFinalConfirmation_checkboxGone_eraseEsimChecked() {
        mResetNetwork.mEsimContainer.setVisibility(View.GONE);
        mResetNetwork.mEsimCheckbox.setChecked(true);

        mResetNetwork.showFinalConfirmation();

        Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getStringExtra(ResetNetworkRequest.KEY_ESIM_PACKAGE))
                .isNull();
    }

    @Test
    public void showFinalConfirmation_checkboxGone_eraseEsimUnchecked() {
        mResetNetwork.mEsimContainer.setVisibility(View.GONE);
        mResetNetwork.mEsimCheckbox.setChecked(false);

        mResetNetwork.showFinalConfirmation();

        Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getStringExtra(ResetNetworkRequest.KEY_ESIM_PACKAGE))
                .isNull();
    }
}
