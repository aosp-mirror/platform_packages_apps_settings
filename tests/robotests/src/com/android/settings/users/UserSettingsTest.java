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

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.RestrictedPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class UserSettingsTest {

    private static final String KEY_USER_GUEST = "user_guest";
    private int mProvisioned;

    @Mock
    private Drawable mDefaultIconDrawable;
    @Mock
    private PreferenceManager mMockPreferenceManager;
    @Mock
    private UserPreference mMePreference;
    @Mock
    private UserManager mUserManager;
    @Mock
    private SummaryLoader mSummaryLoader;

    private Activity mActivity;
    private Context mContext;
    private UserSettings mFragment;
    private UserCapabilities mUserCapabilities;
    private SummaryLoader.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(Robolectric.buildActivity(Activity.class).get());
        mContext = spy(RuntimeEnvironment.application);
        mFragment = spy(new UserSettings());
        ReflectionHelpers.setField(mFragment, "mAddUserWhenLockedPreferenceController",
                mock(AddUserWhenLockedPreferenceController.class));
        ReflectionHelpers.setField(mFragment, "mMultiUserFooterPreferenceController",
                mock(MultiUserFooterPreferenceController.class));
        mUserCapabilities = UserCapabilities.create(mContext);
        when((Object) mActivity.getSystemService(UserManager.class)).thenReturn(mUserManager);
        doReturn(mActivity).when(mFragment).getActivity();

        mProvisioned = Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 0);
        final SharedPreferences prefs = mock(SharedPreferences .class);
        when(mMockPreferenceManager.getSharedPreferences()).thenReturn(prefs);
        when(mMockPreferenceManager.getContext()).thenReturn(mContext);

        mSummaryProvider =
            UserSettings.SUMMARY_PROVIDER_FACTORY.createSummaryProvider(mActivity, mSummaryLoader);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, mProvisioned);
    }

    @Test
    public void setListening_shouldSetSummaryWithUserName() {
        final String name = "John";
        final UserInfo userInfo = new UserInfo();
        userInfo.name = name;
        when(mUserManager.getUserInfo(anyInt())).thenReturn(userInfo);

        mSummaryProvider.setListening(true);

        verify(mSummaryLoader)
            .setSummary(mSummaryProvider, mActivity.getString(R.string.users_summary, name));
    }

    @Test
    public void testAssignDefaultPhoto_ContextNull_ReturnFalseAndNotCrash() {
        // Should not crash here
        assertThat(UserSettings.assignDefaultPhoto(null, 0)).isFalse();
    }

    @Test
    public void updateUserList_cannotSwitchUser_shouldNotBeSelectableForGuest() {
        final RestrictedPreference addUser = spy(new RestrictedPreference(mContext));
        final PreferenceGroup userListCategory = spy(new PreferenceCategory(mContext));

        mUserCapabilities.mIsGuest = false;
        mUserCapabilities.mCanAddGuest = true;
        mUserCapabilities.mDisallowAddUser = false;
        mUserCapabilities.mDisallowSwitchUser = false;
        mUserCapabilities.mUserSwitcherEnabled = true;

        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mUserCaps", mUserCapabilities);
        ReflectionHelpers.setField(mFragment, "mDefaultIconDrawable", mDefaultIconDrawable);
        mFragment.mMePreference = mMePreference;
        mFragment.mUserListCategory = userListCategory;
        mFragment.mAddUser = addUser;

        when(mUserManager.canSwitchUsers()).thenReturn(false);
        doReturn(mMockPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();
        doReturn(mMockPreferenceManager).when(userListCategory).getPreferenceManager();

        mFragment.updateUserList();

        final Preference guest = userListCategory.findPreference(KEY_USER_GUEST);
        assertThat(guest.isSelectable()).isFalse();
    }

    @Test
    public void updateUserList_cannotSwitchUser_shouldDisableAddUser() {
        Settings.Global.putInt(mContext.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 1);
        final RestrictedPreference addUser = spy(new RestrictedPreference(mContext));
        final PreferenceGroup userListCategory = spy(new PreferenceCategory(mContext));

        mUserCapabilities.mCanAddUser = true;
        mUserCapabilities.mDisallowAddUser = false;
        mUserCapabilities.mUserSwitcherEnabled = true;

        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mUserCaps", mUserCapabilities);
        ReflectionHelpers.setField(mFragment, "mDefaultIconDrawable", mDefaultIconDrawable);
        ReflectionHelpers.setField(mFragment, "mAddingUser", false);
        mFragment.mMePreference = mMePreference;
        mFragment.mUserListCategory = userListCategory;
        mFragment.mAddUser = addUser;

        when(mUserManager.canSwitchUsers()).thenReturn(false);
        when(mUserManager.canAddMoreUsers()).thenReturn(true);
        doReturn(mMockPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();
        doReturn(mMockPreferenceManager).when(userListCategory).getPreferenceManager();

        mFragment.updateUserList();

        assertThat(addUser.isEnabled()).isFalse();
    }
}
