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
package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SystemUpdatePreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PreferenceScreen mScreen;

    private SystemUpdatePreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new SystemUpdatePreferenceController(mContext);
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void updateNonIndexable_ifAvailable_shouldNotUpdate() {
        final List<String> keys = new ArrayList<>();
        when(mUserManager.isAdminUser()).thenReturn(true);

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }

    @Test
    public void updateNonIndexable_ifNotAvailable_shouldUpdate() {
        // mUserManager.isAdminUser() returns false here
        final List<String> keys = new ArrayList<>();

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).hasSize(1);
    }

    @Test
    public void displayPrefs_ifNotAvailable_shouldNotDisplay() {
        // mUserManager.isAdminUser() returns false here
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPrefs_ifAvailable_shouldDisplay() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_shouldSetToAndroidVersion() {
        mController = new SystemUpdatePreferenceController(RuntimeEnvironment.application);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.about_summary,
                        Build.VERSION.RELEASE));
    }
}
