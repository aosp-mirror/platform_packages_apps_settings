/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.accounts;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.provider.Settings.EXTRA_AUTHORITIES;

/**
 * Settings screen for the account types on the device.
 * This shows all account types available for personal and work profiles.
 *
 * An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER}, if the user for
 * which the action needs to be performed is different to the one the Settings App will run in.
 */
public class AccountSettings extends SettingsPreferenceFragment implements Indexable {
    public static final String TAG = "AccountSettings";


    private String[] mAuthorities;

    private AccountPreferenceController mAccountPreferenceController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthorities = getActivity().getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        setHasOptionsMenu(true);
        mAccountPreferenceController =
            new AccountPreferenceController(getActivity(), this, mAuthorities);
        getLifecycle().addObserver(mAccountPreferenceController);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.account_settings, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final UserHandle currentProfile = Process.myUserHandle();
        SparseArray<AccountPreferenceController.ProfileData> profiles =
            mAccountPreferenceController.getProfileData();

        if (profiles.size() == 1) {
            menu.findItem(R.id.account_settings_menu_auto_sync)
                .setVisible(true)
                .setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile))
                .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                    currentProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync_personal).setVisible(false);
            menu.findItem(R.id.account_settings_menu_auto_sync_work).setVisible(false);
        } else if (profiles.size() > 1) {
            // We assume there's only one managed profile, otherwise UI needs to change
            final UserHandle managedProfile = profiles.valueAt(1).userInfo.getUserHandle();

            menu.findItem(R.id.account_settings_menu_auto_sync_personal)
                .setVisible(true)
                .setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile))
                .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                    currentProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync_work)
                .setVisible(true)
                .setOnMenuItemClickListener(new MasterSyncStateClickListener(managedProfile))
                .setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(
                    managedProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync).setVisible(false);
        } else {
            Log.w(TAG, "Method onPrepareOptionsMenu called before profiles was initialized");
        }
    }

    private class MasterSyncStateClickListener implements MenuItem.OnMenuItemClickListener {
        private final UserHandle mUserHandle;

        public MasterSyncStateClickListener(UserHandle userHandle) {
            mUserHandle = userHandle;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (ActivityManager.isUserAMonkey()) {
                Log.d(TAG, "ignoring monkey's attempt to flip sync state");
            } else {
                AutoSyncDataPreferenceController.ConfirmAutoSyncChangeFragment.show(
                    AccountSettings.this, !item.isChecked(), mUserHandle, null/*preference*/);
            }
            return true;
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.account_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                new AccountPreferenceController(context, null, null).updateRawDataToIndex(result);
                return result;
            }
        };
}
