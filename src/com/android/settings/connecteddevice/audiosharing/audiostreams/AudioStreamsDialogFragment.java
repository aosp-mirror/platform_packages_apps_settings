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
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import com.google.common.base.Strings;

import java.util.function.Consumer;

public class AudioStreamsDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioStreamsDialogFragment";
    private final DialogBuilder mDialogBuilder;

    AudioStreamsDialogFragment(DialogBuilder dialogBuilder) {
        mDialogBuilder = dialogBuilder;
    }

    @Override
    public int getMetricsCategory() {
        // TODO(chelseahao): update metrics id
        return 0;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mDialogBuilder.build();
    }

    static void show(Fragment host, DialogBuilder dialogBuilder) {
        FragmentManager manager = host.getChildFragmentManager();
        (new AudioStreamsDialogFragment(dialogBuilder)).show(manager, TAG);
    }

    static class DialogBuilder {
        private final Context mContext;
        private final AlertDialog.Builder mBuilder;
        private String mTitle;
        private String mSubTitle1;
        private String mSubTitle2;
        private String mLeftButtonText;
        private String mRightButtonText;
        private Consumer<AlertDialog> mLeftButtonOnClickListener;
        private Consumer<AlertDialog> mRightButtonOnClickListener;

        DialogBuilder(Context context) {
            mContext = context;
            mBuilder = new AlertDialog.Builder(context);
        }

        DialogBuilder setTitle(String title) {
            mTitle = title;
            return this;
        }

        DialogBuilder setSubTitle1(String subTitle1) {
            mSubTitle1 = subTitle1;
            return this;
        }

        DialogBuilder setSubTitle2(String subTitle2) {
            mSubTitle2 = subTitle2;
            return this;
        }

        DialogBuilder setLeftButtonText(String text) {
            mLeftButtonText = text;
            return this;
        }

        DialogBuilder setLeftButtonOnClickListener(Consumer<AlertDialog> listener) {
            mLeftButtonOnClickListener = listener;
            return this;
        }

        DialogBuilder setRightButtonText(String text) {
            mRightButtonText = text;
            return this;
        }

        DialogBuilder setRightButtonOnClickListener(Consumer<AlertDialog> listener) {
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
                leftButton.setOnClickListener(unused -> mLeftButtonOnClickListener.accept(dialog));
            }
            if (!Strings.isNullOrEmpty(mRightButtonText)) {
                Button rightButton = rootView.requireViewById(R.id.right_button);
                rightButton.setText(mRightButtonText);
                rightButton.setVisibility(View.VISIBLE);
                rightButton.setOnClickListener(
                        unused -> mRightButtonOnClickListener.accept(dialog));
            }

            return dialog;
        }
    }
}
