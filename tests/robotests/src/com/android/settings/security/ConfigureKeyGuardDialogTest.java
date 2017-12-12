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

package com.android.settings.security;


import android.content.DialogInterface;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.util.FragmentController;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = ShadowEventLogWriter.class
)
public class ConfigureKeyGuardDialogTest {

    @Test
    public void displayDialog_clickPositiveButton_launchSetNewPassword() {
        final FragmentController<ConfigureKeyGuardDialog> fragmentController =
                Robolectric.buildFragment(ConfigureKeyGuardDialog.class);
        final ConfigureKeyGuardDialog fragment = spy(fragmentController.get());
        doNothing().when(fragment).startPasswordSetup();
        fragmentController.attach().create().start().resume();
        fragment.onClick(null /* dialog */, DialogInterface.BUTTON_POSITIVE);
        fragment.onDismiss(null /* dialog */);

        verify(fragment).startPasswordSetup();
    }
}
