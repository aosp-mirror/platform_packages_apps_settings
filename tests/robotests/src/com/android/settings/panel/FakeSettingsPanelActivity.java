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

package com.android.settings.panel;

import android.content.ComponentName;
import android.content.Intent;

@Deprecated(forRemoval = true)
public class FakeSettingsPanelActivity extends SettingsPanelActivity {
    @Override
    public ComponentName getCallingActivity() {
        return new ComponentName("fake-package", "fake-class");
    }

    @Override
    public Intent getIntent() {
        final Intent intent = new Intent(FakePanelContent.FAKE_ACTION);
        return intent;
    }
}
