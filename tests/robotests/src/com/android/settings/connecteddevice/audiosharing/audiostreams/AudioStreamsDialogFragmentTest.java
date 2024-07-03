/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialog.class,
        })
public class AudioStreamsDialogFragmentTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AudioStreamsDialogFragment.DialogBuilder mDialogBuilder;
    private AudioStreamsDialogFragment mFragment;

    @Before
    public void setUp() {
        mDialogBuilder = spy(new AudioStreamsDialogFragment.DialogBuilder(mContext));
        mFragment = new AudioStreamsDialogFragment(mDialogBuilder, SettingsEnums.PAGE_UNKNOWN);
    }

    @After
    public void tearDown() {
        ShadowAlertDialog.reset();
    }

    @Test
    public void testGetMetricsCategory() {
        int dialogId = mFragment.getMetricsCategory();

        assertThat(dialogId).isEqualTo(SettingsEnums.PAGE_UNKNOWN);
    }

    @Test
    public void testOnCreateDialog() {
        mFragment.onCreateDialog(Bundle.EMPTY);

        verify(mDialogBuilder).build();
    }

    @Test
    public void testShowDialog_dismissAll() {
        FragmentController.setupFragment(mFragment);
        AudioStreamsDialogFragment.show(mFragment, mDialogBuilder, SettingsEnums.PAGE_UNKNOWN);
        ShadowLooper.idleMainLooper();

        var dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();

        AudioStreamsDialogFragment.dismissAll(mFragment);
        assertThat(dialog.isShowing()).isFalse();
    }
}
