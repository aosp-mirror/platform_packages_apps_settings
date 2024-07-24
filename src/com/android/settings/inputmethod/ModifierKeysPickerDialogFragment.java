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

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifierKeysPickerDialogFragment extends DialogFragment {

    static final String DEFAULT_KEY = "default_key";
    static final String SELECTION_KEY = "delection_key";

    private Preference mPreference;
    private String mKeyDefaultName;
    private String mKeyFocus;
    private Activity mActivity;
    private KeyboardSettingsFeatureProvider mFeatureProvider;
    private Drawable mActionKeyDrawable;
    private TextView mLeftBracket;
    private TextView mRightBracket;
    private ImageView mActionKeyIcon;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private List<int[]> mRemappableKeyList =
            new ArrayList<>(Arrays.asList(
                    new int[]{KeyEvent.KEYCODE_CAPS_LOCK},
                    new int[]{KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT},
                    new int[]{KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT},
                    new int[]{KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT}));

    private Map<String, int[]> mRemappableKeyMap = new HashMap<>();

    public ModifierKeysPickerDialogFragment() {}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(SELECTION_KEY, mKeyFocus);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        mActivity = getActivity();
        FeatureFactory featureFactory = FeatureFactory.getFeatureFactory();
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
        mFeatureProvider = featureFactory.getKeyboardSettingsFeatureProvider();
        InputManager inputManager = mActivity.getSystemService(InputManager.class);
        mKeyDefaultName = getArguments().getString(DEFAULT_KEY);
        mKeyFocus = getArguments().getString(SELECTION_KEY);
        if (savedInstanceState != null) {
            mKeyFocus = savedInstanceState.getString(SELECTION_KEY);
        }
        List<String> modifierKeys = new ArrayList<String>(Arrays.asList(
                mActivity.getString(R.string.modifier_keys_caps_lock),
                mActivity.getString(R.string.modifier_keys_ctrl),
                mActivity.getString(R.string.modifier_keys_meta),
                mActivity.getString(R.string.modifier_keys_alt)));
        for (int i = 0; i < modifierKeys.size(); i++) {
            mRemappableKeyMap.put(modifierKeys.get(i), mRemappableKeyList.get(i));
        }
        Drawable drawable = mFeatureProvider.getActionKeyIcon(mActivity);
        if (drawable != null) {
            mActionKeyDrawable = DrawableCompat.wrap(drawable);
        }

        View dialoglayout  =
                LayoutInflater.from(mActivity).inflate(R.layout.modifier_key_picker_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
        dialogBuilder.setView(dialoglayout);

        TextView summary = dialoglayout.findViewById(R.id.modifier_key_picker_summary);
        CharSequence summaryText = mActivity.getString(
                R.string.modifier_keys_picker_summary, mKeyDefaultName);
        summary.setText(summaryText);

        ModifierKeyAdapter adapter = new ModifierKeyAdapter(modifierKeys);
        ListView listView = dialoglayout.findViewById(R.id.modifier_key_picker);
        listView.setAdapter(adapter);
        setInitialFocusItem(modifierKeys, adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                adapter.setCurrentItem(i);
                adapter.notifyDataSetChanged();
            }
        });

        AlertDialog modifierKeyDialog = dialogBuilder.create();
        Button doneButton = dialoglayout.findViewById(R.id.modifier_key_done_button);
        doneButton.setOnClickListener(v -> {
            String selectedItem = modifierKeys.get(adapter.getCurrentItem());
            Spannable itemSummary;
            logMetricsForRemapping(selectedItem);
            if (selectedItem.equals(mKeyDefaultName)) {
                itemSummary = new SpannableString(
                        mActivity.getString(R.string.modifier_keys_default_summary));
                itemSummary.setSpan(
                        new ForegroundColorSpan(getColorOfTextColorSecondary()),
                        0, itemSummary.length(), 0);
                // Set keys to default.
                int[] keys = mRemappableKeyMap.get(mKeyDefaultName);
                for (int i = 0; i < keys.length; i++) {
                    inputManager.remapModifierKey(keys[i], keys[i]);
                }
            } else {
                itemSummary = new SpannableString(selectedItem);
                itemSummary.setSpan(
                        new ForegroundColorSpan(getColorOfMaterialColorPrimary()),
                        0, itemSummary.length(), 0);
                int[] fromKeys = mRemappableKeyMap.get(mKeyDefaultName);
                int[] toKeys = mRemappableKeyMap.get(selectedItem);
                // CAPS_LOCK only one key, so always choose the left key for remapping.
                if (isKeyCapsLock(mActivity, mKeyDefaultName)) {
                    inputManager.remapModifierKey(fromKeys[0], toKeys[0]);
                }
                // Remap KEY_LEFT and KEY_RIGHT to CAPS_LOCK.
                if (!isKeyCapsLock(mActivity, mKeyDefaultName)
                        && isKeyCapsLock(mActivity, selectedItem)) {
                    inputManager.remapModifierKey(fromKeys[0], toKeys[0]);
                    inputManager.remapModifierKey(fromKeys[1], toKeys[0]);
                }
                // Auto handle left and right keys remapping.
                if (!isKeyCapsLock(mActivity, mKeyDefaultName)
                        && !isKeyCapsLock(mActivity, selectedItem)) {
                    inputManager.remapModifierKey(fromKeys[0], toKeys[0]);
                    inputManager.remapModifierKey(fromKeys[1], toKeys[1]);
                }
            }
            dismiss();
            mActivity.recreate();
        });

        Button cancelButton = dialoglayout.findViewById(R.id.modifier_key_cancel_button);
        cancelButton.setOnClickListener(v -> {
            dismiss();
        });

        final Window window = modifierKeyDialog.getWindow();
        window.setType(TYPE_SYSTEM_DIALOG);

        return modifierKeyDialog;
    }

    private void logMetricsForRemapping(String selectedItem) {
        if (mKeyDefaultName.equals("Caps lock")) {
            mMetricsFeatureProvider.action(
                    mActivity, SettingsEnums.ACTION_FROM_CAPS_LOCK_TO, selectedItem);
        }

        if (mKeyDefaultName.equals("Ctrl")) {
            mMetricsFeatureProvider.action(
                    mActivity, SettingsEnums.ACTION_FROM_CTRL_TO, selectedItem);
        }

        if (mKeyDefaultName.equals("Action key")) {
            mMetricsFeatureProvider.action(
                    mActivity, SettingsEnums.ACTION_FROM_ACTION_KEY_TO, selectedItem);
        }

        if (mKeyDefaultName.equals("Alt")) {
            mMetricsFeatureProvider.action(
                    mActivity, SettingsEnums.ACTION_FROM_ALT_TO, selectedItem);
        }
    }

    private void setInitialFocusItem(
            List<String> modifierKeys, ModifierKeyAdapter adapter) {
        if (modifierKeys.indexOf(mKeyFocus) == -1) {
            adapter.setCurrentItem(modifierKeys.indexOf(mKeyDefaultName));
        } else {
            adapter.setCurrentItem(modifierKeys.indexOf(mKeyFocus));
        }
        adapter.notifyDataSetChanged();
    }

    private static boolean isKeyCapsLock(Context context, String key) {
        return key.equals(context.getString(R.string.modifier_keys_caps_lock));
    }

    class ModifierKeyAdapter extends BaseAdapter {
        private int mCurrentItem = 0;
        private List<String> mList;

        ModifierKeyAdapter(List<String> list) {
            this.mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int i) {
            return mList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(mActivity).inflate(R.layout.modifier_key_item, null);
            }
            TextView textView = view.findViewById(R.id.modifier_key_text);
            ImageView checkIcon = view.findViewById(R.id.modifier_key_check_icon);
            textView.setText(mList.get(i));
            if (mCurrentItem == i) {
                mKeyFocus = mList.get(i);
                textView.setTextColor(getColorOfMaterialColorPrimary());
                checkIcon.setImageAlpha(255);
                view.setBackground(
                        mActivity.getDrawable(R.drawable.modifier_key_lisetview_background));
                if (mActionKeyDrawable != null && i == 2) {
                    setActionKeyIcon(view);
                    setActionKeyColor(getColorOfMaterialColorPrimary());
                }
            } else {
                textView.setTextColor(getColorOfTextColorPrimary());
                checkIcon.setImageAlpha(0);
                view.setBackground(null);
                if (mActionKeyDrawable != null && i == 2) {
                    setActionKeyIcon(view);
                    setActionKeyColor(getColorOfTextColorPrimary());
                }
            }
            return view;
        }

        public void setCurrentItem(int currentItem) {
            this.mCurrentItem = currentItem;
        }

        public int getCurrentItem() {
            return this.mCurrentItem;
        }
    }

    private void setActionKeyIcon(View view) {
        mLeftBracket = view.findViewById(R.id.modifier_key_left_bracket);
        mRightBracket = view.findViewById(R.id.modifier_key_right_bracket);
        mActionKeyIcon = view.findViewById(R.id.modifier_key_action_key_icon);
        mLeftBracket.setText("(");
        mRightBracket.setText(")");
        mActionKeyIcon.setImageDrawable(mActionKeyDrawable);
    }

    private void setActionKeyColor(int color) {
        mLeftBracket.setTextColor(color);
        mRightBracket.setTextColor(color);
        DrawableCompat.setTint(mActionKeyDrawable, color);
    }

    private int getColorOfTextColorPrimary() {
        return Utils.getColorAttrDefaultColor(mActivity, android.R.attr.textColorPrimary);
    }

    private int getColorOfTextColorSecondary() {
        return Utils.getColorAttrDefaultColor(mActivity, android.R.attr.textColorSecondary);
    }

    private int getColorOfMaterialColorPrimary() {
        return Utils.getColorAttrDefaultColor(
                mActivity, com.android.internal.R.attr.materialColorPrimary);
    }
}
