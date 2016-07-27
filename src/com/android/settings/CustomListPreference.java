/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v14.preference.ListPreferenceDialogFragment;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

public class CustomListPreference extends ListPreference {

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
    }

    protected void onDialogClosed(boolean positiveResult) {
    }

    protected void onDialogCreated(Dialog dialog) {
    }

    protected boolean isAutoClosePreference() {
        return true;
    }

    /**
     * Called when a user is about to choose the given value, to determine if we
     * should show a confirmation dialog.
     *
     * @param value the value the user is about to choose
     * @return the message to show in a confirmation dialog, or {@code null} to
     *         not request confirmation
     */
    protected CharSequence getConfirmationMessage(String value) {
        return null;
    }

    protected void onDialogStateRestored(Dialog dialog, Bundle savedInstanceState) {
    }

    public static class CustomListPreferenceDialogFragment extends ListPreferenceDialogFragment {

        private static final java.lang.String KEY_CLICKED_ENTRY_INDEX
                = "settings.CustomListPrefDialog.KEY_CLICKED_ENTRY_INDEX";

        private int mClickedDialogEntryIndex;

        public static ListPreferenceDialogFragment newInstance(String key) {
            final ListPreferenceDialogFragment fragment = new CustomListPreferenceDialogFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        private CustomListPreference getCustomizablePreference() {
            return (CustomListPreference) getPreference();
        }

        @Override
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            mClickedDialogEntryIndex = getCustomizablePreference()
                    .findIndexOfValue(getCustomizablePreference().getValue());
            getCustomizablePreference().onPrepareDialogBuilder(builder, getOnItemClickListener());
            if (!getCustomizablePreference().isAutoClosePreference()) {
                builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onItemChosen();
                    }
                });
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);
            if (savedInstanceState != null) {
                mClickedDialogEntryIndex = savedInstanceState.getInt(KEY_CLICKED_ENTRY_INDEX,
                        mClickedDialogEntryIndex);
            }
            getCustomizablePreference().onDialogCreated(dialog);
            return dialog;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(KEY_CLICKED_ENTRY_INDEX, mClickedDialogEntryIndex);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getCustomizablePreference().onDialogStateRestored(getDialog(), savedInstanceState);
        }

        protected DialogInterface.OnClickListener getOnItemClickListener() {
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setClickedDialogEntryIndex(which);
                    if (getCustomizablePreference().isAutoClosePreference()) {
                        onItemChosen();
                    }
                }
            };
        }

        protected void setClickedDialogEntryIndex(int which) {
            mClickedDialogEntryIndex = which;
        }

        private String getValue() {
            final ListPreference preference = getCustomizablePreference();
            if (mClickedDialogEntryIndex >= 0 && preference.getEntryValues() != null) {
                return preference.getEntryValues()[mClickedDialogEntryIndex].toString();
            } else {
                return null;
            }
        }

        /**
         * Called when user has made a concrete item choice, but we might need
         * to make a quick detour to confirm that choice with a second dialog.
         */
        protected void onItemChosen() {
            final CharSequence message = getCustomizablePreference()
                    .getConfirmationMessage(getValue());
            if (message != null) {
                final Fragment f = new ConfirmDialogFragment();
                final Bundle args = new Bundle();
                args.putCharSequence(Intent.EXTRA_TEXT, message);
                f.setArguments(args);
                f.setTargetFragment(CustomListPreferenceDialogFragment.this, 0);
                final FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(f, getTag() + "-Confirm");
                ft.commitAllowingStateLoss();
            } else {
                onItemConfirmed();
            }
        }

        /**
         * Called when user has made a concrete item choice and we've fully
         * confirmed they want to move forward (if we took a detour above).
         */
        protected void onItemConfirmed() {
            onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
            getDialog().dismiss();
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            getCustomizablePreference().onDialogClosed(positiveResult);
            final ListPreference preference = getCustomizablePreference();
            final String value = getValue();
            if (positiveResult && value != null) {
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                }
            }
        }
    }

    public static class ConfirmDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(getArguments().getCharSequence(Intent.EXTRA_TEXT))
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Fragment f = getTargetFragment();
                            if (f != null) {
                                ((CustomListPreferenceDialogFragment) f).onItemConfirmed();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
    }
}
