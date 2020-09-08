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
 *
 */
package com.android.settings.notification.zen;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.google.common.truth.Truth.assertThat;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.notification.zen.ZenModeSliceBuilder;
import com.android.settings.testutils.shadow.ShadowNotificationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@Config(shadows = ShadowNotificationManager.class)
@RunWith(RobolectricTestRunner.class)
public class ZenModeSliceBuilderTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void getZenModeSlice_correctSliceContent() {
        final Slice dndSlice = ZenModeSliceBuilder.getSlice(mContext);

        final SliceMetadata metadata = SliceMetadata.from(mContext, dndSlice);
        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.zen_mode_settings_title));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        assertThat(primaryAction.getIcon()).isNull();
    }

    @Test
    public void handleUriChange_turnOn_zenModeTurnsOn() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_TOGGLE_STATE, true);
        NotificationManager.from(mContext).setZenMode(Settings.Global.ZEN_MODE_OFF, null, "");

        ZenModeSliceBuilder.handleUriChange(mContext, intent);

        final int zenMode = NotificationManager.from(mContext).getZenMode();
        assertThat(zenMode).isEqualTo(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
    }

    @Test
    public void handleUriChange_turnOff_zenModeTurnsOff() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_TOGGLE_STATE, false);
        NotificationManager.from(mContext).setZenMode(
                Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, "");

        ZenModeSliceBuilder.handleUriChange(mContext, intent);

        final int zenMode = NotificationManager.from(mContext).getZenMode();
        assertThat(zenMode).isEqualTo(Settings.Global.ZEN_MODE_OFF);
    }
}
