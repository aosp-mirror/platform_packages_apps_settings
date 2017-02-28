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


import android.annotation.IdRes;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppHeaderControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private Fragment mFragment;
    @Mock
    private View mAppHeader;

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
    }

    @Test
    public void testBuildView_constructedWithoutView_shouldCreateNewView() {
        mController = new AppHeaderController(mShadowContext, mFragment, null);
        View view = mController.done();

        assertThat(view).isNotNull();
    }

    @Test
    public void testBuildView_withContext_shouldBuildPreference() {
        mController = new AppHeaderController(mShadowContext, mFragment, null);
        Preference preference = mController.done(mShadowContext);

        assertThat(preference instanceof LayoutPreference).isTrue();
    }

    @Test
    public void testBuildView_constructedWithView_shouldReturnSameView() {
        View inputView = mLayoutInflater.inflate(R.layout.app_details, null /* root */);
        mController = new AppHeaderController(mShadowContext, mFragment, inputView);
        View view = mController.done();

        assertThat(view).isSameAs(inputView);
    }

    @Test
    public void bindViews_shouldBindAllData() {
        final String testString = "test";
        final View appHeader = mLayoutInflater.inflate(R.layout.app_details, null /* root */);
        final TextView label = (TextView) appHeader.findViewById(R.id.app_detail_title);
        final TextView version = (TextView) appHeader.findViewById(R.id.app_detail_summary);

        mController = new AppHeaderController(mShadowContext, mFragment, appHeader);
        mController.setLabel(testString);
        mController.setSummary(testString);
        mController.setIcon(mShadowContext.getDrawable(R.drawable.ic_add));
        mController.done();

        assertThat(label.getText()).isEqualTo(testString);
        assertThat(version.getText()).isEqualTo(testString);
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

        mController = new AppHeaderController(mContext, mFragment, appLinks);
        mController.setButtonActions(
                AppHeaderController.ActionType.ACTION_APP_PREFERENCE,
                AppHeaderController.ActionType.ACTION_NONE);
        mController.done();

        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.GONE);
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

        mController = new AppHeaderController(mContext, mFragment, appLinks);
        mController.setButtonActions(
                AppHeaderController.ActionType.ACTION_APP_PREFERENCE,
                AppHeaderController.ActionType.ACTION_NONE);
        mController.done();

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

        mController = new AppHeaderController(mContext, mFragment, appLinks);
        mController.setButtonActions(
                AppHeaderController.ActionType.ACTION_STORE_DEEP_LINK,
                AppHeaderController.ActionType.ACTION_NONE);
        mController.done();

        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.GONE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void bindButton_noAppInfo_shouldNotShowButton() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.app_details, null /* root */);

        mController = new AppHeaderController(mContext, mFragment, appLinks);
        mController.setPackageName(null)
                .setButtonActions(
                        AppHeaderController.ActionType.ACTION_APP_INFO,
                        AppHeaderController.ActionType.ACTION_NONE);
        mController.done();

        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.GONE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void bindButton_hasAppInfo_shouldShowButton() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.app_details, null /* root */);
        when(mFragment.getActivity()).thenReturn(mock(Activity.class));

        mController = new AppHeaderController(mContext, mFragment, appLinks);
        mController.setPackageName("123")
                .setUid(UserHandle.USER_SYSTEM)
                .setButtonActions(
                        AppHeaderController.ActionType.ACTION_APP_INFO,
                        AppHeaderController.ActionType.ACTION_NOTIF_PREFERENCE);
        mController.done();

        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void bindButton_hasAppNotifIntent_shouldShowButton() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.app_details, null /* root */);

        mController = new AppHeaderController(mContext, mFragment, appLinks);
        mController.setAppNotifPrefIntent(new Intent())
                .setButtonActions(
                        AppHeaderController.ActionType.ACTION_NOTIF_PREFERENCE,
                        AppHeaderController.ActionType.ACTION_NONE);
        mController.done();

        assertThat(appLinks.findViewById(R.id.left_button).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(appLinks.findViewById(R.id.right_button).getVisibility())
                .isEqualTo(View.GONE);
    }

    // Ensure that the instant app label does not show up when we haven't told the controller the
    // app is instant.
    @Test
    public void instantApps_normalAppsDontGetLabel() {
        final View appHeader = mLayoutInflater.inflate(R.layout.app_details, null /* root */);
        mController = new AppHeaderController(mContext, mFragment, appHeader);
        mController.done();
        assertThat(appHeader.findViewById(R.id.install_type).getVisibility())
                .isEqualTo(View.GONE);
    }

    // Test that the "instant apps" label is present in the header when we have an instant app.
    @Test
    public void instantApps_expectedHeaderItem() {
        final View appHeader = mLayoutInflater.inflate(R.layout.app_details, null /* root */);
        mController = new AppHeaderController(mContext, mFragment, appHeader);
        mController.setIsInstantApp(true);
        mController.done();
        TextView label = (TextView)appHeader.findViewById(R.id.install_type);
        assertThat(label.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(label.getText()).isEqualTo(
                appHeader.getResources().getString(R.string.install_type_instant));
    }
}
