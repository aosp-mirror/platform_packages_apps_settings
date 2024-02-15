/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ModifierKeysPreferenceController extends BasePreferenceController {

    private static final String KEY_TAG = "modifier_keys_dialog_tag";
    private static final String KEY_RESTORE_PREFERENCE = "modifier_keys_restore";

    private static final String KEY_PREFERENCE_CAPS_LOCK = "modifier_keys_caps_lock";
    private static final String KEY_PREFERENCE_CTRL = "modifier_keys_ctrl";
    private static final String KEY_PREFERENCE_META = "modifier_keys_meta";
    private static final String KEY_PREFERENCE_ALT = "modifier_keys_alt";

    private Fragment mParent;
    private FragmentManager mFragmentManager;
    private final InputManager mIm;
    private PreferenceScreen mScreen;
    private Drawable mDrawable;

    private final List<Integer> mRemappableKeys = new ArrayList<>(
            Arrays.asList(
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
                    KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT,
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
                    KeyEvent.KEYCODE_CAPS_LOCK));

    private final List<Pair<String, Integer>> mKeys = new ArrayList<>(
            Arrays.asList(
                    Pair.create(KEY_PREFERENCE_CTRL, R.string.modifier_keys_ctrl),
                    Pair.create(KEY_PREFERENCE_META, R.string.modifier_keys_meta),
                    Pair.create(KEY_PREFERENCE_ALT, R.string.modifier_keys_alt),
                    Pair.create(KEY_PREFERENCE_CAPS_LOCK, R.string.modifier_keys_caps_lock)
            ));

    private String[] mKeyNames = new String[] {
            mContext.getString(R.string.modifier_keys_ctrl),
            mContext.getString(R.string.modifier_keys_ctrl),
            mContext.getString(R.string.modifier_keys_meta),
            mContext.getString(R.string.modifier_keys_meta),
            mContext.getString(R.string.modifier_keys_alt),
            mContext.getString(R.string.modifier_keys_alt),
            mContext.getString(R.string.modifier_keys_caps_lock)};

    public ModifierKeysPreferenceController(Context context, String key) {
        super(context, key);
        mIm = context.getSystemService(InputManager.class);
        Objects.requireNonNull(mIm, "InputManager service cannot be null");
        KeyboardSettingsFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getKeyboardSettingsFeatureProvider();
        mDrawable = featureProvider.getActionKeyIcon(context);
    }

    public void setFragment(Fragment parent) {
        mParent = parent;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mParent == null) {
            return;
        }
        mScreen = screen;
        refreshUi();
    }

    private void refreshUi() {
        initDefaultKeysName();
        for (Map.Entry<Integer, Integer> entry : mIm.getModifierKeyRemapping().entrySet()) {
            int fromKey = entry.getKey();
            int toKey = entry.getValue();
            int index = mRemappableKeys.indexOf(toKey);

            if (isCtrl(fromKey) && mRemappableKeys.contains(toKey)) {
                setSummaryColor(KEY_PREFERENCE_CTRL, index);
            }

            if (isMeta(fromKey) && mRemappableKeys.contains(toKey)) {
                setSummaryColor(KEY_PREFERENCE_META, index);
            }

            if (isAlt(fromKey) && mRemappableKeys.contains(toKey)) {
                setSummaryColor(KEY_PREFERENCE_ALT, index);
            }

            if (isCapLock(fromKey) && mRemappableKeys.contains(toKey)) {
                setSummaryColor(KEY_PREFERENCE_CAPS_LOCK, index);
            }
        }
    }

    private void initDefaultKeysName() {
        for (Pair<String, Integer> key : mKeys) {
            LayoutPreference layoutPreference = mScreen.findPreference(key.first);
            TextView title = layoutPreference.findViewById(R.id.title);
            TextView summary = layoutPreference.findViewById(R.id.summary);
            title.setText(key.second);
            summary.setText(R.string.modifier_keys_default_summary);

            if (key.first.equals(KEY_PREFERENCE_META) && mDrawable != null) {
                setActionKeyIcon(layoutPreference, mDrawable);
            }
        }
    }

    private static void setActionKeyIcon(LayoutPreference preference, Drawable drawable) {
        TextView leftBracket = preference.findViewById(R.id.modifier_key_left_bracket);
        TextView rightBracket = preference.findViewById(R.id.modifier_key_right_bracket);
        ImageView actionKeyIcon = preference.findViewById(R.id.modifier_key_action_key_icon);
        leftBracket.setText("(");
        rightBracket.setText(")");
        actionKeyIcon.setImageDrawable(drawable);
    }

    private void setSummaryColor(String key, int targetIndex) {
        LayoutPreference layoutPreference = mScreen.findPreference(key);
        TextView summary = layoutPreference.findViewById(R.id.summary);
        summary.setText(changeSummaryColor(mKeyNames[targetIndex]));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_RESTORE_PREFERENCE)) {
            return false;
        }
        showModifierKeysDialog(preference);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private void showModifierKeysDialog(Preference preference) {
        mFragmentManager = mParent.getFragmentManager();
        ModifierKeysPickerDialogFragment fragment = new ModifierKeysPickerDialogFragment();
        fragment.setTargetFragment(mParent, 0);
        Bundle bundle = new Bundle();
        TextView title = ((LayoutPreference) preference).findViewById(R.id.title);
        TextView summary = ((LayoutPreference) preference).findViewById(R.id.summary);
        bundle.putString(
                ModifierKeysPickerDialogFragment.DEFAULT_KEY,
                title.getText().toString());
        bundle.putString(
                ModifierKeysPickerDialogFragment.SELECTION_KEY,
                summary.getText().toString());
        fragment.setArguments(bundle);
        fragment.show(mFragmentManager, KEY_TAG);
    }

    private Spannable changeSummaryColor(String summary) {
        Spannable spannableSummary = new SpannableString(summary);
        spannableSummary.setSpan(
                new ForegroundColorSpan(getColorOfMaterialColorPrimary()),
                0, spannableSummary.length(), 0);
        return spannableSummary;
    }

    private int getColorOfMaterialColorPrimary() {
        return Utils.getColorAttrDefaultColor(
                mContext, com.android.internal.R.attr.materialColorPrimary);
    }

    private static boolean isCtrl(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT;
    }

    private static boolean isMeta(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT;
    }

    private static boolean isAlt(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT;
    }

    private static boolean isCapLock(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_CAPS_LOCK;
    }
}
