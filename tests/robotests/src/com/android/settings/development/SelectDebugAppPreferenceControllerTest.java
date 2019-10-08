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

package com.android.settings.development;

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes
        .REQUEST_CODE_DEBUG_APP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SelectDebugAppPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private SelectDebugAppPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new SelectDebugAppPreferenceController(mContext, mFragment));
        ReflectionHelpers
            .setField(mController, "mPackageManager" /* field name */, mPackageManager);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void handlePreferenceTreeClick_preferenceClicked_launchActivity() {
        final Intent activityStartIntent = new Intent(mContext, AppPicker.class);
        final String preferenceKey = mController.getPreferenceKey();
        doReturn(activityStartIntent).when(mController).getActivityStartIntent();
        when(mPreference.getKey()).thenReturn(preferenceKey);
        mController.handlePreferenceTreeClick(mPreference);

        verify(mFragment).startActivityForResult(activityStartIntent, REQUEST_CODE_DEBUG_APP);
    }

    @Test
    public void updateState_foobarAppSelected_shouldUpdateSummaryWithDebugAppLabel() {
        final String debugApp = "foobar";
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, Settings.Global.DEBUG_APP, debugApp);
        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.debug_app_set, debugApp));
    }

    @Test
    public void updateState_noAppSelected_shouldUpdateSummaryWithNoAppSelected() {
        final String debugApp = null;
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, Settings.Global.DEBUG_APP, debugApp);
        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.debug_app_not_set));
    }

    @Test
    public void onActivityResult_foobarAppSelected_shouldUpdateSummaryWithDebugLabel() {
        Intent activityResultIntent = new Intent(mContext, AppPicker.class);
        final String appLabel = "foobar";
        activityResultIntent.setAction(appLabel);
        final boolean result = mController
            .onActivityResult(REQUEST_CODE_DEBUG_APP, Activity.RESULT_OK, activityResultIntent);

        assertThat(result).isTrue();
        verify(mPreference).setSummary(mContext.getString(R.string.debug_app_set, appLabel));
    }

    @Test
    public void onActivityResult_badRequestCode_shouldReturnFalse() {
        assertThat(mController.onActivityResult(
                -1 /* requestCode */, -1 /* resultCode */, null /* intent */)).isFalse();
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setSummary(mContext.getString(R.string.debug_app_not_set));
    }
}
