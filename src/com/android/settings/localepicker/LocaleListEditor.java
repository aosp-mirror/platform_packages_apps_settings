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

import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_APP_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Drag-and-drop editor for the user-ordered locale lists.
 */
@SearchIndexable
public class LocaleListEditor extends RestrictedSettingsFragment implements View.OnTouchListener {
    private static final String TAG = LocaleListEditor.class.getSimpleName();
    protected static final String INTENT_LOCALE_KEY = "localeInfo";
    private static final String CFGKEY_REMOVE_MODE = "localeRemoveMode";
    private static final String CFGKEY_REMOVE_DIALOG = "showingLocaleRemoveDialog";
    private static final String CFGKEY_ADD_LOCALE = "localeAdded";
    private static final int MENU_ID_REMOVE = Menu.FIRST + 1;
    private static final int REQUEST_LOCALE_PICKER = 0;

    private static final String INDEX_KEY_ADD_LANGUAGE = "add_language";
    private static final String KEY_LANGUAGES_PICKER = "languages_picker";
    private static final String TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT = "dialog_confirm_system_default";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";
    static final String EXTRA_SYSTEM_LOCALE_DIALOG_TYPE = "system_locale_dialog_type";
    private static final String LOCALE_SUGGESTION = "locale_suggestion";

    private LocaleDragAndDropAdapter mAdapter;
    private Menu mMenu;
    private View mAddLanguage;
    private AlertDialog mSuggestionDialog = null;
    private boolean mRemoveMode;
    private boolean mShowingRemoveDialog;
    private boolean mLocaleAdditionMode = false;
    private boolean mIsUiRestricted;

    private LayoutPreference mLocalePickerPreference;
    private LocaleHelperPreferenceController mLocaleHelperPreferenceController;
    private FragmentManager mFragmentManager;

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

        addPreferencesFromResource(R.xml.languages);
        final Activity activity = getActivity();
        activity.setTitle(R.string.language_picker_title);
        mLocaleHelperPreferenceController = new LocaleHelperPreferenceController(activity);
        final PreferenceScreen screen = getPreferenceScreen();
        mLocalePickerPreference = screen.findPreference(KEY_LANGUAGES_PICKER);
        mLocaleHelperPreferenceController.displayPreference(screen);

