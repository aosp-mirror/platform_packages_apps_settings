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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import android.app.Activity;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SettingsRobolectricTestRunner.class)
public class ChooseLockGenericTest {

    @Test
    public void onActivityResult_nullIntentData_shouldNotCrash() {
        ChooseLockGenericFragment fragment = spy(new ChooseLockGenericFragment());
        doNothing().when(fragment).updatePreferencesOrFinish(anyBoolean());

        fragment.onActivityResult(
                fragment.CONFIRM_EXISTING_REQUEST, Activity.RESULT_OK, null /* data */);
        // no crash
    }

}
