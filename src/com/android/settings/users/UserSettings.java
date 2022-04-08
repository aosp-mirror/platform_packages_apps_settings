/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static android.os.Process.myUserHandle;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SimpleAdapter;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.UserIcons;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.drawable.CircleFramedDrawable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Screen that manages the list of users on the device.
 * Secondary users and a guest user can be created if there is no restriction.
 *
 * The first user in the list is always the current user.
 * Owner is the primary user.
 */
@SearchIndexable
public class UserSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener,
        MultiUserSwitchBarController.OnMultiUserSwitchChangedListener,
        DialogInterface.OnDismissListener {

    private static final String TAG = "UserSettings";

    /** UserId of the user being removed */
    private static final String SAVE_REMOVING_USER = "removing_user";

    private static final String KEY_USER_LIST = "user_list";
    private static final String KEY_USER_ME = "user_me";
    private static final String KEY_USER_GUEST = "user_guest";
    private static final String KEY_ADD_GUEST = "guest_add";
    private static final String KEY_ADD_USER = "user_add";
    private static final String KEY_ADD_USER_WHEN_LOCKED = "user_settings_add_users_when_locked";
    private static final String KEY_MULTIUSER_FOOTER = "multiuser_footer";

    private static final int MENU_REMOVE_USER = Menu.FIRST;

    private static final IntentFilter USER_REMOVED_INTENT_FILTER;

    private static final int DIALOG_CONFIRM_REMOVE = 1;
    private static final int DIALOG_ADD_USER = 2;
    // Dialogs with id 3 and 4 got removed
    private static final int DIALOG_USER_CANNOT_MANAGE = 5;
    private static final int DIALOG_CHOOSE_USER_TYPE = 6;
    private static final int DIALOG_NEED_LOCKSCREEN = 7;
    private static final int DIALOG_CONFIRM_EXIT_GUEST = 8;
    private static final int DIALOG_USER_PROFILE_EDITOR = 9;
    private static final int DIALOG_USER_PROFILE_EDITOR_ADD_USER = 10;
    private static final int DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE = 11;

    private static final int MESSAGE_UPDATE_LIST = 1;
    private static final int MESSAGE_USER_CREATED = 2;

    private static final int USER_TYPE_USER = 1;
    private static final int USER_TYPE_RESTRICTED_PROFILE = 2;

    private static final int REQUEST_CHOOSE_LOCK = 10;

    private static final String KEY_ADD_USER_LONG_MESSAGE_DISPLAYED =
            "key_add_user_long_message_displayed";

    private static final String KEY_TITLE = "title";
    private static final String KEY_SUMMARY = "summary";

    static {
        USER_REMOVED_INTENT_FILTER = new IntentFilter(Intent.ACTION_USER_REMOVED);
        USER_REMOVED_INTENT_FILTER.addAction(Intent.ACTION_USER_INFO_CHANGED);
    }

    @VisibleForTesting
    PreferenceGroup mUserListCategory;
    @VisibleForTesting
    UserPreference mMePreference;
    @VisibleForTesting
    RestrictedPreference mAddGuest;
    @VisibleForTesting
    RestrictedPreference mAddUser;
    @VisibleForTesting
    SparseArray<Bitmap> mUserIcons = new SparseArray<>();
    private int mRemovingUserId = -1;
    private boolean mAddingUser;
    private String mAddingUserName;
    private UserCapabilities mUserCaps;
    private boolean mShouldUpdateUserList = true;
    private final Object mUserLock = new Object();
    private UserManager mUserManager;
    private static SparseArray<Bitmap> sDarkDefaultUserBitmapCache = new SparseArray<>();

    private MultiUserSwitchBarController mSwitchBarController;
    private EditUserInfoController mEditUserInfoController = new EditUserInfoController();
    private AddUserWhenLockedPreferenceController mAddUserWhenLockedPreferenceController;
    private MultiUserFooterPreferenceController mMultiUserFooterPreferenceController;

    private CharSequence mPendingUserName;
    private Drawable mPendingUserIcon;

    // A place to cache the generated default avatar
    private Drawable mDefaultIconDrawable;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_LIST:
                    updateUserList();
                    break;
                case MESSAGE_USER_CREATED:
                    onUserCreated(msg.arg1);
                    break;
            }
        }
    };

    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_USER_REMOVED)) {
                mRemovingUserId = -1;
            } else if (intent.getAction().equals(Intent.ACTION_USER_INFO_CHANGED)) {
                int userHandle = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                if (userHandle != -1) {
                    mUserIcons.remove(userHandle);
                }
            }
            mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
        }
    };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Assume we are in a SettingsActivity. This is only safe because we currently use
        // SettingsActivity as base for all preference fragments.
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SwitchBar switchBar = activity.getSwitchBar();
        mSwitchBarController = new MultiUserSwitchBarController(activity,
                new SwitchBarController(switchBar), this /* listener */);
        getSettingsLifecycle().addObserver(mSwitchBarController);
        switchBar.show();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.user_settings);
        final Activity activity = getActivity();
        if (!WizardManagerHelper.isDeviceProvisioned(activity)) {
            activity.finish();
            return;
        }

        mAddUserWhenLockedPreferenceController = new AddUserWhenLockedPreferenceController(
                activity, KEY_ADD_USER_WHEN_LOCKED);

        mMultiUserFooterPreferenceController = new MultiUserFooterPreferenceController(activity,
                KEY_MULTIUSER_FOOTER);

        final PreferenceScreen screen = getPreferenceScreen();
        mAddUserWhenLockedPreferenceController.displayPreference(screen);
        mMultiUserFooterPreferenceController.displayPreference(screen);

        screen.findPreference(mAddUserWhenLockedPreferenceController.getPreferenceKey())
                .setOnPreferenceChangeListener(mAddUserWhenLockedPreferenceController);

        if (icicle != null) {
            if (icicle.containsKey(SAVE_REMOVING_USER)) {
                mRemovingUserId = icicle.getInt(SAVE_REMOVING_USER);
            }
            mEditUserInfoController.onRestoreInstanceState(icicle);
        }

        mUserCaps = UserCapabilities.create(activity);
        mUserManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        if (!mUserCaps.mEnabled) {
            return;
        }

        final int myUserId = UserHandle.myUserId();

        mUserListCategory = (PreferenceGroup) findPreference(KEY_USER_LIST);
        mMePreference = new UserPreference(getPrefContext(), null /* attrs */, myUserId);
        mMePreference.setKey(KEY_USER_ME);
        mMePreference.setOnPreferenceClickListener(this);
        if (mUserCaps.mIsAdmin) {
            mMePreference.setSummary(R.string.user_admin);
        }

        mAddGuest = findPreference(KEY_ADD_GUEST);
        mAddGuest.setOnPreferenceClickListener(this);

        mAddUser = findPreference(KEY_ADD_USER);
        if (!mUserCaps.mCanAddRestrictedProfile) {
            // Label should only mention adding a "user", not a "profile"
            mAddUser.setTitle(R.string.user_add_user_menu);
        }
        mAddUser.setOnPreferenceClickListener(this);

        activity.registerReceiverAsUser(
                mUserChangeReceiver, UserHandle.ALL, USER_REMOVED_INTENT_FILTER, null, mHandler);

        updateUI();
        mShouldUpdateUserList = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mUserCaps.mEnabled) {
            return;
        }
        final PreferenceScreen screen = getPreferenceScreen();

        mAddUserWhenLockedPreferenceController.updateState(screen.findPreference(
                mAddUserWhenLockedPreferenceController.getPreferenceKey()));

        if (mShouldUpdateUserList) {
            updateUI();
        }
    }

    @Override
    public void onPause() {
        mShouldUpdateUserList = true;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mUserCaps == null || !mUserCaps.mEnabled) {
            return;
        }

        getActivity().unregisterReceiver(mUserChangeReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mEditUserInfoController.onSaveInstanceState(outState);
        outState.putInt(SAVE_REMOVING_USER, mRemovingUserId);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        int pos = 0;
        if (!mUserCaps.mIsAdmin && canSwitchUserNow()) {
            String nickname = mUserManager.getUserName();
            MenuItem removeThisUser = menu.add(0, MENU_REMOVE_USER, pos++,
                    getResources().getString(R.string.user_remove_user_menu, nickname));
            removeThisUser.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            final EnforcedAdmin disallowRemoveUserAdmin =
                    RestrictedLockUtilsInternal.checkIfRestrictionEnforced(getContext(),
                            UserManager.DISALLOW_REMOVE_USER, UserHandle.myUserId());
            RestrictedLockUtilsInternal.setMenuItemAsDisabledByAdmin(getContext(), removeThisUser,
                    disallowRemoveUserAdmin);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_REMOVE_USER) {
            onRemoveUserClicked(UserHandle.myUserId());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMultiUserSwitchChanged(boolean newState) {
        updateUI();
    }

    private void updateUI() {
        mUserCaps.updateAddUserCapabilities(getActivity());
        loadProfile();
        updateUserList();
    }

    /**
     * Loads profile information for the current user.
     */
    private void loadProfile() {
        if (isCurrentUserGuest()) {
            // No need to load profile information
            mMePreference.setIcon(getEncircledDefaultIcon());
            mMePreference.setTitle(R.string.user_exit_guest_title);
            mMePreference.setSelectable(true);
            // removing a guest will result in switching back to the admin user
            mMePreference.setEnabled(canSwitchUserNow());
            return;
        }

        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPostExecute(String result) {
                finishLoadProfile(result);
            }

            @Override
            protected String doInBackground(Void... values) {
                UserInfo user = mUserManager.getUserInfo(UserHandle.myUserId());
                if (user.iconPath == null || user.iconPath.equals("")) {
                    // Assign profile photo.
                    copyMeProfilePhoto(getActivity(), user);
                }
                return user.name;
            }
        }.execute();
    }

    private void finishLoadProfile(String profileName) {
        if (getActivity() == null) {
            return;
        }
        mMePreference.setTitle(getString(R.string.user_you, profileName));
        int myUserId = UserHandle.myUserId();
        Bitmap b = mUserManager.getUserIcon(myUserId);
        if (b != null) {
            mMePreference.setIcon(encircle(b));
            mUserIcons.put(myUserId, b);
        }
    }

    private boolean hasLockscreenSecurity() {
        LockPatternUtils lpu = new LockPatternUtils(getActivity());
        return lpu.isSecure(UserHandle.myUserId());
    }

    private void launchChooseLockscreen() {
        Intent chooseLockIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
        chooseLockIntent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        startActivityForResult(chooseLockIntent, REQUEST_CHOOSE_LOCK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHOOSE_LOCK) {
            if (resultCode != Activity.RESULT_CANCELED && hasLockscreenSecurity()) {
                addUserNow(USER_TYPE_RESTRICTED_PROFILE);
            }
        } else {
            mEditUserInfoController.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onAddUserClicked(int userType) {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                switch (userType) {
                    case USER_TYPE_USER:
                        showDialog(DIALOG_ADD_USER);
                        break;
                    case USER_TYPE_RESTRICTED_PROFILE:
                        if (hasLockscreenSecurity()) {
                            showDialog(DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE);
                        } else {
                            showDialog(DIALOG_NEED_LOCKSCREEN);
                        }
                        break;
                }
            }
        }
    }

    private void onRemoveUserClicked(int userId) {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                mRemovingUserId = userId;
                showDialog(DIALOG_CONFIRM_REMOVE);
            }
        }
    }

    private void onUserCreated(int userId) {
        mAddingUser = false;
        UserInfo userInfo = mUserManager.getUserInfo(userId);
        openUserDetails(userInfo, true);
    }

    private void openUserDetails(UserInfo userInfo, boolean newUser) {
        Bundle extras = new Bundle();
        extras.putInt(UserDetailsSettings.EXTRA_USER_ID, userInfo.id);
        extras.putBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, newUser);
        new SubSettingLauncher(getContext())
                .setDestination(UserDetailsSettings.class.getName())
                .setArguments(extras)
                .setTitleText(userInfo.name)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    @Override
    public void onDialogShowing() {
        super.onDialogShowing();

        setOnDismissListener(this);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Context context = getActivity();
        if (context == null) {
            return null;
        }
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE: {
                Dialog dlg =
                        UserDialogs.createRemoveDialog(getActivity(), mRemovingUserId,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeUserNow();
                                    }
                                }
                        );
                return dlg;
            }
            case DIALOG_USER_CANNOT_MANAGE:
                return new AlertDialog.Builder(context)
                        .setMessage(R.string.user_cannot_manage_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
            case DIALOG_ADD_USER: {
                final SharedPreferences preferences = getActivity().getPreferences(
                        Context.MODE_PRIVATE);
                final boolean longMessageDisplayed = preferences.getBoolean(
                        KEY_ADD_USER_LONG_MESSAGE_DISPLAYED, false);
                final int messageResId = longMessageDisplayed
                        ? com.android.settingslib.R.string.user_add_user_message_short
                        : com.android.settingslib.R.string.user_add_user_message_long;
                Dialog dlg = new AlertDialog.Builder(context)
                        .setTitle(com.android.settingslib.R.string.user_add_user_title)
                        .setMessage(messageResId)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        showDialog(DIALOG_USER_PROFILE_EDITOR_ADD_USER);
                                        if (!longMessageDisplayed) {
                                            preferences.edit().putBoolean(
                                                    KEY_ADD_USER_LONG_MESSAGE_DISPLAYED,
                                                    true).apply();
                                        }
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_CHOOSE_USER_TYPE: {
                List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
                HashMap<String, String> addUserItem = new HashMap<String, String>();
                addUserItem.put(KEY_TITLE, getString(
                        com.android.settingslib.R.string.user_add_user_item_title));
                addUserItem.put(KEY_SUMMARY, getString(
                        com.android.settingslib.R.string.user_add_user_item_summary));
                HashMap<String, String> addProfileItem = new HashMap<String, String>();
                addProfileItem.put(KEY_TITLE, getString(
                        com.android.settingslib.R.string.user_add_profile_item_title));
                addProfileItem.put(KEY_SUMMARY, getString(
                        com.android.settingslib.R.string.user_add_profile_item_summary));
                data.add(addUserItem);
                data.add(addProfileItem);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                SimpleAdapter adapter = new SimpleAdapter(builder.getContext(),
                        data, R.layout.two_line_list_item,
                        new String[]{KEY_TITLE, KEY_SUMMARY},
                        new int[]{R.id.title, R.id.summary});
                builder.setTitle(com.android.settingslib.R.string.user_add_user_type_title);
                builder.setAdapter(adapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onAddUserClicked(which == 0
                                        ? USER_TYPE_USER
                                        : USER_TYPE_RESTRICTED_PROFILE);
                            }
                        });
                return builder.create();
            }
            case DIALOG_NEED_LOCKSCREEN: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setMessage(com.android.settingslib.R.string.user_need_lock_message)
                        .setPositiveButton(com.android.settingslib.R.string.user_set_lock_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        launchChooseLockscreen();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_CONFIRM_EXIT_GUEST: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setTitle(R.string.user_exit_guest_confirm_title)
                        .setMessage(R.string.user_exit_guest_confirm_message)
                        .setPositiveButton(R.string.user_exit_guest_dialog_remove,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        exitGuest();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_USER_PROFILE_EDITOR: {
                UserHandle user = myUserHandle();
                UserInfo info = mUserManager.getUserInfo(user.getIdentifier());
                return mEditUserInfoController.createDialog(
                        this,
                        Utils.getUserIcon(getPrefContext(), mUserManager, info),
                        info.name,
                        getString(com.android.settingslib.R.string.profile_info_settings_title),
                        new EditUserInfoController.OnContentChangedCallback() {
                            @Override
                            public void onPhotoChanged(UserHandle user, Drawable photo) {
                                ThreadUtils.postOnBackgroundThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mUserManager.setUserIcon(user.getIdentifier(),
                                                UserIcons.convertToBitmap(photo));
                                    }
                                });
                                mMePreference.setIcon(photo);
                            }

                            @Override
                            public void onLabelChanged(UserHandle user, CharSequence label) {
                                mMePreference.setTitle(label.toString());
                                mUserManager.setUserName(user.getIdentifier(), label.toString());
                            }
                        },
                        user,
                        null);
            }
            case DIALOG_USER_PROFILE_EDITOR_ADD_USER: {
                synchronized (mUserLock) {
                    mPendingUserIcon = UserIcons.getDefaultUserIcon(getPrefContext().getResources(),
                            new Random(System.currentTimeMillis()).nextInt(8), false);
                    mPendingUserName = getString(
                            com.android.settingslib.R.string.user_new_user_name);
                }
                return buildAddUserProfileEditorDialog(USER_TYPE_USER);
            }
            case DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE: {
                synchronized (mUserLock) {
                    mPendingUserIcon = UserIcons.getDefaultUserIcon(getPrefContext().getResources(),
                            new Random(System.currentTimeMillis()).nextInt(8), false);
                    mPendingUserName = getString(
                            com.android.settingslib.R.string.user_new_profile_name);
                }
                return buildAddUserProfileEditorDialog(USER_TYPE_RESTRICTED_PROFILE);
            }
            default:
                return null;
        }
    }

    private Dialog buildAddUserProfileEditorDialog(int userType) {
        Dialog d;
        synchronized (mUserLock) {
            d = mEditUserInfoController.createDialog(
                    this,
                    mPendingUserIcon,
                    mPendingUserName,
                    getString(userType == USER_TYPE_USER
                            ? com.android.settingslib.R.string.user_info_settings_title
                            : com.android.settingslib.R.string.profile_info_settings_title),
                    new EditUserInfoController.OnContentChangedCallback() {
                        @Override
                        public void onPhotoChanged(UserHandle user, Drawable photo) {
                            mPendingUserIcon = photo;
                        }

                        @Override
                        public void onLabelChanged(UserHandle user, CharSequence label) {
                            mPendingUserName = label;
                        }
                    },
                    myUserHandle(),
                    new EditUserInfoController.OnDialogCompleteCallback() {
                        @Override
                        public void onPositive() {
                            addUserNow(userType);
                        }

                        @Override
                        public void onNegativeOrCancel() {
                            synchronized (mUserLock) {
                                mPendingUserIcon = null;
                                mPendingUserName = null;
                            }
                        }
                    });
        }
        return d;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE:
                return SettingsEnums.DIALOG_USER_REMOVE;
            case DIALOG_USER_CANNOT_MANAGE:
                return SettingsEnums.DIALOG_USER_CANNOT_MANAGE;
            case DIALOG_ADD_USER:
                return SettingsEnums.DIALOG_USER_ADD;
            case DIALOG_CHOOSE_USER_TYPE:
                return SettingsEnums.DIALOG_USER_CHOOSE_TYPE;
            case DIALOG_NEED_LOCKSCREEN:
                return SettingsEnums.DIALOG_USER_NEED_LOCKSCREEN;
            case DIALOG_CONFIRM_EXIT_GUEST:
                return SettingsEnums.DIALOG_USER_CONFIRM_EXIT_GUEST;
            case DIALOG_USER_PROFILE_EDITOR:
            case DIALOG_USER_PROFILE_EDITOR_ADD_USER:
            case DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE:
                return SettingsEnums.DIALOG_USER_EDIT_PROFILE;
            default:
                return 0;
        }
    }

    private void removeUserNow() {
        if (mRemovingUserId == UserHandle.myUserId()) {
            removeThisUser();
        } else {
            ThreadUtils.postOnBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mUserLock) {
                        mUserManager.removeUser(mRemovingUserId);
                        mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
                    }
                }
            });
        }
    }

    private void removeThisUser() {
        if (!canSwitchUserNow()) {
            Log.w(TAG, "Cannot remove current user when switching is disabled");
            return;
        }
        try {
            ActivityManager.getService().switchUser(UserHandle.USER_SYSTEM);
            getContext().getSystemService(UserManager.class).removeUser(UserHandle.myUserId());
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to remove self user");
        }
    }

    private void addUserNow(final int userType) {
        synchronized (mUserLock) {
            mAddingUser = true;
            mAddingUserName = userType == USER_TYPE_USER
                    ? (mPendingUserName != null ? mPendingUserName.toString()
                    : getString(R.string.user_new_user_name))
                    : (mPendingUserName != null ? mPendingUserName.toString()
                            : getString(R.string.user_new_profile_name));
        }
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                UserInfo user;
                String username;

                synchronized (mUserLock) {
                    username = mAddingUserName;
                }

                // Could take a few seconds
                if (userType == USER_TYPE_USER) {
                    user = mUserManager.createUser(username, 0);
                } else {
                    user = mUserManager.createRestrictedProfile(username);
                }

                synchronized (mUserLock) {
                    if (user == null) {
                        mAddingUser = false;
                        mPendingUserIcon = null;
                        mPendingUserName = null;
                        return;
                    }

                    if (mPendingUserIcon != null) {
                        mUserManager.setUserIcon(user.id,
                                UserIcons.convertToBitmap(mPendingUserIcon));
                    }

                    if (userType == USER_TYPE_USER) {
                        mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
                    }

                    mHandler.sendMessage(mHandler.obtainMessage(
                            MESSAGE_USER_CREATED, user.id, user.serialNumber));

                    mPendingUserIcon = null;
                    mPendingUserName = null;
                }
            }
        });
    }

    /**
     * Erase the current user (guest) and switch to another user.
     */
    private void exitGuest() {
        // Just to be safe
        if (!isCurrentUserGuest()) {
            return;
        }
        removeThisUser();
    }

    @VisibleForTesting
    void updateUserList() {
        final Context context = getActivity();
        if (context == null) {
            return;
        }
        final List<UserInfo> users = mUserManager.getUsers(true);

        final ArrayList<Integer> missingIcons = new ArrayList<>();
        final ArrayList<UserPreference> userPreferences = new ArrayList<>();
        userPreferences.add(mMePreference);

        boolean canOpenUserDetails =
                mUserCaps.mIsAdmin || (canSwitchUserNow() && !mUserCaps.mDisallowSwitchUser);
        for (UserInfo user : users) {
            if (!user.supportsSwitchToByUser()) {
                // Only users that can be switched to should show up here.
                // e.g. Managed profiles appear under Accounts Settings instead
                continue;
            }
            UserPreference pref;
            if (user.id == UserHandle.myUserId()) {
                pref = mMePreference;
            } else if (user.isGuest()) {
                pref = new UserPreference(getPrefContext(), null, user.id);
                pref.setTitle(R.string.user_guest);
                pref.setIcon(getEncircledDefaultIcon());
                pref.setKey(KEY_USER_GUEST);
                userPreferences.add(pref);
                pref.setEnabled(canOpenUserDetails);
                pref.setSelectable(true);

                if (mUserCaps.mDisallowSwitchUser) {
                    pref.setDisabledByAdmin(RestrictedLockUtilsInternal.getDeviceOwner(context));
                } else {
                    pref.setDisabledByAdmin(null);
                }
                pref.setOnPreferenceClickListener(this);
            } else {
                pref = new UserPreference(getPrefContext(), null, user.id);
                pref.setKey("id=" + user.id);
                userPreferences.add(pref);
                if (user.isAdmin()) {
                    pref.setSummary(R.string.user_admin);
                }
                pref.setTitle(user.name);
                pref.setOnPreferenceClickListener(this);
                pref.setEnabled(canOpenUserDetails);
                pref.setSelectable(true);
            }
            if (pref == null) {
                continue;
            }
            if (user.id != UserHandle.myUserId() && !user.isGuest() && !user.isInitialized()) {
                // sometimes after creating a guest the initialized flag isn't immediately set
                // and we don't want to show "Not set up" summary for them
                if (user.isRestricted()) {
                    pref.setSummary(R.string.user_summary_restricted_not_set_up);
                } else {
                    pref.setSummary(R.string.user_summary_not_set_up);
                    // Disallow setting up user which results in user switching when the
                    // restriction is set.
                    pref.setEnabled(!mUserCaps.mDisallowSwitchUser && canSwitchUserNow());
                }
            } else if (user.isRestricted()) {
                pref.setSummary(R.string.user_summary_restricted_profile);
            }
            if (user.iconPath != null) {
                if (mUserIcons.get(user.id) == null) {
                    // Icon not loaded yet, print a placeholder
                    missingIcons.add(user.id);
                    pref.setIcon(getEncircledDefaultIcon());
                } else {
                    setPhotoId(pref, user);
                }
            } else {
                // Icon not available yet, print a placeholder
                pref.setIcon(getEncircledDefaultIcon());
            }
        }

        // Add a temporary entry for the user being created
        if (mAddingUser) {
            UserPreference pref = new UserPreference(getPrefContext(), null,
                    UserPreference.USERID_UNKNOWN);
            pref.setEnabled(false);
            pref.setTitle(mAddingUserName);
            pref.setIcon(getEncircledDefaultIcon());
            userPreferences.add(pref);
        }


        // Sort list of users by serialNum
        Collections.sort(userPreferences, UserPreference.SERIAL_NUMBER_COMPARATOR);

        getActivity().invalidateOptionsMenu();

        // Load the icons
        if (missingIcons.size() > 0) {
            loadIconsAsync(missingIcons);
        }

        // If profiles are supported, mUserListCategory will have a special title
        if (mUserCaps.mCanAddRestrictedProfile) {
            mUserListCategory.setTitle(R.string.user_list_title);
        } else {
            mUserListCategory.setTitle(null);
        }

        // Remove everything from mUserListCategory and add new users.
        mUserListCategory.removeAll();

        // If multi-user is disabled, just show footer and return.
        final Preference addUserOnLockScreen = getPreferenceScreen().findPreference(
                mAddUserWhenLockedPreferenceController.getPreferenceKey());
        mAddUserWhenLockedPreferenceController.updateState(addUserOnLockScreen);

        final Preference multiUserFooterPrefence = getPreferenceScreen().findPreference(
                mMultiUserFooterPreferenceController.getPreferenceKey());
        mMultiUserFooterPreferenceController.updateState(multiUserFooterPrefence);
        mUserListCategory.setVisible(mUserCaps.mUserSwitcherEnabled);

        updateAddGuest(context, users.stream().anyMatch(UserInfo::isGuest));
        updateAddUser(context);

        if (!mUserCaps.mUserSwitcherEnabled) {
            return;
        }

        for (UserPreference userPreference : userPreferences) {
            userPreference.setOrder(Preference.DEFAULT_ORDER);
            mUserListCategory.addPreference(userPreference);
        }

    }

    private boolean isCurrentUserGuest() {
        return mUserCaps.mIsGuest;
    }

    private boolean canSwitchUserNow() {
        return mUserManager.getUserSwitchability() == UserManager.SWITCHABILITY_STATUS_OK;
    }

    private void updateAddGuest(Context context, boolean isGuestAlreadyCreated) {
        if (!isGuestAlreadyCreated && mUserCaps.mCanAddGuest
                && WizardManagerHelper.isDeviceProvisioned(context)
                && mUserCaps.mUserSwitcherEnabled) {
            mAddGuest.setVisible(true);
            mAddGuest.setIcon(getEncircledDefaultIcon());
            mAddGuest.setEnabled(canSwitchUserNow());
            mAddGuest.setSelectable(true);
        } else {
            mAddGuest.setVisible(false);
        }
    }

    private void updateAddUser(Context context) {
        if ((mUserCaps.mCanAddUser || mUserCaps.mDisallowAddUserSetByAdmin)
                && WizardManagerHelper.isDeviceProvisioned(context)
                && mUserCaps.mUserSwitcherEnabled) {
            mAddUser.setVisible(true);
            mAddUser.setSelectable(true);
            final boolean canAddMoreUsers = mUserManager.canAddMoreUsers();
            mAddUser.setEnabled(canAddMoreUsers && !mAddingUser && canSwitchUserNow());
            if (!canAddMoreUsers) {
                mAddUser.setSummary(
                        getString(R.string.user_add_max_count, getRealUsersCount()));
            } else {
                mAddUser.setSummary(null);
            }
            if (mAddUser.isEnabled()) {
                mAddUser.setDisabledByAdmin(
                        mUserCaps.mDisallowAddUser ? mUserCaps.mEnforcedAdmin : null);
            }
        } else {
            mAddUser.setVisible(false);
        }
    }

    /**
     * @return number of non-guest non-managed users
     */
    @VisibleForTesting
    int getRealUsersCount() {
        return (int) mUserManager.getUsers()
                .stream()
                .filter(user -> !user.isGuest() && !user.isProfile())
                .count();
    }

    private void loadIconsAsync(List<Integer> missingIcons) {
        new AsyncTask<List<Integer>, Void, Void>() {
            @Override
            protected void onPostExecute(Void result) {
                updateUserList();
            }

            @Override
            protected Void doInBackground(List<Integer>... values) {
                for (int userId : values[0]) {
                    Bitmap bitmap = mUserManager.getUserIcon(userId);
                    if (bitmap == null) {
                        bitmap = getDefaultUserIconAsBitmap(getContext().getResources(), userId);
                    }
                    mUserIcons.append(userId, bitmap);
                }
                return null;
            }
        }.execute(missingIcons);
    }

    private Drawable getEncircledDefaultIcon() {
        if (mDefaultIconDrawable == null) {
            mDefaultIconDrawable = encircle(
                    getDefaultUserIconAsBitmap(getContext().getResources(), UserHandle.USER_NULL));
        }
        return mDefaultIconDrawable;
    }

    private void setPhotoId(Preference pref, UserInfo user) {
        Bitmap bitmap = mUserIcons.get(user.id);
        if (bitmap != null) {
            pref.setIcon(encircle(bitmap));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mMePreference) {
            if (isCurrentUserGuest()) {
                showDialog(DIALOG_CONFIRM_EXIT_GUEST);
            } else {
                showDialog(DIALOG_USER_PROFILE_EDITOR);
            }
            return true;
        } else if (pref instanceof UserPreference) {
            UserInfo userInfo = mUserManager.getUserInfo(((UserPreference) pref).getUserId());
            openUserDetails(userInfo, false);
            return true;
        } else if (pref == mAddUser) {
            // If we allow both types, show a picker, otherwise directly go to
            // flow for full user.
            if (mUserCaps.mCanAddRestrictedProfile) {
                showDialog(DIALOG_CHOOSE_USER_TYPE);
            } else {
                onAddUserClicked(USER_TYPE_USER);
            }
            return true;
        } else if (pref == mAddGuest) {
            mAddGuest.setEnabled(false); // prevent multiple tap issue
            UserInfo guest = mUserManager.createGuest(
                    getContext(), getString(com.android.settingslib.R.string.user_guest));
            openUserDetails(guest, true);
            return true;
        }
        return false;
    }

    private Drawable encircle(Bitmap icon) {
        Drawable circled = CircleFramedDrawable.getInstance(getActivity(), icon);
        return circled;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        synchronized (mUserLock) {
            mRemovingUserId = -1;
            updateUserList();
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_users;
    }

    /**
     * Returns a default user icon (as a {@link Bitmap}) for the given user.
     *
     * Note that for guest users, you should pass in {@code UserHandle.USER_NULL}.
     *
     * @param resources resources object to fetch the user icon.
     * @param userId    the user id or {@code UserHandle.USER_NULL} for a non-user specific icon
     */
    private static Bitmap getDefaultUserIconAsBitmap(Resources resources, int userId) {
        Bitmap bitmap = null;
        // Try finding the corresponding bitmap in the dark bitmap cache
        bitmap = sDarkDefaultUserBitmapCache.get(userId);
        if (bitmap == null) {
            bitmap = UserIcons.convertToBitmap(
                    UserIcons.getDefaultUserIcon(resources, userId, false));
            // Save it to cache
            sDarkDefaultUserBitmapCache.put(userId, bitmap);
        }
        return bitmap;
    }

    /**
     * Assign the default photo to user with {@paramref userId}
     *
     * @param context used to get the {@link UserManager}
     * @param userId  used to get the icon bitmap
     * @return true if assign photo successfully, false if failed
     */
    @VisibleForTesting
    static boolean assignDefaultPhoto(Context context, int userId) {
        if (context == null) {
            return false;
        }
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap bitmap = getDefaultUserIconAsBitmap(context.getResources(), userId);
        um.setUserIcon(userId, bitmap);

        return true;
    }

    @WorkerThread
    static void copyMeProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = ContactsContract.Profile.CONTENT_URI;

        int userId = user != null ? user.id : UserHandle.myUserId();

        InputStream avatarDataStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.getContentResolver(),
                contactUri, true);
        // If there's no profile photo, assign a default avatar
        if (avatarDataStream == null) {
            assignDefaultPhoto(context, userId);
            return;
        }

        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap icon = BitmapFactory.decodeStream(avatarDataStream);
        um.setUserIcon(userId, icon);
        try {
            avatarDataStream.close();
        } catch (IOException ioe) {
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.user_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final UserCapabilities userCaps = UserCapabilities.create(context);
                    return userCaps.mEnabled;
                }

                @Override
                public List<String> getNonIndexableKeysFromXml(Context context, int xmlResId,
                        boolean suppressAllPage) {
                    final List<String> niks = super.getNonIndexableKeysFromXml(context, xmlResId,
                            suppressAllPage);
                    AddUserWhenLockedPreferenceController controller =
                            new AddUserWhenLockedPreferenceController(
                                    context, KEY_ADD_USER_WHEN_LOCKED);
                    controller.updateNonIndexableKeys(niks);
                    new AutoSyncDataPreferenceController(context, null /* parent */)
                            .updateNonIndexableKeys(niks);
                    new AutoSyncPersonalDataPreferenceController(context, null /* parent */)
                            .updateNonIndexableKeys(niks);
                    new AutoSyncWorkDataPreferenceController(context, null /* parent */)
                            .updateNonIndexableKeys(niks);
                    return niks;
                }
            };
}
