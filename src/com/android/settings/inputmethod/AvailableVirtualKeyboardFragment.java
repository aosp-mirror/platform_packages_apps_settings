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

package com.android.settings.inputmethod;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeUtilCompat;
import com.android.settingslib.inputmethod.InputMethodPreference;
import com.android.settingslib.inputmethod.InputMethodSettingValuesWrapper;
import com.android.settingslib.search.SearchIndexable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/**
 * The fragment for on-screen keyboard settings which used to display user installed IMEs.
 */
@SearchIndexable
public class AvailableVirtualKeyboardFragment extends DashboardFragment
        implements InputMethodPreference.OnSavePreferenceListener {
    private static final String TAG = "AvailableVirtualKeyboardFragment";

    @VisibleForTesting
    final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();

    @VisibleForTesting
    InputMethodSettingValuesWrapper mInputMethodSettingValues;

    @VisibleForTesting
    Context mUserAwareContext;

    private int mUserId;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.available_virtual_keyboard);
        mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(mUserAwareContext);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final int profileType = getArguments().getInt(ProfileSelectFragment.EXTRA_PROFILE);
        final UserManager userManager = context.getSystemService(UserManager.class);
        final int currentUserId = UserHandle.myUserId();
        final int newUserId;
        final Context newUserAwareContext;
        switch (profileType) {
            case ProfileSelectFragment.ProfileType.WORK: {
                // If the user is a managed profile user, use currentUserId directly. Or get the
                // managed profile userId instead.
                newUserId = userManager.isManagedProfile()
                        ? currentUserId : Utils.getManagedProfileId(userManager, currentUserId);
                newUserAwareContext = context.createContextAsUser(UserHandle.of(newUserId), 0);
                break;
            }
            case ProfileSelectFragment.ProfileType.PRIVATE: {
                // If the user is a private profile user, use currentUserId directly. Or get the
                // private profile userId instead.
                newUserId = userManager.isPrivateProfile()
                        ? currentUserId
                        : Utils.getCurrentUserIdOfType(
                                userManager, ProfileSelectFragment.ProfileType.PRIVATE);
                newUserAwareContext = context.createContextAsUser(UserHandle.of(newUserId), 0);
                break;
            }
            case ProfileSelectFragment.ProfileType.PERSONAL: {
                // Use the parent user of the current user if the current user is profile.
                final UserHandle currentUser = UserHandle.of(currentUserId);
                final UserHandle userProfileParent = userManager.getProfileParent(currentUser);
                if (userProfileParent != null) {
                    newUserId = userProfileParent.getIdentifier();
                    newUserAwareContext = context.createContextAsUser(userProfileParent, 0);
                } else {
                    newUserId = currentUserId;
                    newUserAwareContext = context;
                }
                break;
            }
            default:
                newUserId = currentUserId;
                newUserAwareContext = context;
        }
        mUserId = newUserId;
        mUserAwareContext = newUserAwareContext;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh internal states in mInputMethodSettingValues to keep the latest
        // "InputMethodInfo"s and "InputMethodSubtype"s
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        updateInputMethodPreferenceViews();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.available_virtual_keyboard;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onSaveInputMethodPreference(final InputMethodPreference pref) {
        final boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard
                == Configuration.KEYBOARD_QWERTY;
        InputMethodAndSubtypeUtilCompat.saveInputMethodSubtypeListForUser(this,
                mUserAwareContext.getContentResolver(),
                getContext().getSystemService(
                        InputMethodManager.class).getInputMethodListAsUser(mUserId),
                hasHardwareKeyboard, mUserId);
        // Update input method settings and preference list.
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        for (final InputMethodPreference p : mInputMethodPreferenceList) {
            p.updatePreferenceViews();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ENABLE_VIRTUAL_KEYBOARDS;
    }

    @VisibleForTesting
    void updateInputMethodPreferenceViews() {
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        // Clear existing "InputMethodPreference"s
        mInputMethodPreferenceList.clear();
        final List<String> permittedList = mUserAwareContext.getSystemService(
                DevicePolicyManager.class).getPermittedInputMethods();
        final Context prefContext = getPrefContext();
        final List<InputMethodInfo> imis = mInputMethodSettingValues.getInputMethodList();
        final List<InputMethodInfo> enabledImis = getContext().getSystemService(
                InputMethodManager.class).getEnabledInputMethodListAsUser(UserHandle.of(mUserId));
        final int numImis = (imis == null ? 0 : imis.size());
        for (int i = 0; i < numImis; ++i) {
            final InputMethodInfo imi = imis.get(i);
            // TODO (b/182876800): Move this logic out of isAllowedByOrganization and
            // into a new boolean.
            // If an input method is enabled but not included in the permitted list, then set it as
            // allowed by organization. Doing so will allow the user to disable the input method and
            // remain complaint with the organization's policy. Once disabled, the input method
            // cannot be re-enabled because it is not in the permitted list.
            final boolean isAllowedByOrganization = permittedList == null
                    || permittedList.contains(imi.getPackageName())
                    || enabledImis.contains(imi);
            final InputMethodPreference pref = new InputMethodPreference(prefContext, imi,
                    isAllowedByOrganization, this, mUserId);
            pref.setIcon(imi.loadIcon(mUserAwareContext.getPackageManager()));
            mInputMethodPreferenceList.add(pref);
        }
        final Collator collator = Collator.getInstance();
        mInputMethodPreferenceList.sort((lhs, rhs) -> lhs.compareTo(rhs, collator));
        getPreferenceScreen().removeAll();
        for (int i = 0; i < numImis; ++i) {
            final InputMethodPreference pref = mInputMethodPreferenceList.get(i);
            pref.setOrder(i);
            getPreferenceScreen().addPreference(pref);
            InputMethodAndSubtypeUtilCompat.removeUnnecessaryNonPersistentPreference(pref);
            pref.updatePreferenceViews();
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    List<SearchIndexableResource> res = new ArrayList<>();
                    SearchIndexableResource index = new SearchIndexableResource(context);
                    index.xmlResId = R.xml.available_virtual_keyboard;
                    res.add(index);
                    return res;
                }
            };
}
