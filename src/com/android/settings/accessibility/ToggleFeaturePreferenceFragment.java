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

import static com.android.settings.accessibility.AccessibilityUtil.getScreenHeightPixels;
import static com.android.settings.accessibility.AccessibilityUtil.getScreenWidthPixels;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.icu.text.CaseMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.widget.FooterPreference;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Base class for accessibility fragments with toggle, shortcut, some helper functions
 * and dialog management.
 */
public abstract class ToggleFeaturePreferenceFragment extends SettingsPreferenceFragment
        implements ShortcutPreference.OnClickCallback {

    protected DividerSwitchPreference mToggleServiceDividerSwitchPreference;
    protected ShortcutPreference mShortcutPreference;
    protected Preference mSettingsPreference;
    protected String mPreferenceKey;

    protected CharSequence mSettingsTitle;
    protected Intent mSettingsIntent;
    // The mComponentName maybe null, such as Magnify
    protected ComponentName mComponentName;
    protected CharSequence mPackageName;
    protected Uri mImageUri;
    private CharSequence mDescription;
    protected CharSequence mHtmlDescription;
    // Used to restore the edit dialog status.
    protected int mUserShortcutTypesCache = UserShortcutType.EMPTY;
    private static final String DRAWABLE_FOLDER = "drawable";
    protected static final String KEY_USE_SERVICE_PREFERENCE = "use_service";
    protected static final String KEY_GENERAL_CATEGORY = "general_categories";
    protected static final String KEY_INTRODUCTION_CATEGORY = "introduction_categories";
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcut_type";
    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;
    private int mUserShortcutTypes = UserShortcutType.EMPTY;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;
    private SettingsContentObserver mSettingsContentObserver;

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
        setupDefaultShortcutIfNecessary(getPrefContext());
        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getPrefContext());
            setPreferenceScreen(preferenceScreen);
        }

        final List<String> shortcutFeatureKeys = new ArrayList<>();
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
        mSettingsContentObserver = new SettingsContentObserver(new Handler(), shortcutFeatureKeys) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateShortcutPreferenceData();
                updateShortcutPreference();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
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
        final SwitchBar switchBar = activity.getSwitchBar();
        switchBar.hide();

        // Need to be called as early as possible. Protected variables will be assigned here.
        onProcessArguments(getArguments());

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (mImageUri != null) {
            final int screenHalfHeight = getScreenHeightPixels(getPrefContext()) / /* half */ 2;
            final AnimatedImagePreference animatedImagePreference = new AnimatedImagePreference(
                    getPrefContext());
            animatedImagePreference.setImageUri(mImageUri);
            animatedImagePreference.setSelectable(false);
            animatedImagePreference.setMaxHeight(screenHalfHeight);
            preferenceScreen.addPreference(animatedImagePreference);
        }

        mToggleServiceDividerSwitchPreference = new DividerSwitchPreference(getPrefContext());
        mToggleServiceDividerSwitchPreference.setKey(KEY_USE_SERVICE_PREFERENCE);
        if (getArguments().containsKey(AccessibilitySettings.EXTRA_CHECKED)) {
            final boolean enabled = getArguments().getBoolean(AccessibilitySettings.EXTRA_CHECKED);
            mToggleServiceDividerSwitchPreference.setChecked(enabled);
        }

        preferenceScreen.addPreference(mToggleServiceDividerSwitchPreference);

        updateToggleServiceTitle(mToggleServiceDividerSwitchPreference);

        final PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
        groupCategory.setKey(KEY_GENERAL_CATEGORY);
        groupCategory.setTitle(R.string.accessibility_screen_option);
        preferenceScreen.addPreference(groupCategory);

        initShortcutPreference(savedInstanceState);
        groupCategory.addPreference(mShortcutPreference);

        // Show the "Settings" menu as if it were a preference screen.
        if (mSettingsTitle != null && mSettingsIntent != null) {
            mSettingsPreference = new Preference(getPrefContext());
            mSettingsPreference.setTitle(mSettingsTitle);
            mSettingsPreference.setIconSpaceReserved(true);
            mSettingsPreference.setIntent(mSettingsIntent);
        }

        // The downloaded app may not show Settings. The framework app has Settings.
        if (mSettingsPreference != null) {
            groupCategory.addPreference(mSettingsPreference);
        }

        if (!TextUtils.isEmpty(mHtmlDescription)) {
            final PreferenceCategory introductionCategory = new PreferenceCategory(
                    getPrefContext());
            final CharSequence title = getString(R.string.accessibility_introduction_title,
                    mPackageName);
            introductionCategory.setKey(KEY_INTRODUCTION_CATEGORY);
            introductionCategory.setTitle(title);
            preferenceScreen.addPreference(introductionCategory);

            final HtmlTextPreference htmlTextPreference = new HtmlTextPreference(getPrefContext());
            htmlTextPreference.setSummary(mHtmlDescription);
            htmlTextPreference.setImageGetter(mImageGetter);
            htmlTextPreference.setSelectable(false);
            introductionCategory.addPreference(htmlTextPreference);
        }

        if (!TextUtils.isEmpty(mDescription)) {
            createFooterPreference(mDescription);
        }

        if (TextUtils.isEmpty(mHtmlDescription) && TextUtils.isEmpty(mDescription)) {
            final CharSequence defaultDescription = getText(
                    R.string.accessibility_service_default_description);
            createFooterPreference(defaultDescription);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        installActionBarToggleSwitch();
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
    }

    @Override
    public void onPause() {
        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mUserShortcutTypesCache);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Dialog dialog;
        switch (dialogId) {
            case DialogEnums.EDIT_SHORTCUT:
                final CharSequence dialogTitle = getPrefContext().getString(
                        R.string.accessibility_shortcut_title, mPackageName);
                dialog = AccessibilityEditDialogUtils.showEditShortcutDialog(
                        getPrefContext(), dialogTitle, this::callOnAlertDialogCheckboxClicked);
                initializeDialogCheckBox(dialog);
                return dialog;
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                dialog = AccessibilityGestureNavigationTutorial
                        .createAccessibilityTutorialDialog(getPrefContext(),
                                getUserShortcutTypes());
                dialog.setCanceledOnTouchOutside(false);
                return dialog;
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

    /** Denotes the dialog emuns for show dialog */
    @Retention(RetentionPolicy.SOURCE)
    protected @interface DialogEnums {

        /** OPEN: Settings > Accessibility > Any toggle service > Shortcut > Settings. */
        int EDIT_SHORTCUT = 1;

        /** OPEN: Settings > Accessibility > Magnification > Shortcut > Settings. */
        int MAGNIFICATION_EDIT_SHORTCUT = 1001;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle use service to
         * enable service.
         */
        int ENABLE_WARNING_FROM_TOGGLE = 1002;

        /** OPEN: Settings > Accessibility > Downloaded toggle service > Shortcut checkbox. */
        int ENABLE_WARNING_FROM_SHORTCUT = 1003;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Shortcut checkbox
         * toggle.
         */
        int ENABLE_WARNING_FROM_SHORTCUT_TOGGLE = 1004;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle use service to
         * disable service.
         */
        int DISABLE_WARNING_FROM_TOGGLE = 1005;

        /**
         * OPEN: Settings > Accessibility > Magnification > Toggle user service in button
         * navigation.
         */
        int ACCESSIBILITY_BUTTON_TUTORIAL = 1006;

        /**
         * OPEN: Settings > Accessibility > Magnification > Toggle user service in gesture
         * navigation.
         */
        int GESTURE_NAVIGATION_TUTORIAL = 1007;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle user service > Show
         * launch tutorial.
         */
        int LAUNCH_ACCESSIBILITY_TUTORIAL = 1008;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_SERVICE;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeActionBarToggleSwitch();
    }

    /**
     * Returns the shortcut type list which has been checked by user.
     */
    abstract int getUserShortcutTypes();

    protected void updateToggleServiceTitle(SwitchPreference switchPreference) {
        switchPreference.setTitle(R.string.accessibility_service_master_switch_title);
    }

    protected abstract void onPreferenceToggled(String preferenceKey, boolean enabled);

    protected void onInstallSwitchPreferenceToggleSwitch() {
        // Implement this to set a checked listener.
    }

    protected void onRemoveSwitchPreferenceToggleSwitch() {
        // Implement this to reset a checked listener.
    }

    private void installActionBarToggleSwitch() {
        onInstallSwitchPreferenceToggleSwitch();
    }

    private void removeActionBarToggleSwitch() {
        mToggleServiceDividerSwitchPreference.setOnPreferenceClickListener(null);
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
        final int screenHalfHeight = getScreenHeightPixels(getPrefContext()) / /* half */ 2;
        if ((imageWidth > getScreenWidthPixels(getPrefContext()))
                || (imageHeight > screenHalfHeight)) {
            return null;
        }

        drawable.setBounds(/* left= */0, /* top= */0, drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());

        return drawable;
    }

    static final class AccessibilityUserShortcutType {
        private static final char COMPONENT_NAME_SEPARATOR = ':';
        private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
                new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

        private String mComponentName;
        private int mType;

        AccessibilityUserShortcutType(String componentName, int type) {
            this.mComponentName = componentName;
            this.mType = type;
        }

        AccessibilityUserShortcutType(String flattenedString) {
            sStringColonSplitter.setString(flattenedString);
            if (sStringColonSplitter.hasNext()) {
                this.mComponentName = sStringColonSplitter.next();
                this.mType = Integer.parseInt(sStringColonSplitter.next());
            }
        }

        String getComponentName() {
            return mComponentName;
        }

        void setComponentName(String componentName) {
            this.mComponentName = componentName;
        }

        int getType() {
            return mType;
        }

        void setType(int type) {
            this.mType = type;
        }

        String flattenToString() {
            final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));
            joiner.add(mComponentName);
            joiner.add(String.valueOf(mType));
            return joiner.toString();
        }
    }

    private void setDialogTextAreaClickListener(View dialogView, CheckBox checkBox) {
        final View dialogTextArea = dialogView.findViewById(R.id.container);
        dialogTextArea.setOnClickListener(v -> {
            checkBox.toggle();
            updateUserShortcutType(/* saveChanges= */ false);
        });
    }

    private void initializeDialogCheckBox(Dialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogSoftwareView, mSoftwareTypeCheckBox);

        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogHardwareView, mHardwareTypeCheckBox);

        updateAlertDialogCheckState();
    }

    private void updateAlertDialogCheckState() {
        if (mUserShortcutTypesCache != UserShortcutType.EMPTY) {
            updateCheckStatus(mSoftwareTypeCheckBox, UserShortcutType.SOFTWARE);
            updateCheckStatus(mHardwareTypeCheckBox, UserShortcutType.HARDWARE);
        }
    }

    private void updateCheckStatus(CheckBox checkBox, @UserShortcutType int type) {
        checkBox.setChecked((mUserShortcutTypesCache & type) == type);
    }

    private void updateUserShortcutType(boolean saveChanges) {
        mUserShortcutTypesCache = UserShortcutType.EMPTY;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mUserShortcutTypesCache |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mUserShortcutTypesCache |= UserShortcutType.HARDWARE;
        }

        if (saveChanges) {
            final boolean isChanged = (mUserShortcutTypesCache != UserShortcutType.EMPTY);
            if (isChanged) {
                setUserShortcutType(getPrefContext(), mUserShortcutTypesCache);
            }
            mUserShortcutTypes = mUserShortcutTypesCache;
        }
    }

    private void setUserShortcutType(Context context, int type) {
        if (mComponentName == null) {
            return;
        }

        Set<String> info = SharedPreferenceUtils.getUserShortcutTypes(context);
        final String componentName = mComponentName.flattenToString();
        if (info.isEmpty()) {
            info = new HashSet<>();
        } else {
            final Set<String> filtered = info.stream()
                    .filter(str -> str.contains(componentName))
                    .collect(Collectors.toSet());
            info.removeAll(filtered);
        }
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                componentName, type);
        info.add(shortcut.flattenToString());
        SharedPreferenceUtils.setUserShortcutType(context, info);
    }

    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isSettingsEditable()) {
            return context.getText(R.string.accessibility_shortcut_edit_dialog_title_hardware);
        }

        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.switch_off_text);
        }

        final int shortcutTypes = getUserShortcutTypes(context, UserShortcutType.SOFTWARE);
        int resId = R.string.accessibility_shortcut_edit_summary_software;
        if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = AccessibilityUtil.isTouchExploreEnabled(context)
                    ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture_talkback
                    : R.string.accessibility_shortcut_edit_dialog_title_software_gesture;
        }
        final CharSequence softwareTitle = context.getText(resId);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_hardware_keyword);
            list.add(hardwareTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }
        final String joinStrings = TextUtils.join(/* delimiter= */", ", list);

        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, joinStrings);
    }

    protected int getUserShortcutTypes(Context context, @UserShortcutType int defaultValue) {
        if (mComponentName == null) {
            return defaultValue;
        }

        final Set<String> info = SharedPreferenceUtils.getUserShortcutTypes(context);
        final String componentName = mComponentName.flattenToString();
        final Set<String> filtered = info.stream()
                .filter(str -> str.contains(componentName))
                .collect(Collectors.toSet());
        if (filtered.isEmpty()) {
            return defaultValue;
        }

        final String str = (String) filtered.toArray()[0];
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(str);
        return shortcut.getType();
    }

    /**
     * This method will be invoked when a button in the edit shortcut dialog is clicked.
     *
     * @param dialog The dialog that received the click
     * @param which The button that was clicked
     */
    protected void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        if (mComponentName == null) {
            return;
        }

        updateUserShortcutType(/* saveChanges= */ true);
        AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), mUserShortcutTypes,
                mComponentName);
        AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), ~mUserShortcutTypes,
                mComponentName);
        mShortcutPreference.setChecked(mUserShortcutTypes != UserShortcutType.EMPTY);
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    protected void updateShortcutPreferenceData() {
        if (mComponentName == null) {
            return;
        }

        // Get the user shortcut type from settings provider.
        mUserShortcutTypes = AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
        if (mUserShortcutTypes != UserShortcutType.EMPTY) {
            setUserShortcutType(getPrefContext(), mUserShortcutTypes);
        } else {
            //  Get the user shortcut type from shared_prefs if cannot get from settings provider.
            mUserShortcutTypes = getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE);
        }
    }

    private void initShortcutPreference(Bundle savedInstanceState) {
        // Restore the user shortcut type.
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SHORTCUT_TYPE)) {
            mUserShortcutTypesCache = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE,
                    UserShortcutType.EMPTY);
        }

        // Initial the shortcut preference.
        mShortcutPreference = new ShortcutPreference(getPrefContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setOnClickCallback(this);

        final CharSequence title = getString(R.string.accessibility_shortcut_title, mPackageName);
        mShortcutPreference.setTitle(title);
    }

    protected void updateShortcutPreference() {
        if (mComponentName == null) {
            return;
        }

        final int shortcutTypes = getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE);
        mShortcutPreference.setChecked(
                    AccessibilityUtil.hasValuesInSettings(getPrefContext(), shortcutTypes,
                            mComponentName));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    private String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        if (mComponentName == null) {
            return;
        }

        final int shortcutTypes = getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE);
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
        // Do not restore shortcut in shortcut chooser dialog when shortcutPreference is turned off.
        mUserShortcutTypesCache = mShortcutPreference.isChecked()
                ? getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE)
                : UserShortcutType.EMPTY;
    }

    private void createFooterPreference(CharSequence title) {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.addPreference(new FooterPreference.Builder(getActivity()).setTitle(
                title).build());
    }

    /**
     *  Setups a configurable default if the setting has never been set.
     */
    private static void setupDefaultShortcutIfNecessary(Context context) {
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
}
