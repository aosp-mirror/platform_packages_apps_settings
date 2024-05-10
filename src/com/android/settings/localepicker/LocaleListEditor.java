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

import static com.android.settings.flags.Flags.localeNotificationEnabled;
import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_APP_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_ADD_SYSTEM_LOCALE;
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
import java.util.List;
import java.util.Locale;

/**
 * Drag-and-drop editor for the user-ordered locale lists.
 */
@SearchIndexable
public class LocaleListEditor extends RestrictedSettingsFragment implements View.OnTouchListener {
    protected static final String INTENT_LOCALE_KEY = "localeInfo";
    protected static final String EXTRA_SYSTEM_LOCALE_DIALOG_TYPE = "system_locale_dialog_type";
    protected static final String LOCALE_SUGGESTION = "locale_suggestion";

    private static final String TAG = LocaleListEditor.class.getSimpleName();
    private static final String CFGKEY_REMOVE_MODE = "localeRemoveMode";
    private static final String CFGKEY_REMOVE_DIALOG = "showingLocaleRemoveDialog";
    private static final String CFGKEY_ADD_LOCALE = "localeAdded";
    private static final String INDEX_KEY_ADD_LANGUAGE = "add_language";
    private static final String KEY_LANGUAGES_PICKER = "languages_picker";
    private static final String KEY_CATEGORY_TERMS_OF_ADDRESS = "key_category_terms_of_address";
    private static final String TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT = "dialog_confirm_system_default";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";
    private static final String TAG_DIALOG_ADD_SYSTEM_LOCALE = "dialog_add_system_locale";
    private static final int MENU_ID_REMOVE = Menu.FIRST + 1;
    private static final int REQUEST_LOCALE_PICKER = 0;

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
    private TermsOfAddressCategoryController mTermsOfAddressCategoryController;

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
        mLocaleHelperPreferenceController = new LocaleHelperPreferenceController(activity);
        final PreferenceScreen screen = getPreferenceScreen();
        mLocalePickerPreference = screen.findPreference(KEY_LANGUAGES_PICKER);
        mLocaleHelperPreferenceController.displayPreference(screen);
        mTermsOfAddressCategoryController = new TermsOfAddressCategoryController(activity,
                KEY_CATEGORY_TERMS_OF_ADDRESS);
        mTermsOfAddressCategoryController.displayPreference(screen);

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
        Log.d(TAG, "LocaleAdditionMode:" + mLocaleAdditionMode);
        if (!mLocaleAdditionMode && shouldShowConfirmationDialog()) {
            showDialogForAddedLocale();
            mLocaleAdditionMode = true;
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
            localeInfo = mayAppendUnicodeTags(localeInfo, preferencesTags);
            mAdapter.addLocale(localeInfo);
            updateVisibilityOfRemoveMenu();
            mMetricsFeatureProvider.action(getContext(), SettingsEnums.ACTION_ADD_LANGUAGE);
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
                    mMetricsFeatureProvider.action(getContext(),
                            SettingsEnums.ACTION_NOT_SUPPORTED_SYSTEM_LANGUAGE);
                }
            } else {
                mAdapter.notifyListChanged(localeInfo);
            }
            mAdapter.setCacheItemList();
        } else if (requestCode == DIALOG_ADD_SYSTEM_LOCALE) {
            if (resultCode == Activity.RESULT_OK) {
                localeInfo = (LocaleStore.LocaleInfo) data.getExtras().getSerializable(
                        LocaleDialogFragment.ARG_TARGET_LOCALE);
                String preferencesTags = Settings.System.getString(
                        getContext().getContentResolver(),
                        Settings.System.LOCALE_PREFERENCES);
                mAdapter.addLocale(mayAppendUnicodeTags(localeInfo, preferencesTags));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        String callingPackage = getActivity().getCallingPackage();
        if (!localeNotificationEnabled()
                || !getContext().getPackageName().equals(callingPackage)
                || !isValidDialogType(dialogType)
                || !isValidLocale(localeTag)
                || LocaleUtils.isInSystemLocale(localeTag)) {
            return false;
        }
        return true;
    }

    @VisibleForTesting
    NotificationController getNotificationController() {
        return NotificationController.getInstance(getContext());
    }

    private boolean isValidDialogType(String type) {
        return LOCALE_SUGGESTION.equals(type);
    }

    private boolean isValidLocale(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return false;
        }
        String[] systemLocales = getSupportedLocales();
        for (String systemTag : systemLocales) {
            if (systemTag.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    String[] getSupportedLocales() {
        return LocalePicker.getSupportedLocales(getContext());
    }

    private void showDialogForAddedLocale() {
        Log.d(TAG, "show confirmation dialog");
        Intent intent = this.getIntent();
        String dialogType = intent.getStringExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE);
        String appLocaleTag = intent.getStringExtra(EXTRA_APP_LOCALE);

        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(
                Locale.forLanguageTag(appLocaleTag));
        final LocaleDialogFragment localeDialogFragment =
                LocaleDialogFragment.newInstance();
        Bundle args = new Bundle();
        args.putInt(LocaleDialogFragment.ARG_DIALOG_TYPE, DIALOG_ADD_SYSTEM_LOCALE);
        args.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, localeInfo);
        localeDialogFragment.setArguments(args);
        localeDialogFragment.show(mFragmentManager, TAG_DIALOG_ADD_SYSTEM_LOCALE);
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
                                LocaleStore.LocaleInfo firstLocale =
                                        mAdapter.getFeedItemList().get(0);
                                mAdapter.removeChecked();
                                boolean isFirstRemoved =
                                        firstLocale != mAdapter.getFeedItemList().get(0);
                                showConfirmDialog(isFirstRemoved, isFirstRemoved ? firstLocale
                                        : mAdapter.getFeedItemList().get(0));
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
        llm.setLocaleListEditor(this);
        llm.setAutoMeasureEnabled(true);
        list.setLayoutManager(llm);
        list.setHasFixedSize(true);
        list.setNestedScrollingEnabled(false);
        mAdapter.setRecyclerView(list);
        list.setAdapter(mAdapter);
        list.setOnTouchListener(this);
        list.requestFocus();

        mAddLanguage = layout.findViewById(R.id.add_language);
        mAddLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
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
            showConfirmDialog(false, mAdapter.getFeedItemList().get(0));
        }
        return false;
    }

    public void showConfirmDialog(boolean isFirstRemoved, LocaleStore.LocaleInfo localeInfo) {
        Locale currentSystemLocale = LocalePicker.getLocales().get(0);
        if (!localeInfo.getLocale().equals(currentSystemLocale)) {
            final LocaleDialogFragment localeDialogFragment =
                    LocaleDialogFragment.newInstance();
            Bundle args = new Bundle();
            args.putInt(LocaleDialogFragment.ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
            args.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE,
                    isFirstRemoved ? LocaleStore.getLocaleInfo(currentSystemLocale) : localeInfo);
            localeDialogFragment.setArguments(args);
            localeDialogFragment.show(mFragmentManager, TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT);
        } else {
            mAdapter.doTheUpdate();
        }
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
