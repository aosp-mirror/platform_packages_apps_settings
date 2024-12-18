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

package com.android.settings.notification.modes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.modes.ZenModesListAddModePreferenceController.ModeType;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class ZenModesListAddModeTypeChooserDialog extends InstrumentedDialogFragment {

    private static final String TAG = "ZenModesListAddModeTypeChooserDialog";

    private OnChooseModeTypeListener mChooseModeTypeListener;
    private ImmutableList<ModeType> mOptions;

    interface OnChooseModeTypeListener {
        void onTypeSelected(ModeType type);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_MODE_NEW_TYPE_CHOOSER_DIALOG;
    }

    static void show(DashboardFragment parent,
            OnChooseModeTypeListener onChooseModeTypeListener,
            List<ModeType> options) {
        ZenModesListAddModeTypeChooserDialog dialog = new ZenModesListAddModeTypeChooserDialog();
        dialog.mChooseModeTypeListener = onChooseModeTypeListener;
        dialog.mOptions = ImmutableList.copyOf(options);
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getParentFragmentManager(), TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mChooseModeTypeListener == null) {
            // Probably the dialog fragment was recreated after its activity being destroyed.
            // It's pointless to re-show the dialog if we can't do anything when its options are
            // selected, so we don't.
            dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        checkState(getContext() != null);
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.zen_mode_new_title)
                .setAdapter(new OptionsAdapter(getContext(), mOptions),
                        (dialog, which) -> mChooseModeTypeListener.onTypeSelected(
                                mOptions.get(which)))
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private static class OptionsAdapter extends ArrayAdapter<ModeType> {

        private final LayoutInflater mInflater;

        private OptionsAdapter(Context context,
                ImmutableList<ModeType> availableModeProviders) {
            super(context, R.layout.zen_mode_type_item, availableModeProviders);
            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.zen_mode_type_item, parent, false);
            }
            ImageView imageView = checkNotNull(convertView.findViewById(R.id.icon));
            TextView title = checkNotNull(convertView.findViewById(R.id.title));
            TextView subtitle = checkNotNull(convertView.findViewById(R.id.subtitle));

            ModeType option = checkNotNull(getItem(position));
            imageView.setImageDrawable(option.icon());
            title.setText(option.name());
            subtitle.setText(option.summary());
            subtitle.setVisibility(
                    Strings.isNullOrEmpty(option.summary()) ? View.GONE : View.VISIBLE);

            return convertView;
        }
    }
}
