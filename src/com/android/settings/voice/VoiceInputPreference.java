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

package com.android.settings.voice;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.RadioButton;


import com.android.settings.R;
import com.android.settings.Utils;

public final class VoiceInputPreference extends Preference {

    private static final String TAG = "VoiceInputPreference";

    private final CharSequence mLabel;

    private final CharSequence mAppLabel;

    private final CharSequence mAlertText;

    private final ComponentName mSettingsComponent;

    /**
     * The shared radio button state, which button is checked etc.
     */
    private final RadioButtonGroupState mSharedState;

    /**
     * When true, the change callbacks on the radio button will not
     * fire.
     */
    private volatile boolean mPreventRadioButtonCallbacks;

    private View mSettingsIcon;
    private RadioButton mRadioButton;

    private final CompoundButton.OnCheckedChangeListener mRadioChangeListener =
        new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onRadioButtonClicked(buttonView, isChecked);
            }
        };

    public VoiceInputPreference(Context context, VoiceInputHelper.BaseInfo info,
            CharSequence summary, CharSequence alertText, RadioButtonGroupState state) {
        super(context);
        setLayoutResource(R.layout.preference_tts_engine);

        mSharedState = state;
        mLabel = info.label;
        mAppLabel = info.appLabel;
        mAlertText = alertText;
        mSettingsComponent = info.settings;
        mPreventRadioButtonCallbacks = false;

        setKey(info.key);
        setTitle(info.label);
        setSummary(summary);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        if (mSharedState == null) {
            throw new IllegalStateException("Call to getView() before a call to" +
                    "setSharedState()");
        }

        View view = super.getView(convertView, parent);
        final RadioButton rb = (RadioButton) view.findViewById(R.id.tts_engine_radiobutton);
        rb.setOnCheckedChangeListener(mRadioChangeListener);

        boolean isChecked = getKey().equals(mSharedState.getCurrentKey());
        if (isChecked) {
            mSharedState.setCurrentChecked(rb);
        }

        mPreventRadioButtonCallbacks = true;
        rb.setChecked(isChecked);
        mPreventRadioButtonCallbacks = false;

        mRadioButton = rb;

        View textLayout = view.findViewById(R.id.tts_engine_pref_text);
        textLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!rb.isChecked()) {
                    onRadioButtonClicked(rb, true);
                }
            }
        });

        mSettingsIcon = view.findViewById(R.id.tts_engine_settings);
        mSettingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(mSettingsComponent);
                getContext().startActivity(new Intent(intent));
            }
        });
        updateCheckedState(isChecked);

        return view;
    }

    private boolean shouldDisplayAlert() {
        return mAlertText != null;
    }

    private void displayAlert(
            final DialogInterface.OnClickListener positiveOnClickListener,
            final DialogInterface.OnClickListener negativeOnClickListener) {
        Log.i(TAG, "Displaying data alert for :" + getKey());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String msg = String.format(getContext().getResources().getConfiguration().locale,
                mAlertText.toString(), mAppLabel);
        builder.setTitle(android.R.string.dialog_alert_title)
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, positiveOnClickListener)
                .setNegativeButton(android.R.string.cancel, negativeOnClickListener)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override public void onCancel(DialogInterface dialog) {
                        negativeOnClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void doClick() {
        mRadioButton.performClick();
    }

    void updateCheckedState(boolean isChecked) {
        if (mSettingsComponent != null) {
            mSettingsIcon.setVisibility(View.VISIBLE);
            if (isChecked) {
                mSettingsIcon.setEnabled(true);
                mSettingsIcon.setAlpha(1);
            } else {
                mSettingsIcon.setEnabled(false);
                mSettingsIcon.setAlpha(Utils.DISABLED_ALPHA);
            }
        } else {
            mSettingsIcon.setVisibility(View.GONE);
        }
    }

    void onRadioButtonClicked(final CompoundButton buttonView, boolean isChecked) {
        if (mPreventRadioButtonCallbacks) {
            return;
        }
        if (mSharedState.getCurrentChecked() == buttonView) {
            updateCheckedState(isChecked);
            return;
        }

        if (isChecked) {
            // Should we alert user? if that's true, delay making engine current one.
            if (shouldDisplayAlert()) {
                displayAlert(new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                     makeCurrentChecked(buttonView);
                                 }
                             }, new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                     // Undo the click.
                                     buttonView.setChecked(false);
                                 }
                             }
                );
            } else {
                // Privileged engine, set it current
                makeCurrentChecked(buttonView);
            }
        } else {
            updateCheckedState(isChecked);
        }
    }

    void makeCurrentChecked(Checkable current) {
        if (mSharedState.getCurrentChecked() != null) {
            mSharedState.getCurrentChecked().setChecked(false);
        }
        mSharedState.setCurrentChecked(current);
        mSharedState.setCurrentKey(getKey());
        updateCheckedState(true);
        callChangeListener(mSharedState.getCurrentKey());
        current.setChecked(true);
    }

    /**
     * Holds all state that is common to this group of radio buttons, such
     * as the currently selected key and the currently checked compound button.
     * (which corresponds to this key).
     */
    public interface RadioButtonGroupState {
        String getCurrentKey();
        Checkable getCurrentChecked();

        void setCurrentKey(String key);
        void setCurrentChecked(Checkable current);
    }
}
