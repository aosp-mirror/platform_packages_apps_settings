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

package com.android.settings.connecteddevice.audiosharing;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import javax.annotation.CheckReturnValue;

public class AudioSharingDialogFactory {
    private static final String TAG = "AudioSharingDialogFactory";

    /**
     * Initializes a builder for the dialog to be shown for audio sharing.
     *
     * @param context The {@link Context} that will be used to create the dialog.
     * @return A configurable builder for the dialog.
     */
    @NonNull
    public static AudioSharingDialogFactory.DialogBuilder newBuilder(@NonNull Context context) {
        return new AudioSharingDialogFactory.DialogBuilder(context);
    }

    /** Builder class with configurable options for the dialog to be shown for audio sharing. */
    public static class DialogBuilder {
        private Context mContext;
        private AlertDialog.Builder mBuilder;
        private View mCustomTitle;
        private View mCustomBody;
        private boolean mIsCustomBodyEnabled;

        /**
         * Private constructor for the dialog builder class. Should not be invoked directly;
         * instead, use {@link AudioSharingDialogFactory#newBuilder(Context)}.
         *
         * @param context The {@link Context} that will be used to create the dialog.
         */
        private DialogBuilder(@NonNull Context context) {
            mContext = context;
            mBuilder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(mBuilder.getContext());
            mCustomTitle =
                    inflater.inflate(R.layout.dialog_custom_title_audio_sharing, /* root= */ null);
            mCustomBody =
                    inflater.inflate(R.layout.dialog_custom_body_audio_sharing, /* parent= */ null);
        }

        /**
         * Sets title of the dialog custom title.
         *
         * @param titleRes Resource ID of the string to be used for the dialog title.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setTitle(@StringRes int titleRes) {
            TextView title = mCustomTitle.findViewById(R.id.title_text);
            title.setText(titleRes);
            return this;
        }

        /**
         * Sets title of the dialog custom title.
         *
         * @param titleText The text to be used for the title.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setTitle(@NonNull CharSequence titleText) {
            TextView title = mCustomTitle.findViewById(R.id.title_text);
            title.setText(titleText);
            return this;
        }

        /**
         * Sets the title icon of the dialog custom title.
         *
         * @param iconRes The text to be used for the title.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setTitleIcon(@DrawableRes int iconRes) {
            ImageView icon = mCustomTitle.findViewById(R.id.title_icon);
            icon.setImageResource(iconRes);
            return this;
        }

        /**
         * Sets the message body of the dialog.
         *
         * @param messageRes Resource ID of the string to be used for the message body.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setMessage(@StringRes int messageRes) {
            mBuilder.setMessage(messageRes);
            return this;
        }

        /**
         * Sets the message body of the dialog.
         *
         * @param message The text to be used for the message body.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setMessage(@NonNull CharSequence message) {
            mBuilder.setMessage(message);
            return this;
        }

        /** Whether to use custom body. */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setIsCustomBodyEnabled(
                boolean isCustomBodyEnabled) {
            mIsCustomBodyEnabled = isCustomBodyEnabled;
            return this;
        }

