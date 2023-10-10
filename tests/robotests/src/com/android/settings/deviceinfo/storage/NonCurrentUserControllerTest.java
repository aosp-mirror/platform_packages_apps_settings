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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.SparseArray;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.drawable.UserIconDrawable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowActivityManager.class})
public class NonCurrentUserControllerTest {

    private static final String TEST_NAME = "Fred";
    private static final String TARGET_PREFERENCE_GROUP_KEY = "pref_secondary_users";
    @Mock
    private UserManager mUserManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceGroup mGroup;
    @Mock
    private IActivityManager mActivityService;

    private Context mContext;
    private NonCurrentUserController mController;
    private UserInfo mPrimaryUser;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPrimaryUser = new UserInfo();
        mPrimaryUser.flags = UserInfo.FLAG_PRIMARY | UserInfo.FLAG_FULL;
        mController = new NonCurrentUserController(mContext, mPrimaryUser);
        ShadowActivityManager.setService(mActivityService);

        when(mScreen.getContext()).thenReturn(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mGroup);
        when(mGroup.getKey()).thenReturn(TARGET_PREFERENCE_GROUP_KEY);

    }

    @After
    public void tearDown() {
        ShadowActivityManager.setCurrentUser(mPrimaryUser.id);
    }

    @Test
    public void controllerAddsSecondaryUser() {
        mPrimaryUser.name = TEST_NAME;
        mController.displayPreference(mScreen);

        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mGroup).addPreference(argumentCaptor.capture());
        final Preference preference = argumentCaptor.getValue();
        assertThat(preference.getTitle()).isEqualTo(TEST_NAME);
    }

    @Test
    public void controllerUpdatesSummaryOfNewPreference() {
        mPrimaryUser.name = TEST_NAME;
        mController.displayPreference(mScreen);
        mController.setSize(MEGABYTE_IN_BYTES * 10, false /* animate */);
        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);

        verify(mGroup).addPreference(argumentCaptor.capture());

        final Preference preference = argumentCaptor.getValue();
        assertThat(preference.getSummary()).isEqualTo("10 MB");
    }

    @Test
    public void noNonCurrentUserAddedIfNoneExist() {
        final ArrayList<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(mPrimaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        final List<NonCurrentUserController> controllers =
                NonCurrentUserController.getNonCurrentUserControllers(mContext, mUserManager);
        assertThat(controllers).hasSize(0);
    }

    @Test
    public void getNonCurrentUserControllers_notWorkProfile_addNonCurrentUserController() {
        final ArrayList<UserInfo> userInfos = new ArrayList<>();
        final UserInfo secondaryUser = spy(new UserInfo());
        secondaryUser.id = 10;
        secondaryUser.profileGroupId = 101010; // this just has to be something not 0
        when(secondaryUser.isManagedProfile()).thenReturn(false);
        userInfos.add(mPrimaryUser);
        userInfos.add(secondaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        ShadowActivityManager.setCurrentUser(secondaryUser.id);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        final List<NonCurrentUserController> controllers =
                NonCurrentUserController.getNonCurrentUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(1);
    }

    @Test
    public void getNonCurrentUserControllers_workProfileOfNonCurrentUser() {
        final ArrayList<UserInfo> userInfos = new ArrayList<>();
        final UserInfo secondaryUser = spy(new UserInfo());
        secondaryUser.id = 10;
        secondaryUser.profileGroupId = 101010; // this just has to be something not 0
        when(secondaryUser.isManagedProfile()).thenReturn(true);
        userInfos.add(mPrimaryUser);
        userInfos.add(secondaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(secondaryUser.isProfile()).thenReturn(true);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        final List<NonCurrentUserController> controllers =
                NonCurrentUserController.getNonCurrentUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(0);
    }

    @Test
    public void profilesOfCurrentUserAreIgnored() {
        final ArrayList<UserInfo> userInfos = new ArrayList<>();
        final UserInfo secondaryUser = new UserInfo();
        secondaryUser.id = mPrimaryUser.id;
        userInfos.add(mPrimaryUser);
        userInfos.add(secondaryUser);
        userInfos.add(secondaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);

        final List<NonCurrentUserController> controllers =
                NonCurrentUserController.getNonCurrentUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(0);
    }

    @Test
    public void handleResult_noStatsResult_shouldShowCachedData() {
        mPrimaryUser.name = TEST_NAME;
        mPrimaryUser.id = 10;
        int[] profiles = {mPrimaryUser.id};
        mController = new NonCurrentUserController(mContext, mPrimaryUser, profiles);
        mController.displayPreference(mScreen);
        final StorageAsyncLoader.StorageResult userResult =
                new StorageAsyncLoader.StorageResult();
        final SparseArray<StorageAsyncLoader.StorageResult> result = new SparseArray<>();
        userResult.externalStats =
                new StorageStatsSource.ExternalStorageStats(
                        MEGABYTE_IN_BYTES * 30,
                        MEGABYTE_IN_BYTES * 10,
                        MEGABYTE_IN_BYTES * 10,
                        MEGABYTE_IN_BYTES * 10, 0);
        result.put(10, userResult);
        // Cache size info at first time
        mController.handleResult(result);

        // Retrieve cache size info if stats result is null
        mController.handleResult(null);
        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mGroup).addPreference(argumentCaptor.capture());
        final Preference preference = argumentCaptor.getValue();

        assertThat(preference.getSummary()).isEqualTo("30 MB");
    }

    @Test
    public void dontAddPrimaryProfileAsASecondaryProfile() {
        final ArrayList<UserInfo> userInfos = new ArrayList<>();
        // The primary UserInfo may be a different object with a different name... but represent the
        // same user!
        final UserInfo primaryUserRenamed = new UserInfo();
        primaryUserRenamed.name = "Owner";
        primaryUserRenamed.flags = UserInfo.FLAG_PRIMARY;
        userInfos.add(primaryUserRenamed);
        ShadowActivityManager.setCurrentUser(primaryUserRenamed.id);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        final List<NonCurrentUserController> controllers =
                NonCurrentUserController.getNonCurrentUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(0);
    }

    @Test
    public void iconCallbackChangesPreferenceIcon() {
        final SparseArray<Drawable> icons = new SparseArray<>();
        final UserIconDrawable drawable = mock(UserIconDrawable.class);
        when(drawable.mutate()).thenReturn(drawable);
        mPrimaryUser.name = TEST_NAME;
        mPrimaryUser.id = 10;
        icons.put(mPrimaryUser.id, drawable);
        mController.displayPreference(mScreen);

        mController.handleUserIcons(icons);

        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mGroup).addPreference(argumentCaptor.capture());
        final Preference preference = argumentCaptor.getValue();
        assertThat(preference.getIcon()).isEqualTo(drawable);
    }

    @Test
    public void setIcon_doesntNpeOnNullPreference() {
        final SparseArray<Drawable> icons = new SparseArray<>();
        final UserIconDrawable drawable = mock(UserIconDrawable.class);
        mPrimaryUser.name = TEST_NAME;
        mPrimaryUser.id = 10;
        icons.put(mPrimaryUser.id, drawable);

        mController.handleUserIcons(icons);

        // Doesn't crash
    }

    @Test
    public void getNonCurrentUserControllers_switchUsers() {
        final ArrayList<UserInfo> userInfo = new ArrayList<>();
        final UserInfo secondaryUser = spy(new UserInfo());
        secondaryUser.id = 10;
        final UserInfo secondaryUser1 = spy(new UserInfo());
        secondaryUser1.id = 11;
        secondaryUser.flags = UserInfo.FLAG_FULL;
        secondaryUser1.flags = UserInfo.FLAG_FULL;
        userInfo.add(mPrimaryUser);
        userInfo.add(secondaryUser);
        userInfo.add(secondaryUser1);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfo);
        List<NonCurrentUserController> controllers =
                NonCurrentUserController.getNonCurrentUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(2);
        assertThat(controllers.get(0).getUser().id == secondaryUser.id).isTrue();

        ShadowActivityManager.setCurrentUser(secondaryUser.id);
        when(mUserManager.getUsers()).thenReturn(userInfo);
        controllers =
                NonCurrentUserController.getNonCurrentUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(2);
        assertThat(controllers.get(0).getUser().id == mPrimaryUser.id).isTrue();
    }
}
