/*
 * Copyright (C) 2017 Google Inc.
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

package com.android.settings.password;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.Activity;
import android.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowEventLogWriter.class,
                ShadowUtils.class
        })
public class SetupSkipDialogTest {

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void frpMessages_areShownCorrectly_whenNotSupported() {
        SetupSkipDialog setupSkipDialog = SetupSkipDialog.newInstance(false);
        setupSkipDialog.show(mActivity.getFragmentManager());

        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        ShadowAlertDialog shadowAlertDialog = Shadows.shadowOf(alertDialog);
        assertEquals(application.getString(R.string.lock_screen_intro_skip_title),
                shadowAlertDialog.getTitle());
        assertEquals(application.getString(R.string.lock_screen_intro_skip_dialog_text),
                shadowAlertDialog.getMessage());
    }

    @Test
    public void frpMessages_areShownCorrectly_whenSupported() {
        SetupSkipDialog setupSkipDialog = SetupSkipDialog.newInstance(true);
        setupSkipDialog.show(mActivity.getFragmentManager());

        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        ShadowAlertDialog shadowAlertDialog = Shadows.shadowOf(alertDialog);
        assertEquals(application.getString(R.string.lock_screen_intro_skip_title),
                shadowAlertDialog.getTitle());
        assertEquals(application.getString(R.string.lock_screen_intro_skip_dialog_text_frp),
                shadowAlertDialog.getMessage());
    }

}
