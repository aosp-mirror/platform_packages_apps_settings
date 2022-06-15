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

import android.app.settings.SettingsEnums;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.RadioButtonPreference;

/**
 * Display the Open Supported Links page. Allow users choose what kind supported links they need.
 */
public class OpenSupportedLinks extends AppInfoWithHeader implements
        RadioButtonPreference.OnClickListener {
    private static final String TAG = "OpenSupportedLinks";
    private static final String RADIO_GROUP_KEY = "supported_links_radio_group";
    private static final String FOOTER_KEY = "supported_links_footer";
    private static final String KEY_LINK_OPEN_ALWAYS = "app_link_open_always";
    private static final String KEY_LINK_OPEN_ASK = "app_link_open_ask";
    private static final String KEY_LINK_OPEN_NEVER = "app_link_open_never";

    private static final int ALLOW_ALWAYS_OPENING = 0;
    private static final int ASK_EVERY_TIME = 1;
    private static final int NOT_ALLOWED_OPENING = 2;

    private int mCurrentIndex;
    private String[] mRadioKeys = {KEY_LINK_OPEN_ALWAYS, KEY_LINK_OPEN_ASK, KEY_LINK_OPEN_NEVER};

    @VisibleForTesting
    PackageManager mPackageManager;
    @VisibleForTesting
    PreferenceCategory mPreferenceCategory;
    @VisibleForTesting
    RadioButtonPreference mAllowOpening;
    @VisibleForTesting
    RadioButtonPreference mAskEveryTime;
    @VisibleForTesting
    RadioButtonPreference mNotAllowed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPackageManager = getPackageManager();
        addPreferencesFromResource(R.xml.open_supported_links);
        initRadioPreferencesGroup();
        updateFooterPreference();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.OPEN_SUPPORTED_LINKS;
    }

    /**
     * Here to handle radio group and generate the radios.
     */
    @VisibleForTesting
    void initRadioPreferencesGroup() {
        mPreferenceCategory = findPreference(RADIO_GROUP_KEY);
        mAllowOpening = makeRadioPreference(KEY_LINK_OPEN_ALWAYS, R.string.app_link_open_always);
        final int entriesNo = getEntriesNo();
        //This to avoid the summary line wrap
        mAllowOpening.setAppendixVisibility(View.GONE);
        mAllowOpening.setSummary(getResources().getQuantityString(
                R.plurals.app_link_open_always_summary, entriesNo, entriesNo));
        mAskEveryTime = makeRadioPreference(KEY_LINK_OPEN_ASK, R.string.app_link_open_ask);
        mNotAllowed = makeRadioPreference(KEY_LINK_OPEN_NEVER, R.string.app_link_open_never);

        final int state = mPackageManager.getIntentVerificationStatusAsUser(mPackageName, mUserId);
        mCurrentIndex = linkStateToIndex(state);
        setRadioStatus(mCurrentIndex);
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

    private RadioButtonPreference makeRadioPreference(String key, int stringId) {
        final RadioButtonPreference pref = new RadioButtonPreference(
                mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(stringId);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    @VisibleForTesting
    int getEntriesNo() {
        return Utils.getHandledDomains(mPackageManager, mPackageName).size();
    }

    private int linkStateToIndex(int state) {
        switch (state) {
            case INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS:
                return ALLOW_ALWAYS_OPENING;
            case INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER:
                return NOT_ALLOWED_OPENING;
            default:
                return ASK_EVERY_TIME;
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

    private void setRadioStatus(int index) {
        mAllowOpening.setChecked(index == ALLOW_ALWAYS_OPENING);
        mAskEveryTime.setChecked(index == ASK_EVERY_TIME);
        mNotAllowed.setChecked(index == NOT_ALLOWED_OPENING);
    }

    private int preferenceKeyToIndex(String key) {
        for (int i = 0; i < mRadioKeys.length; i++) {
            if (TextUtils.equals(key, mRadioKeys[i])) {
                return i;
            }
        }
        return ASK_EVERY_TIME;
    }

    private void updateAppLinkState(final int newState) {
        final int priorState = mPackageManager.getIntentVerificationStatusAsUser(mPackageName,
                mUserId);

        if (priorState == newState) {
            return;
        }

        final boolean success = mPackageManager.updateIntentVerificationStatusAsUser(mPackageName,
                newState, mUserId);
        if (success) {
            // Read back the state to see if the change worked
            final int updatedState = mPackageManager.getIntentVerificationStatusAsUser(mPackageName,
                    mUserId);
        } else {
            Log.e(TAG, "Couldn't update intent verification status!");
        }
    }

    /**
     * Here is handle the Footer.
     */
    private void updateFooterPreference() {
        final FooterPreference footer = findPreference(FOOTER_KEY);
        if (footer == null) {
            Log.w(TAG, "Can't find the footer preference.");
            return;
        }
        addLinksToFooter(footer);
    }

    @VisibleForTesting
    void addLinksToFooter(FooterPreference footer) {
        final ArraySet<String> result = Utils.getHandledDomains(mPackageManager, mPackageName);
        if (result.isEmpty()) {
            Log.w(TAG, "Can't find any app links.");
            return;
        }
        CharSequence title = footer.getTitle() + System.lineSeparator();
        for (String link : result) {
            title = title + System.lineSeparator() + link;
        }
        footer.setTitle(title);
    }

    @Override
    protected boolean refreshUi() {
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }
}
