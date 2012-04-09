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

import com.android.settings.R;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Arrays;

public class KeyboardLayoutPicker extends ListFragment
        implements LoaderCallbacks<KeyboardLayout[]> {
    private static final String TAG = "KeyboardLayoutPicker";

    private String mInputDeviceDescriptor;

    /**
     * Intent extra: The input device descriptor of the keyboard whose keyboard
     * layout is to be changed.
     */
    public static final String EXTRA_INPUT_DEVICE_DESCRIPTOR = "input_device_descriptor";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mInputDeviceDescriptor = getActivity().getIntent().getStringExtra(
                EXTRA_INPUT_DEVICE_DESCRIPTOR);
        if (mInputDeviceDescriptor == null) {
            Log.e(TAG, "Missing expected intent parameter: " + EXTRA_INPUT_DEVICE_DESCRIPTOR);
            getActivity().finish();
        }

        setEmptyText(getActivity().getText(R.string.keyboard_layout_picker_empty_text));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().requestFocus();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mInputDeviceDescriptor != null) {
            KeyboardLayout c = (KeyboardLayout)l.getItemAtPosition(position);
            InputManager im = (InputManager)getActivity().getSystemService(Context.INPUT_SERVICE);
            im.setKeyboardLayoutForInputDevice(mInputDeviceDescriptor, c.getDescriptor());
        }

        getActivity().finish();
    }

    @Override
    public Loader<KeyboardLayout[]> onCreateLoader(int id, Bundle args) {
        return new KeyboardLayoutLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<KeyboardLayout[]> loader,
            KeyboardLayout[] data) {
        setListAdapter(new KeyboardLayoutAdapter(getActivity(), data));
    }

    @Override
    public void onLoaderReset(Loader<KeyboardLayout[]> loader) {
        setListAdapter(null);
    }

    private static final class KeyboardLayoutAdapter
            extends ArrayAdapter<KeyboardLayout> {
        public KeyboardLayoutAdapter(Context context, KeyboardLayout[] list) {
            super(context, android.R.layout.simple_list_item_1, list);
        }
    }

    private static final class KeyboardLayoutLoader
            extends AsyncTaskLoader<KeyboardLayout[]> {
        public KeyboardLayoutLoader(Context context) {
            super(context);
        }

        @Override
        public KeyboardLayout[] loadInBackground() {
            InputManager im = (InputManager)getContext().getSystemService(Context.INPUT_SERVICE);
            KeyboardLayout[] list = im.getKeyboardLayouts();
            Arrays.sort(list);
            return list;
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
}
