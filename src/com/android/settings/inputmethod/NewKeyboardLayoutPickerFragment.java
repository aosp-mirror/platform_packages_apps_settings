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

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.hardware.input.Flags;
import com.android.settings.R;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;

//TODO: b/316243168 - [Physical Keyboard Setting] Refactor NewKeyboardLayoutPickerFragment
public class NewKeyboardLayoutPickerFragment extends Fragment {
    private static final int DEFAULT_KEYBOARD_PREVIEW_WIDTH = 1630;
    private static final int DEFAULT_KEYBOARD_PREVIEW_HEIGHT = 540;

    private ImageView mKeyboardLayoutPreview;
    private TextView mKeyboardLayoutPreviewText;
    private InputManager mInputManager;
    private final NewKeyboardLayoutPickerController.KeyboardLayoutSelectedCallback
            mKeyboardLayoutSelectedCallback =
            new NewKeyboardLayoutPickerController.KeyboardLayoutSelectedCallback() {
                @Override
                public void onSelected(KeyboardLayout keyboardLayout) {
                    if (mInputManager != null
                            && mKeyboardLayoutPreview != null
                            && mKeyboardLayoutPreviewText != null && keyboardLayout != null) {
                        Drawable previewDrawable = mInputManager.getKeyboardLayoutPreview(
                                keyboardLayout,
                                DEFAULT_KEYBOARD_PREVIEW_WIDTH, DEFAULT_KEYBOARD_PREVIEW_HEIGHT);
                        mKeyboardLayoutPreview.setVisibility(
                                previewDrawable == null ? GONE : VISIBLE);
                        mKeyboardLayoutPreviewText.setVisibility(
                                previewDrawable == null ? GONE : VISIBLE);
                        if (previewDrawable != null) {
                            mKeyboardLayoutPreviewText.setText(keyboardLayout.getLabel());
                            mKeyboardLayoutPreview.setImageDrawable(previewDrawable);
                        }
                    }
                }
            };

    private final NewKeyboardLayoutPickerContent.ControllerUpdateCallback
            mControllerUpdateCallback =
                    newKeyboardLayoutPickerController -> {
                        if (newKeyboardLayoutPickerController != null) {
                            newKeyboardLayoutPickerController.registerKeyboardSelectedCallback(
                                    mKeyboardLayoutSelectedCallback);
                        }
                    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mInputManager = requireContext().getSystemService(InputManager.class);
        ViewGroup fragmentView = (ViewGroup) inflater.inflate(
                getPickerLayout(getResources().getConfiguration()), container, false);
        mKeyboardLayoutPreview = fragmentView.findViewById(R.id.keyboard_layout_preview);
        mKeyboardLayoutPreviewText = fragmentView.findViewById(R.id.keyboard_layout_preview_name);
        if (!Flags.keyboardLayoutPreviewFlag()) {
            updateViewMarginForPreviewFlagOff(fragmentView);
        }
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.keyboard_layout_title, new NewKeyboardLayoutPickerTitle())
                .commit();

        NewKeyboardLayoutPickerContent fragment = new NewKeyboardLayoutPickerContent();
        fragment.setControllerUpdateCallback(mControllerUpdateCallback);
        fragment.setArguments(getArguments());
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.keyboard_layouts, fragment)
                .commit();
        return fragmentView;
    }

    private int getPickerLayout(Configuration configuration) {
        return !ActivityEmbeddingUtils.isAlreadyEmbedded(this.getActivity())
                && configuration.orientation == ORIENTATION_LANDSCAPE
                ? R.layout.keyboard_layout_picker_one_pane_land : R.layout.keyboard_layout_picker;
    }

    private void updateViewMarginForPreviewFlagOff(ViewGroup fragmentView) {
        LinearLayout previewContainer = fragmentView.findViewById(
                R.id.keyboard_layout_picker_container);
        FrameLayout.LayoutParams previewContainerLayoutParams =
                (FrameLayout.LayoutParams) previewContainer.getLayoutParams();
        previewContainerLayoutParams.setMargins(0, 0, 0, 0);
        previewContainer.setLayoutParams(previewContainerLayoutParams);
    }
}
