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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.icu.text.CaseMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogType;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.utils.LocaleUtils;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;
import com.android.settingslib.widget.TopIntroPreference;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Base class for accessibility fragments with toggle, shortcut, some helper functions
 * and dialog management.
 */
public abstract class ToggleFeaturePreferenceFragment extends SettingsPreferenceFragment
        implements ShortcutPreference.OnClickCallback, OnMainSwitchChangeListener {

    protected TopIntroPreference mTopIntroPreference;
    protected SettingsMainSwitchPreference mToggleServiceSwitchPreference;
    protected ShortcutPreference mShortcutPreference;
    protected Preference mSettingsPreference;
    protected AccessibilityFooterPreferenceController mFooterPreferenceController;
    protected String mPreferenceKey;
    protected Dialog mDialog;

    protected CharSequence mSettingsTitle;
    protected Intent mSettingsIntent;
    // The mComponentName maybe null, such as Magnify
    protected ComponentName mComponentName;
    protected CharSequence mPackageName;
    protected Uri mImageUri;
    private CharSequence mDescription;
    protected CharSequence mHtmlDescription;
    protected CharSequence mTopIntroTitle;

    private static final String DRAWABLE_FOLDER = "drawable";
    protected static final String KEY_TOP_INTRO_PREFERENCE = "top_intro";
    protected static final String KEY_USE_SERVICE_PREFERENCE = "use_service";
    public static final String KEY_GENERAL_CATEGORY = "general_categories";
    protected static final String KEY_HTML_DESCRIPTION_PREFERENCE = "html_description";
    public static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    protected static final String KEY_SAVED_USER_SHORTCUT_TYPE = "shortcut_type";
    protected static final String KEY_SAVED_QS_TOOLTIP_RESHOW = "qs_tooltip_reshow";
    protected static final String KEY_SAVED_QS_TOOLTIP_TYPE = "qs_tooltip_type";
    protected static final String KEY_ANIMATED_IMAGE = "animated_image";

    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;
    private AccessibilitySettingsContentObserver mSettingsContentObserver;

    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;

    private AccessibilityQuickSettingsTooltipWindow mTooltipWindow;
    private boolean mNeedsQSTooltipReshow = false;
    private int mNeedsQSTooltipType = QuickSettingsTooltipType.GUIDE_TO_EDIT;

    public static final int NOT_SET = -1;
    // Save user's shortcutType value when savedInstance has value (e.g. device rotated).
    protected int mSavedCheckBoxValue = NOT_SET;
    private boolean mSavedAccessibilityFloatingMenuEnabled;

    // For html description of accessibility service, must follow the rule, such as
    // <img src="R.drawable.fileName"/>, a11y settings will get the resources successfully.
    private static final String IMG_PREFIX = "R.drawable.";

    private ImageView mImageGetterCacheView;

    private final Html.ImageGetter mImageGetter = (String str) -> {
        if (str != null && str.startsWith(IMG_PREFIX)) {
            final String fileName = str.substring(IMG_PREFIX.length());
            return getDrawableFromUri(Uri.parse(
                    ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                            + mComponentName.getPackageName() + "/" + DRAWABLE_FOLDER + "/"
                            + fileName));
        }
        return null;
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restore the user shortcut type and tooltip.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_SAVED_USER_SHORTCUT_TYPE)) {
                mSavedCheckBoxValue = savedInstanceState.getInt(KEY_SAVED_USER_SHORTCUT_TYPE,
                        NOT_SET);
            }
            if (savedInstanceState.containsKey(KEY_SAVED_QS_TOOLTIP_RESHOW)) {
                mNeedsQSTooltipReshow = savedInstanceState.getBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW);
            }
            if (savedInstanceState.containsKey(KEY_SAVED_QS_TOOLTIP_TYPE)) {
                mNeedsQSTooltipType = savedInstanceState.getInt(KEY_SAVED_QS_TOOLTIP_TYPE);
            }
        }

        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            final PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getPrefContext());
            setPreferenceScreen(preferenceScreen);
        }

        mSettingsContentObserver = new AccessibilitySettingsContentObserver(new Handler());
        registerKeysToObserverCallback(mSettingsContentObserver);
    }

    protected void registerKeysToObserverCallback(
            AccessibilitySettingsContentObserver contentObserver) {
        final List<String> shortcutFeatureKeys = getShortcutFeatureSettingsKeys();

        contentObserver.registerKeysToObserverCallback(shortcutFeatureKeys, key -> {
            updateShortcutPreferenceData();
            updateShortcutPreference();
        });
    }

    protected List<String> getShortcutFeatureSettingsKeys() {
        final List<String> shortcutFeatureKeys = new ArrayList<>();
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
        return shortcutFeatureKeys;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Need to be called as early as possible. Protected variables will be assigned here.
        onProcessArguments(getArguments());

        initTopIntroPreference();
        initAnimatedImagePreference();
        initToggleServiceSwitchPreference();
        initGeneralCategory();
        initShortcutPreference();
        initSettingsPreference();
        initHtmlTextPreference();
        initFooterPreference();

        installActionBarToggleSwitch();

        updateToggleServiceTitle(mToggleServiceSwitchPreference);

        mTouchExplorationStateChangeListener = isTouchExplorationEnabled -> {
            removeDialog(DialogEnums.EDIT_SHORTCUT);
            mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        };
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
        switchBar.hide();

        updatePreferenceOrder();

        // Reshow tooltip when activity recreate, such as rotate device.
        if (mNeedsQSTooltipReshow) {
            getView().post(this::showQuickSettingsTooltipIfNeeded);
        }

        writeDefaultShortcutTargetServiceToSettingsIfNeeded(getPrefContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        mSettingsContentObserver.register(getContentResolver());
        updateShortcutPreferenceData();
        updateShortcutPreference();

        updateEditShortcutDialogIfNeeded();
    }

    @Override
    public void onPause() {
        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        mSettingsContentObserver.unregister(getContentResolver());
        mSavedAccessibilityFloatingMenuEnabled = AccessibilityUtil.isFloatingMenuEnabled(
                getContext());
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        final int value = getShortcutTypeCheckBoxValue();
        if (value != NOT_SET) {
            outState.putInt(KEY_SAVED_USER_SHORTCUT_TYPE, value);
        }
        if (mTooltipWindow != null) {
            outState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, mTooltipWindow.isShowing());
            outState.putInt(KEY_SAVED_QS_TOOLTIP_TYPE, mNeedsQSTooltipType);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogEnums.EDIT_SHORTCUT:
                final int dialogType = WizardManagerHelper.isAnySetupWizard(getIntent())
                        ? DialogType.EDIT_SHORTCUT_GENERIC_SUW : DialogType.EDIT_SHORTCUT_GENERIC;
                mDialog = AccessibilityDialogUtils.showEditShortcutDialog(
                        getPrefContext(), dialogType, getShortcutTitle(),
                        this::callOnAlertDialogCheckboxClicked);
                setupEditShortcutDialog(mDialog);
                return mDialog;
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                mDialog = AccessibilityGestureNavigationTutorial
                        .createAccessibilityTutorialDialog(getPrefContext(),
                                getUserShortcutTypes(), this::callOnTutorialDialogButtonClicked);
                mDialog.setCanceledOnTouchOutside(false);
                return mDialog;
            default:
                throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
        }
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogEnums.EDIT_SHORTCUT:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT;
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                return SettingsEnums.DIALOG_ACCESSIBILITY_TUTORIAL;
            default:
                return SettingsEnums.ACTION_UNKNOWN;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_SERVICE;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeActionBarToggleSwitch();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        onPreferenceToggled(mPreferenceKey, isChecked);
    }

    /**
     * Returns the shortcut type list which has been checked by user.
     */
    abstract int getUserShortcutTypes();

    /** Returns the accessibility tile component name. */
    abstract ComponentName getTileComponentName();

    /** Returns the accessibility tile tooltip content. */
    abstract CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type);

    protected void updateToggleServiceTitle(SettingsMainSwitchPreference switchPreference) {
        final CharSequence title =
            getString(R.string.accessibility_service_primary_switch_title, mPackageName);
        switchPreference.setTitle(title);
    }

    protected CharSequence getShortcutTitle() {
        return getString(R.string.accessibility_shortcut_title, mPackageName);
    }

    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        if (enabled) {
            showQuickSettingsTooltipIfNeeded();
        }
    }

    protected void onInstallSwitchPreferenceToggleSwitch() {
        // Implement this to set a checked listener.
        updateSwitchBarToggleSwitch();
        mToggleServiceSwitchPreference.addOnSwitchChangeListener(this);
    }

    protected void onRemoveSwitchPreferenceToggleSwitch() {
        // Implement this to reset a checked listener.
    }

    protected void updateSwitchBarToggleSwitch() {
        // Implement this to update the state of switch.
    }

    private void installActionBarToggleSwitch() {
        onInstallSwitchPreferenceToggleSwitch();
    }

    private void removeActionBarToggleSwitch() {
        mToggleServiceSwitchPreference.setOnPreferenceClickListener(null);
        onRemoveSwitchPreferenceToggleSwitch();
    }

    public void setTitle(String title) {
        getActivity().setTitle(title);
    }

    protected void onProcessArguments(Bundle arguments) {
        // Key.
        mPreferenceKey = arguments.getString(AccessibilitySettings.EXTRA_PREFERENCE_KEY);

        // Title.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_RESOLVE_INFO)) {
            ResolveInfo info = arguments.getParcelable(AccessibilitySettings.EXTRA_RESOLVE_INFO);
            getActivity().setTitle(info.loadLabel(getPackageManager()).toString());
        } else if (arguments.containsKey(AccessibilitySettings.EXTRA_TITLE)) {
            setTitle(arguments.getString(AccessibilitySettings.EXTRA_TITLE));
        }

        // Summary.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_SUMMARY)) {
            mDescription = arguments.getCharSequence(AccessibilitySettings.EXTRA_SUMMARY);
        }

        // Settings html description.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_HTML_DESCRIPTION)) {
            mHtmlDescription = arguments.getCharSequence(
                    AccessibilitySettings.EXTRA_HTML_DESCRIPTION);
        }

        // Intro.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_INTRO)) {
            mTopIntroTitle = arguments.getCharSequence(AccessibilitySettings.EXTRA_INTRO);
        }
    }

    /** Customizes the order by preference key. */
    protected List<String> getPreferenceOrderList() {
        final List<String> lists = new ArrayList<>();
        lists.add(KEY_TOP_INTRO_PREFERENCE);
        lists.add(KEY_ANIMATED_IMAGE);
        lists.add(KEY_USE_SERVICE_PREFERENCE);
        lists.add(KEY_GENERAL_CATEGORY);
        lists.add(KEY_HTML_DESCRIPTION_PREFERENCE);
        return lists;
    }

    private void updatePreferenceOrder() {
        final List<String> lists = getPreferenceOrderList();

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);

        final int size = lists.size();
        for (int i = 0; i < size; i++) {
            final Preference preference = preferenceScreen.findPreference(lists.get(i));
            if (preference != null) {
                preference.setOrder(i);
            }
        }
    }

    private Drawable getDrawableFromUri(Uri imageUri) {
        if (mImageGetterCacheView == null) {
            mImageGetterCacheView = new ImageView(getPrefContext());
        }

        mImageGetterCacheView.setAdjustViewBounds(true);
        mImageGetterCacheView.setImageURI(imageUri);

        if (mImageGetterCacheView.getDrawable() == null) {
            return null;
        }

        final Drawable drawable =
                mImageGetterCacheView.getDrawable().mutate().getConstantState().newDrawable();
        mImageGetterCacheView.setImageURI(null);
        final int imageWidth = drawable.getIntrinsicWidth();
        final int imageHeight = drawable.getIntrinsicHeight();
        final int screenHalfHeight = AccessibilityUtil.getScreenHeightPixels(getPrefContext()) / 2;
        if ((imageWidth > AccessibilityUtil.getScreenWidthPixels(getPrefContext()))
                || (imageHeight > screenHalfHeight)) {
            return null;
        }

        drawable.setBounds(/* left= */0, /* top= */0, drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());

        return drawable;
    }

    private void initAnimatedImagePreference() {
        if (mImageUri == null) {
            return;
        }

        final int displayHalfHeight =
                AccessibilityUtil.getDisplayBounds(getPrefContext()).height() / 2;
        final IllustrationPreference illustrationPreference =
                new IllustrationPreference(getPrefContext());
        illustrationPreference.setImageUri(mImageUri);
        illustrationPreference.setSelectable(false);
        illustrationPreference.setMaxHeight(displayHalfHeight);
        illustrationPreference.setKey(KEY_ANIMATED_IMAGE);

        getPreferenceScreen().addPreference(illustrationPreference);
    }

    @VisibleForTesting
    void initTopIntroPreference() {
        if (TextUtils.isEmpty(mTopIntroTitle)) {
            return;
        }
        mTopIntroPreference = new TopIntroPreference(getPrefContext());
        mTopIntroPreference.setKey(KEY_TOP_INTRO_PREFERENCE);
        mTopIntroPreference.setTitle(mTopIntroTitle);
        getPreferenceScreen().addPreference(mTopIntroPreference);
    }

    private void initToggleServiceSwitchPreference() {
        mToggleServiceSwitchPreference = new SettingsMainSwitchPreference(getPrefContext());
        mToggleServiceSwitchPreference.setKey(KEY_USE_SERVICE_PREFERENCE);
        if (getArguments().containsKey(AccessibilitySettings.EXTRA_CHECKED)) {
            final boolean enabled = getArguments().getBoolean(AccessibilitySettings.EXTRA_CHECKED);
            mToggleServiceSwitchPreference.setChecked(enabled);
        }

        getPreferenceScreen().addPreference(mToggleServiceSwitchPreference);
    }

    private void initGeneralCategory() {
        final PreferenceCategory generalCategory = new PreferenceCategory(getPrefContext());
        generalCategory.setKey(KEY_GENERAL_CATEGORY);
        generalCategory.setTitle(R.string.accessibility_screen_option);

        getPreferenceScreen().addPreference(generalCategory);
    }

    protected void initShortcutPreference() {
        // Initial the shortcut preference.
        mShortcutPreference = new ShortcutPreference(getPrefContext(), /* attrs= */ null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setOnClickCallback(this);
        mShortcutPreference.setTitle(getShortcutTitle());

        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        generalCategory.addPreference(mShortcutPreference);
    }

    protected void initSettingsPreference() {
        if (mSettingsTitle == null || mSettingsIntent == null) {
            return;
        }

        // Show the "Settings" menu as if it were a preference screen.
        mSettingsPreference = new Preference(getPrefContext());
        mSettingsPreference.setTitle(mSettingsTitle);
        mSettingsPreference.setIconSpaceReserved(false);
        mSettingsPreference.setIntent(mSettingsIntent);

        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        generalCategory.addPreference(mSettingsPreference);
    }

    private void initHtmlTextPreference() {
        if (TextUtils.isEmpty(mHtmlDescription)) {
            return;
        }
        final PreferenceScreen screen = getPreferenceScreen();
        final CharSequence htmlDescription = Html.fromHtml(mHtmlDescription.toString(),
                Html.FROM_HTML_MODE_COMPACT, mImageGetter, /* tagHandler= */ null);

        final AccessibilityFooterPreference htmlFooterPreference =
                new AccessibilityFooterPreference(screen.getContext());
        htmlFooterPreference.setKey(KEY_HTML_DESCRIPTION_PREFERENCE);
        htmlFooterPreference.setSummary(htmlDescription);
        screen.addPreference(htmlFooterPreference);

        // TODO(b/171272809): Migrate to DashboardFragment.
        final String title = getString(R.string.accessibility_introduction_title, mPackageName);
        mFooterPreferenceController = new AccessibilityFooterPreferenceController(
            screen.getContext(), htmlFooterPreference.getKey());
        mFooterPreferenceController.setIntroductionTitle(title);
        mFooterPreferenceController.displayPreference(screen);
    }

    private void initFooterPreference() {
        if (!TextUtils.isEmpty(mDescription)) {
            createFooterPreference(getPreferenceScreen(), mDescription,
                    getString(R.string.accessibility_introduction_title, mPackageName));
        }
    }


    /**
     * Creates {@link AccessibilityFooterPreference} and append into {@link PreferenceScreen}
     *
     * @param screen The preference screen to add the footer preference
     * @param summary The summary of the preference summary.
     * @param introductionTitle The title of introduction in the footer.
     */
    @VisibleForTesting
    void createFooterPreference(PreferenceScreen screen, CharSequence summary,
            String introductionTitle) {
        final AccessibilityFooterPreference footerPreference =
                new AccessibilityFooterPreference(screen.getContext());
        footerPreference.setSummary(summary);
        screen.addPreference(footerPreference);

        mFooterPreferenceController = new AccessibilityFooterPreferenceController(
            screen.getContext(), footerPreference.getKey());
        mFooterPreferenceController.setIntroductionTitle(introductionTitle);
        mFooterPreferenceController.displayPreference(screen);
    }

    @VisibleForTesting
    void setupEditShortcutDialog(Dialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogSoftwareView, mSoftwareTypeCheckBox);

        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogHardwareView, mHardwareTypeCheckBox);

        updateEditShortcutDialogCheckBox();
    }

    private void setDialogTextAreaClickListener(View dialogView, CheckBox checkBox) {
        final View dialogTextArea = dialogView.findViewById(R.id.container);
        dialogTextArea.setOnClickListener(v -> checkBox.toggle());
    }

    private void updateEditShortcutDialogCheckBox() {
        // If it is during onConfigChanged process then restore the value, or get the saved value
        // when shortcutPreference is checked.
        int value = restoreOnConfigChangedValue();
        if (value == NOT_SET) {
            final int lastNonEmptyUserShortcutType = PreferredShortcuts.retrieveUserShortcutType(
                    getPrefContext(), mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
            value = mShortcutPreference.isChecked() ? lastNonEmptyUserShortcutType
                    : UserShortcutType.EMPTY;
        }

        mSoftwareTypeCheckBox.setChecked(
                hasShortcutType(value, UserShortcutType.SOFTWARE));
        mHardwareTypeCheckBox.setChecked(
                hasShortcutType(value, UserShortcutType.HARDWARE));
    }

    private int restoreOnConfigChangedValue() {
        final int savedValue = mSavedCheckBoxValue;
        mSavedCheckBoxValue = NOT_SET;
        return savedValue;
    }

    private boolean hasShortcutType(int value, @UserShortcutType int type) {
        return (value & type) == type;
    }

    /**
     * Returns accumulated {@link UserShortcutType} checkbox value or {@code NOT_SET} if checkboxes
     * did not exist.
     */
    protected int getShortcutTypeCheckBoxValue() {
        if (mSoftwareTypeCheckBox == null || mHardwareTypeCheckBox == null) {
            return NOT_SET;
        }

        int value = UserShortcutType.EMPTY;
        if (mSoftwareTypeCheckBox.isChecked()) {
            value |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            value |= UserShortcutType.HARDWARE;
        }
        return value;
    }

    private static CharSequence getSoftwareShortcutTypeSummary(Context context) {
        int resId;
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software_gesture;
        } else {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        }
        return context.getText(resId);
    }

    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isSettingsEditable()) {
            return context.getText(R.string.accessibility_shortcut_edit_dialog_title_hardware);
        }

        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.switch_off_text);
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(context,
                mComponentName.flattenToString(), UserShortcutType.SOFTWARE);

        final List<CharSequence> list = new ArrayList<>();
        if (hasShortcutType(shortcutTypes, UserShortcutType.SOFTWARE)) {
            list.add(getSoftwareShortcutTypeSummary(context));
        }
        if (hasShortcutType(shortcutTypes, UserShortcutType.HARDWARE)) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_hardware_keyword);
            list.add(hardwareTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(getSoftwareShortcutTypeSummary(context));
        }

        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, LocaleUtils.getConcatenatedString(list));
    }

    /**
     * This method will be invoked when a button in the tutorial dialog is clicked.
     *
     * @param dialog The dialog that received the click
     * @param which  The button that was clicked
     */
    private void callOnTutorialDialogButtonClicked(DialogInterface dialog, int which) {
        dialog.dismiss();
        showQuickSettingsTooltipIfNeeded();
    }

    /**
     * This method will be invoked when a button in the edit shortcut dialog is clicked.
     *
     * @param dialog The dialog that received the click
     * @param which  The button that was clicked
     */
    protected void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        if (mComponentName == null) {
            return;
        }

        final int value = getShortcutTypeCheckBoxValue();
        saveNonEmptyUserShortcutType(value);
        AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), value, mComponentName);
        AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), ~value, mComponentName);
        final boolean shortcutAssigned = value != UserShortcutType.EMPTY;
        mShortcutPreference.setChecked(shortcutAssigned);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));

        if (mHardwareTypeCheckBox.isChecked()) {
            AccessibilityUtil.skipVolumeShortcutDialogTimeoutRestriction(getPrefContext());
        }

        // Show the quick setting tooltip if the shortcut assigned in the first time
        if (shortcutAssigned) {
            showQuickSettingsTooltipIfNeeded();
        }
    }

    protected void updateShortcutPreferenceData() {
        if (mComponentName == null) {
            return;
        }

        final int shortcutTypes = AccessibilityUtil.getUserShortcutTypesFromSettings(
                getPrefContext(), mComponentName);
        if (shortcutTypes != UserShortcutType.EMPTY) {
            final PreferredShortcut shortcut = new PreferredShortcut(
                    mComponentName.flattenToString(), shortcutTypes);
            PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
        }
    }

    protected void updateShortcutPreference() {
        if (mComponentName == null) {
            return;
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(getPrefContext(),
                mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        mShortcutPreference.setChecked(
                AccessibilityUtil.hasValuesInSettings(getPrefContext(), shortcutTypes,
                        mComponentName));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    protected String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        if (mComponentName == null) {
            return;
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(getPrefContext(),
                mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        if (preference.isChecked()) {
            AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), shortcutTypes,
                    mComponentName);
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        } else {
            AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), shortcutTypes,
                    mComponentName);
        }
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        showDialog(DialogEnums.EDIT_SHORTCUT);
    }

    /**
     * Setups a configurable default if the setting has never been set.
     *
     * TODO(b/228562075): Remove this function when correcting the format in config file
     * `config_defaultAccessibilityService`.
     */
    private void writeDefaultShortcutTargetServiceToSettingsIfNeeded(Context context) {
        if (mComponentName == null) {
            return;
        }

        final ComponentName defaultService = ComponentName.unflattenFromString(context.getString(
                com.android.internal.R.string.config_defaultAccessibilityService));
        // write default accessibility service only when user enter into corresponding page.
        if (!mComponentName.equals(defaultService)) {
            return;
        }

        final String targetKey = Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
        String targetString = Settings.Secure.getString(context.getContentResolver(), targetKey);
        if (!TextUtils.isEmpty(targetString)) {
            // The shortcut setting has been set
            return;
        }

        // AccessibilityManager#getAccessibilityShortcutTargets may not return correct shortcut
        // targets during boot. Needs to read settings directly here.
        targetString = AccessibilityUtils.getShortcutTargetServiceComponentNameString(context,
                UserHandle.myUserId());
        if (TextUtils.isEmpty(targetString)) {
            // No configurable default accessibility service
            return;
        }

        // Only fallback to default accessibility service when setting is never updated.
        final ComponentName shortcutName = ComponentName.unflattenFromString(targetString);
        if (shortcutName != null) {
            Settings.Secure.putString(context.getContentResolver(), targetKey,
                    shortcutName.flattenToString());
        }
    }

    private void updateEditShortcutDialogIfNeeded() {
        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }
        AccessibilityDialogUtils.updateShortcutInDialog(getContext(), mDialog);
    }

    @VisibleForTesting
    void saveNonEmptyUserShortcutType(int type) {
        if (type == UserShortcutType.EMPTY) {
            return;
        }

        final PreferredShortcut shortcut = new PreferredShortcut(
                mComponentName.flattenToString(), type);
        PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
    }

    /**
     * Shows the quick settings tooltip if the quick settings feature is assigned. The tooltip only
     * shows once.
     *
     * @param type The quick settings tooltip type
     */
    protected void showQuickSettingsTooltipIfNeeded(@QuickSettingsTooltipType int type) {
        mNeedsQSTooltipType = type;
        showQuickSettingsTooltipIfNeeded();
    }

    private void showQuickSettingsTooltipIfNeeded() {
        final ComponentName tileComponentName = getTileComponentName();
        if (tileComponentName == null) {
            // Returns if no tile service assigned.
            return;
        }

        if (!mNeedsQSTooltipReshow && AccessibilityQuickSettingUtils.hasValueInSharedPreferences(
                getContext(), tileComponentName)) {
            // Returns if quick settings tooltip only show once.
            return;
        }

        final CharSequence content = getTileTooltipContent(mNeedsQSTooltipType);
        if (TextUtils.isEmpty(content)) {
            // Returns if no content of tile tooltip assigned.
            return;
        }

        final int imageResId = mNeedsQSTooltipType == QuickSettingsTooltipType.GUIDE_TO_EDIT
                ? R.drawable.accessibility_qs_tooltip_illustration
                : R.drawable.accessibility_auto_added_qs_tooltip_illustration;
        mTooltipWindow = new AccessibilityQuickSettingsTooltipWindow(getContext());
        mTooltipWindow.setup(content, imageResId);
        mTooltipWindow.showAtTopCenter(getView());
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(getContext(),
                tileComponentName);
        mNeedsQSTooltipReshow = false;
    }

    /** Returns user visible name of the tile by given {@link ComponentName}. */
    protected CharSequence loadTileLabel(Context context, ComponentName componentName) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent queryIntent = new Intent(TileService.ACTION_QS_TILE);
        final List<ResolveInfo> resolveInfos =
                packageManager.queryIntentServices(queryIntent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : resolveInfos) {
            final ServiceInfo serviceInfo = info.serviceInfo;
            if (TextUtils.equals(componentName.getPackageName(), serviceInfo.packageName)
                    && TextUtils.equals(componentName.getClassName(), serviceInfo.name)) {
                return serviceInfo.loadLabel(packageManager);
            }
        }
        return null;
    }
}
