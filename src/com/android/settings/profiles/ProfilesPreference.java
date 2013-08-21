/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ProfilesPreference extends CheckBoxPreference {
    private static final String TAG = ProfilesPreference.class.getSimpleName();
    private static final float DISABLED_ALPHA = 0.4f;
    private final SettingsPreferenceFragment mFragment;
    private final Bundle mSettingsBundle;

    // constant value that can be used to check return code from sub activity.
    private static final int PROFILE_DETAILS = 1;

    private ImageView mProfilesSettingsButton;
    private TextView mTitleText;
    private TextView mSummaryText;
    private View mProfilesPref;

    private final OnClickListener mPrefOnclickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (!isEnabled() || isChecked()) {
                return;
            }
            setChecked(true);
            callChangeListener(getKey());
        }
    };

    public ProfilesPreference(SettingsPreferenceFragment fragment, Bundle settingsBundle) {
        super(fragment.getActivity(), null, R.style.ProfilesPreferenceStyle);
        setLayoutResource(R.layout.preference_profiles);
        setWidgetLayoutResource(R.layout.preference_profiles_widget);
        mFragment = fragment;
        mSettingsBundle = settingsBundle;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mProfilesPref = view.findViewById(R.id.profiles_pref);
        mProfilesPref.setOnClickListener(mPrefOnclickListener);
        mProfilesSettingsButton = (ImageView)view.findViewById(R.id.profiles_settings);
        mTitleText = (TextView)view.findViewById(android.R.id.title);
        mSummaryText = (TextView)view.findViewById(android.R.id.summary);

        if (mSettingsBundle != null) {
            mProfilesSettingsButton.setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            try {
                                startProfileConfigActivity();
                            } catch (ActivityNotFoundException e) {
                                // If the settings activity does not exist, we can just
                                // do nothing...
                            }
                        }
                    });
        }
        if (mSettingsBundle == null) {
            mProfilesSettingsButton.setVisibility(View.GONE);
        } else {
            updatePreferenceViews();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            updatePreferenceViews();
        } else {
            disablePreferenceViews();
        }
    }

    private void disablePreferenceViews() {
        if (mProfilesSettingsButton != null) {
            mProfilesSettingsButton.setEnabled(false);
            mProfilesSettingsButton.setAlpha(DISABLED_ALPHA);
        }
        if (mProfilesPref != null) {
            mProfilesPref.setEnabled(false);
            mProfilesPref.setBackgroundColor(0);
        }
    }

    private void updatePreferenceViews() {
        final boolean checked = isChecked();
        if (mProfilesSettingsButton != null) {
            mProfilesSettingsButton.setEnabled(true);
            mProfilesSettingsButton.setClickable(true);
            mProfilesSettingsButton.setFocusable(true);
        }
        if (mTitleText != null) {
            mTitleText.setEnabled(true);
        }
        if (mSummaryText != null) {
            mSummaryText.setEnabled(checked);
        }
        if (mProfilesPref != null) {
            mProfilesPref.setEnabled(true);
            mProfilesPref.setLongClickable(checked);
            final boolean enabled = isEnabled();
            mProfilesPref.setOnClickListener(enabled ? mPrefOnclickListener : null);
            if (!enabled) {
                mProfilesPref.setBackgroundColor(0);
            }
        }
    }

    // utility method used to start sub activity
    private void startProfileConfigActivity() {
        PreferenceActivity pa = (PreferenceActivity) mFragment.getActivity();
        pa.startPreferencePanel(ProfileConfig.class.getName(), mSettingsBundle,
                R.string.profile_profile_manage, null, mFragment, PROFILE_DETAILS);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
    }
}
