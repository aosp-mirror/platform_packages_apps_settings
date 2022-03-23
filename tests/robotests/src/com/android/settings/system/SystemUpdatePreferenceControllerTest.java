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
package com.android.settings.system;

import static android.os.SystemUpdateManager.KEY_STATUS;
import static android.os.SystemUpdateManager.KEY_TITLE;
import static android.os.SystemUpdateManager.STATUS_IDLE;
import static android.os.SystemUpdateManager.STATUS_UNKNOWN;
import static android.os.SystemUpdateManager.STATUS_WAITING_DOWNLOAD;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemUpdateManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUserManager.class)
public class SystemUpdatePreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SystemUpdateManager mSystemUpdateManager;

    private Context mContext;
    private ShadowUserManager mShadowUserManager;
    private SystemUpdatePreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mShadowUserManager = ShadowUserManager.getShadow();

        ShadowApplication.getInstance().setSystemService(Context.SYSTEM_UPDATE_SERVICE,
                mSystemUpdateManager);
        mController = new SystemUpdatePreferenceController(mContext);
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @After
    public void cleanUp() {
        mShadowUserManager.setIsAdminUser(false);
    }

    @Test
    public void updateNonIndexable_ifAvailable_shouldNotUpdate() {
        final List<String> keys = new ArrayList<>();
        mShadowUserManager.setIsAdminUser(true);

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }

    @Test
    public void updateNonIndexable_ifNotAvailable_shouldUpdate() {
        mShadowUserManager.setIsAdminUser(false);
        final List<String> keys = new ArrayList<>();

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).hasSize(1);
    }

    @Test
    public void displayPrefs_ifVisible_butNotAdminUser_shouldNotDisplay() {
        mShadowUserManager.setIsAdminUser(false);
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void displayPrefs_ifAdminUser_butNotVisible_shouldNotDisplay() {
        mShadowUserManager.setIsAdminUser(true);
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPrefs_ifAvailable_shouldDisplay() {
        mShadowUserManager.setIsAdminUser(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_systemUpdateStatusUnknown_shouldSetToAndroidVersion() {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_STATUS, STATUS_UNKNOWN);
        when(mSystemUpdateManager.retrieveSystemUpdateInfo()).thenReturn(bundle);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.android_version_summary,
                        Build.VERSION.RELEASE_OR_CODENAME));
    }

    @Test
    public void updateState_systemUpdateStatusIdle_shouldSetToAndroidVersion() {
        final String testReleaseName = "ANDROID TEST VERSION";

        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_STATUS, STATUS_IDLE);
        bundle.putString(KEY_TITLE, testReleaseName);
        when(mSystemUpdateManager.retrieveSystemUpdateInfo()).thenReturn(bundle);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.android_version_summary, testReleaseName));
    }

    @Test
    public void updateState_systemUpdateInProgress_shouldSetToUpdatePending() {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_STATUS, STATUS_WAITING_DOWNLOAD);
        when(mSystemUpdateManager.retrieveSystemUpdateInfo()).thenReturn(bundle);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.android_version_pending_update_summary));
    }
}