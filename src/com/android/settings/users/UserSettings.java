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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class UserSettings extends SettingsPreferenceFragment
        implements OnPreferenceClickListener, OnClickListener, DialogInterface.OnDismissListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "UserSettings";

    private static final String KEY_USER_NICKNAME = "user_nickname";
    private static final String KEY_USER_LIST = "user_list";
    private static final String KEY_USER_ME = "user_me";

    private static final int MENU_ADD_USER = Menu.FIRST;
    private static final int MENU_REMOVE_USER = Menu.FIRST+1;

    private static final int DIALOG_CONFIRM_REMOVE = 1;
    private static final int DIALOG_ADD_USER = 2;

    private static final int MESSAGE_UPDATE_LIST = 1;

    private static final int[] USER_DRAWABLES = {
        R.drawable.ic_user,
        R.drawable.ic_user_cyan,
        R.drawable.ic_user_green,
        R.drawable.ic_user_purple,
        R.drawable.ic_user_red,
        R.drawable.ic_user_yellow
    };

    private static final String[] CONTACT_PROJECTION = new String[] {
        Phone._ID,                      // 0
        Phone.DISPLAY_NAME,             // 1
    };

    private PreferenceGroup mUserListCategory;
    private Preference mMePreference;
    private EditTextPreference mNicknamePreference;
    private int mRemovingUserId = -1;
    private boolean mAddingUser;
    private boolean mProfileExists;

    private final Object mUserLock = new Object();
    private UserManager mUserManager;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_UPDATE_LIST:
                updateUserList();
                break;
            }
        }
    };

    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        addPreferencesFromResource(R.xml.user_settings);
        mUserListCategory = (PreferenceGroup) findPreference(KEY_USER_LIST);
        mMePreference = (Preference) findPreference(KEY_USER_ME);
        mMePreference.setOnPreferenceClickListener(this);
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            mMePreference.setSummary(null);
        }
        mNicknamePreference = (EditTextPreference) findPreference(KEY_USER_NICKNAME);
        mNicknamePreference.setOnPreferenceChangeListener(this);
        mNicknamePreference.setSummary(mUserManager.getUserInfo(UserHandle.myUserId()).name);
        loadProfile();
        setHasOptionsMenu(true);
        getActivity().registerReceiver(mUserChangeReceiver,
                new IntentFilter(Intent.ACTION_USER_REMOVED));
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            if (mUserManager.getMaxSupportedUsers() > mUserManager.getUsers().size()) {
                MenuItem addUserItem = menu.add(0, MENU_ADD_USER, 0, R.string.user_add_user_menu);
                addUserItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                        | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
        } else {
            MenuItem removeThisUser = menu.add(0, MENU_REMOVE_USER, 0, R.string.user_remove_user_menu);
            removeThisUser.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                    | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
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
        setPhotoId(mMePreference, mUserManager.getUserInfo(UserHandle.myUserId()));
    }

    private void onAddUserClicked() {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                showDialog(DIALOG_ADD_USER);
                setOnDismissListener(this);
            }
        }
    }

    private void onRemoveUserClicked(int userId) {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                mRemovingUserId = userId;
                showDialog(DIALOG_CONFIRM_REMOVE);
                setOnDismissListener(this);
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.user_confirm_remove_title)
                    .setMessage(R.string.user_confirm_remove_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeUserNow();
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            case DIALOG_ADD_USER:
                return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.user_add_user_title)
                .setMessage(R.string.user_add_user_message)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            addUserNow();
                        }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
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
                        // TODO: Show some progress while removing the user
                        mUserManager.removeUser(mRemovingUserId);
                        mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
                        mRemovingUserId = -1;
                    }
                }
            }.start();
        }
    }

    private void removeThisUser() {
        // TODO:
        Toast.makeText(getActivity(), "Not implemented yet!", Toast.LENGTH_SHORT).show();

        synchronized (mUserLock) {
            mRemovingUserId = -1;
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
                    }
                }
            }.start();
        }
    }

    private void updateUserList() {
        List<UserInfo> users = mUserManager.getUsers();

        mUserListCategory.removeAll();
        mUserListCategory.setOrderingAsAdded(false);

        for (UserInfo user : users) {
            Preference pref;
            if (user.id == UserHandle.myUserId()) {
                pref = mMePreference;
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
            }
            if (user.iconPath != null) {
                setPhotoId(pref, user);
            }
        }
        // Add a temporary entry for the user being created
        if (mAddingUser) {
            Preference pref = new UserPreference(getActivity(), null, UserPreference.USERID_UNKNOWN,
                    false, null);
            pref.setEnabled(false);
            pref.setTitle(R.string.user_new_user_name);
            pref.setSummary(R.string.user_adding_new_user);
            pref.setIcon(R.drawable.ic_user);
            mUserListCategory.addPreference(pref);
        }
        getActivity().invalidateOptionsMenu();
    }

    private void assignProfilePhoto(final UserInfo user) {
        if (!ProfileUpdateReceiver.copyProfilePhoto(getActivity(), user)) {
            assignDefaultPhoto(user);
        }
    }

    private String getProfileName() {
        Cursor cursor = getActivity().getContentResolver().query(
                    Profile.CONTENT_URI, CONTACT_PROJECTION, null, null, null);
        if (cursor == null) {
            Log.w(TAG, "getProfileName() returned NULL cursor!"
                    + " contact uri used " + Profile.CONTENT_URI);
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                mProfileExists = true;
                return cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    private void assignDefaultPhoto(UserInfo user) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                USER_DRAWABLES[user.id % USER_DRAWABLES.length]);
        ParcelFileDescriptor fd = mUserManager.setUserIcon(user.id);
        if (fd != null) {
            bitmap.compress(CompressFormat.PNG, 100,
                    new ParcelFileDescriptor.AutoCloseOutputStream(fd));
        }
    }

    private void setPhotoId(Preference pref, UserInfo user) {
        ParcelFileDescriptor fd = mUserManager.setUserIcon(user.id);
        Drawable d = Drawable.createFromStream(new ParcelFileDescriptor.AutoCloseInputStream(fd),
                user.iconPath);
        if (d == null) return;
        pref.setIcon(d);
    }

    private void setUserName(String name) {
        mUserManager.setUserName(UserHandle.myUserId(), name);
        mNicknamePreference.setSummary(name);
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
        }
        return false;
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

}
