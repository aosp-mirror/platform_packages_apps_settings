/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display;

import android.app.UiModeManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.SearchIndexableResource;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The screen for selecting the dark theme preference for this device. Automatically updates
 * the associated footer view with any needed information.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class DarkUISettings extends RadioButtonPickerFragment implements Indexable {

  private DarkUISettingsRadioButtonsController mController;
  private Preference mFooter;

  @Override
  protected int getPreferenceScreenResId() {
      return R.xml.dark_ui_settings;
  }

  @Override
  public void onAttach(Context context) {
      super.onAttach(context);
      // TODO(b/128686189): add illustration once it is ready
      setIllustration(0, 0);
      mFooter = new FooterPreference(context);
      mFooter.setIcon(android.R.color.transparent);
      mController = new DarkUISettingsRadioButtonsController(context, mFooter);
  }

  @Override
  protected List<? extends CandidateInfo> getCandidates() {
      final Context context = getContext();
      final List<CandidateInfo> candidates = new ArrayList<>();
      candidates.add(new DarkUISettingsCandidateInfo(
              DarkUISettingsRadioButtonsController.modeToDescription(
                      context, UiModeManager.MODE_NIGHT_YES),
              /* summary */ null,
              DarkUISettingsRadioButtonsController.KEY_DARK,
              /* enabled */ true));
      candidates.add(new DarkUISettingsCandidateInfo(
              DarkUISettingsRadioButtonsController.modeToDescription(
                      context, UiModeManager.MODE_NIGHT_NO),
              /* summary */ null,
              DarkUISettingsRadioButtonsController.KEY_LIGHT,
              /* enabled */ true));
      return candidates;
  }

  @Override
  protected void addStaticPreferences(PreferenceScreen screen) {
      screen.addPreference(mFooter);
  }

  @Override
  protected String getDefaultKey() {
      return mController.getDefaultKey();
  }

  @Override
  protected boolean setDefaultKey(String key) {
      return mController.setDefaultKey(key);
  }

  @Override
  public int getMetricsCategory() {
      return SettingsEnums.DARK_UI_SETTINGS;
  }

  static class DarkUISettingsCandidateInfo extends CandidateInfo {

      private final CharSequence mLabel;
      private final CharSequence mSummary;
      private final String mKey;

      DarkUISettingsCandidateInfo(CharSequence label, CharSequence summary, String key,
              boolean enabled) {
          super(enabled);
          mLabel = label;
          mKey = key;
          mSummary = summary;
      }

      @Override
      public CharSequence loadLabel() {
          return mLabel;
      }

      @Override
      public Drawable loadIcon() {
          return null;
      }

      @Override
      public String getKey() {
          return mKey;
      }

      public CharSequence getSummary() {
          return mSummary;
      }
  }

  public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
      new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
            Context context, boolean enabled) {
          final SearchIndexableResource sir = new SearchIndexableResource(context);
          sir.xmlResId = R.xml.dark_ui_settings;
          return Arrays.asList(sir);
        }
      };
}
