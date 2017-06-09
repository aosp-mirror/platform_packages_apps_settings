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
 * limitations under the License
 */

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;



@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MasterClearTest {

    @Mock
    private MasterClear mMasterClear;
    @Mock
    private ScrollView mScrollView;
    @Mock
    private LinearLayout mLinearLayout;
    private ShadowActivity mShadowActivity;
    private Activity mActivity;
    private View mContentView;

    private class ActivityForTest extends SettingsActivity {
        private Bundle mArgs;

        @Override
        public void startPreferencePanel(Fragment caller, String fragmentClass, Bundle args,
            int titleRes, CharSequence titleText, Fragment resultTo, int resultRequestCode) {
            mArgs = args;
        }

        public Bundle getArgs() {
            return mArgs;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mMasterClear = spy(new MasterClear());
        mActivity = Robolectric.setupActivity(Activity.class);
        mShadowActivity = shadowOf(mActivity);
        mContentView = LayoutInflater.from(mActivity).inflate(R.layout.master_clear, null);

        // Make scrollView only have one child
        when(mScrollView.getChildAt(0)).thenReturn(mLinearLayout);
        when(mScrollView.getChildCount()).thenReturn(1);
    }

    @Test
    public void testShowWipeEuicc_euiccDisabled() {
        prepareEuiccState(
                false /* isEuiccEnabled */, true /* isEuiccProvisioned */,
                true /* isDevelopmentSettingsEnabled */);
        assertThat(mMasterClear.showWipeEuicc()).isFalse();
    }

    @Test
    public void testShowWipeEuicc_euiccEnabled_unprovisioned() {
        prepareEuiccState(
                true /* isEuiccEnabled */, false /* isEuiccProvisioned */,
                false /* isDevelopmentSettingsEnabled */);
        assertThat(mMasterClear.showWipeEuicc()).isFalse();
    }

    @Test
    public void testShowWipeEuicc_euiccEnabled_provisioned() {
        prepareEuiccState(
                true /* isEuiccEnabled */, true /* isEuiccProvisioned */,
                false /* isDevelopmentSettingsEnabled */);
        assertThat(mMasterClear.showWipeEuicc()).isTrue();
    }

    @Test
    public void testShowWipeEuicc_euiccEnabled_developmentSettingsEnabled() {
        prepareEuiccState(
                true /* isEuiccEnabled */, false /* isEuiccProvisioned */,
                true /* isDevelopmentSettingsEnabled */);
        assertThat(mMasterClear.showWipeEuicc()).isTrue();
    }

    @Test
    public void testShowWipeEuicc_euiccEnabled_provisioned_developmentSettingsEnabled() {
        prepareEuiccState(
                true /* isEuiccEnabled */, true /* isEuiccProvisioned */,
                true /* isDevelopmentSettingsEnabled */);
        assertThat(mMasterClear.showWipeEuicc()).isTrue();
    }

    private void prepareEuiccState(
            boolean isEuiccEnabled,
            boolean isEuiccProvisioned,
            boolean isDevelopmentSettingsEnabled) {
        doReturn(mActivity).when(mMasterClear).getContext();
        doReturn(isEuiccEnabled).when(mMasterClear).isEuiccEnabled(any());
        ContentResolver cr = mActivity.getContentResolver();
        Settings.Global.putInt(
                cr, android.provider.Settings.Global.EUICC_PROVISIONED, isEuiccProvisioned ? 1 : 0);
        Settings.Global.putInt(
                cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                isDevelopmentSettingsEnabled ? 1 : 0);
    }

    @Test
    public void testShowFinalConfirmation_EraseEsimChecked() {
        ActivityForTest testActivity = new ActivityForTest();
        when(mMasterClear.getActivity()).thenReturn(testActivity);

        mMasterClear.mEsimStorage = (CheckBox) mContentView.findViewById(R.id.erase_esim);
        mMasterClear.mExternalStorage = (CheckBox) mContentView.findViewById(R.id.erase_external);
        mMasterClear.mEsimStorage.setChecked(true);
        mMasterClear.showFinalConfirmation();
        assertThat(testActivity.getArgs().getBoolean(MasterClear.ERASE_ESIMS_EXTRA, false))
                .isTrue();
    }

    @Test
    public void testShowFinalConfirmation_EraseEsimUnchecked() {
        ActivityForTest testActivity = new ActivityForTest();
        when(mMasterClear.getActivity()).thenReturn(testActivity);

        mMasterClear.mEsimStorage = (CheckBox) mContentView.findViewById(R.id.erase_esim);
        mMasterClear.mExternalStorage = (CheckBox) mContentView.findViewById(R.id.erase_external);
        mMasterClear.mEsimStorage.setChecked(false);
        mMasterClear.showFinalConfirmation();
        assertThat(testActivity.getArgs().getBoolean(MasterClear.ERASE_ESIMS_EXTRA, true))
                .isFalse();
    }

    @Test
    public void testHasReachedBottom_NotScrollDown_returnFalse() {
        initScrollView(100, 0, 200);

        assertThat(mMasterClear.hasReachedBottom(mScrollView)).isFalse();
    }

    @Test
    public void testHasReachedBottom_CanNotScroll_returnTrue() {
        initScrollView(100, 0, 80);

        assertThat(mMasterClear.hasReachedBottom(mScrollView)).isTrue();
    }

    @Test
    public void testHasReachedBottom_ScrollToBottom_returnTrue() {
        initScrollView(100, 100, 200);

        assertThat(mMasterClear.hasReachedBottom(mScrollView)).isTrue();
    }

    private void initScrollView(int height, int scrollY, int childBottom) {
        when(mScrollView.getHeight()).thenReturn(height);
        when(mScrollView.getScrollY()).thenReturn(scrollY);
        when(mLinearLayout.getBottom()).thenReturn(childBottom);
    }
}
