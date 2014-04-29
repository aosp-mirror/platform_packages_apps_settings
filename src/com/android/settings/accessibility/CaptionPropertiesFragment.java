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

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFrameLayout;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.ListDialogPreference.OnValueChangedListener;
import com.android.settings.accessibility.ToggleSwitch.OnBeforeCheckedChangeListener;

import java.util.Locale;

/**
 * Settings fragment containing captioning properties.
 */
public class CaptionPropertiesFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnValueChangedListener {
    private static final String PREF_BACKGROUND_COLOR = "captioning_background_color";
    private static final String PREF_BACKGROUND_OPACITY = "captioning_background_opacity";
    private static final String PREF_FOREGROUND_COLOR = "captioning_foreground_color";
    private static final String PREF_FOREGROUND_OPACITY = "captioning_foreground_opacity";
    private static final String PREF_EDGE_COLOR = "captioning_edge_color";
    private static final String PREF_EDGE_TYPE = "captioning_edge_type";
    private static final String PREF_FONT_SIZE = "captioning_font_size";
    private static final String PREF_TYPEFACE = "captioning_typeface";
    private static final String PREF_LOCALE = "captioning_locale";
    private static final String PREF_PRESET = "captioning_preset";
    private static final String PREF_CUSTOM = "custom";

    private static final float DEFAULT_FONT_SIZE = 48f;

    private CaptioningManager mCaptioningManager;
    private SubtitleView mPreviewText;

    // Standard options.
    private LocalePreference mLocale;
    private ListPreference mFontSize;
    private PresetPreference mPreset;

    // Custom options.
    private ListPreference mTypeface;
    private ColorPreference mForegroundColor;
    private ColorPreference mForegroundOpacity;
    private EdgeTypePreference mEdgeType;
    private ColorPreference mEdgeColor;
    private ColorPreference mBackgroundColor;
    private ColorPreference mBackgroundOpacity;
    private PreferenceCategory mCustom;

