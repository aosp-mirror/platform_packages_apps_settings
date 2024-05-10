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

package com.android.settings.development;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.test.rule.ActivityTestRule;

import com.android.settings.R;
import com.android.settings.security.ZygoteShadow;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.testutils.shadow.ShadowInteractionJankMonitor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemProperties;

@Ignore("b/313564061")
@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                ZygoteShadow.class,
                ShadowDeviceConfig.class,
                ShadowInteractionJankMonitor.class,
                ShadowRestrictedLockUtilsInternal.class
        })
public class DevelopmentMemtagPreferenceControllerTest {
    private final String mMemtagSupportedProperty = "ro.arm64.memtag.bootctl_supported";

    @Rule
    public ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private DevelopmentMemtagPage mMemtagPage;
    private DevelopmentMemtagPreferenceController mController;
    private Context mContext;
    private TestActivity mActivity;

    private static final String FRAGMENT_TAG = "development_memtag_page";

    @Before
    public void setUp() {
        ShadowSystemProperties.override(mMemtagSupportedProperty, "true");

        mContext = RuntimeEnvironment.application;
        mMemtagPage = new DevelopmentMemtagPage();
        mActivity = mActivityTestRule.getActivity();
        mActivity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(TestActivity.CONTAINER_VIEW_ID, mMemtagPage)
                .commit();
        mController = new DevelopmentMemtagPreferenceController(mContext, FRAGMENT_TAG);
        mController.setFragment(mMemtagPage);
    }

    @Test
    public void getSliceHighlightMenuRes_isMenu_key_security() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(R.string.menu_key_security);
    }

    @Test
    public void setChecked_isChecked_updatesSummary() {
        ZygoteShadow.setSupportsMemoryTagging(true);
        mController.setChecked(true);
        assertThat(mController.getSummary())
                .isEqualTo(mContext.getResources().getString(R.string.memtag_on));
    }

    @Test
    public void setChecked_isUnchecked_updatesSummary() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        mController.setChecked(false);
        assertThat(mController.getSummary())
                .isEqualTo(mContext.getResources().getString(R.string.memtag_off));
    }

    @Test
    public void setChecked_isCheckedPending_updatesSummary() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        mController.setChecked(true);
        assertThat(mController.getSummary())
                .isEqualTo(mContext.getResources().getString(R.string.memtag_on_pending));
    }

    @Test
    public void setChecked_isUncheckedPending_updatesSummary() {
        ZygoteShadow.setSupportsMemoryTagging(true);
        mController.setChecked(false);
        assertThat(mController.getSummary())
                .isEqualTo(mContext.getResources().getString(R.string.memtag_off_pending));
    }

    @Test
    public void setChecked_isCheckedPending_showsDialog() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        mController.setChecked(true);
        onView(withText(R.string.memtag_reboot_title)).inRoot(isDialog());
    }

    @Test
    public void setChecked_isUncheckedPending_showsDialog() {
        ZygoteShadow.setSupportsMemoryTagging(true);
        mController.setChecked(false);
        onView(withText(R.string.memtag_reboot_title)).inRoot(isDialog());
    }

    @Test
    public void setChecked_isChecked_doesNotShowDialog() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        mController.setChecked(false);
        onView(withText(R.string.memtag_reboot_title)).inRoot(isDialog()).check(doesNotExist());
    }

    @Test
    public void setChecked_isUnchecked_doesNotShowDialog() {
        ZygoteShadow.setSupportsMemoryTagging(true);
        mController.setChecked(true);
        onView(withText(R.string.memtag_reboot_title)).inRoot(isDialog()).check(doesNotExist());
    }

    @Test
    public void updateState_disabledByAdmin_disablesPreference() {
        ShadowRestrictedLockUtilsInternal.setMteIsDisabled(true);
        RestrictedSwitchPreference preference = new RestrictedSwitchPreference(mContext);
        mController.updateState(preference);
        assertThat(preference.isDisabledByAdmin()).isTrue();
    }

    private static final class TestActivity extends FragmentActivity {
        private static final int CONTAINER_VIEW_ID = 1234;

        @Override
        protected void onCreate(Bundle bundle) {
            super.onCreate(bundle);

            FragmentContainerView contentView = new FragmentContainerView(this);
            contentView.setId(CONTAINER_VIEW_ID);
            setContentView(contentView);
        }
    }
}
