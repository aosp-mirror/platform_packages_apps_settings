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
 * limitations under the License
 */

package com.android.settings.password;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings.Global;

import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
public class ChooseLockGenericTest {

    @After
    public void tearDown() {
        Global.putInt(RuntimeEnvironment.application.getContentResolver(),
            Global.DEVICE_PROVISIONED, 1);
    }

    @Test
    @Config(shadows = SettingsShadowResources.SettingsShadowTheme.class)
    public void onCreate_deviceNotProvisioned_shouldFinishActivity() {
        final Context context = RuntimeEnvironment.application;
        Global.putInt(context.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        final Activity activity = mock(Activity.class);
        when(activity.getContentResolver()).thenReturn(context.getContentResolver());
        when(activity.getTheme()).thenReturn(context.getTheme());

        final ChooseLockGenericFragment fragment = spy(new ChooseLockGenericFragment());
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getArguments()).thenReturn(Bundle.EMPTY);

        fragment.onCreate(Bundle.EMPTY);
        verify(activity).finish();
    }

}
