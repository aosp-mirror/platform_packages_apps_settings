/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.content.Intent;

import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.textservice.SpellCheckerInfo;
import android.widget.ImageView;
import android.widget.TextView;

public class SingleSpellCheckerPreference extends Preference {
    private static final float DISABLED_ALPHA = 0.4f;

    private final SpellCheckerInfo mSpellCheckerInfo;

    private SettingsPreferenceFragment mFragment;
    private TextView mTitleText;
    private TextView mSummaryText;
    private View mPrefAll;
    private View mPrefLeftButton;
    private ImageView mSetingsButton;
    private Intent mSettingsIntent;
    private boolean mSelected;

    public SingleSpellCheckerPreference(SettingsPreferenceFragment fragment, Intent settingsIntent,
            SpellCheckerInfo sci) {
        super(fragment.getActivity(), null, 0);
        setLayoutResource(R.layout.preference_spellchecker);
        mSpellCheckerInfo = sci;
        mSelected = false;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mPrefAll = view.findViewById(R.id.pref_all);
        mPrefLeftButton = view.findViewById(R.id.pref_left_button);
        mPrefLeftButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        onLeftButtonClicked(arg0);
                    }
                });
        mSetingsButton = (ImageView)view.findViewById(R.id.pref_right_button);
        mTitleText = (TextView)view.findViewById(android.R.id.title);
        mSummaryText = (TextView)view.findViewById(android.R.id.summary);
        mSetingsButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        onSettingsButtonClicked(arg0);
                    }
                });
        updateSelectedState(mSelected);
    }

    private void onLeftButtonClicked(View arg0) {
        final OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null) {
            listener.onPreferenceClick(this);
        }
    }

    public SpellCheckerInfo getSpellCheckerInfo() {
        return mSpellCheckerInfo;
    }

    public void updateSelectedState(boolean selected) {
        if (mPrefAll != null) {
            if (selected) {
                // TODO: Use a color defined by the design guideline.
                mPrefAll.setBackgroundColor(0x88006666);
            } else {
                mPrefAll.setBackgroundColor(0);
            }
            enableSettingsButton(selected);
        }
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    protected void onSettingsButtonClicked(View arg0) {
        if (mFragment != null && mSettingsIntent != null) {
            mFragment.startActivity(mSettingsIntent);
        }
    }

    private void enableSettingsButton(boolean enabled) {
        if (mSetingsButton != null) {
            if (mSettingsIntent == null) {
                mSetingsButton.setVisibility(View.GONE);
            } else {
                mSetingsButton.setEnabled(enabled);
                mSetingsButton.setClickable(enabled);
                mSetingsButton.setFocusable(enabled);
                if (!enabled) {
                    mSetingsButton.setAlpha(DISABLED_ALPHA);
                }
            }
        }
    }
}
