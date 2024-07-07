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

package com.android.settings.notification.modes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class ZenModeNotifVisLinkPreferenceControllerTest {

    private ZenModeNotifVisLinkPreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    @Mock private ZenHelperBackend mHelperBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        mController = new ZenModeNotifVisLinkPreferenceController(
                mContext, "something", mHelperBackend);
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void testHasSummary() {
        Preference pref = mock(Preference.class);
        mController.updateZenMode(pref, TestModeBuilder.EXAMPLE);
        verify(pref).setSummary(any());
    }
}