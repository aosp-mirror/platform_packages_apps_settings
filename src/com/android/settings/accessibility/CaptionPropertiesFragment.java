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

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.ListDialogPreference.OnValueChangedListener;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Settings fragment containing captioning properties.
 */
public class CaptionPropertiesFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnValueChangedListener {
    private static final String PREF_CAPTION_PREVIEW = "caption_preview";
    private static final String PREF_BACKGROUND_COLOR = "captioning_background_color";
    private static final String PREF_BACKGROUND_OPACITY = "captioning_background_opacity";
    private static final String PREF_FOREGROUND_COLOR = "captioning_foreground_color";
    private static final String PREF_FOREGROUND_OPACITY = "captioning_foreground_opacity";
    private static final String PREF_WINDOW_COLOR = "captioning_window_color";
    private static final String PREF_WINDOW_OPACITY = "captioning_window_opacity";
    private static final String PREF_EDGE_COLOR = "captioning_edge_color";
    private static final String PREF_EDGE_TYPE = "captioning_edge_type";
    private static final String PREF_FONT_SIZE = "captioning_font_size";
    private static final String PREF_TYPEFACE = "captioning_typeface";
    private static final String PREF_LOCALE = "captioning_locale";
    private static final String PREF_PRESET = "captioning_preset";
    private static final String PREF_SWITCH = "captioning_preference_switch";
    private static final String PREF_CUSTOM = "custom";

    /** WebVtt specifies line height as 5.3% of the viewport height. */
    private static final float LINE_HEIGHT_RATIO = 0.0533f;

    private CaptioningManager mCaptioningManager;
    private SubtitleView mPreviewText;
    private View mPreviewWindow;
    private View mPreviewViewport;

    // Standard options.
    private SwitchPreference mSwitch;
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
    private ColorPreference mWindowColor;
    private ColorPreference mWindowOpacity;
    private PreferenceCategory mCustom;

    private boolean mShowingCustom;

