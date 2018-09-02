/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.Intent.EXTRA_USER;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.collect.Maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER}, if the user for
 * which the action needs to be performed is different to the one the Settings App will run in.
 */
public class ChooseAccountPreferenceController extends BasePreferenceController {

    private static final String TAG = "ChooseAccountPrefCtrler";

    private final List<ProviderEntry> mProviderList;
    private final Map<String, AuthenticatorDescription> mTypeToAuthDescription;

    private String[] mAuthorities;
    private Set<String> mAccountTypesFilter;
    private AuthenticatorDescription[] mAuthDescs;
    private Map<String, List<String>> mAccountTypeToAuthorities;
    // The UserHandle of the user we are choosing an account for
    private UserHandle mUserHandle;
    private Activity mActivity;
    private PreferenceScreen mScreen;

    public ChooseAccountPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mProviderList = new ArrayList<>();
        mTypeToAuthDescription = new HashMap<>();
    }

    public void initialize(String[] authorities, String[] accountTypesFilter, UserHandle userHandle,
            Activity activity) {
        mActivity = activity;
        mAuthorities = authorities;
        mUserHandle = userHandle;

        if (accountTypesFilter != null) {
            mAccountTypesFilter = new HashSet<>();
            for (String accountType : accountTypesFilter) {
                mAccountTypesFilter.add(accountType);
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        updateAuthDescriptions();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof ProviderPreference)) {
            return false;
        }

        ProviderPreference pref = (ProviderPreference) preference;
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Attempting to add account of type " + pref.getAccountType());
        }
        finishWithAccountType(pref.getAccountType());
        return true;
    }

    /**
     * Updates provider icons. Subclasses should call this in onCreate()
     * and update any UI that depends on AuthenticatorDescriptions in onAuthDescriptionsUpdated().
     */
    private void updateAuthDescriptions() {
        mAuthDescs = AccountManager.get(mContext).getAuthenticatorTypesAsUser(
                mUserHandle.getIdentifier());
        for (int i = 0; i < mAuthDescs.length; i++) {
            mTypeToAuthDescription.put(mAuthDescs[i].type, mAuthDescs[i]);
        }
        onAuthDescriptionsUpdated();
    }

    private void onAuthDescriptionsUpdated() {
        // Create list of providers to show on preference screen
        for (int i = 0; i < mAuthDescs.length; i++) {
            final String accountType = mAuthDescs[i].type;
            final CharSequence providerName = getLabelForType(accountType);

            // Skip preferences for authorities not specified. If no authorities specified,
            // then include them all.
            final List<String> accountAuths = getAuthoritiesForAccountType(accountType);
            boolean addAccountPref = true;
            if (mAuthorities != null && mAuthorities.length > 0 && accountAuths != null) {
                addAccountPref = false;
                for (int k = 0; k < mAuthorities.length; k++) {
                    if (accountAuths.contains(mAuthorities[k])) {
                        addAccountPref = true;
                        break;
                    }
                }
            }
            if (addAccountPref && mAccountTypesFilter != null
                    && !mAccountTypesFilter.contains(accountType)) {
                addAccountPref = false;
            }
            if (addAccountPref) {
                mProviderList.add(
                        new ProviderEntry(providerName, accountType));
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Skipped pref " + providerName + ": has no authority we need");
                }
            }
        }
        final Context context = mScreen.getContext();
        if (mProviderList.size() == 1) {
            // There's only one provider that matches. If it is disabled by admin show the
            // support dialog otherwise run it.
            final RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtilsInternal.checkIfAccountManagementDisabled(
                            context, mProviderList.get(0).getType(), mUserHandle.getIdentifier());
            if (admin != null) {
                mActivity.setResult(RESULT_CANCELED,
                        RestrictedLockUtils.getShowAdminSupportDetailsIntent(
                                context, admin));
                mActivity.finish();
            } else {
                finishWithAccountType(mProviderList.get(0).getType());
            }
        } else if (mProviderList.size() > 0) {
            Collections.sort(mProviderList);
            for (ProviderEntry pref : mProviderList) {
                final Drawable drawable = getDrawableForType(pref.getType());
                final ProviderPreference p = new ProviderPreference(context,
                        pref.getType(), drawable, pref.getName());
                p.setKey(pref.getType().toString());
                p.checkAccountManagementAndSetDisabled(mUserHandle.getIdentifier());
                mScreen.addPreference(p);
            }
        } else {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                final StringBuilder auths = new StringBuilder();
                for (String a : mAuthorities) {
                    auths.append(a);
                    auths.append(' ');
                }
                Log.v(TAG, "No providers found for authorities: " + auths);
            }
            mActivity.setResult(RESULT_CANCELED);
            mActivity.finish();
        }
    }

    private List<String> getAuthoritiesForAccountType(String type) {
        if (mAccountTypeToAuthorities == null) {
            mAccountTypeToAuthorities = Maps.newHashMap();
            final SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(
                    mUserHandle.getIdentifier());
            for (int i = 0, n = syncAdapters.length; i < n; i++) {
                final SyncAdapterType sa = syncAdapters[i];
                List<String> authorities = mAccountTypeToAuthorities.get(sa.accountType);
                if (authorities == null) {
                    authorities = new ArrayList<>();
                    mAccountTypeToAuthorities.put(sa.accountType, authorities);
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "added authority " + sa.authority + " to accountType "
                            + sa.accountType);
                }
                authorities.add(sa.authority);
            }
        }
        return mAccountTypeToAuthorities.get(type);
    }

    /**
     * Gets an icon associated with a particular account type. If none found, return null.
     *
     * @param accountType the type of account
     * @return a drawable for the icon or a default icon returned by
     * {@link PackageManager#getDefaultActivityIcon} if one cannot be found.
     */
    @VisibleForTesting
    Drawable getDrawableForType(final String accountType) {
        Drawable icon = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            try {
                final AuthenticatorDescription desc = mTypeToAuthDescription.get(accountType);
                final Context authContext = mActivity
                        .createPackageContextAsUser(desc.packageName, 0, mUserHandle);
                icon = mContext.getPackageManager().getUserBadgedIcon(
                        authContext.getDrawable(desc.iconId), mUserHandle);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "No icon name for account type " + accountType);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "No icon resource for account type " + accountType);
            }
        }
        if (icon != null) {
            return icon;
        } else {
            return mContext.getPackageManager().getDefaultActivityIcon();
        }
    }

    /**
     * Gets the label associated with a particular account type. If none found, return null.
     *
     * @param accountType the type of account
     * @return a CharSequence for the label or null if one cannot be found.
     */
    @VisibleForTesting
    CharSequence getLabelForType(final String accountType) {
        CharSequence label = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            try {
                final AuthenticatorDescription desc = mTypeToAuthDescription.get(accountType);
                final Context authContext = mActivity
                        .createPackageContextAsUser(desc.packageName, 0, mUserHandle);
                label = authContext.getResources().getText(desc.labelId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "No label name for account type " + accountType);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "No label resource for account type " + accountType);
            }
        }
        return label;
    }

    private void finishWithAccountType(String accountType) {
        Intent intent = new Intent();
        intent.putExtra(AddAccountSettings.EXTRA_SELECTED_ACCOUNT, accountType);
        intent.putExtra(EXTRA_USER, mUserHandle);
        mActivity.setResult(RESULT_OK, intent);
        mActivity.finish();
    }
}
