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

package com.android.settings.localepicker;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowSettingsPreferenceFragment;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = { ShadowSettingsPreferenceFragment.class })
public class LocaleListEditorTest {

    private LocaleListEditor mLocaleListEditor;

    @Before
    public void setUp() {
        mLocaleListEditor = new LocaleListEditor();
        ReflectionHelpers.setField(mLocaleListEditor, "mEmptyTextView",
                new TextView(RuntimeEnvironment.application));
        ReflectionHelpers.setField(mLocaleListEditor, "mRestrictionsManager",
                RuntimeEnvironment.application.getSystemService(Context.RESTRICTIONS_SERVICE));
        ReflectionHelpers.setField(mLocaleListEditor, "mUserManager",
                RuntimeEnvironment.application.getSystemService(Context.USER_SERVICE));
    }

    @Test
    public void testDisallowConfigLocale_unrestrict() {
        ReflectionHelpers.setField(mLocaleListEditor, "mIsUiRestricted", true);
        mLocaleListEditor.onResume();
        Assert.assertEquals(View.GONE, mLocaleListEditor.getEmptyTextView().getVisibility());
    }

    @Test
    public void testDisallowConfigLocale_restrict() {
        ReflectionHelpers.setField(mLocaleListEditor, "mIsUiRestricted", false);
        mLocaleListEditor.onResume();
        Assert.assertEquals(View.VISIBLE, mLocaleListEditor.getEmptyTextView().getVisibility());
    }
}
