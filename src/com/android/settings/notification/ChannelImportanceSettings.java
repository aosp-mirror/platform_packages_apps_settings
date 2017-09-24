/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import static android.app.NotificationChannel.USER_LOCKED_IMPORTANCE;
import static android.app.NotificationChannel.USER_LOCKED_SOUND;
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MAX;
import static android.app.NotificationManager.IMPORTANCE_MIN;

import android.content.Context;
import android.media.RingtoneManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

public class ChannelImportanceSettings extends NotificationSettingsBase
        implements RadioButtonPreference.OnClickListener, Indexable {
    private static final String TAG = "NotiImportance";

    private static final String KEY_IMPORTANCE_HIGH = "importance_high";
    private static final String KEY_IMPORTANCE_DEFAULT = "importance_default";
    private static final String KEY_IMPORTANCE_LOW = "importance_low";
    private static final String KEY_IMPORTANCE_MIN = "importance_min";

    List<RadioButtonPreference> mImportances = new ArrayList<>();

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_CHANNEL_IMPORTANCE;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mChannel == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or channel");
            finish();
            return;
        }
        createPreferenceHierarchy();
    }

    @Override
    void setupBadge() {}

    @Override
    void updateDependents(boolean banned) {}

    @Override
    public void onPause() {
        super.onPause();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.notification_importance);
        root = getPreferenceScreen();

        for (int i = 0; i < root.getPreferenceCount(); i++) {
            Preference pref = root.getPreference(i);
            if (pref instanceof RadioButtonPreference) {
                RadioButtonPreference radioPref = (RadioButtonPreference) pref;
                radioPref.setOnClickListener(this);
                mImportances.add(radioPref);
            }
        }

        switch (mChannel.getImportance()) {
            case IMPORTANCE_MIN:
                updateRadioButtons(KEY_IMPORTANCE_MIN);
                break;
            case IMPORTANCE_LOW:
                updateRadioButtons(KEY_IMPORTANCE_LOW);
                break;
            case IMPORTANCE_DEFAULT:
                updateRadioButtons(KEY_IMPORTANCE_DEFAULT);
                break;
            case IMPORTANCE_HIGH:
            case IMPORTANCE_MAX:
                updateRadioButtons(KEY_IMPORTANCE_HIGH);
                break;
        }

        return root;
    }

    private void updateRadioButtons(String selectionKey) {
        for (RadioButtonPreference pref : mImportances) {
            if (selectionKey.equals(pref.getKey())) {
                pref.setChecked(true);
            } else {
                pref.setChecked(false);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference clicked) {
        int oldImportance = mChannel.getImportance();
        switch (clicked.getKey()) {
            case KEY_IMPORTANCE_HIGH:
                mChannel.setImportance(IMPORTANCE_HIGH);
                break;
            case KEY_IMPORTANCE_DEFAULT:
                mChannel.setImportance(IMPORTANCE_DEFAULT);
                break;
            case KEY_IMPORTANCE_LOW:
                mChannel.setImportance(IMPORTANCE_LOW);
                break;
            case KEY_IMPORTANCE_MIN:
                mChannel.setImportance(IMPORTANCE_MIN);
                break;
        }
        updateRadioButtons(clicked.getKey());

        // If you are moving from an importance level without sound to one with sound,
        // but the sound you had selected was "Silence",
        // then set sound for this channel to your default sound,
        // because you probably intended to cause this channel to actually start making sound.
        if (oldImportance < IMPORTANCE_DEFAULT && !hasValidSound(mChannel) &&
                mChannel.getImportance() >= IMPORTANCE_DEFAULT) {
            mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    mChannel.getAudioAttributes());
            mChannel.lockFields(USER_LOCKED_SOUND);
        }
        mChannel.lockFields(USER_LOCKED_IMPORTANCE);
        mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, mChannel);
    }

    // This page exists per notification channel; should not be included
    // in search
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    return null;
                }
            };
}
