/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.content.Intent.EXTRA_USER;

import android.accounts.Account;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;

import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.apppreference.AppPreference;

public class AccountTypePreference extends AppPreference implements OnPreferenceClickListener {
    /**
     * Title of the tile that is shown to the user.
     *
     * @attr ref android.R.styleable#PreferenceHeader_title
     */
    private final CharSequence mTitle;

    /**
     * Packange name used to resolve the resources of the title shown to the user in the new
     * fragment.
     */
    private final String mTitleResPackageName;

    /**
     * Resource id of the title shown to the user in the new fragment.
     */
    private final int mTitleResId;

    /**
     * Summary of the tile that is shown to the user.
     */
    private final CharSequence mSummary;

    /**
     * Full class name of the fragment to display when this tile is
     * selected.
     *
     * @attr ref android.R.styleable#PreferenceHeader_fragment
     */
    private final String mFragment;

    /**
     * Optional arguments to supply to the fragment when it is
     * instantiated.
     */
    private final Bundle mFragmentArguments;

    private final int mMetricsCategory;

    public AccountTypePreference(Context context, int metricsCategory, Account account,
            String titleResPackageName, int titleResId, CharSequence summary, String fragment,
            Bundle fragmentArguments, Drawable icon) {
        super(context);
        mTitle = account.name;
        mTitleResPackageName = titleResPackageName;
        mTitleResId = titleResId;
        mSummary = summary;
        mFragment = fragment;
        mFragmentArguments = fragmentArguments;
        mMetricsCategory = metricsCategory;

        setKey(buildKey(account));
        setTitle(mTitle);
        setSummary(summary);
        setIcon(icon);

        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mFragment != null) {
            UserManager userManager =
                    (UserManager) getContext().getSystemService(Context.USER_SERVICE);
            UserHandle user = mFragmentArguments.getParcelable(EXTRA_USER);
            if (user != null && Utils.startQuietModeDialogIfNecessary(getContext(), userManager,
                    user.getIdentifier())) {
                return true;
            } else if (user != null && Utils.unlockWorkProfileIfNecessary(getContext(),
                    user.getIdentifier())) {
                return true;
            }
            new SubSettingLauncher(getContext())
                    .setDestination(mFragment)
                    .setArguments(mFragmentArguments)
                    .setTitleRes(mTitleResPackageName, mTitleResId)
                    .setSourceMetricsCategory(mMetricsCategory)
                    .launch();
            return true;
        }
        return false;
    }

    /**
     * Build a unique preference key based on account.
     */
    public static String buildKey(Account account) {
        return String.valueOf(account.hashCode());
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public CharSequence getSummary() {
        return mSummary;
    }
}
