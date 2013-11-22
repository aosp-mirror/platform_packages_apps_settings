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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.ListDialogPreference.OnValueChangedListener;

/**
 * Settings fragment containing captioning properties.
 */
public class CaptionPropertiesFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnValueChangedListener {
    private ToggleCaptioningPreferenceFragment mParent;
    private CaptioningManager mCaptioningManager;

    // Standard options.
    private LocalePreference mLocale;
    private ListPreference mFontSize;
    private PresetPreference mPreset;

    // Custom options.
    private ListPreference mTypeface;
    private ColorPreference mForegroundColor;
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

    /**
     * Sets the parent fragment, which is used to update the live preview.
     *
     * @param parent the parent fragment
     */
    public void setParent(ToggleCaptioningPreferenceFragment parent) {
        mParent = parent;
    }

    private void initializeAllPreferences() {
        mLocale = (LocalePreference) findPreference("captioning_locale");
        mFontSize = (ListPreference) findPreference("captioning_font_size");

        final Resources res = getResources();
        final int[] presetValues = res.getIntArray(R.array.captioning_preset_selector_values);
        final String[] presetTitles = res.getStringArray(R.array.captioning_preset_selector_titles);
        mPreset = (PresetPreference) findPreference("captioning_preset");
        mPreset.setValues(presetValues);
        mPreset.setTitles(presetTitles);

        mCustom = (PreferenceCategory) findPreference("custom");
        mShowingCustom = true;

        final int[] colorValues = res.getIntArray(R.array.captioning_color_selector_values);
        final String[] colorTitles = res.getStringArray(R.array.captioning_color_selector_titles);
        mForegroundColor = (ColorPreference) mCustom.findPreference("captioning_foreground_color");
        mForegroundColor.setTitles(colorTitles);
        mForegroundColor.setValues(colorValues);
        mEdgeColor = (ColorPreference) mCustom.findPreference("captioning_edge_color");
        mEdgeColor.setTitles(colorTitles);
        mEdgeColor.setValues(colorValues);

        // Add "none" as an additional option for backgrounds.
        final int[] bgColorValues = new int[colorValues.length + 1];
        final String[] bgColorTitles = new String[colorTitles.length + 1];
        System.arraycopy(colorValues, 0, bgColorValues, 1, colorValues.length);
        System.arraycopy(colorTitles, 0, bgColorTitles, 1, colorTitles.length);
        bgColorValues[0] = Color.TRANSPARENT;
        bgColorTitles[0] = getString(R.string.color_none);
        mBackgroundColor = (ColorPreference) mCustom.findPreference("captioning_background_color");
        mBackgroundColor.setTitles(bgColorTitles);
        mBackgroundColor.setValues(bgColorValues);

        final int[] opacityValues = res.getIntArray(R.array.captioning_opacity_selector_values);
        final String[] opacityTitles = res.getStringArray(
                R.array.captioning_opacity_selector_titles);
        mBackgroundOpacity = (ColorPreference) mCustom.findPreference(
                "captioning_background_opacity");
        mBackgroundOpacity.setTitles(opacityTitles);
        mBackgroundOpacity.setValues(opacityValues);

        mEdgeType = (EdgeTypePreference) mCustom.findPreference("captioning_edge_type");
        mTypeface = (ListPreference) mCustom.findPreference("captioning_typeface");
    }

    private void installUpdateListeners() {
        mPreset.setOnValueChangedListener(this);
        mForegroundColor.setOnValueChangedListener(this);
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
        mForegroundColor.setValue(attrs.foregroundColor);
        mEdgeType.setValue(attrs.edgeType);
        mEdgeColor.setValue(attrs.edgeColor);

        final int backgroundColor = attrs.backgroundColor;
        final int bgColor;
        final int bgAlpha;
        if (Color.alpha(backgroundColor) == 0) {
            bgColor = Color.TRANSPARENT;
            bgAlpha = (backgroundColor & 0xFF) << 24;
        } else {
            bgColor = backgroundColor | 0xFF000000;
            bgAlpha = backgroundColor & 0xFF000000;
        }
        mBackgroundColor.setValue(bgColor);
        mBackgroundOpacity.setValue(bgAlpha | 0xFFFFFF);

        final String rawTypeface = attrs.mRawTypeface;
        mTypeface.setValue(rawTypeface == null ? "" : rawTypeface);

        final String rawLocale = mCaptioningManager.getRawLocale();
        mLocale.setValue(rawLocale == null ? "" : rawLocale);
    }

    private void refreshPreviewText() {
        if (mParent != null) {
            mParent.refreshPreviewText();
        }
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
        if (mForegroundColor == preference) {
            Settings.Secure.putInt(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR, value);
        } else if (mBackgroundColor == preference || mBackgroundOpacity == preference) {
            final int bgColor = mBackgroundColor.getValue();
            final int bgAlpha = mBackgroundOpacity.getValue();
            final int argb;
            if (Color.alpha(bgColor) == 0) {
                argb = Color.alpha(bgAlpha);
            } else {
                argb = bgColor & 0x00FFFFFF | bgAlpha & 0xFF000000;
            }
            Settings.Secure.putInt(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR, argb);
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
