/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.ArrayList;
import java.util.Collections;

public class KeyboardLayoutDialogFragment extends InstrumentedDialogFragment
        implements InputDeviceListener, LoaderCallbacks<KeyboardLayoutDialogFragment.Keyboards> {
    private static final String KEY_INPUT_DEVICE_IDENTIFIER = "inputDeviceIdentifier";

    private InputDeviceIdentifier mInputDeviceIdentifier;
    private int mInputDeviceId = -1;
    private InputManager mIm;
    private KeyboardLayoutAdapter mAdapter;

    public KeyboardLayoutDialogFragment() {
    }

    public KeyboardLayoutDialogFragment(InputDeviceIdentifier inputDeviceIdentifier) {
        mInputDeviceIdentifier = inputDeviceIdentifier;
    }


    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_KEYBOARD_LAYOUT;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Context context = activity.getBaseContext();
        mIm = (InputManager)context.getSystemService(Context.INPUT_SERVICE);
        mAdapter = new KeyboardLayoutAdapter(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mInputDeviceIdentifier = savedInstanceState.getParcelable(KEY_INPUT_DEVICE_IDENTIFIER);
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_INPUT_DEVICE_IDENTIFIER, mInputDeviceIdentifier);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        LayoutInflater inflater = LayoutInflater.from(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(R.string.keyboard_layout_dialog_title)
            .setPositiveButton(R.string.keyboard_layout_dialog_setup_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onSetupLayoutsButtonClicked();
                        }
                    })
            .setSingleChoiceItems(mAdapter, -1,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onKeyboardLayoutClicked(which);
                        }
                    })
            .setView(inflater.inflate(R.layout.keyboard_layout_dialog_switch_hint, null));
        updateSwitchHintVisibility();
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        mIm.registerInputDeviceListener(this, null);

        InputDevice inputDevice =
                mIm.getInputDeviceByDescriptor(mInputDeviceIdentifier.getDescriptor());
        if (inputDevice == null) {
            dismiss();
            return;
        }
        mInputDeviceId = inputDevice.getId();
    }

    @Override
    public void onPause() {
        mIm.unregisterInputDeviceListener(this);
        mInputDeviceId = -1;

        super.onPause();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        dismiss();
    }

    private void onSetupLayoutsButtonClicked() {
        ((OnSetupKeyboardLayoutsListener)getTargetFragment()).onSetupKeyboardLayouts(
                mInputDeviceIdentifier);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        show(getActivity().getSupportFragmentManager(), "layout");
    }

    private void onKeyboardLayoutClicked(int which) {
        if (which >= 0 && which < mAdapter.getCount()) {
            KeyboardLayout keyboardLayout = mAdapter.getItem(which);
            if (keyboardLayout != null) {
                mIm.setCurrentKeyboardLayoutForInputDevice(mInputDeviceIdentifier,
                        keyboardLayout.getDescriptor());
            }
            dismiss();
        }
    }

    @Override
    public Loader<Keyboards> onCreateLoader(int id, Bundle args) {
        return new KeyboardLayoutLoader(getActivity().getBaseContext(), mInputDeviceIdentifier);
    }

    @Override
    public void onLoadFinished(Loader<Keyboards> loader, Keyboards data) {
        mAdapter.clear();
        mAdapter.addAll(data.keyboardLayouts);
        mAdapter.setCheckedItem(data.current);
        AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            dialog.getListView().setItemChecked(data.current, true);
        }
        updateSwitchHintVisibility();
    }

    @Override
    public void onLoaderReset(Loader<Keyboards> loader) {
        mAdapter.clear();
        updateSwitchHintVisibility();
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            dismiss();
        }
    }

    private void updateSwitchHintVisibility() {
        AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null) {
            View customPanel = dialog.findViewById(com.google.android.material.R.id.customPanel);
            customPanel.setVisibility(mAdapter.getCount() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private static final class KeyboardLayoutAdapter extends ArrayAdapter<KeyboardLayout> {
        private final LayoutInflater mInflater;
        private int mCheckedItem = -1;

        public KeyboardLayoutAdapter(Context context) {
            super(context, com.android.internal.R.layout.simple_list_item_2_single_choice);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setCheckedItem(int position) {
            mCheckedItem = position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            KeyboardLayout item = getItem(position);
            String label, collection;
            if (item != null) {
                label = item.getLabel();
                collection = item.getCollection();
            } else {
                label = getContext().getString(R.string.keyboard_layout_default_label);
                collection = "";
            }

            boolean checked = (position == mCheckedItem);
            if (collection.isEmpty()) {
                return inflateOneLine(convertView, parent, label, checked);
            } else {
                return inflateTwoLine(convertView, parent, label, collection, checked);
            }
        }

        private View inflateOneLine(View convertView, ViewGroup parent,
                String label, boolean checked) {
            View view = convertView;
            if (view == null || isTwoLine(view)) {
                view = mInflater.inflate(
                        com.android.internal.R.layout.simple_list_item_single_choice,
                        parent, false);
                setTwoLine(view, false);
            }
            CheckedTextView headline = (CheckedTextView) view.findViewById(android.R.id.text1);
            headline.setText(label);
            headline.setChecked(checked);
            return view;
        }

        private View inflateTwoLine(View convertView, ViewGroup parent,
                String label, String collection, boolean checked) {
            View view = convertView;
            if (view == null || !isTwoLine(view)) {
                view = mInflater.inflate(
                        com.android.internal.R.layout.simple_list_item_2_single_choice,
                        parent, false);
                setTwoLine(view, true);
            }
            TextView headline = (TextView) view.findViewById(android.R.id.text1);
            TextView subText = (TextView) view.findViewById(android.R.id.text2);
            RadioButton radioButton =
                    (RadioButton)view.findViewById(com.android.internal.R.id.radio);
            headline.setText(label);
            subText.setText(collection);
            radioButton.setChecked(checked);
            return view;
        }

        private static boolean isTwoLine(View view) {
            return view.getTag() == Boolean.TRUE;
        }

        private static void setTwoLine(View view, boolean twoLine) {
            view.setTag(Boolean.valueOf(twoLine));
        }
    }

    private static final class KeyboardLayoutLoader extends AsyncTaskLoader<Keyboards> {
        private final InputDeviceIdentifier mInputDeviceIdentifier;

        public KeyboardLayoutLoader(Context context, InputDeviceIdentifier inputDeviceIdentifier) {
            super(context);
            mInputDeviceIdentifier = inputDeviceIdentifier;
        }

        @Override
        public Keyboards loadInBackground() {
            Keyboards keyboards = new Keyboards();
            InputManager im = (InputManager)getContext().getSystemService(Context.INPUT_SERVICE);
            if (mInputDeviceIdentifier == null || NewKeyboardSettingsUtils.getInputDevice(
                    im, mInputDeviceIdentifier) == null) {
                keyboards.keyboardLayouts.add(null); // default layout
                keyboards.current = 0;
                return keyboards;
            }
            String[] keyboardLayoutDescriptors = im.getEnabledKeyboardLayoutsForInputDevice(
                    mInputDeviceIdentifier);
            for (String keyboardLayoutDescriptor : keyboardLayoutDescriptors) {
                KeyboardLayout keyboardLayout = im.getKeyboardLayout(keyboardLayoutDescriptor);
                if (keyboardLayout != null) {
                    keyboards.keyboardLayouts.add(keyboardLayout);
                }
            }
            Collections.sort(keyboards.keyboardLayouts);

            String currentKeyboardLayoutDescriptor =
                    im.getCurrentKeyboardLayoutForInputDevice(mInputDeviceIdentifier);
            if (currentKeyboardLayoutDescriptor != null) {
                final int numKeyboardLayouts = keyboards.keyboardLayouts.size();
                for (int i = 0; i < numKeyboardLayouts; i++) {
                    if (keyboards.keyboardLayouts.get(i).getDescriptor().equals(
                            currentKeyboardLayoutDescriptor)) {
                        keyboards.current = i;
                        break;
                    }
                }
            }

            if (keyboards.keyboardLayouts.isEmpty()) {
                keyboards.keyboardLayouts.add(null); // default layout
                keyboards.current = 0;
            }
            return keyboards;
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            super.onStopLoading();
            cancelLoad();
        }
    }

    public static final class Keyboards {
        public final ArrayList<KeyboardLayout> keyboardLayouts = new ArrayList<KeyboardLayout>();
        public int current = -1;
    }

    public interface OnSetupKeyboardLayoutsListener {
        public void onSetupKeyboardLayouts(InputDeviceIdentifier mInputDeviceIdentifier);
    }
}
