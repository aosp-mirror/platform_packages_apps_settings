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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Base class for accessibility fragments with toggle, shortcut, some helper functions
 * and dialog management.
 */
public abstract class ToggleFeaturePreferenceFragment extends SettingsPreferenceFragment
        implements ShortcutPreference.OnClickListener {

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
    protected CharSequence mHtmlDescription;
    private static final String ANCHOR_TAG = "a";
    private static final String DRAWABLE_FOLDER = "drawable";
    protected static final String KEY_USE_SERVICE_PREFERENCE = "use_service";
    protected static final String KEY_GENERAL_CATEGORY = "general_categories";
    protected static final String KEY_INTRODUCTION_CATEGORY = "introduction_categories";
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcut_type";
    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;
    private int mUserShortcutType = UserShortcutType.DEFAULT;
    // Used to restore the edit dialog status.
    private int mUserShortcutTypeCache = UserShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;

    // For html description of accessibility service, third party developer must follow the rule,
    // such as <img src="R.drawable.fileName"/>, a11y settings will get third party resources
    // by this.
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

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (mImageUri != null) {
            final AnimatedImagePreference animatedImagePreference = new AnimatedImagePreference(
                    getPrefContext());
            animatedImagePreference.setImageUri(mImageUri);
            animatedImagePreference.setSelectable(false);
            preferenceScreen.addPreference(animatedImagePreference);
        }

        mToggleServiceDividerSwitchPreference = new DividerSwitchPreference(getPrefContext());
        mToggleServiceDividerSwitchPreference.setKey(KEY_USE_SERVICE_PREFERENCE);
        preferenceScreen.addPreference(mToggleServiceDividerSwitchPreference);

        onProcessArguments(getArguments());
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

        if (mHtmlDescription != null) {
            final PreferenceCategory introductionCategory = new PreferenceCategory(
                    getPrefContext());
            final CharSequence title = getString(R.string.accessibility_introduction_title,
                    mPackageName);
            introductionCategory.setKey(KEY_INTRODUCTION_CATEGORY);
            introductionCategory.setTitle(title);
            preferenceScreen.addPreference(introductionCategory);

            // For accessibility service, avoid malicious links made by third party developer.
            final List<String> unsupportedTagList = new ArrayList<>();
            unsupportedTagList.add(ANCHOR_TAG);

            final HtmlTextPreference htmlTextPreference = new HtmlTextPreference(getPrefContext());
            htmlTextPreference.setSummary(mHtmlDescription);
            htmlTextPreference.setImageGetter(mImageGetter);
            htmlTextPreference.setUnsupportedTagList(unsupportedTagList);
            htmlTextPreference.setSelectable(false);
            introductionCategory.addPreference(htmlTextPreference);
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
        updateShortcutPreferenceData();
        updateShortcutPreference();
    }

    @Override
    public void onPause() {
        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mUserShortcutTypeCache);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogEnums.EDIT_SHORTCUT:
                final CharSequence dialogTitle = getPrefContext().getString(
                        R.string.accessibility_shortcut_title, mPackageName);
                Dialog dialog = AccessibilityEditDialogUtils.showEditShortcutDialog(
                        getPrefContext(), dialogTitle, this::callOnAlertDialogCheckboxClicked);
                initializeDialogCheckBox(dialog);
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
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle use service to
         * disable service.
         */
        int DISABLE_WARNING_FROM_TOGGLE = 1004;

        /**
         * OPEN: Settings > Accessibility > Magnification > Toggle user service in button
         * navigation.
         */
        int ACCESSIBILITY_BUTTON_TUTORIAL = 1005;

        /**
         * OPEN: Settings > Accessibility > Magnification > Toggle user service in gesture
         * navigation.
         */
        int GESTURE_NAVIGATION_TUTORIAL = 1006;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle user service > Show
         * launch tutorial.
         */
        int LAUNCH_ACCESSIBILITY_TUTORIAL = 1007;
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

        // Enabled.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_CHECKED)) {
            final boolean enabled = arguments.getBoolean(AccessibilitySettings.EXTRA_CHECKED);
            mToggleServiceDividerSwitchPreference.setChecked(enabled);
        }

        // Title.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_RESOLVE_INFO)) {
            ResolveInfo info = arguments.getParcelable(AccessibilitySettings.EXTRA_RESOLVE_INFO);
            getActivity().setTitle(info.loadLabel(getPackageManager()).toString());
        } else if (arguments.containsKey(AccessibilitySettings.EXTRA_TITLE)) {
            setTitle(arguments.getString(AccessibilitySettings.EXTRA_TITLE));
        }

        // Summary.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_SUMMARY_RES)) {
            final int summary = arguments.getInt(AccessibilitySettings.EXTRA_SUMMARY_RES);
            createFooterPreference(getText(summary));
        } else if (arguments.containsKey(AccessibilitySettings.EXTRA_SUMMARY)) {
            final CharSequence summary = arguments.getCharSequence(
                    AccessibilitySettings.EXTRA_SUMMARY);
            createFooterPreference(summary);
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

        final Drawable drawable = mImageGetterCacheView.getDrawable().mutate();
        if (drawable != null) {
            drawable.setBounds(/* left= */0, /* top= */0, drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());
        }

        mImageGetterCacheView.setImageURI(null);
        mImageGetterCacheView.setImageDrawable(null);
        return drawable;
    }

    static final class AccessibilityUserShortcutType {
        private static final char COMPONENT_NAME_SEPARATOR = ':';
        private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
                new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

        private String mComponentName;
        private int mUserShortcutType;

        AccessibilityUserShortcutType(String componentName, int userShortcutType) {
            this.mComponentName = componentName;
            this.mUserShortcutType = userShortcutType;
        }

        AccessibilityUserShortcutType(String flattenedString) {
            sStringColonSplitter.setString(flattenedString);
            if (sStringColonSplitter.hasNext()) {
                this.mComponentName = sStringColonSplitter.next();
                this.mUserShortcutType = Integer.parseInt(sStringColonSplitter.next());
            }
        }

        String getComponentName() {
            return mComponentName;
        }

        void setComponentName(String componentName) {
            this.mComponentName = componentName;
        }

        int getUserShortcutType() {
            return mUserShortcutType;
        }

        void setUserShortcutType(int userShortcutType) {
            this.mUserShortcutType = userShortcutType;
        }

        String flattenToString() {
            final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));
            joiner.add(mComponentName);
            joiner.add(String.valueOf(mUserShortcutType));
            return joiner.toString();
        }
    }


    private void initializeDialogCheckBox(Dialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        updateAlertDialogCheckState();
        updateAlertDialogEnableState();
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mSoftwareTypeCheckBox, UserShortcutType.SOFTWARE);
        updateCheckStatus(mHardwareTypeCheckBox, UserShortcutType.HARDWARE);
    }

    private void updateAlertDialogEnableState() {
        if (!mSoftwareTypeCheckBox.isChecked()) {
            mHardwareTypeCheckBox.setEnabled(false);
        } else if (!mHardwareTypeCheckBox.isChecked()) {
            mSoftwareTypeCheckBox.setEnabled(false);
        } else {
            mSoftwareTypeCheckBox.setEnabled(true);
            mHardwareTypeCheckBox.setEnabled(true);
        }
    }

    private void updateCheckStatus(CheckBox checkBox, @UserShortcutType int type) {
        checkBox.setChecked((mUserShortcutTypeCache & type) == type);
        checkBox.setOnClickListener(v -> {
            updateUserShortcutType(/* saveChanges= */ false);
            updateAlertDialogEnableState();
        });
    }

    private void updateUserShortcutType(boolean saveChanges) {
        mUserShortcutTypeCache = UserShortcutType.DEFAULT;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mUserShortcutTypeCache |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mUserShortcutTypeCache |= UserShortcutType.HARDWARE;
        }
        if (saveChanges) {
            mUserShortcutType = mUserShortcutTypeCache;
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        }
    }

    private void setUserShortcutType(Context context, int type) {
        if (mComponentName == null) {
            return;
        }

        Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
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

    private String getShortcutTypeSummary(Context context) {
        final int shortcutType = getUserShortcutType(context, UserShortcutType.SOFTWARE);
        int resId = R.string.accessibility_shortcut_edit_summary_software;
        if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = AccessibilityUtil.isTouchExploreEnabled(context)
                    ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture_talkback
                    : R.string.accessibility_shortcut_edit_dialog_title_software_gesture;
        }
        final CharSequence softwareTitle = context.getText(resId);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_edit_dialog_title_hardware);
            list.add(hardwareTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }
        final String joinStrings = TextUtils.join(/* delimiter= */", ", list);
        return AccessibilityUtil.capitalize(joinStrings);
    }

    protected int getUserShortcutType(Context context, @UserShortcutType int defaultValue) {
        if (mComponentName == null) {
            return defaultValue;
        }

        final Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
        final String componentName = mComponentName.flattenToString();
        final Set<String> filtered = info.stream()
                .filter(str -> str.contains(componentName))
                .collect(Collectors.toSet());
        if (filtered.isEmpty()) {
            return defaultValue;
        }

        final String str = (String) filtered.toArray()[0];
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(str);
        return shortcut.getUserShortcutType();
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        if (mComponentName == null) {
            return;
        }

        updateUserShortcutType(/* saveChanges= */ true);
        if (mShortcutPreference.getChecked()) {
            AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), mUserShortcutType,
                    mComponentName);
            AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), ~mUserShortcutType,
                    mComponentName);
        }
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    private void updateShortcutPreferenceData() {
        if (mComponentName == null) {
            return;
        }

        // Get the user shortcut type from settings provider.
        mUserShortcutType = AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
        if (mUserShortcutType != UserShortcutType.DEFAULT) {
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        } else {
            //  Get the user shortcut type from shared_prefs if cannot get from settings provider.
            mUserShortcutType = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
        }
    }

    private void initShortcutPreference(Bundle savedInstanceState) {
        // Restore the user shortcut type.
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SHORTCUT_TYPE)) {
            mUserShortcutTypeCache = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE,
                    UserShortcutType.DEFAULT);
        }

        // Initial the shortcut preference.
        mShortcutPreference = new ShortcutPreference(getPrefContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setOnClickListener(this);

        final CharSequence title = getString(R.string.accessibility_shortcut_title, mPackageName);
        mShortcutPreference.setTitle(title);
    }

    private void updateShortcutPreference() {
        if (mComponentName == null) {
            return;
        }

        final int shortcutTypes = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
        mShortcutPreference.setChecked(
                    AccessibilityUtil.hasValuesInSettings(getPrefContext(), shortcutTypes,
                            mComponentName));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    private String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        if (mComponentName == null) {
            return;
        }

        final int shortcutTypes = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
        if (preference.getChecked()) {
            AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), shortcutTypes,
                    mComponentName);
        } else {
            AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), shortcutTypes,
                    mComponentName);
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        mUserShortcutTypeCache = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
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
