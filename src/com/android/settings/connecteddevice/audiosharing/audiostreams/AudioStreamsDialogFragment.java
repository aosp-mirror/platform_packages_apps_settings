/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import com.google.common.base.Strings;

import java.util.function.Consumer;

/** A dialog fragment for constructing and showing audio stream dialogs. */
public class AudioStreamsDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioStreamsDialogFragment";
    private final DialogBuilder mDialogBuilder;
    private int mDialogId = SettingsEnums.PAGE_UNKNOWN;

    AudioStreamsDialogFragment(DialogBuilder dialogBuilder, int dialogId) {
        mDialogBuilder = dialogBuilder;
        mDialogId = dialogId;
    }

    @Override
    public int getMetricsCategory() {
        return mDialogId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mDialogBuilder.build();
    }

    /**
     * Displays the audio stream dialog on the specified host fragment.
     *
     * @param host The fragment to host the dialog.
     * @param dialogBuilder The builder for constructing the dialog.
     * @param dialogId The dialog settings enum for logging
     */
    public static void show(Fragment host, DialogBuilder dialogBuilder, int dialogId) {
        if (!host.isAdded()) {
            Log.w(TAG, "The host fragment is not added to the activity!");
            return;
        }
        FragmentManager manager = host.getChildFragmentManager();
        (new AudioStreamsDialogFragment(dialogBuilder, dialogId)).show(manager, TAG);
    }

    static void dismissAll(Fragment host) {
        if (!host.isAdded()) {
            Log.w(TAG, "The host fragment is not added to the activity!");
            return;
        }
        FragmentManager manager = host.getChildFragmentManager();
        Fragment dialog = manager.findFragmentByTag(TAG);
        if (dialog != null
                && ((DialogFragment) dialog).getDialog() != null
                && ((DialogFragment) dialog).getDialog().isShowing()) {
            ((DialogFragment) dialog).dismiss();
        }
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        Fragment dialog = manager.findFragmentByTag(TAG);
        if (dialog != null
                && ((DialogFragment) dialog).getDialog() != null
                && ((DialogFragment) dialog).getDialog().isShowing()) {
            Log.w(TAG, "Dialog already showing, ignore");
            return;
        }
        super.show(manager, tag);
    }

    /** A builder class for constructing the audio stream dialog. */
    public static class DialogBuilder {
        private final Context mContext;
        private final AlertDialog.Builder mBuilder;
        @Nullable private String mTitle;
        @Nullable private String mSubTitle1;
        @Nullable private String mSubTitle2;
        @Nullable private String mLeftButtonText;
        @Nullable private String mRightButtonText;
        @Nullable private Consumer<AlertDialog> mLeftButtonOnClickListener;
        @Nullable private Consumer<AlertDialog> mRightButtonOnClickListener;

        /**
         * Constructs a new instance of DialogBuilder.
         *
         * @param context The context used for building the dialog.
         */
        public DialogBuilder(Context context) {
            mContext = context;
            mBuilder = new AlertDialog.Builder(context);
        }

        /**
         * Sets the title of the dialog.
         *
         * @param title The title text.
         * @return This DialogBuilder instance.
         */
        public DialogBuilder setTitle(String title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the first subtitle of the dialog.
         *
         * @param subTitle1 The text of the first subtitle.
         * @return This DialogBuilder instance.
         */
        public DialogBuilder setSubTitle1(String subTitle1) {
            mSubTitle1 = subTitle1;
            return this;
        }

        /**
         * Sets the second subtitle of the dialog.
         *
         * @param subTitle2 The text of the second subtitle.
         * @return This DialogBuilder instance.
         */
        public DialogBuilder setSubTitle2(String subTitle2) {
            mSubTitle2 = subTitle2;
            return this;
        }

        /**
         * Sets the text of the left button.
         *
         * @param text The text of the left button.
         * @return This DialogBuilder instance.
         */
        public DialogBuilder setLeftButtonText(String text) {
            mLeftButtonText = text;
            return this;
        }

        /**
         * Sets the click listener of the left button.
         *
         * @param listener The click listener for the left button.
         * @return This DialogBuilder instance.
         */
        public DialogBuilder setLeftButtonOnClickListener(Consumer<AlertDialog> listener) {
            mLeftButtonOnClickListener = listener;
            return this;
        }

        /**
         * Sets the text of the right button.
         *
         * @param text The text of the right button.
         * @return This DialogBuilder instance.
         */
        public DialogBuilder setRightButtonText(String text) {
            mRightButtonText = text;
            return this;
        }

        /**
         * Sets the click listener of the right button.
         *
         * @param listener The click listener for the right button.
         * @return This DialogBuilder instance.
         */
        public DialogBuilder setRightButtonOnClickListener(Consumer<AlertDialog> listener) {
            mRightButtonOnClickListener = listener;
            return this;
        }

        AlertDialog build() {
            View rootView =
                    LayoutInflater.from(mContext)
                            .inflate(R.xml.bluetooth_audio_streams_dialog, /* parent= */ null);

            AlertDialog dialog = mBuilder.setView(rootView).setCancelable(false).create();
            dialog.setCanceledOnTouchOutside(false);

            TextView title = rootView.requireViewById(R.id.dialog_title);
            title.setText(mTitle);

            if (!Strings.isNullOrEmpty(mSubTitle1)) {
                TextView subTitle1 = rootView.requireViewById(R.id.dialog_subtitle);
                subTitle1.setText(mSubTitle1);
                subTitle1.setVisibility(View.VISIBLE);
            }
            if (!Strings.isNullOrEmpty(mSubTitle2)) {
                TextView subTitle2 = rootView.requireViewById(R.id.dialog_subtitle_2);
                subTitle2.setText(mSubTitle2);
                subTitle2.setVisibility(View.VISIBLE);
            }
            if (!Strings.isNullOrEmpty(mLeftButtonText)) {
                Button leftButton = rootView.requireViewById(R.id.left_button);
                leftButton.setText(mLeftButtonText);
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setOnClickListener(
                        unused -> {
                            if (mLeftButtonOnClickListener != null) {
                                mLeftButtonOnClickListener.accept(dialog);
                            }
                        });
            }
            if (!Strings.isNullOrEmpty(mRightButtonText)) {
                Button rightButton = rootView.requireViewById(R.id.right_button);
                rightButton.setText(mRightButtonText);
                rightButton.setVisibility(View.VISIBLE);
                rightButton.setOnClickListener(
                        unused -> {
                            if (mRightButtonOnClickListener != null) {
                                mRightButtonOnClickListener.accept(dialog);
                            }
                        });
            }

            return dialog;
        }
    }
}
