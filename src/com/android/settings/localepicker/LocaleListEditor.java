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

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LocaleList;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Drag-and-drop editor for the user-ordered locale lists.
 */
public class LocaleListEditor extends SettingsPreferenceFragment
        implements LocalePickerWithRegion.LocaleSelectedListener {

    private static final int MENU_ID_REMOVE = Menu.FIRST + 1;

    private LocaleDragAndDropAdapter mAdapter;
    private Menu mMenu;
    private boolean mRemoveMode;
    private View mAddLanguage;

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.USER_LOCALE_LIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        LocaleStore.fillCache(this.getContext());
        final List<LocaleStore.LocaleInfo> feedsList = getUserLocaleList(this.getContext());
        mAdapter = new LocaleDragAndDropAdapter(this.getContext(), feedsList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstState) {
        View result = super.onCreateView(inflater, container, savedInstState);
        LinearLayout ll = (LinearLayout) result;
        View myLayout = inflater.inflate(R.layout.locale_order_list, ll);

        getActivity().setTitle(R.string.pref_title_lang_selection);

        configureDragAndDrop(myLayout);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == MENU_ID_REMOVE) {
            if (mRemoveMode) {
                removeLocaleWarningDialog();
            } else {
                setRemoveMode(true);
            }
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void setRemoveMode(boolean mRemoveMode) {
        this.mRemoveMode = mRemoveMode;
        mAdapter.setRemoveMode(mRemoveMode);
        mMenu.findItem(MENU_ID_REMOVE).setShowAsAction(
                mRemoveMode ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
        mAddLanguage.setVisibility(mRemoveMode ? View.INVISIBLE : View.VISIBLE);
    }

    private void removeLocaleWarningDialog() {
        int checkedCount = mAdapter.getCheckedCount();

        // Nothing checked, just exit remove mode without a warning dialog
        if (checkedCount == 0) {
            setRemoveMode(!mRemoveMode);
            return;
        }

        // All locales selected, warning dialog, can't remove them all
        if (checkedCount == mAdapter.getItemCount()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dlg_remove_locales_error_title)
                    .setMessage(R.string.dlg_remove_locales_error_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create()
                    .show();
            return;
        }

        final String title = getResources().getQuantityString(R.plurals.dlg_remove_locales_title,
                checkedCount);
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(R.string.dlg_remove_locales_message)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setRemoveMode(!mRemoveMode);
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAdapter.removeChecked();
                        setRemoveMode(!mRemoveMode);
                        updateVisibilityOfRemoveMenu();
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

    private static List<LocaleStore.LocaleInfo> getUserLocaleList(Context context) {
        final List<LocaleStore.LocaleInfo> result = new ArrayList<>();

        final LocaleList localeList = LocalePicker.getLocales();
        for (int i = 0; i < localeList.size(); i++) {
            Locale locale = localeList.get(i);
            result.add(LocaleStore.getLocaleInfo(locale));
        }

        return result;
    }

    private void configureDragAndDrop(View view) {
        final RecyclerView list = (RecyclerView) view.findViewById(R.id.dragList);
        final LinearLayoutManager llm = new LinearLayoutManager(this.getContext());
        llm.setAutoMeasureEnabled(true);
        list.setLayoutManager(llm);

        list.setHasFixedSize(true);
        mAdapter.setRecyclerView(list);
        list.setAdapter(mAdapter);

        mAddLanguage = view.findViewById(R.id.add_language);
        mAddLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final LocalePickerWithRegion selector = LocalePickerWithRegion.createLanguagePicker(
                        getContext(), LocaleListEditor.this, false /* translate only */);
                getFragmentManager()
                        .beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(getId(), selector)
                        .addToBackStack("localeListEditor")
                        .commit();
            }
        });
    }

    @Override
    public void onLocaleSelected(LocaleStore.LocaleInfo locale) {
        mAdapter.addLocale(locale);
        updateVisibilityOfRemoveMenu();
    }

    // Hide the "Remove" menu if there is only one locale in the list, show it otherwise
    // This is called when the menu is first created, and then one add / remove locale
    private void updateVisibilityOfRemoveMenu() {
        final MenuItem menuItemRemove = mMenu.findItem(MENU_ID_REMOVE);
        if (menuItemRemove != null) {
            menuItemRemove.setVisible(mAdapter.getItemCount() > 1);
        }
    }
}
