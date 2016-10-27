/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.applications;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppHeaderControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private Fragment mFragment;

    private Context mShadowContext;
    private LayoutInflater mLayoutInflater;
    private PackageInfo mInfo;
    private AppHeaderController mController;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowContext = ShadowApplication.getInstance().getApplicationContext();
        mLayoutInflater = LayoutInflater.from(mShadowContext);
        mInfo = new PackageInfo();
        mInfo.versionName = "1234";
        mController = new AppHeaderController(mContext);
    }

    @Test
    public void bindViews_shouldBindAllData() {
        final String testString = "test";
        final View appHeader = mLayoutInflater.inflate(R.layout.app_details, null /* root */);
        final TextView label = (TextView) appHeader.findViewById(android.R.id.title);
        final TextView version = (TextView) appHeader.findViewById(android.R.id.summary);
        label.setText(testString);
        label.setText(testString);

        mController.bindAppHeader(appHeader, mInfo, mAppEntry);

        assertThat(label.getText()).isNotEqualTo(testString);
        assertThat(version.getText())
                .isEqualTo(mShadowContext.getString(R.string.version_text, mInfo.versionName));
    }

    @Test
    public void bindButton_hasAppPref_shouldShowButton() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = "123";
        info.activityInfo.name = "321";
        final View appLinks = mLayoutInflater
                .inflate(R.layout.app_details, null /* root */);
        when(mContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(info);
        mController.bindAppHeaderButtons(mFragment, appLinks,
                mShadowContext.getPackageName(),
                AppHeaderController.ActionType.ACTION_APP_PREFERENCE,
                AppHeaderController.ActionType.ACTION_APP_PREFERENCE);
        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.VISIBLE);
        try {
            appLinks.findViewById(R.id.left_button).performClick();
        } catch (Exception e) {
            // Ignore exception because the launching intent is fake.
        }
        verify(mFragment).startActivity(any(Intent.class));
    }

    @Test
    public void bindButton_noAppPref_shouldNotShowButton() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.app_details, null /* root */);
        when(mContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(null);
        mController.bindAppHeaderButtons(mFragment, appLinks,
                mShadowContext.getPackageName(),
                AppHeaderController.ActionType.ACTION_APP_PREFERENCE,
                AppHeaderController.ActionType.ACTION_APP_PREFERENCE);
        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.GONE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void bindButton_noStoreLink_shouldNotShowButton() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.app_details, null /* root */);
        when(mContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(null);
        mController.bindAppHeaderButtons(mFragment, appLinks,
                mShadowContext.getPackageName(),
                AppHeaderController.ActionType.ACTION_STORE_DEEP_LINK,
                AppHeaderController.ActionType.ACTION_STORE_DEEP_LINK);
        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.GONE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.GONE);
    }
}
