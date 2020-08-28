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

package com.android.settings.notification.zen;

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;

import static com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_GONE;
import static com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_SETTING;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Contacts;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPreferenceWithExtraWidget;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Common preference controller functionality shared by
 * ZenModePriorityMessagesPreferenceController and ZenModePriorityCallsPreferenceController.
 *
 * This includes the options to choose the priority senders that are allowed to bypass DND for
 * calls or messages. This can be one of four values: starred contacts, all contacts, anyone, or
 * no one.
 */
public class ZenModePrioritySendersPreferenceController
        extends AbstractZenModePreferenceController {
    @VisibleForTesting static final String KEY_ANY = "senders_anyone";
    @VisibleForTesting static final String KEY_CONTACTS = "senders_contacts";
    @VisibleForTesting static final String KEY_STARRED = "senders_starred_contacts";
    @VisibleForTesting static final String KEY_NONE = "senders_none";

    private static final Intent ALL_CONTACTS_INTENT =
            new Intent(Contacts.Intents.UI.LIST_DEFAULT);
    private static final Intent STARRED_CONTACTS_INTENT =
            new Intent(Contacts.Intents.UI.LIST_STARRED_ACTION);
    private static final Intent FALLBACK_INTENT = new Intent(Intent.ACTION_MAIN);

    private final PackageManager mPackageManager;
    private final boolean mIsMessages; // if this is false, then this preference is for calls

    private PreferenceCategory mPreferenceCategory;
    private List<RadioButtonPreferenceWithExtraWidget> mRadioButtonPreferences = new ArrayList<>();

    public ZenModePrioritySendersPreferenceController(Context context, String key,
            Lifecycle lifecycle, boolean isMessages) {
        super(context, key, lifecycle);
        mIsMessages = isMessages;

        mPackageManager = mContext.getPackageManager();
        if (!FALLBACK_INTENT.hasCategory(Intent.CATEGORY_APP_CONTACTS)) {
            FALLBACK_INTENT.addCategory(Intent.CATEGORY_APP_CONTACTS);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        if (mPreferenceCategory.findPreference(KEY_ANY) == null) {
            makeRadioPreference(KEY_STARRED,
                    com.android.settings.R.string.zen_mode_from_starred);
            makeRadioPreference(KEY_CONTACTS,
                    com.android.settings.R.string.zen_mode_from_contacts);
            makeRadioPreference(KEY_ANY,
                    com.android.settings.R.string.zen_mode_from_anyone);
            makeRadioPreference(KEY_NONE,
                    mIsMessages
                            ? com.android.settings.R.string.zen_mode_none_messages
                            : com.android.settings.R.string.zen_mode_none_calls);
            updateSummaries();
        }

        super.displayPreference(screen);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        final int currSetting = getPrioritySenders();

        for (RadioButtonPreferenceWithExtraWidget pref : mRadioButtonPreferences) {
            pref.setChecked(keyToSetting(pref.getKey()) == currSetting);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
    }

    private void updateSummaries() {
        for (RadioButtonPreferenceWithExtraWidget pref : mRadioButtonPreferences) {
            pref.setSummary(getSummary(pref.getKey()));
        }
    }

    private static int keyToSetting(String key) {
        switch (key) {
            case KEY_STARRED:
                return NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
            case KEY_CONTACTS:
                return NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
            case KEY_ANY:
                return NotificationManager.Policy.PRIORITY_SENDERS_ANY;
            case KEY_NONE:
            default:
                return ZenModeBackend.SOURCE_NONE;
        }
    }

    private String getSummary(String key) {
        switch (key) {
            case KEY_STARRED:
                return mBackend.getStarredContactsSummary(mContext);
            case KEY_CONTACTS:
                return mBackend.getContactsNumberSummary(mContext);
            case KEY_ANY:
                return mContext.getResources().getString(mIsMessages
                                ? R.string.zen_mode_all_messages_summary
                                : R.string.zen_mode_all_calls_summary);
            case KEY_NONE:
            default:
                return null;
        }
    }

    private int getPrioritySenders() {
        if (mIsMessages) {
            return mBackend.getPriorityMessageSenders();
        } else {
            return mBackend.getPriorityCallSenders();
        }
    }

    private RadioButtonPreferenceWithExtraWidget makeRadioPreference(String key, int titleId) {
        RadioButtonPreferenceWithExtraWidget pref =
                new RadioButtonPreferenceWithExtraWidget(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(mRadioButtonClickListener);

        View.OnClickListener widgetClickListener = getWidgetClickListener(key);
        if (widgetClickListener != null) {
            pref.setExtraWidgetOnClickListener(widgetClickListener);
            pref.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_SETTING);
        } else {
            pref.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_GONE);
        }

        mPreferenceCategory.addPreference(pref);
        mRadioButtonPreferences.add(pref);
        return pref;
    }

    private RadioButtonPreference.OnClickListener mRadioButtonClickListener =
            new RadioButtonPreference.OnClickListener() {
        @Override
        public void onRadioButtonClicked(RadioButtonPreference preference) {
            int selectedSetting = keyToSetting(preference.getKey());
            if (selectedSetting != getPrioritySenders()) {
                mBackend.saveSenders(
                        mIsMessages ? PRIORITY_CATEGORY_MESSAGES : PRIORITY_CATEGORY_CALLS,
                        selectedSetting);
            }
        }
    };

    private View.OnClickListener getWidgetClickListener(String key) {
        if (!KEY_CONTACTS.equals(key) && !KEY_STARRED.equals(key)) {
            return null;
        }

        if (KEY_STARRED.equals(key) && !isStarredIntentValid()) {
            return null;
        }

        if (KEY_CONTACTS.equals(key) && !isContactsIntentValid()) {
            return null;
        }

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (KEY_STARRED.equals(key)
                        && STARRED_CONTACTS_INTENT.resolveActivity(mPackageManager) != null) {
                    mContext.startActivity(STARRED_CONTACTS_INTENT);
                } else if (KEY_CONTACTS.equals(key)
                        && ALL_CONTACTS_INTENT.resolveActivity(mPackageManager) != null) {
                    mContext.startActivity(ALL_CONTACTS_INTENT);
                } else {
                    mContext.startActivity(FALLBACK_INTENT);
                }
            }
        };
    }

    private boolean isStarredIntentValid() {
        return STARRED_CONTACTS_INTENT.resolveActivity(mPackageManager) != null
                || FALLBACK_INTENT.resolveActivity(mPackageManager) != null;
    }

    private boolean isContactsIntentValid() {
        return ALL_CONTACTS_INTENT.resolveActivity(mPackageManager) != null
                || FALLBACK_INTENT.resolveActivity(mPackageManager) != null;
    }
}
