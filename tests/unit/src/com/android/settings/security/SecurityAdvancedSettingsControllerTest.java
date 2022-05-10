/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.CrossProfileApps;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class SecurityAdvancedSettingsControllerTest {

    private static final String PREFERENCE_KEY = "security_advanced_settings";
    private static final String NO_WORK_PROFILE_SUMMARY_RESOURCE_NAME =
            "security_advanced_settings_no_work_profile_settings_summary";
    private static final String WORK_PROFILE_SUMMARY_RESOURCE_NAME =
            "security_advanced_settings_work_profile_settings_summary";

    @Mock
    private CrossProfileApps mCrossProfileApps;
    private Context mContext;

    private SecurityAdvancedSettingsController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(CrossProfileApps.class)).thenReturn(mCrossProfileApps);

        mController = new SecurityAdvancedSettingsController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void getSummary_withoutWorkProfile() {
        when(mCrossProfileApps.getTargetUserProfiles()).thenReturn(Collections.emptyList());

        assertThat(mController.getSummary())
                .isEqualTo(getResourcesString(NO_WORK_PROFILE_SUMMARY_RESOURCE_NAME));
    }

    @Test
    public void getSummary_withWorkProfile() {
        when(mCrossProfileApps.getTargetUserProfiles()).thenReturn(
                Collections.singletonList(new UserHandle(0))
        );

        assertThat(mController.getSummary())
                .isEqualTo(getResourcesString(WORK_PROFILE_SUMMARY_RESOURCE_NAME));
    }

    private String getResourcesString(String resName) {
        return ResourcesUtils.getResourcesString(mContext, resName);
    }
}
