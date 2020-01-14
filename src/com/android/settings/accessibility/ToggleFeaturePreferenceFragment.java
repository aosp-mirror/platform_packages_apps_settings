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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public abstract class ToggleFeaturePreferenceFragment extends SettingsPreferenceFragment {

    protected SwitchBar mSwitchBar;
    protected ToggleSwitch mToggleSwitch;

    protected String mPreferenceKey;

    protected CharSequence mSettingsTitle;
    protected Intent mSettingsIntent;
    protected ComponentName mComponentName;
    protected Uri mImageUri;
    protected CharSequence mStaticDescription;
    protected CharSequence mHtmlDescription;
    private static final String ANCHOR_TAG = "a";
    private static final String DRAWABLE_FOLDER = "drawable";

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
        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getActivity());
            setPreferenceScreen(preferenceScreen);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mToggleSwitch = mSwitchBar.getSwitch();

        onProcessArguments(getArguments());
        updateSwitchBarText(mSwitchBar);

        PreferenceScreen preferenceScreen = getPreferenceScreen();

        // Show the "Settings" menu as if it were a preference screen
        if (mSettingsTitle != null && mSettingsIntent != null) {
            Preference settingsPref = new Preference(preferenceScreen.getContext());
            settingsPref.setTitle(mSettingsTitle);
            settingsPref.setIconSpaceReserved(true);
            settingsPref.setIntent(mSettingsIntent);
            preferenceScreen.addPreference(settingsPref);
        }

        if (mImageUri != null) {
            final AnimatedImagePreference animatedImagePreference = new AnimatedImagePreference(
                    preferenceScreen.getContext());
            animatedImagePreference.setImageUri(mImageUri);
            animatedImagePreference.setDividerAllowedAbove(true);
            preferenceScreen.addPreference(animatedImagePreference);
        }

        if (mStaticDescription != null) {
            final StaticTextPreference staticTextPreference = new StaticTextPreference(
                    preferenceScreen.getContext());
            staticTextPreference.setSummary(mStaticDescription);
            preferenceScreen.addPreference(staticTextPreference);
        }

        if (mHtmlDescription != null) {
            // For accessibility service, avoid malicious links made by third party developer
            final List<String> unsupportedTagList = new ArrayList<>();
            unsupportedTagList.add(ANCHOR_TAG);

            final HtmlTextPreference htmlTextPreference = new HtmlTextPreference(
                    preferenceScreen.getContext());
            htmlTextPreference.setSummary(mHtmlDescription);
            htmlTextPreference.setImageGetter(mImageGetter);
            htmlTextPreference.setUnsupportedTagList(unsupportedTagList);
            htmlTextPreference.setDividerAllowedAbove(true);
            preferenceScreen.addPreference(htmlTextPreference);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        installActionBarToggleSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        removeActionBarToggleSwitch();
    }

    protected void updateSwitchBarText(SwitchBar switchBar) {
        // Implement this to provide meaningful text in switch bar
        switchBar.setSwitchBarText(R.string.accessibility_service_master_switch_title,
                R.string.accessibility_service_master_switch_title);
    }

    protected abstract void onPreferenceToggled(String preferenceKey, boolean enabled);

    protected void onInstallSwitchBarToggleSwitch() {
        // Implement this to set a checked listener.
    }

    protected void onRemoveSwitchBarToggleSwitch() {
        // Implement this to reset a checked listener.
    }

    private void installActionBarToggleSwitch() {
        mSwitchBar.show();
        onInstallSwitchBarToggleSwitch();
    }

    private void removeActionBarToggleSwitch() {
        mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        onRemoveSwitchBarToggleSwitch();
        mSwitchBar.hide();
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
            mSwitchBar.setCheckedInternal(enabled);
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
            mStaticDescription = getText(summary);
        } else if (arguments.containsKey(AccessibilitySettings.EXTRA_SUMMARY)) {
            final CharSequence summary = arguments.getCharSequence(
                    AccessibilitySettings.EXTRA_SUMMARY);
            mStaticDescription = summary;
        }
    }

    private Drawable getDrawableFromUri(Uri imageUri) {
        if (mImageGetterCacheView == null) {
            mImageGetterCacheView = new ImageView(getContext());
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
}
