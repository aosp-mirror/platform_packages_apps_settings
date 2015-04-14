/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static android.provider.Settings.EXTRA_DO_NOT_DISTURB_MODE_MINUTES;
import static android.provider.Settings.EXTRA_DO_NOT_DISTURB_MODE_ENABLED;

import com.android.settings.R;
import com.android.settings.utils.VoiceSelectionAdapter;
import com.android.settings.utils.VoiceSelection;
import com.android.settings.utils.VoiceSelectionFragment;
import com.android.settings.utils.VoiceSettingsActivity;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for modifying the Zen mode (Do not disturb) by voice
 * using the Voice Interaction API.
 */
public class ZenModeVoiceActivity extends VoiceSettingsActivity {
    private static final String TAG = "ZenModeVoiceActivity";
    private static final int MINUTES_MS = 60 * 1000;

    @Override
    protected boolean onVoiceSettingInteraction(Intent intent) {
        setContentView(R.layout.voice_interaction);
        pickNotificationMode(intent);
        return false;
    }

    /**
     * Start a voice interaction to ask what kind of interruptions should
     * be permitted. The intent can optionally include extra information about the type
     * of interruptions desired or how long interruptions should be limited to that are
     * used as hints.
     */
    private void pickNotificationMode(final Intent intent) {
        boolean enabled = intent.getBooleanExtra(EXTRA_DO_NOT_DISTURB_MODE_ENABLED, false);
        boolean specified = intent.hasExtra(EXTRA_DO_NOT_DISTURB_MODE_ENABLED);

        List<VoiceSelection> states = new ArrayList<VoiceSelection>();
        if (!specified || enabled) {
            states.add(new ModeSelection(this, Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                    R.string.zen_mode_option_important_interruptions,
                    R.string.zen_mode_option_important_voice_synonyms));
            states.add(new ModeSelection(this, Global.ZEN_MODE_ALARMS,
                    R.string.zen_mode_option_alarms,
                    R.string.zen_mode_option_alarms_voice_synonyms));
            states.add(new ModeSelection(this, Global.ZEN_MODE_NO_INTERRUPTIONS,
                    R.string.zen_mode_option_no_interruptions,
                    R.string.zen_mode_option_no_interruptions_voice_synonyms));
        }
        if (!specified || !enabled) {
            states.add(new ModeSelection(this, Global.ZEN_MODE_OFF,
                    R.string.zen_mode_option_off,
                    R.string.zen_mode_option_off_voice_synonyms));
        }
        VoiceSelectionFragment fragment = new VoiceSelectionFragment();
        fragment.setArguments(VoiceSelectionFragment.createArguments(
                getString(R.string.zen_mode_interruptions_voice_prompt)));
        fragment.setListAdapter(
                new VoiceSelectionAdapter(this, R.layout.voice_item_row, states));
        fragment.setOnItemSelectedHandler(new VoiceSelection.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int index, VoiceSelection selection) {
                int mode = ((ModeSelection) selection).mMode;
                ConditionSelection conditionSelection = getConditionSelection(
                        intent.getIntExtra(EXTRA_DO_NOT_DISTURB_MODE_MINUTES, 0));
                if (mode != Global.ZEN_MODE_OFF) {
                    if (conditionSelection == null) {
                        pickDuration(selection.getLabel(), mode);
                        return;
                    }
                }
                setZenModeConfig(mode, conditionSelection.mCondition);
                notifySuccess(getChangeSummary(mode, conditionSelection));
                finish();
            }
        });
        showFragment(fragment, "pick_mode_fragment");
    }

    /**
     * Start a voice interaction to ask for the zen mode duration.
     */
    private void pickDuration(CharSequence label, final int mode) {
        setTitle(label.toString());
        List<VoiceSelection> states = new ArrayList<VoiceSelection>();
        states.add(new ConditionSelection(null, -1,
              getString(R.string.zen_mode_duration_indefinte_voice_label),
              getString(R.string.zen_mode_duration_indefinite_voice_synonyms)));
        for (int i = ZenModeConfig.MINUTE_BUCKETS.length - 1; i >= 0; --i) {
            states.add(getConditionSelection(ZenModeConfig.MINUTE_BUCKETS[i]));
        }

        VoiceSelectionFragment fragment = new VoiceSelectionFragment();
        fragment.setArguments(VoiceSelectionFragment.createArguments(
                getString(R.string.zen_mode_duration_voice_prompt)));
        fragment.setListAdapter(
                new VoiceSelectionAdapter(this, R.layout.voice_item_row, states));
        fragment.setOnItemSelectedHandler(new VoiceSelection.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int index, VoiceSelection item) {
                ConditionSelection selection = ((ConditionSelection) item);
                setZenModeConfig(mode, selection.mCondition);
                notifySuccess(getChangeSummary(mode, selection));
                finish();
            }
        });
        showFragment(fragment, "pick_duration_fragment");
    }

    private void showFragment(Fragment fragment, String tag) {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_root, fragment, tag)
                .commit();
    }

    private void setZenModeConfig(int mode, Condition condition) {
        if (condition != null) {
            NotificationManager.from(this).setZenMode(mode, condition.id, TAG);
        } else {
            NotificationManager.from(this).setZenMode(mode, null, TAG);
        }
    }

    /**
     * Produce a summary of the Zen mode change to be read aloud as TTS.
     */
    private CharSequence getChangeSummary(int mode, ConditionSelection duration) {
        int indefinite = -1;
        int byMinute = -1;
        int byHour = -1;

        switch (mode) {
            case Global.ZEN_MODE_ALARMS:
                indefinite = R.string.zen_mode_summary_alarams_only_indefinite;
                byMinute = R.plurals.zen_mode_summary_alarms_only_by_minute;
                byHour = R.plurals.zen_mode_summary_alarms_only_by_hour;
                break;
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                indefinite = R.string.zen_mode_summary_no_interruptions_indefinite;
                byMinute = R.plurals.zen_mode_summary_no_interruptions_by_minute;
                byHour = R.plurals.zen_mode_summary_no_interruptions_by_hour;
                break;
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                indefinite = R.string.zen_mode_summary_priority_indefinitely;
                byMinute = R.plurals.zen_mode_summary_priority_by_minute;
                byHour = R.plurals.zen_mode_summary_priority_by_hour;
                break;
            default:
            case Global.ZEN_MODE_OFF:
                indefinite = R.string.zen_mode_summary_always;
                break;
        };

        if (duration == null || duration.mCondition == null) {
            return getString(indefinite);
        }

        long time = System.currentTimeMillis() + duration.mMinutes * MINUTES_MS;
        String skeleton = DateFormat.is24HourFormat(this, UserHandle.myUserId()) ? "Hm" : "hma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        CharSequence formattedTime = DateFormat.format(pattern, time);
        Resources res = getResources();

        if (duration.mMinutes < 60) {
            return res.getQuantityString(byMinute,
                    duration.mMinutes, duration.mMinutes, formattedTime);
        } else {
            int hours = duration.mMinutes / 60;
            return res.getQuantityString(byHour, hours, hours, formattedTime);
        }
    }

    private ConditionSelection getConditionSelection(int minutes) {
        Condition condition = ZenModeConfig.toTimeCondition(this, minutes, UserHandle.myUserId());
        Resources res = getResources();
        if (minutes <= 0) {
            return null;
        } else if (minutes < 60) {
            String label = res.getQuantityString(R.plurals.zen_mode_duration_minutes_voice_label,
                    minutes, minutes);
            return new ConditionSelection(condition, minutes, label, Integer.toString(minutes));
        } else {
            int hours = minutes / 60;
            String label = res.getQuantityString(R.plurals.zen_mode_duration_hours_voice_label,
                    hours, hours);
            return new ConditionSelection(condition, minutes, label, Integer.toString(hours));
        }
    }

    private static class ConditionSelection extends VoiceSelection {
        Condition mCondition;
        int mMinutes;

        public ConditionSelection(Condition condition, int minutes, CharSequence label,
                CharSequence synonyms) {
            super(label, synonyms);
            mMinutes = minutes;
            mCondition = condition;
        }
    }

    private static class ModeSelection extends VoiceSelection {
        int mMode;

        public ModeSelection(Context context, int mode, int label, int synonyms) {
            super(context.getString(label), context.getString(synonyms));
            mMode = mode;
        }
    }
}
