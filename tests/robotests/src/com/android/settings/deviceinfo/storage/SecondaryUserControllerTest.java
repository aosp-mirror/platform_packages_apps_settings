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

import static com.android.settings.utils.FileSizeFormatter.MEGABYTE_IN_BYTES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settingslib.R;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawable.UserIconDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SecondaryUserControllerTest {
    private static final String TEST_NAME = "Fred";
    private static final String TARGET_PREFERENCE_GROUP_KEY = "pref_secondary_users";
    @Mock
    private UserManagerWrapper mUserManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceGroup mGroup;

    private Context mContext;
    private SecondaryUserController mController;
    private UserInfo mPrimaryUser;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPrimaryUser = new UserInfo();
        mPrimaryUser.flags = UserInfo.FLAG_PRIMARY;
        mController = new SecondaryUserController(mContext, mPrimaryUser);

        when(mScreen.getContext()).thenReturn(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mGroup);
        when(mGroup.getKey()).thenReturn(TARGET_PREFERENCE_GROUP_KEY);
    }

    @Test
    public void controllerAddsSecondaryUser() throws Exception {
        mPrimaryUser.name = TEST_NAME;
        mController.displayPreference(mScreen);

        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mGroup).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();
        assertThat(preference.getTitle()).isEqualTo(TEST_NAME);
    }

    @Test
    public void controllerUpdatesSummaryOfNewPreference() throws Exception {
        mPrimaryUser.name = TEST_NAME;
        mController.displayPreference(mScreen);
        mController.setSize(MEGABYTE_IN_BYTES * 10);
        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);

        verify(mGroup).addPreference(argumentCaptor.capture());

        Preference preference = argumentCaptor.getValue();
        assertThat(preference.getSummary()).isEqualTo("0.01 GB");
    }

    @Test
    public void noSecondaryUserAddedIfNoneExist() throws Exception {
        ArrayList<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(mPrimaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        List<AbstractPreferenceController> controllers =
                SecondaryUserController.getSecondaryUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(1);
        // We should have the NoSecondaryUserController.
        assertThat(controllers.get(0) instanceof SecondaryUserController).isFalse();
    }

    @Test
    public void secondaryUserAddedIfHasDistinctId() throws Exception {
        ArrayList<UserInfo> userInfos = new ArrayList<>();
        UserInfo secondaryUser = new UserInfo();
        secondaryUser.id = 10;
        secondaryUser.profileGroupId = 101010; // this just has to be something not 0
        userInfos.add(mPrimaryUser);
        userInfos.add(secondaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        List<AbstractPreferenceController> controllers =
                SecondaryUserController.getSecondaryUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(1);
        assertThat(controllers.get(0) instanceof SecondaryUserController).isTrue();
    }

    @Test
    public void profilesOfPrimaryUserAreNotIgnored() throws Exception {
        ArrayList<UserInfo> userInfos = new ArrayList<>();
        UserInfo secondaryUser = new UserInfo();
        secondaryUser.id = mPrimaryUser.id;
        userInfos.add(mPrimaryUser);
        userInfos.add(secondaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);

        List<AbstractPreferenceController> controllers =
                SecondaryUserController.getSecondaryUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(2);
        assertThat(controllers.get(0) instanceof UserProfileController).isTrue();
        assertThat(controllers.get(1) instanceof SecondaryUserController).isFalse();
    }

    @Test
    public void controllerUpdatesPreferenceOnAcceptingResult() throws Exception {
        mPrimaryUser.name = TEST_NAME;
        mPrimaryUser.id = 10;
        mController.displayPreference(mScreen);
        StorageAsyncLoader.AppsStorageResult userResult =
                new StorageAsyncLoader.AppsStorageResult();
        SparseArray<StorageAsyncLoader.AppsStorageResult> result = new SparseArray<>();
        userResult.externalStats =
                new StorageStatsSource.ExternalStorageStats(
                        MEGABYTE_IN_BYTES * 30,
                        MEGABYTE_IN_BYTES * 10,
                        MEGABYTE_IN_BYTES * 10,
                        MEGABYTE_IN_BYTES * 10, 0);
        result.put(10, userResult);

        mController.handleResult(result);
        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mGroup).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();

        assertThat(preference.getSummary()).isEqualTo("0.03 GB");
    }

    @Test
    public void dontAddPrimaryProfileAsASecondaryProfile() throws Exception {
        ArrayList<UserInfo> userInfos = new ArrayList<>();
        // The primary UserInfo may be a different object with a different name... but represent the
        // same user!
        UserInfo primaryUserRenamed = new UserInfo();
        primaryUserRenamed.name = "Owner";
        primaryUserRenamed.flags = UserInfo.FLAG_PRIMARY;
        userInfos.add(primaryUserRenamed);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        List<AbstractPreferenceController> controllers =
                SecondaryUserController.getSecondaryUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(1);
        // We should have the NoSecondaryUserController.
        assertThat(controllers.get(0) instanceof SecondaryUserController).isFalse();
    }

    @Test
    public void iconCallbackChangesPreferenceIcon() throws Exception {
        SparseArray<Drawable> icons = new SparseArray<>();
        Bitmap userBitmap =
                BitmapFactory.decodeResource(
                        RuntimeEnvironment.application.getResources(), R.drawable.home);
        UserIconDrawable drawable = new UserIconDrawable(100 /* size */).setIcon(userBitmap).bake();
        icons.put(10, drawable);
        mPrimaryUser.name = TEST_NAME;
        mPrimaryUser.id = 10;
        mController.displayPreference(mScreen);

        mController.handleUserIcons(icons);

        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mGroup).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();
        assertThat(preference.getIcon()).isEqualTo(drawable);
    }

    @Test
    public void setIcon_doesntNpeOnNullPreference() throws Exception {
        SparseArray<Drawable> icons = new SparseArray<>();
        Bitmap userBitmap =
                BitmapFactory.decodeResource(
                        RuntimeEnvironment.application.getResources(), R.drawable.home);
        UserIconDrawable drawable = new UserIconDrawable(100 /* size */).setIcon(userBitmap).bake();
        icons.put(10, drawable);
        mPrimaryUser.name = TEST_NAME;
        mPrimaryUser.id = 10;

        mController.handleUserIcons(icons);

        // Doesn't crash
    }
}
