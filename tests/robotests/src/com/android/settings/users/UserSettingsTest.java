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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class})
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

    private FragmentActivity mActivity;
    private Context mContext;
    private UserSettings mFragment;
    private UserCapabilities mUserCapabilities;
    private SummaryLoader.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(ActivityController.of(new FragmentActivity()).get());
        mContext = spy(RuntimeEnvironment.application);
        mUserCapabilities = UserCapabilities.create(mContext);

        mFragment = spy(new UserSettings());
        ReflectionHelpers.setField(mFragment, "mAddUserWhenLockedPreferenceController",
                mock(AddUserWhenLockedPreferenceController.class));
        ReflectionHelpers.setField(mFragment, "mMultiUserFooterPreferenceController",
                mock(MultiUserFooterPreferenceController.class));
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mUserCaps", mUserCapabilities);
        ReflectionHelpers.setField(mFragment, "mDefaultIconDrawable", mDefaultIconDrawable);
        ReflectionHelpers.setField(mFragment, "mAddingUser", false);
        mFragment.mMePreference = mMePreference;

        when((Object) mActivity.getSystemService(UserManager.class)).thenReturn(mUserManager);
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mMockPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
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

    @Test
    public void updateUserList_cannotAddUserButCanSwitchUser_shouldNotShowAddUser() {
        Settings.Global.putInt(mContext.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 1);
        final RestrictedPreference addUser = mock(RestrictedPreference.class);

        mUserCapabilities.mCanAddUser = false;
        mUserCapabilities.mDisallowAddUser = true;
        mUserCapabilities.mUserSwitcherEnabled = true;

        mFragment.mUserListCategory = mock(PreferenceCategory.class);
        mFragment.mAddUser = addUser;

        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();

        mFragment.updateUserList();

        verify(addUser, never()).setVisible(true);
    }

    @Test
    public void withDisallowRemoveUser_ShouldDisableRemoveUser() {
        // TODO(b/115781615): Tidy robolectric tests
        // Arrange
        final int userId = UserHandle.myUserId();
        final List<UserManager.EnforcingUser> enforcingUsers = Collections.singletonList(
                new UserManager.EnforcingUser(userId,
                        UserManager.RESTRICTION_SOURCE_DEVICE_OWNER)
        );
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_REMOVE_USER,
                UserHandle.of(userId),
                enforcingUsers);

        ShadowDevicePolicyManager.getShadow().setDeviceOwnerComponentOnAnyUser(
                new ComponentName("test", "test"));

        doReturn(true).when(mUserManager).canSwitchUsers();
        mUserCapabilities.mIsAdmin = false;

        Menu menu = mock(Menu.class);
        MenuItem menuItem = mock(MenuItem.class);
        final String title = "title";

        doReturn(title).when(menuItem).getTitle();
        doReturn(menuItem).when(menu).add(
                anyInt(), eq(Menu.FIRST), anyInt(), any(CharSequence.class));

        // Act
        mFragment.onCreateOptionsMenu(menu, mock(MenuInflater.class));

        // Assert
        // Expect that the click will be overridden and the color will be faded
        // (by RestrictedLockUtilsInternal)
        verify(menuItem).setOnMenuItemClickListener(notNull());
        SpannableStringBuilder defaultTitle = new SpannableStringBuilder(title);
        verify(menuItem).setTitle(AdditionalMatchers.not(eq(defaultTitle)));
    }

    @Test
    public void withoutDisallowRemoveUser_ShouldNotDisableRemoveUser() {
        // Arrange
        doReturn(true).when(mUserManager).canSwitchUsers();
        mUserCapabilities.mIsAdmin = false;

        Menu menu = mock(Menu.class);
        MenuItem menuItem = mock(MenuItem.class);
        final String title = "title";

        doReturn(title).when(menuItem).getTitle();
        doReturn(menuItem).when(menu).add(
                anyInt(), eq(Menu.FIRST), anyInt(), any(CharSequence.class));

        // Act
        mFragment.onCreateOptionsMenu(menu, mock(MenuInflater.class));

        // Assert
        // Expect that a click listener will not be added and the title will not be changed
        verify(menuItem, never()).setOnMenuItemClickListener(notNull());
        SpannableStringBuilder defaultTitle = new SpannableStringBuilder(title);
        verify(menuItem, never()).setTitle(AdditionalMatchers.not(eq(defaultTitle)));
    }

    @Test
    public void updateUserList_canAddUserAndSwitchUser_shouldShowAddUser() {
        Settings.Global.putInt(mContext.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 1);
        final RestrictedPreference addUser = mock(RestrictedPreference.class);

        mUserCapabilities.mCanAddUser = true;
        mUserCapabilities.mDisallowAddUser = false;
        mUserCapabilities.mUserSwitcherEnabled = true;

        mFragment.mAddUser = addUser;
        mFragment.mUserListCategory = mock(PreferenceCategory.class);

        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();
        doReturn("Test summary").when(mFragment).getString(anyInt(), anyInt());

        mFragment.updateUserList();

        verify(addUser).setVisible(true);
    }

    @Test
    public void updateUserList_addUserDisallowedByAdmin_shouldShowAddUserDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        final RestrictedPreference addUser = mock(RestrictedPreference.class);

        mUserCapabilities.mCanAddUser = false;
        mUserCapabilities.mDisallowAddUser = true;
        mUserCapabilities.mDisallowAddUserSetByAdmin = true;
        mUserCapabilities.mUserSwitcherEnabled = true;

        mFragment.mUserListCategory = mock(PreferenceCategory.class);
        mFragment.mAddUser = addUser;

        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();

        mFragment.updateUserList();

        verify(addUser).setVisible(true);
        assertThat(addUser.isEnabled()).isFalse();
    }
}
