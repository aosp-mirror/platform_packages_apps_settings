/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterysaver;

import android.app.settings.SettingsEnums;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

/**
 * Battery saver settings page
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class BatterySaverSettings extends DashboardFragment {
    private static final String TAG = "BatterySaverSettings";
    private static final String KEY_FOOTER_PREFERENCE = "battery_saver_footer_preference";
    private SpannableStringBuilder mFooterText;
    private String mHelpUri;

    @Override
    public void onStart() {
        super.onStart();
        setupFooter();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_BATTERY_SAVER;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.battery_saver_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery_saver_settings;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.battery_saver_settings);

    // Updates the footer for this page.
    @VisibleForTesting
    void setupFooter() {
        mFooterText =  new SpannableStringBuilder(getText(
                com.android.internal.R.string.battery_saver_description_with_learn_more));
        mHelpUri = getString(R.string.help_url_battery_saver_settings);
        if (!TextUtils.isEmpty(mHelpUri)) {
            addHelpLink();
        }
    }

    // Changes the text to include a learn more link if possible.
    @VisibleForTesting
    void addHelpLink() {
        FooterPreference pref = getPreferenceScreen().findPreference(KEY_FOOTER_PREFERENCE);
        if (pref != null) {
            SupportPageLearnMoreSpan.linkify(mFooterText, this, mHelpUri);
            pref.setTitle(mFooterText);
        }
    }

    /**
     * A {@link URLSpan} that opens a support page when clicked
     */
    public static class SupportPageLearnMoreSpan extends URLSpan {


        private static final String ANNOTATION_URL = "url";
        private final Fragment mFragment;
        private final String mUriString;

        public SupportPageLearnMoreSpan(Fragment fragment, String uriString) {
            // sets the url to empty string so we can prevent any other span processing from
            // from clearing things we need in this string.
            super("");
            mFragment = fragment;
            mUriString = uriString;
        }

        @Override
        public void onClick(View widget) {
            if (mFragment != null) {
                // launch the support page
                mFragment.startActivityForResult(HelpUtils.getHelpIntent(mFragment.getContext(),
                        mUriString, ""), 0);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            // remove underline
            ds.setUnderlineText(false);
        }

        /**
         * This method takes a string and turns it into a url span that will launch a support page
         * @param msg The text to turn into a link
         * @param fragment The fragment which contains this span
         * @param uriString The URI string of the help article to open when clicked
         * @return A CharSequence containing the original text content as a url
         */
        public static CharSequence linkify(Spannable msg, Fragment fragment, String uriString) {
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            for (Annotation annotation : spans) {
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                if (ANNOTATION_URL.equals(annotation.getValue())) {
                    SupportPageLearnMoreSpan link =
                            new SupportPageLearnMoreSpan(fragment, uriString);
                    msg.removeSpan(annotation);
                    msg.setSpan(link, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return msg;
        }
    }
}
