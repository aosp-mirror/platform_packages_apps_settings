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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.android.settings.R;

public class NewKeyboardLayoutPickerFragment extends Fragment {

    private ViewGroup mFragmentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mFragmentView = (ViewGroup) inflater.inflate(
                R.layout.keyboard_layout_picker, container, false);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.keyboard_layout_title, new NewKeyboardLayoutPickerTitle())
                .commit();

        NewKeyboardLayoutPickerContent fragment = new NewKeyboardLayoutPickerContent();
        fragment.setArguments(getArguments());
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.keyboard_layouts, fragment)
                .commit();

        return mFragmentView;
    }
}
