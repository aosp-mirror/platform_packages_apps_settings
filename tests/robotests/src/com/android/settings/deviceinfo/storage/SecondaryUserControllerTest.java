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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.core.PreferenceController;

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

    private Context mContext;
    private SecondaryUserController mController;
    private UserInfo mPrimaryUser;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPrimaryUser = new UserInfo();
        mController = new SecondaryUserController(mContext, mPrimaryUser);
    }

    @Test
    public void controllerAddsSecondaryUser() throws Exception {
        mPrimaryUser.name = TEST_NAME;
        PreferenceScreen screen = mock(PreferenceScreen.class);
        PreferenceGroup group = mock(PreferenceGroup.class);
        when(screen.findPreference(anyString())).thenReturn(group);
        when(group.getKey()).thenReturn(TARGET_PREFERENCE_GROUP_KEY);
        mController.displayPreference(screen);

        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(group).addPreference(argumentCaptor.capture());
        Preference preference = argumentCaptor.getValue();
        assertThat(preference.getTitle()).isEqualTo(TEST_NAME);
    }

    @Test
    public void controllerUpdatesSummaryOfNewPreference() throws Exception {
        mPrimaryUser.name = TEST_NAME;
        PreferenceScreen screen = mock(PreferenceScreen.class);
        PreferenceGroup group = mock(PreferenceGroup.class);
        when(screen.findPreference(anyString())).thenReturn(group);
        when(group.getKey()).thenReturn(TARGET_PREFERENCE_GROUP_KEY);
        mController.displayPreference(screen);
        mController.setSize(10L);
        final ArgumentCaptor<Preference> argumentCaptor = ArgumentCaptor.forClass(Preference.class);

        verify(group).addPreference(argumentCaptor.capture());

        Preference preference = argumentCaptor.getValue();
        assertThat(preference.getSummary()).isEqualTo("10.00B");
    }

    @Test
    public void noSecondaryUserAddedIfNoneExist() throws Exception {
        ArrayList<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(mPrimaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);
        List<PreferenceController> controllers =
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
        List<PreferenceController> controllers =
                SecondaryUserController.getSecondaryUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(1);
        assertThat(controllers.get(0) instanceof SecondaryUserController).isTrue();
    }

    @Test
    public void profilesOfPrimaryUserAreIgnored() throws Exception {
        ArrayList<UserInfo> userInfos = new ArrayList<>();
        UserInfo secondaryUser = new UserInfo();
        secondaryUser.id = mPrimaryUser.id;
        userInfos.add(mPrimaryUser);
        userInfos.add(secondaryUser);
        when(mUserManager.getPrimaryUser()).thenReturn(mPrimaryUser);
        when(mUserManager.getUsers()).thenReturn(userInfos);

        List<PreferenceController> controllers =
                SecondaryUserController.getSecondaryUserControllers(mContext, mUserManager);

        assertThat(controllers).hasSize(1);
        assertThat(controllers.get(0) instanceof SecondaryUserController).isFalse();
    }
}
