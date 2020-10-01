/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.interactacrossprofiles;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class InteractAcrossProfilesControllerTest {
    private static final int PERSONAL_PROFILE_ID = 0;
    private static final int WORK_PROFILE_ID = 10;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final UserManager mUserManager = mContext.getSystemService(UserManager.class);
    private final InteractAcrossProfilesController mController =
            new InteractAcrossProfilesController(mContext, "test_key");

    @Test
    public void getAvailabilityStatus_multipleProfiles_returnsAvailable() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        shadowOf(mUserManager).addProfile(
                PERSONAL_PROFILE_ID,
                WORK_PROFILE_ID,
                "work-profile"/* profileName */,
                UserInfo.FLAG_MANAGED_PROFILE);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_oneProfile_returnsDisabled() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }
}
