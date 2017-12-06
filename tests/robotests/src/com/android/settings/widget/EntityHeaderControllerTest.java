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

package com.android.settings.widget;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EntityHeaderControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private Fragment mFragment;

    private Context mShadowContext;
    private LayoutInflater mLayoutInflater;
    private PackageInfo mInfo;
    private EntityHeaderController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest(mContext);
        mShadowContext = RuntimeEnvironment.application;
        when(mActivity.getApplicationContext()).thenReturn(mShadowContext);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mShadowContext);
        mLayoutInflater = LayoutInflater.from(mShadowContext);
        mInfo = new PackageInfo();
        mInfo.versionName = "1234";
    }

    @Test
    public void testBuildView_constructedWithoutView_shouldCreateNewView() {
        mController = EntityHeaderController.newInstance(mActivity, mFragment, null);
        View view = mController.done(mActivity);

        assertThat(view).isNotNull();
    }

    @Test
    public void testBuildView_withContext_shouldBuildPreference() {
        mController = EntityHeaderController.newInstance(mActivity, mFragment, null);
        Preference preference = mController.done(mActivity, mShadowContext);

        assertThat(preference instanceof LayoutPreference).isTrue();
    }

    @Test
    public void testBuildView_constructedWithView_shouldReturnSameView() {
        View inputView = mLayoutInflater.inflate(R.layout.settings_entity_header, null /* root */);
        mController = EntityHeaderController.newInstance(mActivity, mFragment, inputView);
        View view = mController.done(mActivity);

        assertThat(view).isSameAs(inputView);
    }

    @Test
    public void bindViews_shouldBindAllData() {
        final String testString = "test";
        final View header = mLayoutInflater.inflate(
                R.layout.settings_entity_header, null /* root */);
        final TextView label = header.findViewById(R.id.entity_header_title);
        final TextView version = header.findViewById(R.id.entity_header_summary);

        mController = EntityHeaderController.newInstance(mActivity, mFragment, header);
        mController.setLabel(testString);
        mController.setSummary(testString);
        mController.setIcon(mShadowContext.getDrawable(R.drawable.ic_add));
        mController.done(mActivity);

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
                .inflate(R.layout.settings_entity_header, null /* root */);
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(info);

        mController = EntityHeaderController.newInstance(mActivity, mFragment, appLinks);
        mController.setButtonActions(
                EntityHeaderController.ActionType.ACTION_APP_PREFERENCE,
                EntityHeaderController.ActionType.ACTION_NONE);
        mController.done(mActivity);

        final ImageButton button1 = appLinks.findViewById(android.R.id.button1);
        assertThat(button1.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(button1.getDrawable()).isNotNull();
        assertThat(appLinks.findViewById(android.R.id.button2).getVisibility())
                .isEqualTo(View.GONE);
        try {
            appLinks.findViewById(android.R.id.button1).performClick();
        } catch (Exception e) {
            // Ignore exception because the launching intent is fake.
        }
        verify(mFeatureFactory.metricsFeatureProvider).actionWithSource(mContext,
                MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                MetricsProto.MetricsEvent.ACTION_OPEN_APP_SETTING);
        verify(mFragment).startActivity(any(Intent.class));
    }

    @Test
    public void bindButton_noAppPref_shouldNotShowButton() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.settings_entity_header, null /* root */);
        when(mContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(null);

        mController = EntityHeaderController.newInstance(mActivity, mFragment, appLinks);
        mController.setButtonActions(
                EntityHeaderController.ActionType.ACTION_APP_PREFERENCE,
                EntityHeaderController.ActionType.ACTION_NONE);
        mController.done(mActivity);

        final ImageButton button1 = appLinks.findViewById(android.R.id.button1);
        assertThat(button1.getVisibility()).isEqualTo(View.GONE);
        assertThat(button1.getDrawable()).isNull();
        assertThat(appLinks.findViewById(android.R.id.button2).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void bindButton_noAppInfo_shouldNotAttachClickListener() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.settings_entity_header, null /* root */);
        final Activity activity = mock(Activity.class);
        when(mFragment.getActivity()).thenReturn(activity);

        mController = EntityHeaderController.newInstance(mActivity, mFragment, appLinks);
        mController.setPackageName(null)
                .setHasAppInfoLink(true)
                .setButtonActions(
                        EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE);
        mController.done(mActivity);

        assertThat(appLinks.findViewById(android.R.id.button1).getVisibility())
                .isEqualTo(View.GONE);
        assertThat(appLinks.findViewById(android.R.id.button2).getVisibility())
                .isEqualTo(View.GONE);

        appLinks.findViewById(R.id.entity_header_content).performClick();
        verify(mFragment, never()).getActivity();
        verify(activity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void bindButton_hasAppInfo_shouldAttachClickListener() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.settings_entity_header, null /* root */);
        final Activity activity = mock(Activity.class);
        when(mFragment.getActivity()).thenReturn(activity);
        when(mContext.getString(eq(R.string.application_info_label))).thenReturn("App Info");

        mController = EntityHeaderController.newInstance(mActivity, mFragment, appLinks);
        mController.setPackageName("123")
                .setUid(UserHandle.USER_SYSTEM)
                .setHasAppInfoLink(true)
                .setButtonActions(
                        EntityHeaderController.ActionType.ACTION_NOTIF_PREFERENCE,
                        EntityHeaderController.ActionType.ACTION_NONE);
        mController.done(mActivity);

        appLinks.findViewById(R.id.entity_header_content).performClick();
        verify(activity).startActivityForResultAsUser(
                any(Intent.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void iconContentDescription_shouldWorkWithSetIcon() {
        final View view = mLayoutInflater
                .inflate(R.layout.settings_entity_header, null /* root */);
        when(mFragment.getActivity()).thenReturn(mock(Activity.class));
        mController = EntityHeaderController.newInstance(mActivity, mFragment, view);
        String description = "Fake Description";
        mController.setIcon(mShadowContext.getDrawable(R.drawable.ic_add));
        mController.setIconContentDescription(description);
        mController.done(mActivity);
        assertThat(view.findViewById(R.id.entity_header_icon).getContentDescription().toString())
                .isEqualTo(description);
    }

    @Test
    public void iconContentDescription_shouldWorkWithoutSetIcon() {
        final View view = mLayoutInflater
                .inflate(R.layout.settings_entity_header, null /* root */);
        when(mFragment.getActivity()).thenReturn(mock(Activity.class));
        mController = EntityHeaderController.newInstance(mActivity, mFragment, view);
        String description = "Fake Description";
        mController.setIconContentDescription(description);
        mController.done(mActivity);
        assertThat(view.findViewById(R.id.entity_header_icon).getContentDescription().toString())
                .isEqualTo(description);
    }

    @Test
    public void bindButton_hasAppNotifIntent_shouldShowButton() {
        final View appLinks = mLayoutInflater
                .inflate(R.layout.settings_entity_header, null /* root */);

        mController = EntityHeaderController.newInstance(mActivity, mFragment, appLinks);
        mController.setAppNotifPrefIntent(new Intent())
                .setButtonActions(
                        EntityHeaderController.ActionType.ACTION_NOTIF_PREFERENCE,
                        EntityHeaderController.ActionType.ACTION_NONE);
        mController.done(mActivity);

        assertThat(appLinks.findViewById(android.R.id.button1).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(appLinks.findViewById(android.R.id.button2).getVisibility())
                .isEqualTo(View.GONE);
    }

    // Ensure that the instant app label does not show up when we haven't told the controller the
    // app is instant.
    @Test
    public void instantApps_normalAppsDontGetLabel() {
        final View header = mLayoutInflater.inflate(
                R.layout.settings_entity_header, null /* root */);
        mController = EntityHeaderController.newInstance(mActivity, mFragment, header);
        mController.done(mActivity);

        assertThat(header.findViewById(R.id.install_type).getVisibility())
                .isEqualTo(View.GONE);
    }

    // Test that the "instant apps" label is present in the header when we have an instant app.
    @Test
    public void instantApps_expectedHeaderItem() {
        final View header = mLayoutInflater.inflate(
                R.layout.settings_entity_header, null /* root */);
        mController = EntityHeaderController.newInstance(mActivity, mFragment, header);
        mController.setIsInstantApp(true);
        mController.done(mActivity);
        TextView label = header.findViewById(R.id.install_type);

        assertThat(label.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(label.getText()).isEqualTo(
                header.getResources().getString(R.string.install_type_instant));
        assertThat(header.findViewById(R.id.entity_header_summary).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void styleActionBar_invalidObjects_shouldNotCrash() {
        mController = EntityHeaderController.newInstance(mActivity, mFragment, null);
        mController.styleActionBar(null);

        when(mActivity.getActionBar()).thenReturn(null);
        mController.styleActionBar(mActivity);

        verify(mActivity).getActionBar();
    }

    @Test
    public void styleActionBar_setElevationAndBackground() {
        final ActionBar actionBar = mActivity.getActionBar();

        mController = EntityHeaderController.newInstance(mActivity, mFragment, null);
        mController.styleActionBar(mActivity);

        verify(actionBar).setElevation(0);
        // Enforce a color drawable as background here, as image based drawables might not be
        // wide enough to cover entire action bar.
        verify(actionBar).setBackgroundDrawable(any(ColorDrawable.class));
    }

    @Test
    public void initAppHeaderController_appHeaderNull_useFragmentContext() {
        mController = EntityHeaderController.newInstance(mActivity, mFragment, null);

        // Fragment.getContext() is invoked to inflate the view
        verify(mFragment).getContext();
    }
}
