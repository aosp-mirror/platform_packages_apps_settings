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

import com.android.settings.SettingsPreferenceFragment.SettingsDialogFragment;

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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AlphabetIndexer;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.Locale;

public class UserDictionarySettings extends ListFragment implements DialogCreatable {
    private static final String TAG = "UserDictionarySettings";

    private static final String INSTANCE_KEY_DIALOG_EDITING_WORD = "DIALOG_EDITING_WORD";
    private static final String INSTANCE_KEY_ADDED_WORD = "DIALOG_ADDED_WORD";

    private static final String[] QUERY_PROJECTION = {
        UserDictionary.Words._ID, UserDictionary.Words.WORD
    };
    
    // Either the locale is empty (means the word is applicable to all locales)
    // or the word equals our current locale
    private static final String QUERY_SELECTION = UserDictionary.Words.LOCALE + "=? OR "
            + UserDictionary.Words.LOCALE + " is null";

    private static final String DELETE_SELECTION = UserDictionary.Words.WORD + "=?";

    private static final String EXTRA_WORD = "word";
    
    private static final int CONTEXT_MENU_EDIT = Menu.FIRST;
    private static final int CONTEXT_MENU_DELETE = Menu.FIRST + 1;
    
    private static final int OPTIONS_MENU_ADD = Menu.FIRST;

    private static final int DIALOG_ADD_OR_EDIT = 0;
    
    /** The word being edited in the dialog (null means the user is adding a word). */
    private String mDialogEditingWord;

    private View mView;
    private Cursor mCursor;
    
    private boolean mAddedWordAlready;
    private boolean mAutoReturn;

    private SettingsDialogFragment mDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.list_content_with_empty_view, container, false);
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCursor = createCursor();
        TextView emptyView = (TextView)mView.findViewById(R.id.empty);
        emptyView.setText(R.string.user_dict_settings_empty_text);

        final ListView listView = getListView();
        listView.setAdapter(createAdapter());
        listView.setFastScrollEnabled(true);
        listView.setEmptyView(emptyView);

        registerForContextMenu(listView);
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

    private Cursor createCursor() {
        String currentLocale = Locale.getDefault().toString();
        // Case-insensitive sort
        return getActivity().managedQuery(UserDictionary.Words.CONTENT_URI, QUERY_PROJECTION,
                QUERY_SELECTION, new String[] { currentLocale },
                "UPPER(" + UserDictionary.Words.WORD + ")");
    }

    private ListAdapter createAdapter() {
        return new MyAdapter(getActivity(),
                android.R.layout.simple_list_item_1, mCursor,
                new String[] { UserDictionary.Words.WORD },
                new int[] { android.R.id.text1 });
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getActivity().openContextMenu(v);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (!(menuInfo instanceof AdapterContextMenuInfo)) return;
        
        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(getWord(adapterMenuInfo.position));
        menu.add(0, CONTEXT_MENU_EDIT, 0, 
                R.string.user_dict_settings_context_menu_edit_title);
        menu.add(0, CONTEXT_MENU_DELETE, 0, 
                R.string.user_dict_settings_context_menu_delete_title);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuInfo menuInfo = item.getMenuInfo();
        if (!(menuInfo instanceof AdapterContextMenuInfo)) return false;
        
        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        String word = getWord(adapterMenuInfo.position);
        if (word == null) return true;

        switch (item.getItemId()) {
            case CONTEXT_MENU_DELETE:
                deleteWord(word);
                return true;
                
            case CONTEXT_MENU_EDIT:
                showAddOrEditDialog(word);
                return true;
        }
        
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, OPTIONS_MENU_ADD, 0, R.string.user_dict_settings_add_menu_title)
                .setIcon(R.drawable.ic_menu_add);
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
        mCursor.moveToPosition(position);
        // Handle a possible race-condition
        if (mCursor.isAfterLast()) return null;

        return mCursor.getString(
                mCursor.getColumnIndexOrThrow(UserDictionary.Words.WORD));
    }

    @Override
    public Dialog onCreateDialog(int id) {
        final Activity activity = getActivity();
        final View content = activity.getLayoutInflater().inflate(R.layout.dialog_edittext, null);
        final EditText editText = (EditText) content.findViewById(R.id.edittext);
        editText.setText(mDialogEditingWord);
        // No prediction in soft keyboard mode. TODO: Create a better way to disable prediction
        editText.setInputType(InputType.TYPE_CLASS_TEXT 
                | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

        AlertDialog dialog =  new AlertDialog.Builder(activity)
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
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
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
        UserDictionary.Words.addWord(getActivity(), word.toString(),
                250, UserDictionary.Words.LOCALE_TYPE_ALL);
        if (!mCursor.requery()) {
            throw new IllegalStateException("can't requery on already-closed cursor.");
        }
        mAddedWordAlready = true;
    }

    private void deleteWord(String word) {
        getActivity().getContentResolver().delete(
                UserDictionary.Words.CONTENT_URI, DELETE_SELECTION, new String[] { word });
    }

    private static class MyAdapter extends SimpleCursorAdapter implements SectionIndexer {
        private AlphabetIndexer mIndexer;        
        
        public MyAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);

            int wordColIndex = c.getColumnIndexOrThrow(UserDictionary.Words.WORD);
            String alphabet = context.getString(com.android.internal.R.string.fast_scroll_alphabet);
            mIndexer = new AlphabetIndexer(c, wordColIndex, alphabet); 
        }

        public int getPositionForSection(int section) {
            return mIndexer.getPositionForSection(section);
        }

        public int getSectionForPosition(int position) {
            return mIndexer.getSectionForPosition(position);
        }

        public Object[] getSections() {
            return mIndexer.getSections();
        }
    }
}
