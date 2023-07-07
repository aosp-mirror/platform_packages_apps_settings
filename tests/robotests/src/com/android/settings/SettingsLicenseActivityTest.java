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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
public class SettingsLicenseActivityTest {

    private ActivityController<SettingsLicenseActivity> mActivityController;
    private SettingsLicenseActivity mActivity;
    private Application mApplication;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mApplication = RuntimeEnvironment.application;
        mActivityController = Robolectric.buildActivity(SettingsLicenseActivity.class);
        mActivity = spy(mActivityController.get());
    }

    private void assertEqualIntents(Intent actual, Intent expected) {
        assertThat(actual.getAction()).isEqualTo(expected.getAction());
        assertThat(actual.getDataString()).isEqualTo(expected.getDataString());
        assertThat(actual.getType()).isEqualTo(expected.getType());
        assertThat(actual.getCategories()).isEqualTo(expected.getCategories());
        assertThat(actual.getPackage()).isEqualTo(expected.getPackage());
        assertThat(actual.getFlags()).isEqualTo(expected.getFlags());
    }

    @Test
    public void testOnCreateWithValidHtmlFile() {
        doReturn(true).when(mActivity).isFileValid(any());
        mActivity.onCreate(null);

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file:///system/etc/NOTICE.html.gz"), "text/html");
        intent.putExtra(Intent.EXTRA_TITLE, mActivity.getString(
                R.string.settings_license_activity_title));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.htmlviewer");

        assertEqualIntents(Shadows.shadowOf(mApplication).getNextStartedActivity(), intent);
    }

    @Test
    public void testOnCreateWithGeneratedHtmlFile() {
        doReturn(Uri.parse("content://com.android.settings.files/my_cache/generated_test.html"))
                .when(mActivity).getUriFromGeneratedHtmlFile(any());

        mActivity.onCreate(null);
        mActivity.onLoadFinished(null, new File("/generated_test.html"));

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                Uri.parse("content://com.android.settings.files/my_cache/generated_test.html"),
                "text/html");
        intent.putExtra(Intent.EXTRA_TITLE, mActivity.getString(
                R.string.settings_license_activity_title));
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.htmlviewer");

        assertEqualIntents(Shadows.shadowOf(mApplication).getNextStartedActivity(), intent);
    }
}
