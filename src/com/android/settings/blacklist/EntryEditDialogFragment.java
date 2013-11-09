/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.blacklist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.Telephony.Blacklist;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import com.android.internal.telephony.util.BlacklistUtils;
import com.android.settings.R;

public class EntryEditDialogFragment extends DialogFragment
    implements TextWatcher, DialogInterface.OnClickListener {

    private EditText mEditText;
    private ImageButton mContactPickButton;
    private CheckBox mBlockCalls;
    private CheckBox mBlockMessages;
    private Button mOkButton;

    private static final String[] BLACKLIST_PROJECTION = {
        Blacklist.NUMBER, Blacklist.PHONE_MODE, Blacklist.MESSAGE_MODE
    };
    private static final String[] NUMBER_PROJECTION = {
        CommonDataKinds.Phone.NUMBER
    };
    private static final int COLUMN_NUMBER = 0;
    private static final int COLUMN_PHONE = 1;
    private static final int COLUMN_MESSAGE = 2;

    private static final int REQUEST_CODE_PICKER = 1;

    private static final String DIALOG_STATE = "blacklist_edit_state";
    private static final String STATE_NUMBER = "number";
    private static final String STATE_PHONE = "phone";
    private static final String STATE_MESSAGE = "message";

    public static EntryEditDialogFragment newInstance(long id) {
        Bundle args = new Bundle();
        args.putLong("id", id);

        EntryEditDialogFragment fragment = new EntryEditDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public EntryEditDialogFragment() {
        super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        long id = getEntryId();
        Bundle dialogState = savedInstanceState != null
                ? savedInstanceState.getBundle(DIALOG_STATE) : null;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.blacklist_edit_dialog_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, null)
                .setView(createDialogView(id, dialogState));

        if (id >= 0) {
            builder.setNeutralButton(R.string.blacklist_button_delete, this);
        }

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog dialog = (AlertDialog) getDialog();
        Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EntryEditDialogFragment parent = EntryEditDialogFragment.this;
                DialogFragment confirm = DeleteConfirmationFragment.newInstance(parent);
                confirm.show(getFragmentManager(), "delete_confirm");
            }
        });

        updateOkButtonState();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            updateBlacklistEntry();
        }
    }

    private void onDeleteConfirmResult(boolean confirmed) {
        if (confirmed) {
            Uri uri = ContentUris.withAppendedId(Blacklist.CONTENT_URI, getEntryId());
            getActivity().getContentResolver().delete(uri, null, null);
            dismiss();
        }
    }

    private long getEntryId() {
        return getArguments().getLong("id", -1);
    }

    private View createDialogView(long id, Bundle savedState) {
        final Activity activity = getActivity();
        final LayoutInflater inflater = (LayoutInflater)
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_blacklist_edit_entry, null);

        mEditText = (EditText) view.findViewById(R.id.number_edit);
        mEditText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        mEditText.setKeyListener(DialerKeyListener.getInstance());
        mEditText.addTextChangedListener(this);

        mContactPickButton = (ImageButton) view.findViewById(R.id.select_contact);
        mContactPickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent contactListIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contactListIntent.setType(CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

                startActivityForResult(contactListIntent, REQUEST_CODE_PICKER, null);
            }
        });

        mBlockCalls = (CheckBox) view.findViewById(R.id.incoming_calls);
        mBlockMessages = (CheckBox) view.findViewById(R.id.incoming_messages);

        if (savedState != null) {
            mEditText.setText(savedState.getCharSequence(STATE_NUMBER));
            mBlockCalls.setChecked(savedState.getBoolean(STATE_PHONE));
            mBlockMessages.setChecked(savedState.getBoolean(STATE_MESSAGE));
        } else if (id >= 0) {
            Uri uri = ContentUris.withAppendedId(Blacklist.CONTENT_URI, id);
            Cursor cursor = activity.getContentResolver().query(uri,
                    BLACKLIST_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                mEditText.setText(cursor.getString(COLUMN_NUMBER));
                mBlockCalls.setChecked(cursor.getInt(COLUMN_PHONE) != 0);
                mBlockMessages.setChecked(cursor.getInt(COLUMN_MESSAGE) != 0);
            } else {
                id = -1;
            }
            if (cursor != null) {
                cursor.close();
            }
        }

        if (id < 0) {
            // defaults
            mEditText.setText("");
            mBlockCalls.setChecked(true);
            mBlockMessages.setChecked(true);
        }

        return view;
    }

    private void updateBlacklistEntry() {
        ContentValues cv = new ContentValues();
        String number = mEditText.getText().toString();

        cv.put(Blacklist.NUMBER, number);
        cv.put(Blacklist.PHONE_MODE, mBlockCalls.isChecked() ? 1 : 0);
        cv.put(Blacklist.MESSAGE_MODE, mBlockMessages.isChecked() ? 1 : 0);

        long id = getEntryId();
        Uri uri;

        if (id < 0) {
            uri = Blacklist.CONTENT_FILTER_BYNUMBER_URI.buildUpon()
                    .appendPath(number)
                    .build();
        } else {
            uri = ContentUris.withAppendedId(Blacklist.CONTENT_URI, id);
        }
        getActivity().getContentResolver().update(uri, cv, null, null);
    }

    private void updateOkButtonState() {
        if (mOkButton == null) {
            AlertDialog dialog = (AlertDialog) getDialog();
            if (dialog != null) {
                mOkButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            }
        }

        if (mOkButton != null) {
            mOkButton.setEnabled(mEditText.getText().length() != 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        Bundle dialogState = new Bundle();
        dialogState.putCharSequence(STATE_NUMBER, mEditText.getText());
        dialogState.putBoolean(STATE_PHONE, mBlockCalls.isChecked());
        dialogState.putBoolean(STATE_MESSAGE, mBlockMessages.isChecked());
        state.putBundle(DIALOG_STATE, dialogState);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateOkButtonState();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_PICKER) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            Cursor cursor = getActivity().getContentResolver().query(data.getData(),
                    NUMBER_PROJECTION, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mEditText.setText(cursor.getString(COLUMN_NUMBER));
                }
                cursor.close();
            }
        }
    }

    private static class DeleteConfirmationFragment extends DialogFragment
            implements DialogInterface.OnClickListener {
        public static DialogFragment newInstance(EntryEditDialogFragment parent) {
            DialogFragment fragment = new DeleteConfirmationFragment();
            fragment.setTargetFragment(parent, 0);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.remove_blacklist_number_title)
                    .setMessage(R.string.remove_blacklist_entry)
                    .setPositiveButton(R.string.yes, this)
                    .setNegativeButton(R.string.no, this)
                    .create();

            return dialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            EntryEditDialogFragment parent = (EntryEditDialogFragment) getTargetFragment();
            parent.onDeleteConfirmResult(which == DialogInterface.BUTTON_POSITIVE);
        }
    }
}