    private final List<Preference> mPreferenceList = new ArrayList<>();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_CAPTION_PROPERTIES;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);

        addPreferencesFromResource(R.xml.captioning_settings);
        initializeAllPreferences();
        updateAllPreferences();
        refreshShowingCustom();
        installUpdateListeners();
        refreshPreviewText();
    }

    private void setPreferenceViewEnabled(boolean enabled) {
        for (Preference preference : mPreferenceList) {
            preference.setEnabled(enabled);
        }
    }

    private void refreshPreferenceViewEnabled(boolean enabled) {
        setPreferenceViewEnabled(enabled);
        mPreviewText.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
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
            applyCaptionProperties(mCaptioningManager, preview, mPreviewViewport, styleId);

            final Locale locale = mCaptioningManager.getLocale();
            if (locale != null) {
                final CharSequence localizedText = AccessibilityUtils.getTextForLocale(
                        context, locale, R.string.captioning_preview_text);
                preview.setText(localizedText);
            } else {
                preview.setText(R.string.captioning_preview_text);
            }

            final CaptionStyle style = mCaptioningManager.getUserStyle();
            if (style.hasWindowColor()) {
                mPreviewWindow.setBackgroundColor(style.windowColor);
            } else {
                final CaptionStyle defStyle = CaptionStyle.DEFAULT;
                mPreviewWindow.setBackgroundColor(defStyle.windowColor);
            }
        }
    }

    public static void applyCaptionProperties(CaptioningManager manager, SubtitleView previewText,
            View previewWindow, int styleId) {
        previewText.setStyle(styleId);

        final Context context = previewText.getContext();
        final ContentResolver cr = context.getContentResolver();
        final float fontScale = manager.getFontScale();
        if (previewWindow != null) {
            // Assume the viewport is clipped with a 16:9 aspect ratio.
            final float virtualHeight = Math.max(9 * previewWindow.getWidth(),
                    16 * previewWindow.getHeight()) / 16.0f;
            previewText.setTextSize(virtualHeight * LINE_HEIGHT_RATIO * fontScale);
        } else {
            final float textSize = context.getResources().getDimension(
                    R.dimen.caption_preview_text_size);
            previewText.setTextSize(textSize * fontScale);
        }

        final Locale locale = manager.getLocale();
        if (locale != null) {
            final CharSequence localizedText = AccessibilityUtils.getTextForLocale(
                    context, locale, R.string.captioning_preview_characters);
            previewText.setText(localizedText);
        } else {
            previewText.setText(R.string.captioning_preview_characters);
        }
    }

    private void initializeAllPreferences() {
        final LayoutPreference captionPreview = findPreference(PREF_CAPTION_PREVIEW);

        mPreviewText = captionPreview.findViewById(R.id.preview_text);

        mPreviewWindow = captionPreview.findViewById(R.id.preview_window);

        mPreviewViewport = captionPreview.findViewById(R.id.preview_viewport);
        mPreviewViewport.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> refreshPreviewText());

        final Resources res = getResources();
        final int[] presetValues = res.getIntArray(R.array.captioning_preset_selector_values);
        final String[] presetTitles = res.getStringArray(R.array.captioning_preset_selector_titles);
        mPreset = (PresetPreference) findPreference(PREF_PRESET);
        mPreset.setValues(presetValues);
        mPreset.setTitles(presetTitles);

        mSwitch = (SwitchPreference) findPreference(PREF_SWITCH);
        mLocale = (LocalePreference) findPreference(PREF_LOCALE);
        mFontSize = (ListPreference) findPreference(PREF_FONT_SIZE);

        // Initialize the preference list
        mPreferenceList.add(mLocale);
        mPreferenceList.add(mFontSize);
        mPreferenceList.add(mPreset);

        refreshPreferenceViewEnabled(mCaptioningManager.isEnabled());

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

        mWindowColor = (ColorPreference) mCustom.findPreference(PREF_WINDOW_COLOR);
        mWindowColor.setTitles(bgColorTitles);
        mWindowColor.setValues(bgColorValues);

        mWindowOpacity = (ColorPreference) mCustom.findPreference(PREF_WINDOW_OPACITY);
        mWindowOpacity.setTitles(opacityTitles);
        mWindowOpacity.setValues(opacityValues);

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
        mWindowColor.setOnValueChangedListener(this);
        mWindowOpacity.setOnValueChangedListener(this);
        mEdgeType.setOnValueChangedListener(this);

        mSwitch.setOnPreferenceChangeListener(this);
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

        final int foregroundColor = attrs.hasForegroundColor() ?
                attrs.foregroundColor : CaptionStyle.COLOR_UNSPECIFIED;
        parseColorOpacity(mForegroundColor, mForegroundOpacity, foregroundColor);

        final int backgroundColor = attrs.hasBackgroundColor() ?
                attrs.backgroundColor : CaptionStyle.COLOR_UNSPECIFIED;
        parseColorOpacity(mBackgroundColor, mBackgroundOpacity, backgroundColor);

        final int windowColor = attrs.hasWindowColor() ?
                attrs.windowColor : CaptionStyle.COLOR_UNSPECIFIED;
        parseColorOpacity(mWindowColor, mWindowOpacity, windowColor);

        final String rawTypeface = attrs.mRawTypeface;
        mTypeface.setValue(rawTypeface == null ? "" : rawTypeface);

        final String rawLocale = mCaptioningManager.getRawLocale();
        mLocale.setValue(rawLocale == null ? "" : rawLocale);

        mSwitch.setChecked(mCaptioningManager.isEnabled());
    }

    /**
     * Unpack the specified color value and update the preferences.
     *
     * @param color   color preference
     * @param opacity opacity preference
     * @param value   packed value
     */
    private void parseColorOpacity(ColorPreference color, ColorPreference opacity, int value) {
        final int colorValue;
        final int opacityValue;
        if (!CaptionStyle.hasColor(value)) {
            // "Default" color with variable alpha.
            colorValue = CaptionStyle.COLOR_UNSPECIFIED;
            opacityValue = (value & 0xFF) << 24;
        } else if ((value >>> 24) == 0) {
            // "None" color with variable alpha.
            colorValue = Color.TRANSPARENT;
            opacityValue = (value & 0xFF) << 24;
        } else {
            // Normal color.
            colorValue = value | 0xFF000000;
            opacityValue = value & 0xFF000000;
        }

        // Opacity value is always white.
        opacity.setValue(opacityValue | 0xFFFFFF);
        color.setValue(colorValue);
    }

    private int mergeColorOpacity(ColorPreference color, ColorPreference opacity) {
        final int colorValue = color.getValue();
        final int opacityValue = opacity.getValue();
        final int value;
        // "Default" is 0x00FFFFFF or, for legacy support, 0x00000100.
        if (!CaptionStyle.hasColor(colorValue)) {
            // Encode "default" as 0x00FFFFaa.
            value = 0x00FFFF00 | Color.alpha(opacityValue);
        } else if (colorValue == Color.TRANSPARENT) {
            // Encode "none" as 0x000000aa.
            value = Color.alpha(opacityValue);
        } else {
            // Encode custom color normally.
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
        } else if (mWindowColor == preference || mWindowOpacity == preference) {
            final int merged = mergeColorOpacity(mWindowColor, mWindowOpacity);
            Settings.Secure.putInt(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR, merged);
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
            refreshPreviewText();
        } else if (mFontSize == preference) {
            Settings.Secure.putFloat(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE,
                    Float.parseFloat((String) value));
            refreshPreviewText();
        } else if (mLocale == preference) {
            Settings.Secure.putString(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE, (String) value);
            refreshPreviewText();
        } else if (mSwitch == preference) {
            Settings.Secure.putInt(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, (boolean) value ? 1 : 0);
            refreshPreferenceViewEnabled((boolean) value);
        }

        return true;
    }
}