        /**
         * Sets the custom image of the dialog custom body.
         *
         * @param iconRes The text to be used for the title.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomImage(@DrawableRes int iconRes) {
            ImageView image = mCustomBody.findViewById(R.id.description_image);
            image.setImageResource(iconRes);
            image.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Sets the custom message of the dialog custom body.
         *
         * @param messageRes Resource ID of the string to be used for the message body.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomMessage(@StringRes int messageRes) {
            TextView subTitle = mCustomBody.findViewById(R.id.description_text);
            subTitle.setText(messageRes);
            subTitle.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Sets the custom message of the dialog custom body.
         *
         * @param message The text to be used for the custom message body.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomMessage(
                @NonNull CharSequence message) {
            TextView subTitle = mCustomBody.findViewById(R.id.description_text);
            subTitle.setText(message);
            subTitle.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Sets the custom device actions of the dialog custom body.
         *
         * @param adapter The adapter for device items to build dialog actions.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomDeviceActions(
                @NonNull AudioSharingDeviceAdapter adapter) {
            RecyclerView recyclerView = mCustomBody.findViewById(R.id.device_btn_list);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(
                    new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
            recyclerView.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Sets the positive button label and listener for the dialog.
         *
         * @param labelRes Resource ID of the string to be used for the positive button label.
         * @param listener The listener to be invoked when the positive button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setPositiveButton(
                @StringRes int labelRes, @NonNull DialogInterface.OnClickListener listener) {
            mBuilder.setPositiveButton(labelRes, listener);
            return this;
        }

        /**
         * Sets the positive button label and listener for the dialog.
         *
         * @param label The text to be used for the positive button label.
         * @param listener The listener to be invoked when the positive button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setPositiveButton(
                @NonNull CharSequence label, @NonNull DialogInterface.OnClickListener listener) {
            mBuilder.setPositiveButton(label, listener);
            return this;
        }

        /**
         * Sets the custom positive button label and listener for the dialog custom body.
         *
         * @param labelRes Resource ID of the string to be used for the positive button label.
         * @param listener The listener to be invoked when the positive button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomPositiveButton(
                @StringRes int labelRes, @NonNull View.OnClickListener listener) {
            Button positiveBtn = mCustomBody.findViewById(R.id.positive_btn);
            positiveBtn.setText(labelRes);
            positiveBtn.setOnClickListener(listener);
            positiveBtn.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Sets the custom positive button label and listener for the dialog custom body.
         *
         * @param label The text to be used for the positive button label.
         * @param listener The listener to be invoked when the positive button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomPositiveButton(
                @NonNull CharSequence label, @NonNull View.OnClickListener listener) {
            Button positiveBtn = mCustomBody.findViewById(R.id.positive_btn);
            positiveBtn.setText(label);
            positiveBtn.setOnClickListener(listener);
            positiveBtn.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Sets the negative button label and listener for the dialog.
         *
         * @param labelRes Resource ID of the string to be used for the negative button label.
         * @param listener The listener to be invoked when the negative button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setNegativeButton(
                @StringRes int labelRes, @NonNull DialogInterface.OnClickListener listener) {
            mBuilder.setNegativeButton(labelRes, listener);
            return this;
        }

        /**
         * Sets the negative button label and listener for the dialog.
         *
         * @param label The text to be used for the negative button label.
         * @param listener The listener to be invoked when the negative button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setNegativeButton(
                @NonNull CharSequence label, @NonNull DialogInterface.OnClickListener listener) {
            mBuilder.setNegativeButton(label, listener);
            return this;
        }

        /**
         * Sets the custom negative button label and listener for the dialog custom body.
         *
         * @param labelRes Resource ID of the string to be used for the negative button label.
         * @param listener The listener to be invoked when the negative button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomNegativeButton(
                @StringRes int labelRes, @NonNull View.OnClickListener listener) {
            Button negativeBtn = mCustomBody.findViewById(R.id.negative_btn);
            negativeBtn.setText(labelRes);
            negativeBtn.setOnClickListener(listener);
            negativeBtn.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Sets the custom negative button label and listener for the dialog custom body.
         *
         * @param label The text to be used for the negative button label.
         * @param listener The listener to be invoked when the negative button is pressed.
         * @return This builder.
         */
        @NonNull
        public AudioSharingDialogFactory.DialogBuilder setCustomNegativeButton(
                @NonNull CharSequence label, @NonNull View.OnClickListener listener) {
            Button negativeBtn = mCustomBody.findViewById(R.id.negative_btn);
            negativeBtn.setText(label);
            negativeBtn.setOnClickListener(listener);
            negativeBtn.setVisibility(View.VISIBLE);
            return this;
        }

        /**
         * Builds a dialog with the current configs.
         *
         * @return The dialog to be shown for audio sharing.
         */
        @NonNull
        @CheckReturnValue
        public AlertDialog build() {
            if (mIsCustomBodyEnabled) {
                mBuilder.setView(mCustomBody);
            }
            final AlertDialog dialog =
                    mBuilder.setCustomTitle(mCustomTitle).setCancelable(false).create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

    private AudioSharingDialogFactory() {}
}