    private boolean mShowingCustom;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);

        addPreferencesFromResource(R.xml.captioning_settings);
        initializeAllPreferences();
        updateAllPreferences();
        refreshShowingCustom();
        installUpdateListeners();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.captioning_preview, container, false);

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) rootView.getLayoutParams()).removeBorders = true;
        }

        final View content = super.onCreateView(inflater, container, savedInstanceState);
        ((ViewGroup) rootView.findViewById(R.id.properties_fragment)).addView(
                content, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPreviewText = (SubtitleView) view.findViewById(R.id.preview_text);

        installActionBarToggleSwitch();
        refreshPreviewText();
    }

    private void refreshPreviewText() {
        final Context context = getActivity();
        if (context == null) {
            // We've been destroyed, abort!
            return;
        }

        final SubtitleView preview = mPreviewText;
        if (preview != null) {
            final int styleId = mCaptioningManager.getRawUserStyle();
            applyCaptionProperties(mCaptioningManager, preview, styleId);

            final Locale locale = mCaptioningManager.getLocale();
            if (locale != null) {
                final CharSequence localizedText = AccessibilityUtils.getTextForLocale(
                        context, locale, R.string.captioning_preview_text);
                preview.setText(localizedText);
            } else {
                preview.setText(R.string.captioning_preview_text);
            }
        }
    }

    public static void applyCaptionProperties(
            CaptioningManager manager, SubtitleView previewText, int styleId) {
        previewText.setStyle(styleId);

        final Context context = previewText.getContext();
        final ContentResolver cr = context.getContentResolver();
        final float fontScale = manager.getFontScale();
        previewText.setTextSize(fontScale * DEFAULT_FONT_SIZE);

        final Locale locale = manager.getLocale();
        if (locale != null) {
            final CharSequence localizedText = AccessibilityUtils.getTextForLocale(
                    context, locale, R.string.captioning_preview_characters);
            previewText.setText(localizedText);
        } else {
            previewText.setText(R.string.captioning_preview_characters);
        }
    }

    private void installActionBarToggleSwitch() {
        final Activity activity = getActivity();
        final ToggleSwitch toggleSwitch = new ToggleSwitch(activity);

        final int padding = getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        toggleSwitch.setPaddingRelative(0, 0, padding, 0);

        final ActionBar actionBar = activity.getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);

        final ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END);
        actionBar.setCustomView(toggleSwitch, params);

        final boolean enabled = mCaptioningManager.isEnabled();
        getPreferenceScreen().setEnabled(enabled);
        mPreviewText.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        toggleSwitch.setCheckedInternal(enabled);
        toggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                toggleSwitch.setCheckedInternal(checked);
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, checked ? 1 : 0);
                getPreferenceScreen().setEnabled(checked);
                mPreviewText.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
                return false;
            }
        });
    }

    private void initializeAllPreferences() {
        mLocale = (LocalePreference) findPreference(PREF_LOCALE);
        mFontSize = (ListPreference) findPreference(PREF_FONT_SIZE);

        final Resources res = getResources();
        final int[] presetValues = res.getIntArray(R.array.captioning_preset_selector_values);
        final String[] presetTitles = res.getStringArray(R.array.captioning_preset_selector_titles);
        mPreset = (PresetPreference) findPreference(PREF_PRESET);
        mPreset.setValues(presetValues);
        mPreset.setTitles(presetTitles);

        mCustom = (PreferenceCategory) findPreference(PREF_CUSTOM);
        mShowingCustom = true;

        final int[] colorValues = res.getIntArray(R.array.captioning_color_selector_values);
        final String[] colorTitles = res.getStringArray(R.array.captioning_color_selector_titles);
        mForegroundColor = (ColorPreference) mCustom.findPreference(PREF_FOREGROUND_COLOR);
        mForegroundColor.setTitles(colorTitles);
        mForegroundColor.setValues(colorValues);

        final int[] opacityValues = res.getIntArray(R.array.captioning_opacity_selector_values);
        final String[] opacityTitles = res.getStringArray(
                R.array.captioning_opacity_selector_titles);
        mForegroundOpacity = (ColorPreference) mCustom.findPreference(PREF_FOREGROUND_OPACITY);
        mForegroundOpacity.setTitles(opacityTitles);
        mForegroundOpacity.setValues(opacityValues);

        mEdgeColor = (ColorPreference) mCustom.findPreference(PREF_EDGE_COLOR);
        mEdgeColor.setTitles(colorTitles);
        mEdgeColor.setValues(colorValues);

        // Add "none" as an additional option for backgrounds.
        final int[] bgColorValues = new int[colorValues.length + 1];
        final String[] bgColorTitles = new String[colorTitles.length + 1];
        System.arraycopy(colorValues, 0, bgColorValues, 1, colorValues.length);
        System.arraycopy(colorTitles, 0, bgColorTitles, 1, colorTitles.length);
        bgColorValues[0] = Color.TRANSPARENT;
        bgColorTitles[0] = getString(R.string.color_none);
        mBackgroundColor = (ColorPreference) mCustom.findPreference(PREF_BACKGROUND_COLOR);
        mBackgroundColor.setTitles(bgColorTitles);
        mBackgroundColor.setValues(bgColorValues);

        mBackgroundOpacity = (ColorPreference) mCustom.findPreference(PREF_BACKGROUND_OPACITY);
        mBackgroundOpacity.setTitles(opacityTitles);
        mBackgroundOpacity.setValues(opacityValues);

        mEdgeType = (EdgeTypePreference) mCustom.findPreference(PREF_EDGE_TYPE);
        mTypeface = (ListPreference) mCustom.findPreference(PREF_TYPEFACE);
    }

    private void installUpdateListeners() {
        mPreset.setOnValueChangedListener(this);
        mForegroundColor.setOnValueChangedListener(this);
        mForegroundOpacity.setOnValueChangedListener(this);
        mEdgeColor.setOnValueChangedListener(this);
        mBackgroundColor.setOnValueChangedListener(this);
        mBackgroundOpacity.setOnValueChangedListener(this);
        mEdgeType.setOnValueChangedListener(this);

        mTypeface.setOnPreferenceChangeListener(this);
        mFontSize.setOnPreferenceChangeListener(this);
        mLocale.setOnPreferenceChangeListener(this);
    }

    private void updateAllPreferences() {
        final int preset = mCaptioningManager.getRawUserStyle();
        mPreset.setValue(preset);

        final float fontSize = mCaptioningManager.getFontScale();
        mFontSize.setValue(Float.toString(fontSize));

        final ContentResolver cr = getContentResolver();
        final CaptionStyle attrs = CaptionStyle.getCustomStyle(cr);
        mEdgeType.setValue(attrs.edgeType);
        mEdgeColor.setValue(attrs.edgeColor);

        parseColorOpacity(mForegroundColor, mForegroundOpacity, attrs.foregroundColor);
        parseColorOpacity(mBackgroundColor, mBackgroundOpacity, attrs.backgroundColor);

        final String rawTypeface = attrs.mRawTypeface;
        mTypeface.setValue(rawTypeface == null ? "" : rawTypeface);

        final String rawLocale = mCaptioningManager.getRawLocale();
        mLocale.setValue(rawLocale == null ? "" : rawLocale);
    }

    private void parseColorOpacity(ColorPreference color, ColorPreference opacity, int value) {
        final int colorValue;
        final int opacityValue;
        if (Color.alpha(value) == 0) {
            colorValue = Color.TRANSPARENT;
            opacityValue = (value & 0xFF) << 24;
        } else {
            colorValue = value | 0xFF000000;
            opacityValue = value & 0xFF000000;
        }
        color.setValue(colorValue);
        opacity.setValue(opacityValue | 0xFFFFFF);
    }

    private int mergeColorOpacity(ColorPreference color, ColorPreference opacity) {
        final int colorValue = color.getValue();
        final int opacityValue = opacity.getValue();
        final int value;
        if (Color.alpha(colorValue) == 0) {
            value = Color.alpha(opacityValue);
        } else {
            value = colorValue & 0x00FFFFFF | opacityValue & 0xFF000000;
        }
        return value;
    }

    private void refreshShowingCustom() {
        final boolean customPreset = mPreset.getValue() == CaptionStyle.PRESET_CUSTOM;
        if (!customPreset && mShowingCustom) {
            getPreferenceScreen().removePreference(mCustom);
            mShowingCustom = false;
        } else if (customPreset && !mShowingCustom) {
            getPreferenceScreen().addPreference(mCustom);
            mShowingCustom = true;
        }
    }

    @Override
    public void onValueChanged(ListDialogPreference preference, int value) {
        final ContentResolver cr = getActivity().getContentResolver();
        if (mForegroundColor == preference || mForegroundOpacity == preference) {
            final int merged = mergeColorOpacity(mForegroundColor, mForegroundOpacity);
            Settings.Secure.putInt(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR, merged);
        } else if (mBackgroundColor == preference || mBackgroundOpacity == preference) {
            final int merged = mergeColorOpacity(mBackgroundColor, mBackgroundOpacity);
            Settings.Secure.putInt(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR, merged);
        } else if (mEdgeColor == preference) {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR, value);
        } else if (mPreset == preference) {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, value);
            refreshShowingCustom();
        } else if (mEdgeType == preference) {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE, value);
        }

        refreshPreviewText();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final ContentResolver cr = getActivity().getContentResolver();
        if (mTypeface == preference) {
            Settings.Secure.putString(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE, (String) value);
        } else if (mFontSize == preference) {
            Settings.Secure.putFloat(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE,
                    Float.parseFloat((String) value));
        } else if (mLocale == preference) {
            Settings.Secure.putString(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE, (String) value);
        }

        refreshPreviewText();
        return true;
    }
}
