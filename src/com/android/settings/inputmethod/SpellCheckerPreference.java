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
import android.content.Intent;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.textservice.SpellCheckerInfo;
import android.widget.RadioButton;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * Spell checker service preference.
 *
 * This preference represents a spell checker service. It is used for two purposes. 1) A radio
 * button on the left side is used to choose the current spell checker service. 2) A settings
 * icon on the right side is used to invoke the setting activity of the spell checker service.
 */
class SpellCheckerPreference extends Preference implements OnClickListener {
    interface OnRadioButtonPreferenceListener {
        /**
         * Called when this preference needs to be saved its state.
         *
         * Note that this preference is non-persistent and needs explicitly to be saved its state.
         * Because changing one IME state may change other IMEs' state, this is a place to update
         * other IMEs' state as well.
         *
         * @param pref This preference.
         */
        public void onRadioButtonClicked(SpellCheckerPreference pref);
    }

    private final SpellCheckerInfo mSci;
    private final OnRadioButtonPreferenceListener mOnRadioButtonListener;

    private RadioButton mRadioButton;
    private View mPrefLeftButton;
    private View mSettingsButton;
    private boolean mSelected;

    public SpellCheckerPreference(final Context context, final SpellCheckerInfo sci,
            final OnRadioButtonPreferenceListener onRadioButtonListener) {
        super(context, null, 0);
        setPersistent(false);
        setLayoutResource(R.layout.preference_spellchecker);
        setWidgetLayoutResource(R.layout.preference_spellchecker_widget);
        mSci = sci;
        mOnRadioButtonListener = onRadioButtonListener;
        setKey(sci.getId());
        setTitle(sci.loadLabel(context.getPackageManager()));
        final String settingsActivity = mSci.getSettingsActivity();
        if (TextUtils.isEmpty(settingsActivity)) {
            setIntent(null);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(mSci.getPackageName(), settingsActivity);
            setIntent(intent);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mRadioButton = (RadioButton)view.findViewById(R.id.pref_radio);
        mPrefLeftButton = view.findViewById(R.id.pref_left_button);
        mPrefLeftButton.setOnClickListener(this);
        mSettingsButton = view.findViewById(R.id.pref_right_button);
        mSettingsButton.setOnClickListener(this);
        updateSelectedState(mSelected);
    }

    @Override
    public void onClick(final View v) {
        if (v == mPrefLeftButton) {
            mOnRadioButtonListener.onRadioButtonClicked(this);
            return;
        }
        if (v == mSettingsButton) {
            onSettingsButtonClicked();
            return;
        }
    }

    private void onSettingsButtonClicked() {
        final Context context = getContext();
        try {
            final Intent intent = getIntent();
            if (intent != null) {
                // Invoke a settings activity of an spell checker.
                context.startActivity(intent);
            }
        } catch (final ActivityNotFoundException e) {
            final String message = context.getString(R.string.failed_to_open_app_settings_toast,
                    mSci.loadLabel(context.getPackageManager()));
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    public SpellCheckerInfo getSpellCheckerInfo() {
        return mSci;
    }

    public void setSelected(final boolean selected) {
        mSelected = selected;
        updateSelectedState(selected);
    }

    private void updateSelectedState(final boolean selected) {
        if (mRadioButton != null) {
            mRadioButton.setChecked(selected);
            enableSettingsButton(isEnabled() && selected);
        }
    }

    private void enableSettingsButton(final boolean enabled) {
        if (mSettingsButton == null) {
            return;
        }
        if (getIntent() == null) {
            mSettingsButton.setVisibility(View.GONE);
        } else {
            mSettingsButton.setEnabled(enabled);
            mSettingsButton.setClickable(enabled);
            mSettingsButton.setFocusable(enabled);
            if (!enabled) {
                mSettingsButton.setAlpha(Utils.DISABLED_ALPHA);
            }
        }
    }
}
