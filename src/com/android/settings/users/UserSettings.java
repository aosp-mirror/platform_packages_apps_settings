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

import android.content.pm.UserInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

public class UserSettings extends SettingsPreferenceFragment
        implements OnPreferenceClickListener {

    private static final String KEY_USER_LIST = "user_list";
    private static final int MENU_ADD_USER = Menu.FIRST;

    private PreferenceGroup mUserListCategory;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.user_settings);
        mUserListCategory = (PreferenceGroup) findPreference(KEY_USER_LIST);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem addAccountItem = menu.add(0, MENU_ADD_USER, 0, R.string.user_add_user_menu);
        addAccountItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_ADD_USER) {
            onAddUserClicked();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void onAddUserClicked() {
        ((PreferenceActivity) getActivity()).startPreferencePanel(
                UserDetailsSettings.class.getName(), null, R.string.user_details_title,
                null, this, 0);
    }

    private void updateUserList() {
        List<UserInfo> users = getActivity().getPackageManager().getUsers();

        mUserListCategory.removeAll();
        for (UserInfo user : users) {
            if (user.id == 0) continue;
            Preference pref = new Preference(getActivity());
            pref.setTitle(user.name);
            pref.setOnPreferenceClickListener(this);
            pref.setKey("id=" + user.id);
            mUserListCategory.addPreference(pref);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        String sid = pref.getKey();
        if (sid != null && sid.startsWith("id=")) {
            int id = Integer.parseInt(sid.substring(3));
            Bundle args = new Bundle();
            args.putInt(UserDetailsSettings.EXTRA_USER_ID, id);
            ((PreferenceActivity) getActivity()).startPreferencePanel(
                    UserDetailsSettings.class.getName(),
                    args, 0, pref.getTitle(), this, 0);
            return true;
        }
        return false;
    }
}
