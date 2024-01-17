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

import static android.os.UserManager.SWITCHABILITY_STATUS_OK;
import static android.os.UserManager.SWITCHABILITY_STATUS_USER_IN_CALL;
import static android.os.UserManager.SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
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
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowDevicePolicyManager.class,
        SettingsShadowResources.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class UserSettingsTest {

    private static final String KEY_USER_GUEST = "user_guest";
    private static final String KEY_ALLOW_MULTIPLE_USERS = "allow_multiple_users";
    private static final int ACTIVE_USER_ID = 0;
    private static final int INACTIVE_ADMIN_USER_ID = 1;
    private static final int INACTIVE_SECONDARY_USER_ID = 14;
    private static final int INACTIVE_RESTRICTED_USER_ID = 21;
    private static final int INACTIVE_GUEST_USER_ID = 23;
    private static final int MANAGED_USER_ID = 11;
    private static final String ADMIN_USER_NAME = "Owner";
    private static final String SECONDARY_USER_NAME = "Tom";
    private static final String RESTRICTED_USER_NAME = "Bob";
    private static final String GUEST_USER_NAME = "Guest";
    private static final String MANAGED_USER_NAME = "Work profile";
    private int mProvisionedBackupValue;

    @Mock
    private Drawable mDefaultIconDrawable;
    @Mock
    private PreferenceManager mMockPreferenceManager;
    @Mock
    private UserPreference mMePreference;
    @Mock
    private RestrictedPreference mAddUserPreference;
    @Mock
    private RestrictedPreference mAddSupervisedUserPreference;
    @Mock
    private RestrictedPreference mAddGuestPreference;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private FragmentActivity mActivity;
    private Context mContext;
    private UserSettings mFragment;
    private UserCapabilities mUserCapabilities;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(ActivityController.of(new FragmentActivity()).get());
        mContext = spy(RuntimeEnvironment.application);
        mUserCapabilities = UserCapabilities.create(mContext);
        mUserCapabilities.mUserSwitcherEnabled = true;

        mFragment = spy(new UserSettings());
        ReflectionHelpers.setField(mFragment, "mAddUserWhenLockedPreferenceController",
                mock(AddUserWhenLockedPreferenceController.class));
        ReflectionHelpers.setField(mFragment, "mGuestTelephonyPreferenceController",
                mock(GuestTelephonyPreferenceController.class));
        ReflectionHelpers.setField(mFragment, "mMultiUserTopIntroPreferenceController",
                mock(MultiUserTopIntroPreferenceController.class));
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mUserCaps", mUserCapabilities);
        ReflectionHelpers.setField(mFragment, "mDefaultIconDrawable", mDefaultIconDrawable);
        ReflectionHelpers.setField(mFragment, "mAddingUser", false);
        ReflectionHelpers.setField(mFragment, "mMetricsFeatureProvider", mMetricsFeatureProvider);
        ReflectionHelpers.setField(mFragment, "mRemoveGuestOnExitPreferenceController",
                mock(RemoveGuestOnExitPreferenceController.class));

        doReturn(mUserManager).when(mActivity).getSystemService(UserManager.class);
        doReturn(mPackageManager).when(mActivity).getPackageManager();

        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mMockPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mPackageManager).when(mContext).getPackageManager();

        mProvisionedBackupValue = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1); //default state

        final SharedPreferences prefs = mock(SharedPreferences.class);

        doReturn(prefs).when(mMockPreferenceManager).getSharedPreferences();
        doReturn(mContext).when(mMockPreferenceManager).getContext();
        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();
        doReturn(ACTIVE_USER_ID).when(mContext).getUserId();

        mFragment.mMePreference = mMePreference;
        mFragment.mAddUser = mAddUserPreference;
        mFragment.mAddSupervisedUser = mAddSupervisedUserPreference;
        mFragment.mAddGuest = mAddGuestPreference;
        mFragment.mUserListCategory = mock(PreferenceCategory.class);
        mFragment.mGuestUserCategory = mock(PreferenceCategory.class);
        mFragment.mGuestCategory = mock(PreferenceCategory.class);
        mFragment.mGuestResetPreference = mock(Preference.class);
        mFragment.mGuestExitPreference = mock(Preference.class);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, mProvisionedBackupValue);
        SettingsShadowResources.reset();
    }

    @Test
    public void testAssignDefaultPhoto_ContextNull_ReturnFalseAndNotCrash() {
        // Should not crash here
        assertThat(UserSettings.assignDefaultPhoto(null, ACTIVE_USER_ID)).isFalse();
    }

    @Test
    public void testGetRawDataToIndex_returnAllIndexablePreferences() {
        String[] expectedKeys = {KEY_ALLOW_MULTIPLE_USERS};
        List<String> keysResultList = new ArrayList<>();
        ShadowUserManager.getShadow().setSupportsMultipleUsers(true);
        List<SearchIndexableRaw> rawData =
                UserSettings.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext, true);

        for (SearchIndexableRaw rawDataItem : rawData) {
            keysResultList.add(rawDataItem.key);
        }

        assertThat(keysResultList).containsExactly(expectedKeys);
    }

    @Test
    public void testAssignDefaultPhoto_hasDefaultUserIconSize() {
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        int size = 100;
        try {
            SettingsShadowResources.overrideResource(
                    com.android.internal.R.dimen.user_icon_size,
                    size);
            assertThat(UserSettings.assignDefaultPhoto(mContext, ACTIVE_USER_ID)).isTrue();

            int pxSize = mContext.getResources()
                    .getDimensionPixelSize(com.android.internal.R.dimen.user_icon_size);

            ArgumentCaptor<Bitmap> captor = ArgumentCaptor.forClass(Bitmap.class);
            verify(mUserManager).setUserIcon(eq(ACTIVE_USER_ID), captor.capture());
            Bitmap bitmap = captor.getValue();
            assertThat(bitmap.getWidth()).isEqualTo(pxSize);
            assertThat(bitmap.getHeight()).isEqualTo(pxSize);
        } finally {
            SettingsShadowResources.reset();
        }
    }

    @Test
    public void testExitGuest_ShouldLogAction() {
        mUserCapabilities.mIsGuest = true;
        mFragment.clearAndExitGuest();
        verify(mMetricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_USER_GUEST_EXIT_CONFIRMED));
    }

    @Test
    public void testExitGuestWhenNotGuest_ShouldNotLogAction() {
        mUserCapabilities.mIsGuest = false;
        mFragment.clearAndExitGuest();
        verify(mMetricsFeatureProvider, never()).action(any(),
                eq(SettingsEnums.ACTION_USER_GUEST_EXIT_CONFIRMED));
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

        doReturn(SWITCHABILITY_STATUS_OK).when(mUserManager).getUserSwitchability();
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
        doReturn(SWITCHABILITY_STATUS_OK).when(mUserManager).getUserSwitchability();
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
        mUserCapabilities.mCanAddUser = true;
        doReturn(true)
                .when(mUserManager).canAddMoreUsers(eq(UserManager.USER_TYPE_FULL_SECONDARY));
        doReturn(true).when(mAddUserPreference).isEnabled();
        doReturn(SWITCHABILITY_STATUS_OK).when(mUserManager).getUserSwitchability();

        mFragment.updateUserList();

        verify(mAddUserPreference).setVisible(true);
        verify(mAddUserPreference).setSummary(null);
        verify(mAddUserPreference).setEnabled(true);
        verify(mAddUserPreference).setDisabledByAdmin(null);
        verify(mAddUserPreference).setSelectable(true);
    }

    @Test
    public void updateUserList_canAddGuestAndSwitchUser_shouldShowAddGuest() {
        mUserCapabilities.mCanAddGuest = true;
        doReturn(true)
                .when(mUserManager).canAddMoreUsers(eq(UserManager.USER_TYPE_FULL_GUEST));
        doReturn(SWITCHABILITY_STATUS_OK).when(mUserManager).getUserSwitchability();

        mFragment.updateUserList();

        verify(mAddGuestPreference).setVisible(true);
        verify(mAddGuestPreference).setEnabled(true);
        verify(mAddGuestPreference).setSelectable(true);
    }

    @Test
    public void updateUserList_cannotSwitchUser_shouldDisableAddUser() {
        mUserCapabilities.mCanAddUser = true;
        doReturn(true).when(mUserManager).canAddMoreUsers(anyString());
        doReturn(true).when(mAddUserPreference).isEnabled();
        doReturn(SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED)
                .when(mUserManager).getUserSwitchability();

        mFragment.updateUserList();

        verify(mAddUserPreference).setVisible(true);
        verify(mAddUserPreference).setSummary(null);
        verify(mAddUserPreference).setEnabled(false);
        verify(mAddUserPreference).setSelectable(true);
    }

    @Test
    public void updateUserList_canNotAddMoreUsers_shouldDisableAddUserWithSummary() {
        mUserCapabilities.mCanAddUser = true;
        doReturn(false).when(mUserManager).canAddMoreUsers(anyString());
        doReturn(false).when(mAddUserPreference).isEnabled();
        doReturn(SWITCHABILITY_STATUS_OK).when(mUserManager).getUserSwitchability();
        doReturn(4).when(mFragment).getRealUsersCount();

        mFragment.updateUserList();

        verify(mAddUserPreference).setVisible(true);
        verify(mAddUserPreference).setSummary(
                "You can\u2019t add any more users. Remove a user to add a new one.");
        verify(mAddUserPreference).setEnabled(false);
        verify(mAddUserPreference).setSelectable(true);
    }

    @Test
    public void updateUserList_cannotSwitchUser_shouldDisableAddGuest() {
        mUserCapabilities.mCanAddGuest = true;
        doReturn(true)
                .when(mUserManager).canAddMoreUsers(eq(UserManager.USER_TYPE_FULL_GUEST));
        doReturn(SWITCHABILITY_STATUS_USER_IN_CALL).when(mUserManager).getUserSwitchability();

        mFragment.updateUserList();

        verify(mAddGuestPreference).setVisible(true);
        verify(mAddGuestPreference).setEnabled(false);
        verify(mAddGuestPreference).setSelectable(true);
    }

    @Test
    public void updateUserList_addUserDisallowedByAdmin_shouldNotShowAddUser() {
        RestrictedLockUtils.EnforcedAdmin enforcedAdmin = mock(
                RestrictedLockUtils.EnforcedAdmin.class);
        mUserCapabilities.mEnforcedAdmin = enforcedAdmin;
        mUserCapabilities.mCanAddUser = false;
        mUserCapabilities.mDisallowAddUser = true;
        mUserCapabilities.mDisallowAddUserSetByAdmin = true;
        doReturn(true).when(mAddUserPreference).isEnabled();

        mFragment.updateUserList();

        verify(mAddUserPreference).setVisible(false);
    }

    @Test
    public void updateUserList_cannotAddUserButCanSwitchUser_shouldNotShowAddUser() {
        mUserCapabilities.mCanAddUser = false;

        mFragment.updateUserList();

        verify(mAddUserPreference).setVisible(false);
    }

    @Test
    public void updateUserList_canNotAddGuest_shouldNotShowAddGuest() {
        mUserCapabilities.mCanAddGuest = false;

        mFragment.updateUserList();

        verify(mAddGuestPreference).setVisible(false);
    }

    @Test
    public void updateUserList_notProvisionedDevice_shouldNotShowAddUser() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
        mUserCapabilities.mCanAddUser = true;

        mFragment.updateUserList();

        verify(mAddUserPreference).setVisible(false);
    }

    @Test
    public void updateUserList_notProvisionedDevice_shouldNotShowAddGuest() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
        mUserCapabilities.mCanAddGuest = true;

        mFragment.updateUserList();

        verify(mAddGuestPreference).setVisible(false);
    }

    @Test
    public void updateUserList_userSwitcherDisabled_shouldNotShowAddUser() {
        givenUsers(getAdminUser(true));
        mUserCapabilities.mCanAddUser = true;
        mUserCapabilities.mUserSwitcherEnabled = false;

        mFragment.updateUserList();

        verify(mAddUserPreference).setVisible(false);
    }

    @Test
    public void updateUserList_userSwitcherDisabled_shouldNotShowAddGuest() {
        givenUsers(getAdminUser(true));
        mUserCapabilities.mCanAddGuest = true;
        mUserCapabilities.mUserSwitcherEnabled = false;

        mFragment.updateUserList();

        verify(mAddGuestPreference).setVisible(false);
    }

    @Test
    public void updateUserList_shouldAddAdminUserPreference() {
        givenUsers(getAdminUser(true));

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory).addPreference(captor.capture());
        UserPreference adminPref = captor.getValue();
        assertThat(adminPref).isSameInstanceAs(mMePreference);
    }

    @Test
    public void updateUserList_existingGuest_shouldAddGuestUserPreference() {
        givenUsers(getAdminUser(true), getGuest(false));

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mGuestUserCategory, times(1))
                .addPreference(captor.capture());
        UserPreference guestPref = captor.getAllValues().get(0);
        assertThat(guestPref.getUserId()).isEqualTo(INACTIVE_GUEST_USER_ID);
        assertThat(guestPref.getTitle()).isEqualTo("Guest");
        assertThat(guestPref.getIcon()).isNotNull();
        assertThat(guestPref.getKey()).isEqualTo(KEY_USER_GUEST);
        assertThat(guestPref.isEnabled()).isEqualTo(true);
        assertThat(guestPref.isSelectable()).isEqualTo(true);
        assertThat(guestPref.getOnPreferenceClickListener()).isSameInstanceAs(mFragment);
    }

    @Test
    public void updateUserList_existingSecondaryUser_shouldAddSecondaryUserPreference() {
        givenUsers(getAdminUser(true), getSecondaryUser(false));

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory, times(2))
                .addPreference(captor.capture());
        UserPreference userPref = captor.getAllValues().get(1);
        assertThat(userPref.getUserId()).isEqualTo(INACTIVE_SECONDARY_USER_ID);
        assertThat(userPref.getTitle()).isEqualTo(SECONDARY_USER_NAME);
        assertThat(userPref.getIcon()).isNotNull();
        assertThat(userPref.getKey()).isEqualTo("id=" + INACTIVE_SECONDARY_USER_ID);
        assertThat(userPref.isEnabled()).isEqualTo(true);
        assertThat(userPref.isSelectable()).isEqualTo(true);
        assertThat(userPref.getOnPreferenceClickListener()).isSameInstanceAs(mFragment);
    }

    @Test
    public void updateUserList_existingSecondaryUser_shouldAddOnlyCurrUser_MultiUserIsDisabled() {
        givenUsers(getAdminUser(true), getSecondaryUser(false));
        mUserCapabilities.mUserSwitcherEnabled = false;

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory, times(1))
                .addPreference(captor.capture());

        List<UserPreference> userPrefs = captor.getAllValues();
        assertThat(userPrefs.size()).isEqualTo(1);
        assertThat(userPrefs.get(0)).isSameInstanceAs(mMePreference);
    }

    @Test
    public void updateUserList_existingSecondaryUser_shouldAddSecondaryUser_MultiUserIsEnabled() {
        givenUsers(getAdminUser(true), getSecondaryUser(false));

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory, times(2))
                .addPreference(captor.capture());

        List<UserPreference> userPrefs = captor.getAllValues();
        UserPreference adminPref = userPrefs.get(0);
        UserPreference secondaryPref = userPrefs.get(1);

        assertThat(userPrefs.size()).isEqualTo(2);
        assertThat(adminPref).isSameInstanceAs(mMePreference);
        assertThat(secondaryPref.getUserId()).isEqualTo(INACTIVE_SECONDARY_USER_ID);
        assertThat(secondaryPref.getTitle()).isEqualTo(SECONDARY_USER_NAME);
        assertThat(secondaryPref.getIcon()).isNotNull();
        assertThat(secondaryPref.getKey()).isEqualTo("id=" + INACTIVE_SECONDARY_USER_ID);
        assertThat(secondaryPref.isEnabled()).isEqualTo(true);
        assertThat(secondaryPref.isSelectable()).isEqualTo(true);
        assertThat(secondaryPref.getOnPreferenceClickListener()).isSameInstanceAs(mFragment);
    }

    @Test
    public void updateUserList_existingRestrictedUser_shouldAddRestrictedUserPreference() {
        givenUsers(getAdminUser(true), getRestrictedUser(false));

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory, times(2))
                .addPreference(captor.capture());
        UserPreference userPref = captor.getAllValues().get(1);
        assertThat(userPref.getUserId()).isEqualTo(INACTIVE_RESTRICTED_USER_ID);
        assertThat(userPref.getTitle()).isEqualTo(RESTRICTED_USER_NAME);
        assertThat(userPref.getIcon()).isNotNull();
        assertThat(userPref.getKey()).isEqualTo("id=" + INACTIVE_RESTRICTED_USER_ID);
        assertThat(userPref.getSummary()).isEqualTo("Restricted profile");
        assertThat(userPref.isEnabled()).isEqualTo(true);
        assertThat(userPref.isSelectable()).isEqualTo(true);
        assertThat(userPref.getOnPreferenceClickListener()).isSameInstanceAs(mFragment);
    }

    @Test
    public void updateUserList_existingManagedUser_shouldNotAddUserPreference() {
        givenUsers(getAdminUser(true), getManagedUser());

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory).addPreference(captor.capture());
        List<UserPreference> userPreferences = captor.getAllValues();
        assertThat(userPreferences.size()).isEqualTo(1);
        assertThat(userPreferences.get(0).getUserId()).isEqualTo(ACTIVE_USER_ID);
    }

    @Test
    public void updateUserList_uninitializedRestrictedUser_shouldAddUserPreference() {
        UserInfo restrictedUser = getRestrictedUser(false);
        removeFlag(restrictedUser, UserInfo.FLAG_INITIALIZED);
        givenUsers(getAdminUser(true), restrictedUser);
        doReturn(SWITCHABILITY_STATUS_OK).when(mUserManager).getUserSwitchability();
        mUserCapabilities.mDisallowSwitchUser = false;

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory, times(2))
                .addPreference(captor.capture());
        UserPreference userPref = captor.getAllValues().get(1);
        assertThat(userPref.getUserId()).isEqualTo(INACTIVE_RESTRICTED_USER_ID);
        assertThat(userPref.getTitle()).isEqualTo(RESTRICTED_USER_NAME);
        assertThat(userPref.getIcon()).isNotNull();
        assertThat(userPref.getKey()).isEqualTo("id=" + INACTIVE_RESTRICTED_USER_ID);
        assertThat(userPref.getSummary()).isEqualTo("Not set up - Restricted profile");
        assertThat(userPref.isEnabled()).isEqualTo(true);
        assertThat(userPref.isSelectable()).isEqualTo(true);
        assertThat(userPref.getOnPreferenceClickListener()).isSameInstanceAs(mFragment);
    }

    @Test
    public void updateUserList_uninitializedUserAndCanNotSwitchUser_shouldDisablePref() {
        UserInfo uninitializedUser = getSecondaryUser(false);
        removeFlag(uninitializedUser, UserInfo.FLAG_INITIALIZED);
        givenUsers(getAdminUser(true), uninitializedUser);
        doReturn(SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED)
                .when(mUserManager).getUserSwitchability();
        mUserCapabilities.mDisallowSwitchUser = false;

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory, times(2))
                .addPreference(captor.capture());
        UserPreference userPref = captor.getAllValues().get(1);
        assertThat(userPref.getUserId()).isEqualTo(INACTIVE_SECONDARY_USER_ID);
        assertThat(userPref.getTitle()).isEqualTo(SECONDARY_USER_NAME);
        assertThat(userPref.getIcon()).isNotNull();
        assertThat(userPref.getKey()).isEqualTo("id=" + INACTIVE_SECONDARY_USER_ID);
        assertThat(userPref.getSummary()).isEqualTo("Not set up");
        assertThat(userPref.isEnabled()).isEqualTo(false);
        assertThat(userPref.isSelectable()).isEqualTo(true);
        assertThat(userPref.getOnPreferenceClickListener()).isSameInstanceAs(mFragment);
    }

    @Test
    public void updateUserList_guestWithoutInitializedFlag_shouldNotSetSummary() {
        UserInfo guest = getGuest(false);
        removeFlag(guest, UserInfo.FLAG_INITIALIZED);
        givenUsers(getAdminUser(true), guest);

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mGuestUserCategory, times(1))
                .addPreference(captor.capture());
        UserPreference userPref = captor.getAllValues().get(0);
        assertThat(userPref.getUserId()).isEqualTo(INACTIVE_GUEST_USER_ID);
        assertThat(userPref.getSummary()).isNull();
    }

    @Test
    public void updateUserList_activeUserWithoutInitializedFlag_shouldNotSetSummary() {
        UserInfo activeUser = getSecondaryUser(true);
        removeFlag(activeUser, UserInfo.FLAG_INITIALIZED);
        givenUsers(activeUser);

        mFragment.updateUserList();

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(mFragment.mUserListCategory).addPreference(captor.capture());
        UserPreference userPref = captor.getValue();
        assertThat(userPref.getUserId()).isEqualTo(ACTIVE_USER_ID);
        assertThat(userPref.getSummary()).isNull();
    }

    @Test
    public void updateUserList_guestIsAlreadyCreated_shouldNotShowAddGuest() {
        givenUsers(getAdminUser(true), getGuest(true));
        mUserCapabilities.mCanAddGuest = true;

        mFragment.updateUserList();

        verify(mAddGuestPreference).setVisible(false);
    }

    @Test
    public void updateUserList_userIconLoaded_shouldNotLoadIcon() {
        UserInfo currentUser = getAdminUser(true);
        currentUser.iconPath = "/data/system/users/0/photo.png";
        givenUsers(currentUser);
        mFragment.mUserIcons.put(ACTIVE_USER_ID,
                Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888));

        mFragment.updateUserList();

        verify(mUserManager, never()).getUserIcon(anyInt());
        // updateUserList should be called only once
        verify(mUserManager).getAliveUsers();
    }

    @Ignore
    @Test
    public void updateUserList_userIconMissing_shouldLoadIcon() {
        UserInfo currentUser = getAdminUser(true);
        currentUser.iconPath = "/data/system/users/0/photo.png";
        givenUsers(currentUser);
        // create a non-empty sparsearray
        mFragment.mUserIcons.put(5, Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888));
        Bitmap userIcon = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        doReturn(userIcon).when(mUserManager).getUserIcon(ACTIVE_USER_ID);

        mFragment.updateUserList();
        shadowOf(Looper.getMainLooper()).idle();

        verify(mUserManager).getUserIcon(ACTIVE_USER_ID);
        // updateUserList should be called another time after loading the icons
        verify(mUserManager, times(2)).getAliveUsers();
    }

    @Test
    public void onPreferenceClick_addGuestClicked_createGuestAndOpenDetails() {
        UserInfo createdGuest = getGuest(false);
        removeFlag(createdGuest, UserInfo.FLAG_INITIALIZED);
        doReturn(createdGuest).when(mUserManager).createGuest(mActivity);
        doReturn(mActivity).when(mFragment).getContext();

        mFragment.onPreferenceClick(mAddGuestPreference);

        verify(mUserManager).createGuest(mActivity);
        Intent startedIntent = shadowOf(mActivity).getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);
        assertThat(shadowIntent.getIntentClass()).isEqualTo(SubSettings.class);
        assertThat(startedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(UserDetailsSettings.class.getName());
        Bundle arguments = startedIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(arguments).isNotNull();
        assertThat(arguments.getInt(UserDetailsSettings.EXTRA_USER_ID, 0))
                .isEqualTo(createdGuest.id);
        assertThat(arguments.getBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, false))
                .isEqualTo(true);
        verify(mMetricsFeatureProvider).action(any(), eq(SettingsEnums.ACTION_USER_GUEST_ADD));
    }

    @Test
    public void onPreferenceClick_addSupervisedUserClicked_startIntentWithAction() {
        final String intentPackage = "testPackage";
        final String intentAction = UserManager.ACTION_CREATE_SUPERVISED_USER;
        final int intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK;
        final int metricsAction = SettingsEnums.ACTION_USER_SUPERVISED_ADD;
        try {
            setConfigSupervisedUserCreationPackage(intentPackage);
            doReturn(new ResolveInfo()).when(mPackageManager).resolveActivity(any(), anyInt());
            doNothing().when(mFragment).startActivity(any());

            mFragment.onPreferenceClick(mAddSupervisedUserPreference);

            final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
            verify(mFragment).startActivity(captor.capture());
            assertThat(captor.getValue().getPackage()).isEqualTo(intentPackage);
            assertThat(captor.getValue().getAction()).isEqualTo(intentAction);
            assertThat(captor.getValue().getFlags() & intentFlags).isGreaterThan(0);

            verify(mMetricsFeatureProvider).action(any(), eq(metricsAction));
        } finally {
            SettingsShadowResources.reset();
        }
    }

    @Test
    public void getRealUsersCount_onlyAdmin_shouldCount() {
        givenUsers(getAdminUser(true));

        int result = mFragment.getRealUsersCount();

        assertThat(result).isEqualTo(1);
        verify(mUserManager).getUsers();
    }

    @Test
    public void getRealUsersCount_secondaryUser_shouldCount() {
        givenUsers(getAdminUser(true), getSecondaryUser(false));

        int result = mFragment.getRealUsersCount();

        assertThat(result).isEqualTo(2);
        verify(mUserManager).getUsers();
    }

    @Test
    public void getRealUsersCount_restrictedUser_shouldCount() {
        givenUsers(getAdminUser(true), getSecondaryUser(false));

        int result = mFragment.getRealUsersCount();

        assertThat(result).isEqualTo(2);
        verify(mUserManager).getUsers();
    }

    @Test
    public void getRealUsersCount_guest_shouldNotCount() {
        givenUsers(getAdminUser(true), getGuest(false));

        int result = mFragment.getRealUsersCount();

        assertThat(result).isEqualTo(1);
        verify(mUserManager).getUsers();
    }

    @Test
    public void getRealUsersCount_managedUser_shouldNotCount() {
        givenUsers(getAdminUser(true), getManagedUser());

        int result = mFragment.getRealUsersCount();

        assertThat(result).isEqualTo(1);
        verify(mUserManager).getUsers();
    }

    private void setConfigSupervisedUserCreationPackage(String value) {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_supervisedUserCreationPackage,
                value
        );
        mFragment.setConfigSupervisedUserCreationPackage();
        mUserCapabilities.mCanAddUser = true;
        mFragment.updateUserList();
    }

    @Test
    public void addSupervisedUserOption_resourceIsDefined_shouldBeDisplayed() {
        try {
            setConfigSupervisedUserCreationPackage("test");
            verify(mAddSupervisedUserPreference).setVisible(true);
        } finally {
            SettingsShadowResources.reset();
        }
    }

    @Test
    public void addSupervisedUserOption_resourceIsNotDefined_shouldBeHidden() {
        try {
            setConfigSupervisedUserCreationPackage("");
            verify(mAddSupervisedUserPreference).setVisible(false);
        } finally {
            SettingsShadowResources.reset();
        }
    }

    private void givenUsers(UserInfo... userInfo) {
        List<UserInfo> users = Arrays.asList(userInfo);
        doReturn(users).when(mUserManager).getUsers();
        doReturn(users).when(mUserManager).getAliveUsers();
        users.forEach(user ->
                doReturn(user).when(mUserManager).getUserInfo(user.id));
    }

    private static void removeFlag(UserInfo userInfo, int flag) {
        userInfo.flags &= ~flag;
    }

    private static UserInfo getAdminUser(boolean active) {
        return new UserInfo(active ? ACTIVE_USER_ID : INACTIVE_ADMIN_USER_ID, ADMIN_USER_NAME,
                null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_ADMIN,
                UserManager.USER_TYPE_FULL_SYSTEM);
    }

    private static UserInfo getSecondaryUser(boolean active) {
        return new UserInfo(active ? ACTIVE_USER_ID : INACTIVE_SECONDARY_USER_ID,
                SECONDARY_USER_NAME, null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED,
                UserManager.USER_TYPE_FULL_SECONDARY);
    }

    private static UserInfo getRestrictedUser(boolean active) {
        return new UserInfo(active ? ACTIVE_USER_ID : INACTIVE_RESTRICTED_USER_ID,
                RESTRICTED_USER_NAME, null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_RESTRICTED,
                UserManager.USER_TYPE_FULL_RESTRICTED);
    }

    private static UserInfo getManagedUser() {
        return new UserInfo(MANAGED_USER_ID,
                MANAGED_USER_NAME, null,
                UserInfo.FLAG_PROFILE | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_MANAGED_PROFILE,
                UserManager.USER_TYPE_PROFILE_MANAGED);
    }

    private static UserInfo getGuest(boolean active) {
        return new UserInfo(active ? ACTIVE_USER_ID : INACTIVE_GUEST_USER_ID, GUEST_USER_NAME,
                null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_GUEST,
                UserManager.USER_TYPE_FULL_GUEST);
    }


}
