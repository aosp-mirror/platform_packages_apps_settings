/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.view.contentprotection.flags.Flags.FLAG_SETTING_UI_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ContentProtectionPreferenceControllerTest {

    private static final String PACKAGE_NAME = "com.test.package";

    private static final ComponentName COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, "TestClass");

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private String mConfigDefaultContentProtectionService = COMPONENT_NAME.flattenToString();

    private ContentProtectionPreferenceController mController;

    @Before
    public void setUp() {
        mController = new TestContentProtectionPreferenceController();
    }

    @Test
    public void isAvailable_flagSettingUiDisabled_isFalse() {
        mSetFlagsRule.disableFlags(FLAG_SETTING_UI_ENABLED);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_componentNameNull_isFalse() {
        mConfigDefaultContentProtectionService = null;
        mSetFlagsRule.enableFlags(FLAG_SETTING_UI_ENABLED);
        mController = new TestContentProtectionPreferenceController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_componentNameEmpty_isFalse() {
        mConfigDefaultContentProtectionService = "";
        mSetFlagsRule.enableFlags(FLAG_SETTING_UI_ENABLED);
        mController = new TestContentProtectionPreferenceController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_componentNameBlank_isFalse() {
        mConfigDefaultContentProtectionService = "    ";
        mSetFlagsRule.enableFlags(FLAG_SETTING_UI_ENABLED);
        mController = new TestContentProtectionPreferenceController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_componentNameInvalid_isFalse() {
        mConfigDefaultContentProtectionService = "invalid";
        mSetFlagsRule.enableFlags(FLAG_SETTING_UI_ENABLED);
        mController = new TestContentProtectionPreferenceController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_flagSettingUiEnabled_componentNameValid_isTrue() {
        mSetFlagsRule.enableFlags(FLAG_SETTING_UI_ENABLED);

        assertThat(mController.isAvailable()).isTrue();
    }

    private class TestContentProtectionPreferenceController
            extends ContentProtectionPreferenceController {

        TestContentProtectionPreferenceController() {
            super(ContentProtectionPreferenceControllerTest.this.mContext, "key");
        }

        @Override
        protected String getContentProtectionServiceFlatComponentName() {
            return mConfigDefaultContentProtectionService;
        }
    }
}
