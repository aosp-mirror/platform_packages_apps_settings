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
import com.android.settings.Utils;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import android.view.textservice.TextServicesManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class SingleSpellCheckerPreference extends Preference {
    private static final String TAG = SingleSpellCheckerPreference.class.getSimpleName();
    private static final boolean DBG = false;

    private final SpellCheckerInfo mSpellCheckerInfo;

    private final SpellCheckersSettings mFragment;
    private final Resources mRes;
    private final TextServicesManager mTsm;
    private AlertDialog mDialog = null;
    private TextView mTitleText;
    private TextView mSummaryText;
    private View mPrefAll;
    private RadioButton mRadioButton;
    private View mPrefLeftButton;
    private View mSettingsButton;
    private ImageView mSubtypeButton;
    private Intent mSettingsIntent;
    private boolean mSelected;

    public SingleSpellCheckerPreference(SpellCheckersSettings fragment, Intent settingsIntent,
            SpellCheckerInfo sci, TextServicesManager tsm) {
        super(fragment.getActivity(), null, 0);
        mFragment = fragment;
        mRes = fragment.getActivity().getResources();
        mTsm = tsm;
        setLayoutResource(R.layout.preference_spellchecker);
        mSpellCheckerInfo = sci;
        mSelected = false;
        final String settingsActivity = mSpellCheckerInfo.getSettingsActivity();
        if (!TextUtils.isEmpty(settingsActivity)) {
            mSettingsIntent = new Intent(Intent.ACTION_MAIN);
            mSettingsIntent.setClassName(mSpellCheckerInfo.getPackageName(), settingsActivity);
        } else {
            mSettingsIntent = null;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mPrefAll = view.findViewById(R.id.pref_all);
        mRadioButton = (RadioButton)view.findViewById(R.id.pref_radio);
        mPrefLeftButton = view.findViewById(R.id.pref_left_button);
        mPrefLeftButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        onLeftButtonClicked(arg0);
                    }
                });
        mTitleText = (TextView)view.findViewById(android.R.id.title);
        mSummaryText = (TextView)view.findViewById(android.R.id.summary);
        mSubtypeButton = (ImageView)view.findViewById(R.id.pref_right_button2);
        mSubtypeButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        onSubtypeButtonClicked(arg0);
                    }
                });
        mSettingsButton = view.findViewById(R.id.pref_right_button1);
        mSettingsButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        onSettingsButtonClicked(arg0);
                    }
                });
        updateSelectedState(mSelected);
    }

    private void onLeftButtonClicked(View arg0) {
        mFragment.onPreferenceClick(this);
    }

    public SpellCheckerInfo getSpellCheckerInfo() {
        return mSpellCheckerInfo;
    }

    private void updateSelectedState(boolean selected) {
        if (mPrefAll != null) {
            mRadioButton.setChecked(selected);
            enableButtons(selected);
        }
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
        updateSelectedState(selected);
    }

    private void onSubtypeButtonClicked(View arg0) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(mFragment.getActivity());
        builder.setTitle(R.string.phone_language);
        final int size = mSpellCheckerInfo.getSubtypeCount();
        final CharSequence[] items = new CharSequence[size + 1];
        items[0] = mRes.getString(R.string.use_system_language_to_select_input_method_subtypes);
        for (int i = 0; i < size; ++i) {
            final SpellCheckerSubtype subtype = mSpellCheckerInfo.getSubtypeAt(i);
            final CharSequence label = subtype.getDisplayName(
                    mFragment.getActivity(), mSpellCheckerInfo.getPackageName(),
                    mSpellCheckerInfo.getServiceInfo().applicationInfo);
            items[i + 1] = label;
        }
        // default: "Use system language"
        int checkedItem = 0;
        // Allow no implicitly selected subtypes
        final SpellCheckerSubtype currentScs = mTsm.getCurrentSpellCheckerSubtype(false);
        if (currentScs != null) {
            for (int i = 0; i < size; ++i) {
                if (mSpellCheckerInfo.getSubtypeAt(i).equals(currentScs)) {
                    checkedItem = i + 1;
                    break;
                }
            }
        }
        builder.setSingleChoiceItems(items, checkedItem, new AlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mTsm.setSpellCheckerSubtype(null);
                } else {
                    mTsm.setSpellCheckerSubtype(mSpellCheckerInfo.getSubtypeAt(which - 1));
                }
                if (DBG) {
                    final SpellCheckerSubtype subtype = mTsm.getCurrentSpellCheckerSubtype(true);
                    Log.d(TAG, "Current spell check locale is "
                            + subtype == null ? "null" : subtype.getLocale());
                }
                dialog.dismiss();
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void onSettingsButtonClicked(View arg0) {
        if (mFragment != null && mSettingsIntent != null) {
            try {
                mFragment.startActivity(mSettingsIntent);
            } catch (ActivityNotFoundException e) {
                final String msg = mFragment.getString(R.string.failed_to_open_app_settings_toast,
                        mSpellCheckerInfo.loadLabel(mFragment.getActivity().getPackageManager()));
                Toast.makeText(mFragment.getActivity(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void enableButtons(boolean enabled) {
        if (mSettingsButton != null) {
            if (mSettingsIntent == null) {
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
        if (mSubtypeButton != null) {
            if (mSpellCheckerInfo.getSubtypeCount() <= 0) {
                mSubtypeButton.setVisibility(View.GONE);
            } else {
                mSubtypeButton.setEnabled(enabled);
                mSubtypeButton.setClickable(enabled);
                mSubtypeButton.setFocusable(enabled);
                if (!enabled) {
                    mSubtypeButton.setAlpha(Utils.DISABLED_ALPHA);
                }
            }
        }
    }
}
