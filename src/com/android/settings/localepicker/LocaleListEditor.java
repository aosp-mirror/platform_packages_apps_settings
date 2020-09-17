/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.localepicker;

import static android.os.UserManager.DISALLOW_CONFIG_LOCALE;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Drag-and-drop editor for the user-ordered locale lists.
 */
@SearchIndexable
public class LocaleListEditor extends RestrictedSettingsFragment {

    protected static final String INTENT_LOCALE_KEY = "localeInfo";
    private static final String CFGKEY_REMOVE_MODE = "localeRemoveMode";
    private static final String CFGKEY_REMOVE_DIALOG = "showingLocaleRemoveDialog";
    private static final int MENU_ID_REMOVE = Menu.FIRST + 1;
    private static final int REQUEST_LOCALE_PICKER = 0;

    private static final String INDEX_KEY_ADD_LANGUAGE = "add_language";

    private LocaleDragAndDropAdapter mAdapter;
    private Menu mMenu;
    private View mAddLanguage;
    private boolean mRemoveMode;
    private boolean mShowingRemoveDialog;
    private boolean mIsUiRestricted;

    public LocaleListEditor() {
        super(DISALLOW_CONFIG_LOCALE);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER_LOCALE_LIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        LocaleStore.fillCache(this.getContext());
        final List<LocaleStore.LocaleInfo> feedsList = getUserLocaleList();
        mAdapter = new LocaleDragAndDropAdapter(this.getContext(), feedsList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstState) {
        final View result = super.onCreateView(inflater, container, savedInstState);
        final View myLayout = inflater.inflate(R.layout.locale_order_list, (ViewGroup) result);

        configureDragAndDrop(myLayout);
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();

        final boolean previouslyRestricted = mIsUiRestricted;
        mIsUiRestricted = isUiRestricted();
        final TextView emptyView = getEmptyTextView();
        if (mIsUiRestricted && !previouslyRestricted) {
            // Lock it down.
            emptyView.setText(R.string.language_empty_list_user_restricted);
            emptyView.setVisibility(View.VISIBLE);
            updateVisibilityOfRemoveMenu();
        } else if (!mIsUiRestricted && previouslyRestricted) {
            // Unlock it.
            emptyView.setVisibility(View.GONE);
            updateVisibilityOfRemoveMenu();
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mRemoveMode = savedInstanceState.getBoolean(CFGKEY_REMOVE_MODE, false);
            mShowingRemoveDialog = savedInstanceState.getBoolean(CFGKEY_REMOVE_DIALOG, false);
        }
        setRemoveMode(mRemoveMode);
        mAdapter.restoreState(savedInstanceState);

        if (mShowingRemoveDialog) {
            showRemoveLocaleWarningDialog();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CFGKEY_REMOVE_MODE, mRemoveMode);
        outState.putBoolean(CFGKEY_REMOVE_DIALOG, mShowingRemoveDialog);
        mAdapter.saveState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case MENU_ID_REMOVE:
                if (mRemoveMode) {
                    showRemoveLocaleWarningDialog();
                } else {
                    setRemoveMode(true);
                }
                return true;
            case android.R.id.home:
                if (mRemoveMode) {
                    setRemoveMode(false);
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOCALE_PICKER && resultCode == Activity.RESULT_OK
                && data != null) {
            final LocaleStore.LocaleInfo locale =
                    (LocaleStore.LocaleInfo) data.getSerializableExtra(
                            INTENT_LOCALE_KEY);
            mAdapter.addLocale(locale);
            updateVisibilityOfRemoveMenu();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setRemoveMode(boolean mRemoveMode) {
        this.mRemoveMode = mRemoveMode;
        mAdapter.setRemoveMode(mRemoveMode);
        mAddLanguage.setVisibility(mRemoveMode ? View.INVISIBLE : View.VISIBLE);
        updateVisibilityOfRemoveMenu();
    }

    // Show the appropriate warning when the user tries to remove locales.
    // Shows no warning if there is no locale checked, shows a warning
    // about removing all the locales if all of them are checked, and
    // a "regular" warning otherwise.
    @VisibleForTesting
    void showRemoveLocaleWarningDialog() {
        int checkedCount = mAdapter.getCheckedCount();

        // Nothing checked, just exit remove mode without a warning dialog
        if (checkedCount == 0) {
            setRemoveMode(!mRemoveMode);
            return;
        }

        // All locales selected, warning dialog, can't remove them all
        if (checkedCount == mAdapter.getItemCount()) {
            mShowingRemoveDialog = true;
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dlg_remove_locales_error_title)
                    .setMessage(R.string.dlg_remove_locales_error_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            mShowingRemoveDialog = false;
                        }
                    })
                    .create()
                    .show();
            return;
        }

        final String title = getResources().getQuantityString(R.plurals.dlg_remove_locales_title,
                checkedCount);
        mShowingRemoveDialog = true;

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (mAdapter.isFirstLocaleChecked()) {
            builder.setMessage(R.string.dlg_remove_locales_message);
        }

        builder.setTitle(title)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setRemoveMode(false);
                    }
                })
                .setPositiveButton(R.string.locale_remove_menu,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // This is a sensitive area to change.
                                // removeChecked() triggers a system update and "kills" the frame.
                                // This means that saveState + restoreState are called before
                                // setRemoveMode is called.
                                // So we want that mRemoveMode and dialog status have the right
                                // values
                                // before that save.
                                // We can't just call setRemoveMode(false) before calling
                                // removeCheched
                                // because that unchecks all items and removeChecked would have
                                // nothing
                                // to remove.
                                mRemoveMode = false;
                                mShowingRemoveDialog = false;
                                mAdapter.removeChecked();
                                setRemoveMode(false);
                            }
                        })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mShowingRemoveDialog = false;
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final MenuItem menuItem =
                menu.add(Menu.NONE, MENU_ID_REMOVE, 0, R.string.locale_remove_menu);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menuItem.setIcon(R.drawable.ic_delete);
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
        updateVisibilityOfRemoveMenu();
    }

    private List<LocaleStore.LocaleInfo> getUserLocaleList() {
        final List<LocaleStore.LocaleInfo> result = new ArrayList<>();
        final LocaleList localeList = LocalePicker.getLocales();
        for (int i = 0; i < localeList.size(); i++) {
            Locale locale = localeList.get(i);
            result.add(LocaleStore.getLocaleInfo(locale));
        }
        return result;
    }

    private void configureDragAndDrop(View view) {
        final RecyclerView list = view.findViewById(R.id.dragList);
        final LocaleLinearLayoutManager llm = new LocaleLinearLayoutManager(getContext(), mAdapter);
        llm.setAutoMeasureEnabled(true);
        list.setLayoutManager(llm);

        list.setHasFixedSize(true);
        mAdapter.setRecyclerView(list);
        list.setAdapter(mAdapter);

        mAddLanguage = view.findViewById(R.id.add_language);
        mAddLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider()
                        .logSettingsTileClick(INDEX_KEY_ADD_LANGUAGE, getMetricsCategory());

                final Intent intent = new Intent(getActivity(),
                        LocalePickerWithRegionActivity.class);
                startActivityForResult(intent, REQUEST_LOCALE_PICKER);
            }
        });
    }

    // Hide the "Remove" menu if there is only one locale in the list, show it otherwise
    // This is called when the menu is first created, and then one add / remove locale
    private void updateVisibilityOfRemoveMenu() {
        if (mMenu == null) {
            return;
        }

        final MenuItem menuItemRemove = mMenu.findItem(MENU_ID_REMOVE);
        if (menuItemRemove != null) {
            menuItemRemove.setShowAsAction(
                    mRemoveMode ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            final boolean hasMultipleLanguages = mAdapter.getItemCount() > 1;
            menuItemRemove.setVisible(hasMultipleLanguages && !mIsUiRestricted);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final Resources res = context.getResources();
                    final List<SearchIndexableRaw> indexRaws = new ArrayList<>();
                    final SearchIndexableRaw raw = new SearchIndexableRaw(context);
                    raw.key = INDEX_KEY_ADD_LANGUAGE;
                    raw.title = res.getString(R.string.add_a_language);
                    raw.keywords = res.getString(R.string.keywords_add_language);
                    indexRaws.add(raw);
                    return indexRaws;
                }
            };
}
