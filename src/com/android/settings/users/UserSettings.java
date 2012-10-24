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

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.settings.R;
import com.android.settings.SelectableEditTextPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment.SettingsDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class UserSettings extends SettingsPreferenceFragment
        implements OnPreferenceClickListener, OnClickListener, DialogInterface.OnDismissListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "UserSettings";

    /** UserId of the user being removed */
    private static final String SAVE_REMOVING_USER = "removing_user";
    /** UserId of the user that was just added */
    private static final String SAVE_ADDING_USER = "adding_user";

    private static final String KEY_USER_NICKNAME = "user_nickname";
    private static final String KEY_USER_LIST = "user_list";
    private static final String KEY_USER_ME = "user_me";

    private static final int MENU_ADD_USER = Menu.FIRST;
    private static final int MENU_REMOVE_USER = Menu.FIRST+1;

    private static final int DIALOG_CONFIRM_REMOVE = 1;
    private static final int DIALOG_ADD_USER = 2;
    private static final int DIALOG_SETUP_USER = 3;
    private static final int DIALOG_USER_CANNOT_MANAGE = 4;

    private static final int MESSAGE_UPDATE_LIST = 1;
    private static final int MESSAGE_SETUP_USER = 2;

    private static final String KEY_ADD_USER_LONG_MESSAGE_DISPLAYED =
            "key_add_user_long_message_displayed";

    private static final int[] USER_DRAWABLES = {
        R.drawable.avatar_default_1,
        R.drawable.avatar_default_2,
        R.drawable.avatar_default_3,
        R.drawable.avatar_default_4,
        R.drawable.avatar_default_5,
        R.drawable.avatar_default_6,
        R.drawable.avatar_default_7,
        R.drawable.avatar_default_8
    };

    private PreferenceGroup mUserListCategory;
    private Preference mMePreference;
    private SelectableEditTextPreference mNicknamePreference;
    private int mRemovingUserId = -1;
    private int mAddedUserId = 0;
    private boolean mAddingUser;
    private boolean mProfileExists;

    private final Object mUserLock = new Object();
    private UserManager mUserManager;
    private SparseArray<Drawable> mUserIcons = new SparseArray<Drawable>();
    private boolean mIsOwner = UserHandle.myUserId() == UserHandle.USER_OWNER;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_UPDATE_LIST:
                updateUserList();
                break;
            case MESSAGE_SETUP_USER:
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
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            if (icicle.containsKey(SAVE_ADDING_USER)) {
                mAddedUserId = icicle.getInt(SAVE_ADDING_USER);
            }
            if (icicle.containsKey(SAVE_REMOVING_USER)) {
                mRemovingUserId = icicle.getInt(SAVE_REMOVING_USER);
            }
        }

        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        addPreferencesFromResource(R.xml.user_settings);
        mUserListCategory = (PreferenceGroup) findPreference(KEY_USER_LIST);
        mMePreference = (Preference) findPreference(KEY_USER_ME);
        mMePreference.setOnPreferenceClickListener(this);
        if (!mIsOwner) {
            mMePreference.setSummary(null);
        }
        Preference ownerInfo = findPreference("user_owner_info");
        if (ownerInfo != null && !mIsOwner) {
            ownerInfo.setTitle(R.string.user_info_settings_title);
        }
        mNicknamePreference = (SelectableEditTextPreference) findPreference(KEY_USER_NICKNAME);
        mNicknamePreference.setOnPreferenceChangeListener(this);
        mNicknamePreference.getEditText().setInputType(
                InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mNicknamePreference.setInitialSelectionMode(
                SelectableEditTextPreference.SELECTION_SELECT_ALL);
        loadProfile();
        setHasOptionsMenu(true);
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_REMOVED);
        filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        getActivity().registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null,
                mHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        updateUserList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUserChangeReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SAVE_ADDING_USER, mAddedUserId);
        outState.putInt(SAVE_REMOVING_USER, mRemovingUserId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mIsOwner) {
            if (UserManager.getMaxSupportedUsers() > mUserManager.getUsers(false).size()) {
                MenuItem addUserItem = menu.add(0, MENU_ADD_USER, 0, R.string.user_add_user_menu);
                addUserItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                        | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
        } else {
            String nickname = mUserManager.getUserName();
            MenuItem removeThisUser = menu.add(0, MENU_REMOVE_USER, 0,
                    getResources().getString(R.string.user_remove_user_menu, nickname));
            removeThisUser.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_ADD_USER) {
            onAddUserClicked();
            return true;
        } else if (itemId == MENU_REMOVE_USER) {
            onRemoveUserClicked(UserHandle.myUserId());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void loadProfile() {
        mProfileExists = false;
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPostExecute(String result) {
                finishLoadProfile(result);
            }

            @Override
            protected String doInBackground(Void... values) {
                UserInfo user = mUserManager.getUserInfo(UserHandle.myUserId());
                if (user.iconPath == null || user.iconPath.equals("")) {
                    assignProfilePhoto(user);
                }
                String profileName = getProfileName();
                return profileName;
            }
        }.execute();
    }

    private void finishLoadProfile(String profileName) {
        mMePreference.setTitle(profileName);
        int myUserId = UserHandle.myUserId();
        Bitmap b = mUserManager.getUserIcon(myUserId);
        if (b != null) {
            Drawable d = new BitmapDrawable(b);
            mMePreference.setIcon(d);
            mUserIcons.put(myUserId, d);
        }
    }

    private void onAddUserClicked() {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                showDialog(DIALOG_ADD_USER);
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
        mAddedUserId = userId;
        showDialog(DIALOG_SETUP_USER);
    }

    @Override
    public void onDialogShowing() {
        super.onDialogShowing();

        setOnDismissListener(this);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE: {
                Dialog dlg = new AlertDialog.Builder(getActivity())
                    .setTitle(UserHandle.myUserId() == mRemovingUserId
                            ? R.string.user_confirm_remove_self_title
                            : R.string.user_confirm_remove_title)
                    .setMessage(UserHandle.myUserId() == mRemovingUserId
                            ? R.string.user_confirm_remove_self_message
                            : R.string.user_confirm_remove_message)
                    .setPositiveButton(R.string.user_delete_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeUserNow();
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
                return dlg;
            }
            case DIALOG_USER_CANNOT_MANAGE:
                return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.user_cannot_manage_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            case DIALOG_ADD_USER: {
                final SharedPreferences preferences = getActivity().getPreferences(
                        Context.MODE_PRIVATE);
                final boolean longMessageDisplayed = preferences.getBoolean(
                        KEY_ADD_USER_LONG_MESSAGE_DISPLAYED, false);
                final int messageResId = longMessageDisplayed
                        ? R.string.user_add_user_message_short
                        : R.string.user_add_user_message_long;
                Dialog dlg = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.user_add_user_title)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            addUserNow();
                            if (!longMessageDisplayed) {
                                preferences.edit().putBoolean(KEY_ADD_USER_LONG_MESSAGE_DISPLAYED,
                                        true).commit();
                            }
                        }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
                return dlg;
            }
            case DIALOG_SETUP_USER: {
                Dialog dlg = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.user_setup_dialog_title)
                .setMessage(R.string.user_setup_dialog_message)
                .setPositiveButton(R.string.user_setup_button_setup_now,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            switchUserNow(mAddedUserId);
                        }
                })
                .setNegativeButton(R.string.user_setup_button_setup_later, null)
                .create();
                return dlg;
            }
            default:
                return null;
        }
    }

    private void removeUserNow() {
        if (mRemovingUserId == UserHandle.myUserId()) {
            removeThisUser();
        } else {
            new Thread() {
                public void run() {
                    synchronized (mUserLock) {
                        mUserManager.removeUser(mRemovingUserId);
                        mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
                    }
                }
            }.start();
        }
    }

    private void removeThisUser() {
        try {
            ActivityManagerNative.getDefault().switchUser(UserHandle.USER_OWNER);
            ((UserManager) getActivity().getSystemService(Context.USER_SERVICE))
                    .removeUser(UserHandle.myUserId());
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to remove self user");
        }
    }

    private void addUserNow() {
        synchronized (mUserLock) {
            mAddingUser = true;
            updateUserList();
            new Thread() {
                public void run() {
                    // Could take a few seconds
                    UserInfo user = mUserManager.createUser(
                            getActivity().getResources().getString(R.string.user_new_user_name), 0);
                    if (user != null) {
                        assignDefaultPhoto(user);
                    }
                    synchronized (mUserLock) {
                        mAddingUser = false;
                        mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
                        mHandler.sendMessage(mHandler.obtainMessage(
                                MESSAGE_SETUP_USER, user.id, user.serialNumber));
                    }
                }
            }.start();
        }
    }

    private void switchUserNow(int userId) {
        try {
            ActivityManagerNative.getDefault().switchUser(userId);
        } catch (RemoteException re) {
            // Nothing to do
        }
    }

    private void updateUserList() {
        if (getActivity() == null) return;
        List<UserInfo> users = mUserManager.getUsers(true);

        mUserListCategory.removeAll();
        mUserListCategory.setOrderingAsAdded(false);

        final ArrayList<Integer> missingIcons = new ArrayList<Integer>();
        for (UserInfo user : users) {
            Preference pref;
            if (user.id == UserHandle.myUserId()) {
                pref = mMePreference;
                mNicknamePreference.setText(user.name);
                mNicknamePreference.setSummary(user.name);
            } else {
                pref = new UserPreference(getActivity(), null, user.id,
                        UserHandle.myUserId() == UserHandle.USER_OWNER, this);
                pref.setOnPreferenceClickListener(this);
                pref.setKey("id=" + user.id);
                mUserListCategory.addPreference(pref);
                if (user.id == UserHandle.USER_OWNER) {
                    pref.setSummary(R.string.user_owner);
                }
                pref.setTitle(user.name);
                if (!isInitialized(user)) {
                    pref.setSummary(R.string.user_summary_not_set_up);
                }
            }
            if (user.iconPath != null) {
                if (mUserIcons.get(user.id) == null) {
                    missingIcons.add(user.id);
                    pref.setIcon(R.drawable.avatar_default_1);
                } else {
                    setPhotoId(pref, user);
                }
            }
        }
        // Add a temporary entry for the user being created
        if (mAddingUser) {
            Preference pref = new UserPreference(getActivity(), null, UserPreference.USERID_UNKNOWN,
                    false, null);
            pref.setEnabled(false);
            pref.setTitle(R.string.user_new_user_name);
            pref.setSummary(R.string.user_adding_new_user);
            pref.setIcon(R.drawable.avatar_default_1);
            mUserListCategory.addPreference(pref);
        }
        getActivity().invalidateOptionsMenu();

        // Load the icons
        if (missingIcons.size() > 0) {
            loadIconsAsync(missingIcons);
        }
    }

    private void loadIconsAsync(List<Integer> missingIcons) {
        new AsyncTask<List<Integer>, Void, Void>() {
            @Override
            protected void onPostExecute(Void result) {
                updateUserList();
            }

            @Override
            protected Void doInBackground(List<Integer>... values) {
                if (getActivity() == null) return null;
                for (int userId : values[0]) {
                    Bitmap bitmap = mUserManager.getUserIcon(userId);
                    Drawable d = new BitmapDrawable(getResources(), bitmap);
                    mUserIcons.append(userId, d);
                }
                return null;
            }
        }.execute(missingIcons);

    }
    private void assignProfilePhoto(final UserInfo user) {
        if (!Utils.copyMeProfilePhoto(getActivity(), user)) {
            assignDefaultPhoto(user);
        }
    }

    private String getProfileName() {
        String name = Utils.getMeProfileName(getActivity(), true);
        if (name != null) {
            mProfileExists = true;
        }
        return name;
    }

    private void assignDefaultPhoto(UserInfo user) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                USER_DRAWABLES[user.id % USER_DRAWABLES.length]);
        mUserManager.setUserIcon(user.id, bitmap);
    }

    private void setPhotoId(Preference pref, UserInfo user) {
        Drawable d = mUserIcons.get(user.id); // UserUtils.getUserIcon(mUserManager, user);
        if (d != null) {
            pref.setIcon(d);
        }
    }

    private void setUserName(String name) {
        mUserManager.setUserName(UserHandle.myUserId(), name);
        mNicknamePreference.setSummary(name);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mMePreference) {
            Intent editProfile;
            if (!mProfileExists) {
                editProfile = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                // TODO: Make this a proper API
                editProfile.putExtra("newLocalProfile", true);
            } else {
                editProfile = new Intent(Intent.ACTION_EDIT, ContactsContract.Profile.CONTENT_URI);
            }
            // To make sure that it returns back here when done
            // TODO: Make this a proper API
            editProfile.putExtra("finishActivityOnSaveCompleted", true);
            startActivity(editProfile);
        } else if (pref instanceof UserPreference) {
            int userId = ((UserPreference) pref).getUserId();
            // Get the latest status of the user
            UserInfo user = mUserManager.getUserInfo(userId);
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
                showDialog(DIALOG_USER_CANNOT_MANAGE);
            } else {
                if (!isInitialized(user)) {
                    mHandler.sendMessage(mHandler.obtainMessage(
                            MESSAGE_SETUP_USER, user.id, user.serialNumber));
                }
            }
        }
        return false;
    }

    private boolean isInitialized(UserInfo user) {
        return (user.flags & UserInfo.FLAG_INITIALIZED) != 0;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof UserPreference) {
            int userId = ((UserPreference) v.getTag()).getUserId();
            onRemoveUserClicked(userId);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        synchronized (mUserLock) {
            mAddingUser = false;
            mRemovingUserId = -1;
            updateUserList();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNicknamePreference) {
            String value = (String) newValue;
            if (preference == mNicknamePreference && value != null
                    && value.length() > 0) {
                setUserName(value);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_users;
    }
}
