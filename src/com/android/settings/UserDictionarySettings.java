/**
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.UserDictionary;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AlphabetIndexer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.settings.SettingsPreferenceFragment.SettingsDialogFragment;

import java.util.Locale;

public class UserDictionarySettings extends ListFragment implements DialogCreatable {
    private static final String TAG = "UserDictionarySettings";

    private static final String INSTANCE_KEY_DIALOG_EDITING_WORD = "DIALOG_EDITING_WORD";
    private static final String INSTANCE_KEY_ADDED_WORD = "DIALOG_ADDED_WORD";

    private static final String[] QUERY_PROJECTION = {
        UserDictionary.Words._ID, UserDictionary.Words.WORD
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_WORD = 1;

    // Either the locale is empty (means the word is applicable to all locales)
    // or the word equals our current locale
    private static final String QUERY_SELECTION =
            UserDictionary.Words.LOCALE + "=?";
    private static final String QUERY_SELECTION_ALL_LOCALES =
            UserDictionary.Words.LOCALE + " is null";

    private static final String DELETE_SELECTION = UserDictionary.Words.WORD + "=?";

    private static final String EXTRA_WORD = "word";

    private static final int OPTIONS_MENU_ADD = Menu.FIRST;

    private static final int DIALOG_ADD_OR_EDIT = 0;

    private static final int FREQUENCY_FOR_USER_DICTIONARY_ADDS = 250;

    /** The word being edited in the dialog (null means the user is adding a word). */
    private String mDialogEditingWord;

    private Cursor mCursor;

    protected String mLocale;

    private boolean mAddedWordAlready;
    private boolean mAutoReturn;

    private SettingsDialogFragment mDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(
                com.android.internal.R.layout.preference_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Intent intent = getActivity().getIntent();
        final String localeFromIntent =
                null == intent ? null : intent.getStringExtra("locale");

        final Bundle arguments = getArguments();
        final String localeFromArguments =
                null == arguments ? null : arguments.getString("locale");

        final String locale;
        if (null != localeFromArguments) {
            locale = localeFromArguments;
        } else if (null != localeFromIntent) {
            locale = localeFromIntent;
        } else {
            locale = null;
        }

        mLocale = locale;
        mCursor = createCursor(locale);
        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(R.string.user_dict_settings_empty_text);

        final ListView listView = getListView();
        listView.setAdapter(createAdapter());
        listView.setFastScrollEnabled(true);
        listView.setEmptyView(emptyView);

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mDialogEditingWord = savedInstanceState.getString(INSTANCE_KEY_DIALOG_EDITING_WORD);
            mAddedWordAlready = savedInstanceState.getBoolean(INSTANCE_KEY_ADDED_WORD, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final Intent intent = getActivity().getIntent();
        if (!mAddedWordAlready
                && intent.getAction().equals("com.android.settings.USER_DICTIONARY_INSERT")) {
            final String word = intent.getStringExtra(EXTRA_WORD);
            mAutoReturn = true;
            if (word != null) {
                showAddOrEditDialog(word);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(INSTANCE_KEY_DIALOG_EDITING_WORD, mDialogEditingWord);
        outState.putBoolean(INSTANCE_KEY_ADDED_WORD, mAddedWordAlready);
    }

    private Cursor createCursor(final String locale) {
        // Locale can be any of:
        // - The string representation of a locale, as returned by Locale#toString()
        // - The empty string. This means we want a cursor returning words valid for all locales.
        // - null. This means we want a cursor for the current locale, whatever this is.
        // Note that this contrasts with the data inside the database, where NULL means "all
        // locales" and there should never be an empty string. The confusion is called by the
        // historical use of null for "all locales".
        // TODO: it should be easy to make this more readable by making the special values
        // human-readable, like "all_locales" and "current_locales" strings, provided they
        // can be guaranteed not to match locales that may exist.
        if ("".equals(locale)) {
            // Case-insensitive sort
            return getActivity().managedQuery(UserDictionary.Words.CONTENT_URI, QUERY_PROJECTION,
                    QUERY_SELECTION_ALL_LOCALES, null,
                    "UPPER(" + UserDictionary.Words.WORD + ")");
        } else {
            final String queryLocale = null != locale ? locale : Locale.getDefault().toString();
            return getActivity().managedQuery(UserDictionary.Words.CONTENT_URI, QUERY_PROJECTION,
                    QUERY_SELECTION, new String[] { queryLocale },
                    "UPPER(" + UserDictionary.Words.WORD + ")");
        }
    }

    private ListAdapter createAdapter() {
        return new MyAdapter(getActivity(),
                R.layout.user_dictionary_item, mCursor,
                new String[] { UserDictionary.Words.WORD, UserDictionary.Words._ID },
                new int[] { android.R.id.text1, R.id.delete_button }, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String word = getWord(position);
        if (word != null) {
            showAddOrEditDialog(word);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem actionItem =
                menu.add(0, OPTIONS_MENU_ADD, 0, R.string.user_dict_settings_add_menu_title)
                .setIcon(R.drawable.ic_menu_add);
        actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        showAddOrEditDialog(null);
        return true;
    }

    private void showAddOrEditDialog(String editingWord) {
        mDialogEditingWord = editingWord;
        showDialog(DIALOG_ADD_OR_EDIT);
    }

    private String getWord(int position) {
        if (null == mCursor) return null;
        mCursor.moveToPosition(position);
        // Handle a possible race-condition
        if (mCursor.isAfterLast()) return null;

        return mCursor.getString(
                mCursor.getColumnIndexOrThrow(UserDictionary.Words.WORD));
    }

    @Override
    public Dialog onCreateDialog(int id) {
        final Activity activity = getActivity();
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        final LayoutInflater inflater = LayoutInflater.from(dialogBuilder.getContext());
        final View content = inflater.inflate(R.layout.dialog_edittext, null);
        final EditText editText = (EditText) content.findViewById(R.id.edittext);
        editText.setText(mDialogEditingWord);
        // No prediction in soft keyboard mode. TODO: Create a better way to disable prediction
        editText.setInputType(InputType.TYPE_CLASS_TEXT 
                | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

        AlertDialog dialog = dialogBuilder
                .setTitle(mDialogEditingWord != null 
                        ? R.string.user_dict_settings_edit_dialog_title 
                        : R.string.user_dict_settings_add_dialog_title)
                .setView(content)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onAddOrEditFinished(editText.getText().toString());
                        if (mAutoReturn) activity.onBackPressed();
                    }})
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mAutoReturn) activity.onBackPressed();
                    }})
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    private void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    private void onAddOrEditFinished(String word) {
        if (mDialogEditingWord != null) {
            // The user was editing a word, so do a delete/add
            deleteWord(mDialogEditingWord);
        }

        // Disallow duplicates
        deleteWord(word);

        // TODO: present UI for picking whether to add word to all locales, or current.
        if (null == mLocale) {
            // Null means insert with the default system locale.
            UserDictionary.Words.addWord(getActivity(), word.toString(),
                    FREQUENCY_FOR_USER_DICTIONARY_ADDS, UserDictionary.Words.LOCALE_TYPE_CURRENT);
        } else if ("".equals(mLocale)) {
            // Empty string means insert for all languages.
            UserDictionary.Words.addWord(getActivity(), word.toString(),
                    FREQUENCY_FOR_USER_DICTIONARY_ADDS, UserDictionary.Words.LOCALE_TYPE_ALL);
        } else {
            // TODO: fix the framework so that it can accept a locale when we add a word
            // to the user dictionary instead of querying the system locale.
            final Locale prevLocale = Locale.getDefault();
            Locale.setDefault(Utils.createLocaleFromString(mLocale));
            UserDictionary.Words.addWord(getActivity(), word.toString(),
                    FREQUENCY_FOR_USER_DICTIONARY_ADDS, UserDictionary.Words.LOCALE_TYPE_CURRENT);
            Locale.setDefault(prevLocale);
        }
        if (null != mCursor && !mCursor.requery()) {
            throw new IllegalStateException("can't requery on already-closed cursor.");
        }
        mAddedWordAlready = true;
    }

    private void deleteWord(String word) {
        getActivity().getContentResolver().delete(
                UserDictionary.Words.CONTENT_URI, DELETE_SELECTION, new String[] { word });
    }

    private static class MyAdapter extends SimpleCursorAdapter implements SectionIndexer,
            View.OnClickListener {

        private AlphabetIndexer mIndexer;
        private UserDictionarySettings mSettings;

        private ViewBinder mViewBinder = new ViewBinder() {

            public boolean setViewValue(View v, Cursor c, int columnIndex) {
                if (v instanceof ImageView && columnIndex == INDEX_ID) {
                    v.setOnClickListener(MyAdapter.this);
                    v.setTag(c.getString(INDEX_WORD));
                    return true;
                }

                return false;
            }
        };

        public MyAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
                UserDictionarySettings settings) {
            super(context, layout, c, from, to);

            mSettings = settings;
            if (null != c) {
                final String alphabet = context.getString(
                        com.android.internal.R.string.fast_scroll_alphabet);
                final int wordColIndex = c.getColumnIndexOrThrow(UserDictionary.Words.WORD);
                mIndexer = new AlphabetIndexer(c, wordColIndex, alphabet);
            }
            setViewBinder(mViewBinder);
        }

        public int getPositionForSection(int section) {
            return null == mIndexer ? 0 : mIndexer.getPositionForSection(section);
        }

        public int getSectionForPosition(int position) {
            return null == mIndexer ? 0 : mIndexer.getSectionForPosition(position);
        }

        public Object[] getSections() {
            return null == mIndexer ? null : mIndexer.getSections();
        }

        public void onClick(View v) {
            mSettings.deleteWord((String) v.getTag());
        }
    }
}
