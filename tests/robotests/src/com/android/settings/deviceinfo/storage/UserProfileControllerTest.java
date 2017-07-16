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

package com.android.settings.deviceinfo.storage;

import static com.google.common.truth.Truth.assertThat;
import static com.android.settings.utils.FileSizeFormatter.MEGABYTE_IN_BYTES;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;

import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.SubSettings;
import com.android.settings.TestConfig;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.deviceinfo.StorageProfileFragment;
import com.android.settingslib.R;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.drawable.UserIconDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UserProfileControllerTest {
    private static final String TEST_NAME = "Fred";

    @Mock
    private UserManagerWrapper mUserManager;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private UserProfileController mController;
    private UserInfo mPrimaryProfile;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPrimaryProfile = new UserInfo();
        mController = new UserProfileController(mContext, mPrimaryProfile, mUserManager, 0);
        when(mScreen.getContext()).thenReturn(mContext);
        mPrimaryProfile.name = TEST_NAME;
        mPrimaryProfile.id = 10;
        mController.displayPreference(mScreen);
    }

    @Test
    public void controllerAddsPrimaryProfilePreference() throws Exception {
        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mScreen).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();

        assertThat(preference.getTitle()).isEqualTo(TEST_NAME);
        assertThat(preference.getKey()).isEqualTo("pref_profile_10");
    }

    @Test
    public void tappingProfilePreferenceSendsToStorageProfileFragment() throws Exception {

        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mScreen).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();
        assertThat(mController.handlePreferenceTreeClick(preference)).isTrue();
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                StorageProfileFragment.class.getName());
    }

    @Test
    public void acceptingResultUpdatesPreferenceSize() throws Exception {
        SparseArray<StorageAsyncLoader.AppsStorageResult> result = new SparseArray<>();
        StorageAsyncLoader.AppsStorageResult userResult =
                new StorageAsyncLoader.AppsStorageResult();
        userResult.externalStats =
                new StorageStatsSource.ExternalStorageStats(
                        99 * MEGABYTE_IN_BYTES,
                        33 * MEGABYTE_IN_BYTES,
                        33 * MEGABYTE_IN_BYTES,
                        33 * MEGABYTE_IN_BYTES, 0);
        result.put(10, userResult);

        mController.handleResult(result);
        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mScreen).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();

        assertThat(preference.getSummary()).isEqualTo("0.10 GB");
    }

    @Test
    public void iconCallbackChangesPreferenceIcon() throws Exception {
        SparseArray<Drawable> icons = new SparseArray<>();
        Bitmap userBitmap =
                BitmapFactory.decodeResource(
                        RuntimeEnvironment.application.getResources(), R.drawable.home);
        UserIconDrawable drawable = new UserIconDrawable(100 /* size */).setIcon(userBitmap).bake();
        icons.put(10, drawable);

        mController.handleUserIcons(icons);

        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mScreen).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();
        assertThat(preference.getIcon()).isEqualTo(drawable);
    }
}
