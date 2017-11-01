/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows=SettingsShadowBluetoothDevice.class)
public class BluetoothDetailsControllerEventsTest extends BluetoothDetailsControllerTestBase {

    static class TestController extends BluetoothDetailsController {
        public TestController(Context context, PreferenceFragment fragment,
                CachedBluetoothDevice device,
                Lifecycle lifecycle) {
            super(context, fragment, device, lifecycle);
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }

        @Override
        protected void init(PreferenceScreen screen) {}

        @Override
        protected void refresh() {}
    }

    @Test
    public void pauseResumeEvents() {

        TestController controller = spy(new TestController(mContext, mFragment, mCachedDevice,
                mLifecycle));
        verify(mLifecycle).addObserver(any(BluetoothDetailsController.class));

        showScreen(controller);
        verify(mCachedDevice, times(1)).registerCallback(controller);
        verify(controller, times(1)).refresh();

        controller.onPause();
        verify(controller, times(1)).refresh();
        verify(mCachedDevice).unregisterCallback(controller);

        controller.onResume();
        verify(controller, times(2)).refresh();
        verify(mCachedDevice, times(2)).registerCallback(controller);

        // The init function should only have been called once
        verify(controller, times(1)).init(mScreen);
    }
}
