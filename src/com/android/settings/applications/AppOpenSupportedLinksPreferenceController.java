/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.applications;

import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.RadioButtonPreference;

/**
 *  The radio group controller supports users to choose what kind supported links they need.
 */
public class AppOpenSupportedLinksPreferenceController extends BasePreferenceController
        implements RadioButtonPreference.OnClickListener {
    private static final String TAG = "OpenLinksPrefCtrl";
    private static final String KEY_LINK_OPEN_ALWAYS = "app_link_open_always";
    private static final String KEY_LINK_OPEN_ASK = "app_link_open_ask";
    private static final String KEY_LINK_OPEN_NEVER = "app_link_open_never";

    private Context mContext;
    private PackageManager mPackageManager;
    private String mPackageName;
    private int mCurrentIndex;
    private PreferenceCategory mPreferenceCategory;
    private String[] mRadioKeys = {KEY_LINK_OPEN_ALWAYS, KEY_LINK_OPEN_ASK, KEY_LINK_OPEN_NEVER};

    @VisibleForTesting
    RadioButtonPreference mAllowOpening;
    @VisibleForTesting
    RadioButtonPreference mAskEveryTime;
    @VisibleForTesting
    RadioButtonPreference mNotAllowed;

    public AppOpenSupportedLinksPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mPackageManager = context.getPackageManager();
    }

    /**
     * @param pkg selected package name.
     * @return return controller-self.
     */
    public AppOpenSupportedLinksPreferenceController setInit(String pkg) {
        mPackageName = pkg;
        return this;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mAllowOpening = makeRadioPreference(KEY_LINK_OPEN_ALWAYS, R.string.app_link_open_always);
        final int entriesNo = getEntriesNo();
        //This to avoid the summary line wrap
        mAllowOpening.setAppendixVisibility(View.GONE);
        mAllowOpening.setSummary(
                mContext.getResources().getQuantityString(R.plurals.app_link_open_always_summary,
                        entriesNo, entriesNo));
        mAskEveryTime = makeRadioPreference(KEY_LINK_OPEN_ASK, R.string.app_link_open_ask);
        mNotAllowed = makeRadioPreference(KEY_LINK_OPEN_NEVER, R.string.app_link_open_never);

        final int state = mPackageManager.getIntentVerificationStatusAsUser(mPackageName,
                UserHandle.myUserId());
        mCurrentIndex = linkStateToIndex(state);
        setRadioStatus(mCurrentIndex);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        final int clickedIndex = preferenceKeyToIndex(preference.getKey());
        if (mCurrentIndex != clickedIndex) {
            mCurrentIndex = clickedIndex;
            setRadioStatus(mCurrentIndex);
            updateAppLinkState(indexToLinkState(mCurrentIndex));
        }
    }

    private RadioButtonPreference makeRadioPreference(String key, int resourceId) {
        RadioButtonPreference pref = new RadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(resourceId);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    private int linkStateToIndex(int state) {
        switch (state) {
            case INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS:
                return 0; // Always
            case INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER:
                return 2; // Never
            default:
                return 1; // Ask
        }
    }

    private int indexToLinkState(int index) {
        switch (index) {
            case 0:
                return INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
            case 2:
                return INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;
            default:
                return INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK;
        }
    }

    private int preferenceKeyToIndex(String key) {
        for (int i = 0; i < mRadioKeys.length; i++) {
            if (TextUtils.equals(key, mRadioKeys[i])) {
                return i;
            }
        }
        return 1; // Ask
    }

    private void setRadioStatus(int index) {
        mAllowOpening.setChecked(index == 0 ? true : false);
        mAskEveryTime.setChecked(index == 1 ? true : false);
        mNotAllowed.setChecked(index == 2 ? true : false);
    }

    private boolean updateAppLinkState(final int newState) {
        final int userId = UserHandle.myUserId();
        final int priorState = mPackageManager.getIntentVerificationStatusAsUser(mPackageName,
                userId);

        if (priorState == newState) {
            return false;
        }

        boolean success = mPackageManager.updateIntentVerificationStatusAsUser(mPackageName,
                newState, userId);
        if (success) {
            // Read back the state to see if the change worked
            final int updatedState = mPackageManager.getIntentVerificationStatusAsUser(mPackageName,
                    userId);
            success = (newState == updatedState);
        } else {
            Log.e(TAG, "Couldn't update intent verification status!");
        }
        return success;
    }

    @VisibleForTesting
    int getEntriesNo() {
        return Utils.getHandledDomains(mPackageManager, mPackageName).size();
    }
}
