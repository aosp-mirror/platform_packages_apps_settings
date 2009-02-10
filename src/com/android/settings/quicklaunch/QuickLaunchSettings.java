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

package com.android.settings.quicklaunch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings.Bookmarks;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;

import com.android.settings.R;

/**
 * Settings activity for quick launch.
 * <p>
 * Shows a list of possible shortcuts, the current application each is bound to,
 * and allows choosing a new bookmark for a shortcut.
 */
public class QuickLaunchSettings extends PreferenceActivity implements
        AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    private static final String TAG = "QuickLaunchSettings";

    private static final String KEY_SHORTCUT_CATEGORY = "shortcut_category";

    private static final int DIALOG_CLEAR_SHORTCUT = 0;

    private static final int REQUEST_PICK_BOOKMARK = 1;

    private static final int COLUMN_SHORTCUT = 0;
    private static final int COLUMN_TITLE = 1;
    private static final int COLUMN_INTENT = 2;
    private static final String[] sProjection = new String[] {
            Bookmarks.SHORTCUT, Bookmarks.TITLE, Bookmarks.INTENT
    };
    private static final String sShortcutSelection = Bookmarks.SHORTCUT + "=?";
    
    private Handler mUiHandler = new Handler();
    
    private static final String DEFAULT_BOOKMARK_FOLDER = "@quicklaunch";
    /** Cursor for Bookmarks provider. */
    private Cursor mBookmarksCursor;
    /** Listens for changes to Bookmarks provider. */
    private BookmarksObserver mBookmarksObserver;
    /** Used to keep track of which shortcuts have bookmarks. */
    private SparseBooleanArray mBookmarkedShortcuts;
    
    /** Preference category to hold the shortcut preferences. */
    private PreferenceGroup mShortcutGroup;
    /** Mapping of a shortcut to its preference. */
    private SparseArray<ShortcutPreference> mShortcutToPreference;

    /** The bookmark title of the shortcut that is being cleared. */
    private CharSequence mClearDialogBookmarkTitle;
    private static final String CLEAR_DIALOG_BOOKMARK_TITLE = "CLEAR_DIALOG_BOOKMARK_TITLE";
    /** The shortcut that is being cleared. */
    private char mClearDialogShortcut;
    private static final String CLEAR_DIALOG_SHORTCUT = "CLEAR_DIALOG_SHORTCUT";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.quick_launch_settings);
        
        mShortcutGroup = (PreferenceGroup) findPreference(KEY_SHORTCUT_CATEGORY);
        mShortcutToPreference = new SparseArray<ShortcutPreference>();
        mBookmarksObserver = new BookmarksObserver(mUiHandler);
        initShortcutPreferences();
        mBookmarksCursor = managedQuery(Bookmarks.CONTENT_URI, sProjection, null, null);
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(Bookmarks.CONTENT_URI, true,
                mBookmarksObserver);
        refreshShortcuts();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mBookmarksObserver);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        
        // Restore the clear dialog's info
        mClearDialogBookmarkTitle = state.getString(CLEAR_DIALOG_BOOKMARK_TITLE);
        mClearDialogShortcut = (char) state.getInt(CLEAR_DIALOG_SHORTCUT, 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        // Save the clear dialog's info
        outState.putCharSequence(CLEAR_DIALOG_BOOKMARK_TITLE, mClearDialogBookmarkTitle);
        outState.putInt(CLEAR_DIALOG_SHORTCUT, mClearDialogShortcut);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            
            case DIALOG_CLEAR_SHORTCUT: {
                // Create the dialog for clearing a shortcut
                return new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.quick_launch_clear_dialog_title))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(getString(R.string.quick_launch_clear_dialog_message,
                                mClearDialogShortcut, mClearDialogBookmarkTitle))
                        .setPositiveButton(R.string.quick_launch_clear_ok_button, this)
                        .setNegativeButton(R.string.quick_launch_clear_cancel_button, this)
                        .create();
            }
        }
        
        return super.onCreateDialog(id);
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            
            case DIALOG_CLEAR_SHORTCUT: {
                AlertDialog alertDialog = (AlertDialog) dialog;
                alertDialog.setMessage(getString(R.string.quick_launch_clear_dialog_message,
                        mClearDialogShortcut, mClearDialogBookmarkTitle));
            }
        }
    }

    private void showClearDialog(ShortcutPreference pref) {

        if (!pref.hasBookmark()) return;
        
        mClearDialogBookmarkTitle = pref.getTitle();
        mClearDialogShortcut = pref.getShortcut();
        showDialog(DIALOG_CLEAR_SHORTCUT);
    }
    
    public void onClick(DialogInterface dialog, int which) {
        if (mClearDialogShortcut > 0 && which == AlertDialog.BUTTON1) {
            // Clear the shortcut
            clearShortcut(mClearDialogShortcut);
        }
        mClearDialogBookmarkTitle = null;
        mClearDialogShortcut = 0;
    }

    private void clearShortcut(char shortcut) {
        getContentResolver().delete(Bookmarks.CONTENT_URI, sShortcutSelection,
                new String[] { String.valueOf((int) shortcut) });
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!(preference instanceof ShortcutPreference)) return false;

        // Open the screen to pick a bookmark for this shortcut
        ShortcutPreference pref = (ShortcutPreference) preference;
        Intent intent = new Intent(this, BookmarkPicker.class);
        intent.putExtra(BookmarkPicker.EXTRA_SHORTCUT, pref.getShortcut());
        startActivityForResult(intent, REQUEST_PICK_BOOKMARK);
        
        return true;
    }

    public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
        
        // Open the clear shortcut dialog
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(position);
        if (!(pref instanceof ShortcutPreference)) return false;
        showClearDialog((ShortcutPreference) pref);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_PICK_BOOKMARK) {
            
            // Returned from the 'pick bookmark for this shortcut' screen
            if (data == null) {
                Log.w(TAG, "Result from bookmark picker does not have an intent.");
                return;
            }
            
            String title = data.getStringExtra(BookmarkPicker.EXTRA_TITLE);
            char shortcut = data.getCharExtra(BookmarkPicker.EXTRA_SHORTCUT, (char) 0);
            updateShortcut(shortcut, data);
            
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateShortcut(char shortcut, Intent intent) {
        // Update the bookmark for a shortcut
        // Pass an empty title so it gets resolved each time this bookmark is
        // displayed (since the locale could change after we insert into the provider).
        Bookmarks.add(getContentResolver(), intent, "", DEFAULT_BOOKMARK_FOLDER, shortcut, 0);
    }
    
    private ShortcutPreference getOrCreatePreference(char shortcut) {
        ShortcutPreference pref = mShortcutToPreference.get(shortcut);
        if (pref != null) {
            return pref;
        } else {
            Log.w(TAG, "Unknown shortcut '" + shortcut + "', creating preference anyway");
            return createPreference(shortcut);
        }
    }
    
    private ShortcutPreference createPreference(char shortcut) {
        ShortcutPreference pref = new ShortcutPreference(QuickLaunchSettings.this, shortcut);
        mShortcutGroup.addPreference(pref);
        mShortcutToPreference.put(shortcut, pref);
        return pref;
    }

    private void initShortcutPreferences() {
        
        /** Whether the shortcut has been seen already.  The array index is the shortcut. */
        SparseBooleanArray shortcutSeen = new SparseBooleanArray();
        KeyCharacterMap keyMap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);

        // Go through all the key codes and create a preference for the appropriate keys
        for (int keyCode = KeyEvent.getMaxKeyCode() - 1; keyCode >= 0; keyCode--) {
            // Get the label for the primary char on the key that produces this key code
            char shortcut = (char) Character.toLowerCase(keyMap.getDisplayLabel(keyCode));
            if (shortcut == 0 || shortcutSeen.get(shortcut, false)) continue;
            // TODO: need a to tell if the current keyboard can produce this key code, for now
            // only allow the letter or digits
            if (!Character.isLetterOrDigit(shortcut)) continue;
            shortcutSeen.put(shortcut, true);
            
            createPreference(shortcut);
        }
    }
    
    private synchronized void refreshShortcuts() {
        Cursor c = mBookmarksCursor;
        if (c == null) {
            // Haven't finished querying yet
            return;
        }
        
        if (!c.requery()) {
            Log.e(TAG, "Could not requery cursor when refreshing shortcuts.");
            return;
        }
        
        /**
         * We use the previous bookmarked shortcuts array to filter out those
         * shortcuts that had bookmarks before this method call, and don't after
         * (so we can set the preferences to be without bookmarks).
         */
        SparseBooleanArray noLongerBookmarkedShortcuts = mBookmarkedShortcuts;
        SparseBooleanArray newBookmarkedShortcuts = new SparseBooleanArray(); 
        while (c.moveToNext()) {
            char shortcut = Character.toLowerCase((char) c.getInt(COLUMN_SHORTCUT));
            if (shortcut == 0) continue;
            
            ShortcutPreference pref = getOrCreatePreference(shortcut);
            pref.setTitle(Bookmarks.getTitle(this, c));
            pref.setSummary(getString(R.string.quick_launch_shortcut,
                    String.valueOf(shortcut)));
            pref.setHasBookmark(true);
            
            newBookmarkedShortcuts.put(shortcut, true);
            if (noLongerBookmarkedShortcuts != null) {
                // After this loop, the shortcuts with value true in this array
                // will no longer have bookmarks
                noLongerBookmarkedShortcuts.put(shortcut, false);
            }
        }
        
        if (noLongerBookmarkedShortcuts != null) {
            for (int i = noLongerBookmarkedShortcuts.size() - 1; i >= 0; i--) {
                if (noLongerBookmarkedShortcuts.valueAt(i)) {
                    // True, so there is no longer a bookmark for this shortcut
                    char shortcut = (char) noLongerBookmarkedShortcuts.keyAt(i);
                    ShortcutPreference pref = mShortcutToPreference.get(shortcut);
                    if (pref != null) {
                        pref.setHasBookmark(false);
                    }
                }
            }
        }
        
        mBookmarkedShortcuts = newBookmarkedShortcuts;
        
        c.deactivate();
    }

    private class BookmarksObserver extends ContentObserver {

        public BookmarksObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            
            refreshShortcuts();
        }
    }
}
