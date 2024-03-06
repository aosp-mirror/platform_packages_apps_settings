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

package com.android.settings.inputmethod;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.textservice.SpellCheckerInfo;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.CustomListPreference;
import com.android.settings.R;

/**
 * Spell checker service preference.
 *
 * This preference represents a spell checker service. It is used for two purposes. 1) A radio
 * button on the left side is used to choose the current spell checker service. 2) A settings
 * icon on the right side is used to invoke the setting activity of the spell checker service.
 */
class SpellCheckerPreference extends CustomListPreference {

    private final SpellCheckerInfo[] mScis;
    @VisibleForTesting
    Intent mIntent;

    public SpellCheckerPreference(final Context context, final SpellCheckerInfo[] scis) {
        super(context, null);
        mScis = scis;
        setLayoutResource(
                com.android.settingslib.widget.preference.twotarget.R.layout.preference_two_target);

        setWidgetLayoutResource(R.layout.preference_widget_gear);
        if (scis == null) {
            return;
        }
        CharSequence[] labels = new CharSequence[scis.length];
        CharSequence[] values = new CharSequence[scis.length];
        for (int i = 0 ; i < scis.length; i++) {
            labels[i] = scis[i].loadLabel(context.getPackageManager());
            // Use values as indexing since ListPreference doesn't support generic objects.
            values[i] = String.valueOf(i);
        }
        setEntries(labels);
        setEntryValues(values);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setTitle(R.string.choose_spell_checker);
        builder.setSingleChoiceItems(getEntries(), findIndexOfValue(getValue()), listener);
    }

    public void setSelected(SpellCheckerInfo currentSci) {
        if (currentSci == null) {
            setValue(null);
            return;
        }
        for (int i = 0; i < mScis.length; i++) {
            if (mScis[i].getId().equals(currentSci.getId())) {
                setValueIndex(i);
                return;
            }
        }
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        int index = value != null ? Integer.parseInt(value) : -1;
        if (index == -1) {
            mIntent = null;
            return;
        }
        SpellCheckerInfo sci = mScis[index];
        final String settingsActivity = sci.getSettingsActivity();
        if (TextUtils.isEmpty(settingsActivity)) {
            mIntent = null;
        } else {
            mIntent = new Intent(Intent.ACTION_MAIN);
            mIntent.setClassName(sci.getPackageName(), settingsActivity);
        }
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        newValue = newValue != null ? mScis[Integer.parseInt((String) newValue)] : null;
        return super.callChangeListener(newValue);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final View divider = view.findViewById(
                com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider);
        final View widgetFrame = view.findViewById(android.R.id.widget_frame);
        if (divider != null) {
            divider.setVisibility(mIntent != null ? View.VISIBLE : View.GONE);
        }
        if (widgetFrame != null) {
            widgetFrame.setVisibility(mIntent != null ? View.VISIBLE : View.GONE);
        }

        View settingsButton = view.findViewById(R.id.settings_button);
        if (settingsButton != null) {
            settingsButton.setVisibility(mIntent != null ? View.VISIBLE : View.INVISIBLE);
            settingsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSettingsButtonClicked();
                }
            });
        }
    }

    private void onSettingsButtonClicked() {
        final Context context = getContext();
        try {
            final Intent intent = mIntent;
            if (intent != null) {
                // Invoke a settings activity of an spell checker.
                context.startActivity(intent);
            }
        } catch (final ActivityNotFoundException e) {
        }
    }
}
