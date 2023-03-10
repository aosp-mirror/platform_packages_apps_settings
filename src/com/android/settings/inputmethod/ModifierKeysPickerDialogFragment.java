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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifierKeysPickerDialogFragment extends DialogFragment {

    private Preference mPreference;
    private String mKeyDefaultName;
    private String mKeyFocus;
    private Context mContext;
    private InputManager mIm;

    private List<int[]> mRemappableKeyList =
            new ArrayList<>(Arrays.asList(
                    new int[]{KeyEvent.KEYCODE_CAPS_LOCK},
                    new int[]{KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT},
                    new int[]{KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT},
                    new int[]{KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT}));

    private Map<String, int[]> mRemappableKeyMap = new HashMap<>();

    public ModifierKeysPickerDialogFragment() {
    }

    public ModifierKeysPickerDialogFragment(Preference preference, InputManager inputManager) {
        mPreference = preference;
        mKeyDefaultName = preference.getTitle().toString();
        mKeyFocus = preference.getSummary().toString();
        mIm = inputManager;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        mContext = getActivity();
        List<String> modifierKeys = new ArrayList<String>(Arrays.asList(
                mContext.getString(R.string.modifier_keys_caps_lock),
                mContext.getString(R.string.modifier_keys_ctrl),
                mContext.getString(R.string.modifier_keys_meta),
                mContext.getString(R.string.modifier_keys_alt)));
        for (int i = 0; i < modifierKeys.size(); i++) {
            mRemappableKeyMap.put(modifierKeys.get(i), mRemappableKeyList.get(i));
        }

        View dialoglayout  =
                LayoutInflater.from(mContext).inflate(R.layout.modifier_key_picker_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setView(dialoglayout);

        TextView summary = dialoglayout.findViewById(R.id.modifier_key_picker_summary);
        CharSequence summaryText = mContext.getString(
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
            if (selectedItem.equals(mKeyDefaultName)) {
                itemSummary = new SpannableString(
                        mContext.getString(R.string.modifier_keys_default_summary));
                itemSummary.setSpan(
                        new ForegroundColorSpan(getColorOfTextColorSecondary()),
                        0, itemSummary.length(), 0);
                // Set keys to default.
                int[] keys = mRemappableKeyMap.get(mKeyDefaultName);
                for (int i = 0; i < keys.length; i++) {
                    mIm.remapModifierKey(keys[i], keys[i]);
                }
            } else {
                itemSummary = new SpannableString(selectedItem);
                itemSummary.setSpan(
                        new ForegroundColorSpan(getColorOfColorAccentPrimaryVariant()),
                        0, itemSummary.length(), 0);
                int[] fromKeys = mRemappableKeyMap.get(mKeyDefaultName);
                int[] toKeys = mRemappableKeyMap.get(selectedItem);
                // CAPS_LOCK only one key, so always choose the left key for remapping.
                if (isKeyCapsLock(mContext, mKeyDefaultName)) {
                    mIm.remapModifierKey(fromKeys[0], toKeys[0]);
                }
                // Remap KEY_LEFT and KEY_RIGHT to CAPS_LOCK.
                if (!isKeyCapsLock(mContext, mKeyDefaultName)
                        && isKeyCapsLock(mContext, selectedItem)) {
                    mIm.remapModifierKey(fromKeys[0], toKeys[0]);
                    mIm.remapModifierKey(fromKeys[1], toKeys[0]);
                }
                // Auto handle left and right keys remapping.
                if (!isKeyCapsLock(mContext, mKeyDefaultName)
                        && !isKeyCapsLock(mContext, selectedItem)) {
                    mIm.remapModifierKey(fromKeys[0], toKeys[0]);
                    mIm.remapModifierKey(fromKeys[1], toKeys[1]);
                }
            }
            mPreference.setSummary(itemSummary);
            modifierKeyDialog.dismiss();
        });

        Button cancelButton = dialoglayout.findViewById(R.id.modifier_key_cancel_button);
        cancelButton.setOnClickListener(v -> {
            modifierKeyDialog.dismiss();
        });

        final Window window = modifierKeyDialog.getWindow();
        window.setType(TYPE_SYSTEM_DIALOG);

        return modifierKeyDialog;
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
                view = LayoutInflater.from(mContext).inflate(R.layout.modifier_key_item, null);
            }
            TextView textView = view.findViewById(R.id.modifier_key_text);
            ImageView checkIcon = view.findViewById(R.id.modifier_key_check_icon);
            textView.setText(mList.get(i));
            if (mCurrentItem == i) {
                textView.setTextColor(getColorOfColorAccentPrimaryVariant());
                checkIcon.setImageAlpha(255);
                view.setBackground(
                        mContext.getDrawable(R.drawable.modifier_key_lisetview_background));
            } else {
                textView.setTextColor(getColorOfTextColorPrimary());
                checkIcon.setImageAlpha(0);
                view.setBackground(null);
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

    private int getColorOfTextColorPrimary() {
        return Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);
    }

    private int getColorOfTextColorSecondary() {
        return Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorSecondary);
    }

    private int getColorOfColorAccentPrimaryVariant() {
        return Utils.getColorAttrDefaultColor(
                mContext, com.android.internal.R.attr.colorAccentPrimaryVariant);
    }
}