        LocaleStore.fillCache(this.getContext());
        final List<LocaleStore.LocaleInfo> feedsList = getUserLocaleList();
        mAdapter = new LocaleDragAndDropAdapter(this, feedsList);
        mFragmentManager = getChildFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstState) {
        configureDragAndDrop(mLocalePickerPreference);
        return super.onCreateView(inflater, container, savedInstState);
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
            mLocaleAdditionMode = savedInstanceState.getBoolean(CFGKEY_ADD_LOCALE, false);
        }
        setRemoveMode(mRemoveMode);

        final LocaleDialogFragment dialogFragment =
                (LocaleDialogFragment) mFragmentManager.findFragmentByTag(
                        TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT);
        boolean isDialogShowing = false;
        if (dialogFragment != null && dialogFragment.isAdded()) {
            isDialogShowing = true;
        }
        mAdapter.restoreState(savedInstanceState, isDialogShowing);

        if (mShowingRemoveDialog) {
            showRemoveLocaleWarningDialog();
        }
        if (shouldShowConfirmationDialog() && !mLocaleAdditionMode) {
            getActivity().setResult(Activity.RESULT_OK);
            showDialogForAddedLocale();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CFGKEY_REMOVE_MODE, mRemoveMode);
        outState.putBoolean(CFGKEY_REMOVE_DIALOG, mShowingRemoveDialog);
        outState.putBoolean(CFGKEY_ADD_LOCALE, mLocaleAdditionMode);
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
        LocaleStore.LocaleInfo localeInfo;
        if (requestCode == REQUEST_LOCALE_PICKER && resultCode == Activity.RESULT_OK
                && data != null) {
            localeInfo = (LocaleStore.LocaleInfo) data.getSerializableExtra(INTENT_LOCALE_KEY);
            String preferencesTags = Settings.System.getString(
                    getContext().getContentResolver(), Settings.System.LOCALE_PREFERENCES);

            mAdapter.addLocale(mayAppendUnicodeTags(localeInfo, preferencesTags));
            updateVisibilityOfRemoveMenu();
        } else if (requestCode == DIALOG_CONFIRM_SYSTEM_DEFAULT) {
            localeInfo = mAdapter.getFeedItemList().get(0);
            if (resultCode == Activity.RESULT_OK) {
                mAdapter.doTheUpdate();
                if (!localeInfo.isTranslated()) {
                    Bundle args = new Bundle();
                    args.putInt(LocaleDialogFragment.ARG_DIALOG_TYPE,
                            LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE);
                    args.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, localeInfo);
                    LocaleDialogFragment localeDialogFragment = LocaleDialogFragment.newInstance();
                    localeDialogFragment.setArguments(args);
                    localeDialogFragment.show(mFragmentManager, TAG_DIALOG_NOT_AVAILABLE);
                }
            } else {
                mAdapter.notifyListChanged(localeInfo);
            }
            mAdapter.setCacheItemList();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSuggestionDialog != null) {
            mSuggestionDialog.dismiss();
        }
    }

    @VisibleForTesting
    static LocaleStore.LocaleInfo mayAppendUnicodeTags(
            LocaleStore.LocaleInfo localeInfo, String recordTags) {
        if (TextUtils.isEmpty(recordTags) || TextUtils.equals("und", recordTags)) {
            // No recorded tag, return inputted LocaleInfo.
            return localeInfo;
        }
        Locale recordLocale = Locale.forLanguageTag(recordTags);
        Locale.Builder builder = new Locale.Builder()
                .setLocale(localeInfo.getLocale());
        recordLocale.getUnicodeLocaleKeys().forEach(key ->
                builder.setUnicodeLocaleKeyword(key, recordLocale.getUnicodeLocaleType(key)));
        LocaleStore.LocaleInfo newLocaleInfo = LocaleStore.fromLocale(builder.build());
        newLocaleInfo.setTranslated(localeInfo.isTranslated());
        return newLocaleInfo;
    }

    private void setRemoveMode(boolean mRemoveMode) {
        this.mRemoveMode = mRemoveMode;
        mAdapter.setRemoveMode(mRemoveMode);
        mAddLanguage.setVisibility(mRemoveMode ? View.INVISIBLE : View.VISIBLE);
        updateVisibilityOfRemoveMenu();
    }

    private boolean shouldShowConfirmationDialog() {
        Intent intent = this.getIntent();
        String dialogType = intent.getStringExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE);
        String localeTag = intent.getStringExtra(EXTRA_APP_LOCALE);
        if (!isAllowedPackage()
                || isNullOrEmpty(dialogType)
                || isNullOrEmpty(localeTag)
                || !LOCALE_SUGGESTION.equals(dialogType)
                || !isValidLocale(localeTag)
                || isInSystemLocale(localeTag)) {
            getActivity().setResult(Activity.RESULT_CANCELED);
            return false;
        }
        getActivity().setResult(Activity.RESULT_OK);
        return true;
    }

    private boolean isAllowedPackage() {
        List<String> allowList = Arrays.asList(getContext().getResources().getStringArray(
                R.array.allowed_packages_for_locale_confirmation_diallog));
        String callingPackage = getActivity().getCallingPackage();
        return !isNullOrEmpty(callingPackage) && allowList.contains(callingPackage);
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private boolean isValidLocale(String tag) {
        String[] systemLocales = getSupportedLocales();
        for (String systemTag : systemLocales) {
            if (systemTag.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    protected String[] getSupportedLocales() {
        return LocalePicker.getSupportedLocales(getContext());
    }

    /**
     *  Check if the localeTag is in the system locale. Since in the current design, the system
     *  language list would not show two locales with the same language and region but different
     *  numbering system. So, during the comparison, the u extension has to be stripped out.
     *
     * @param languageTag A language tag
     * @return true if the locale is in the system locale. Otherwise, false.
     */
    private boolean isInSystemLocale(String languageTag) {
        LocaleList systemLocales = LocaleList.getDefault();
        Locale locale = Locale.forLanguageTag(languageTag).stripExtensions();
        for (int i = 0; i < systemLocales.size(); i++) {
            if (systemLocales.get(i).stripExtensions().equals(locale)) {
                return true;
            }
        }
        return false;
    }

    private void showDialogForAddedLocale() {
        Intent intent = this.getIntent();
        String dialogType = intent.getStringExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE);
        String appLocaleTag = intent.getStringExtra(EXTRA_APP_LOCALE);
        Log.d(TAG, "Dialog suggested locale: " + appLocaleTag);
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(
                Locale.forLanguageTag(appLocaleTag));
        if (LOCALE_SUGGESTION.equals(dialogType)) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            customizeLayout(dialogBuilder, localeInfo.getFullNameNative());
            dialogBuilder
                    .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mLocaleAdditionMode = true;
                            String preferencesTags = Settings.System.getString(
                                    getContext().getContentResolver(),
                                    Settings.System.LOCALE_PREFERENCES);
                            mAdapter.addLocale(mayAppendUnicodeTags(localeInfo, preferencesTags));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mLocaleAdditionMode = true;
                                }
                            });
            mSuggestionDialog = dialogBuilder.create();
            mSuggestionDialog.show();
        } else {
            Log.d(TAG, "Invalid parameter, dialogType:" + dialogType);
        }
    }

    private void customizeLayout(AlertDialog.Builder dialogBuilder, String language) {
        View dialogView = getLocaleDialogView();
        dialogBuilder.setView(dialogView);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText(
                String.format(getContext().getResources().getString(
                        R.string.title_system_locale_addition), language));
        TextView message = dialogView.findViewById(R.id.dialog_msg);
        message.setText(R.string.desc_system_locale_addition);
    }

    protected View getLocaleDialogView() {
        LayoutInflater inflater = this.getLayoutInflater();
        return inflater.inflate(R.layout.locale_dialog, null);
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
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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

        final String title = StringUtil.getIcuPluralsString(getContext(), checkedCount,
                R.string.dlg_remove_locales_title);
        mShowingRemoveDialog = true;

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (mAdapter.isFirstLocaleChecked()) {
            builder.setMessage(R.string.dlg_remove_locales_message);
        }

        builder.setTitle(title)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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

    private void configureDragAndDrop(LayoutPreference layout) {
        final RecyclerView list = layout.findViewById(R.id.dragList);
        final LocaleLinearLayoutManager llm = new LocaleLinearLayoutManager(getContext(), mAdapter);
        llm.setAutoMeasureEnabled(true);
        list.setLayoutManager(llm);

        list.setHasFixedSize(true);
        list.setNestedScrollingEnabled(false);
        mAdapter.setRecyclerView(list);
        list.setAdapter(mAdapter);
        list.setOnTouchListener(this);

        mAddLanguage = layout.findViewById(R.id.add_language);
        mAddLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider()
                        .logSettingsTileClick(INDEX_KEY_ADD_LANGUAGE, getMetricsCategory());

                final Intent intent = new Intent(getActivity(),
                        LocalePickerWithRegionActivity.class);
                intent.putExtras(getActivity().getIntent().getExtras());
                startActivityForResult(intent, REQUEST_LOCALE_PICKER);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            LocaleStore.LocaleInfo localeInfo = mAdapter.getFeedItemList().get(0);
            if (!localeInfo.getLocale().equals(LocalePicker.getLocales().get(0))) {
                final LocaleDialogFragment localeDialogFragment =
                        LocaleDialogFragment.newInstance();
                Bundle args = new Bundle();
                args.putInt(LocaleDialogFragment.ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
                args.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, localeInfo);
                localeDialogFragment.setArguments(args);
                localeDialogFragment.show(mFragmentManager, TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT);
            } else {
                mAdapter.doTheUpdate();
            }
        }
        return false;
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
