/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.legal;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ModuleInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class ModuleLicensePreferenceTest {
    public static final String PACKAGE_NAME = "com.android.test_package";
    public static final String NAME = "Test Package";
    private Context mContext;
    private ModuleInfo mModuleInfo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = Robolectric.setupActivity(Activity.class);
        mModuleInfo = new ModuleInfo();
        mModuleInfo.setPackageName(PACKAGE_NAME);
        mModuleInfo.setName(NAME);
    }

    @Test
    public void ctor_properKeyAndTitle() {
        ModuleLicensePreference pref = new ModuleLicensePreference(mContext, mModuleInfo);

        assertThat(pref.getKey()).isEqualTo(PACKAGE_NAME);
        assertThat(pref.getTitle()).isEqualTo(NAME);
    }

    @Test
    public void onClick_sendsCorrectIntent() {
        ModuleLicensePreference pref = new ModuleLicensePreference(mContext, mModuleInfo);

        pref.onClick();

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData())
                .isEqualTo(ModuleLicenseProvider.getUriForPackage(PACKAGE_NAME));
        assertThat(intent.getType()).isEqualTo(ModuleLicenseProvider.LICENSE_FILE_MIME_TYPE);
        assertThat(intent.getCharSequenceExtra(Intent.EXTRA_TITLE)).isEqualTo(NAME);
        assertThat(intent.getFlags()).isEqualTo(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertThat(intent.getCategories()).contains(Intent.CATEGORY_DEFAULT);
        assertThat(intent.getPackage()).isEqualTo("com.android.htmlviewer");
    }
}
