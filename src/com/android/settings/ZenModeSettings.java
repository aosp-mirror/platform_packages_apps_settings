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

package com.android.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class ZenModeSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "ZenModeSettings";
    private static final boolean DEBUG = false;

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private ZenModeConfigView mConfig;
    private Switch mSwitch;
    private Activity mActivity;
    private MenuItem mSearch;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mActivity = getActivity();
        mSwitch = new Switch(mActivity.getActionBar().getThemedContext());
        final int p = getResources().getDimensionPixelSize(R.dimen.content_margin_left);
        mSwitch.setPadding(0, 0, p, 0);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mSearch = menu.findItem(R.id.search);
        if (mSearch != null) mSearch.setVisible(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        mSettingsObserver.register();
        mActivity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        mActivity.getActionBar().setCustomView(mSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));
        if (mSearch != null) mSearch.setVisible(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.unregister();
        mActivity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        if (mSearch != null) mSearch.setVisible(true);
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE_URI = Global.getUriFor(Global.ZEN_MODE);

        public SettingsObserver() {
            super(mHandler);
        }

        public void register() {
            getContentResolver().registerContentObserver(ZEN_MODE_URI, false, this);
        }

        public void unregister() {
            getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (ZEN_MODE_URI.equals(uri)) {
                updateState();
            }
        }
    };

    private void updateState() {
        mSwitch.setOnCheckedChangeListener(null);
        final boolean zenMode = Global.getInt(getContentResolver(),
                Global.ZEN_MODE, Global.ZEN_MODE_OFF) != Global.ZEN_MODE_OFF;
        mSwitch.setChecked(zenMode);
        mSwitch.setOnCheckedChangeListener(mSwitchListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final Context context = getActivity();
        final ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(false);
        sv.setHorizontalScrollBarEnabled(false);
        mConfig = new ZenModeConfigView(context);
        sv.addView(mConfig);
        return sv;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mConfig.resetBackground();
    }

    private final OnCheckedChangeListener mSwitchListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final int v = isChecked ? Global.ZEN_MODE_ON : Global.ZEN_MODE_OFF;
                    Global.putInt(getContentResolver(), Global.ZEN_MODE, v);
                }
            });
        }
    };

    public static final class ZenModeConfigView extends LinearLayout {
        private static final Typeface LIGHT =
                Typeface.create("sans-serif-light", Typeface.NORMAL);
        private static final int BG_COLOR = 0xffe7e8e9;
        private final Context mContext;

        private Drawable mOldBackground;

        public ZenModeConfigView(Context context) {
            super(context);
            mContext = context;
            setOrientation(VERTICAL);

            int p = getResources().getDimensionPixelSize(R.dimen.content_margin_left);
            TextView tv = addHeader("When on");
            tv.setPadding(0, p / 2, 0, p / 4);
            addBuckets();
            tv = addHeader("Automatically turn on");
            tv.setPadding(0, p / 2, 0, p / 4);
            addTriggers();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mOldBackground = getParentView().getBackground();
            if (DEBUG) Log.d(TAG, "onAttachedToWindow mOldBackground=" + mOldBackground);
            getParentView().setBackgroundColor(BG_COLOR);
        }

        public void resetBackground() {
            if (DEBUG) Log.d(TAG, "resetBackground");
            getParentView().setBackground(mOldBackground);
        }

        private View getParentView() {
            return (View)getParent().getParent();
        }

        private TextView addHeader(String text) {
            TextView tv = new TextView(mContext);
            tv.setTypeface(LIGHT);
            tv.setTextColor(0x7f000000);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() * 1.5f);
            tv.setText(text);
            addView(tv);
            return tv;
        }

        private void addTriggers() {
            addView(new TriggerView("Never"));
        }

        private void addBuckets() {
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            BucketView callView = new BucketView("Phone calls", 0,
                    "Block all", "Block all except...", "Allow all");
            addView(callView, lp);
            lp.topMargin = 4;
            BucketView msgView = new BucketView("Texts, SMS, & other calls", 0,
                    "Block all", "Block all except...", "Allow all");
            addView(msgView, lp);
            BucketView alarmView = new BucketView("Alarms & timers", 2,
                    "Block all", "Block all except...", "Allow all");
            addView(alarmView, lp);
            BucketView otherView = new BucketView("Other interruptions", 0,
                    "Block all", "Block all except...", "Allow all");
            addView(otherView, lp);
        }

        private class BucketView extends RelativeLayout {
            public BucketView(String category, int defaultValue, String... values) {
                super(ZenModeConfigView.this.mContext);

                setBackgroundColor(0xffffffff);
                final int p = getResources().getDimensionPixelSize(R.dimen.content_margin_left);
                final int lm = p * 3 / 4;
                TextView title = new TextView(mContext);
                title.setId(android.R.id.title);
                title.setTextColor(0xff000000);
                title.setTypeface(LIGHT);
                title.setText(category);
                title.setTextSize(TypedValue.COMPLEX_UNIT_PX, title.getTextSize() * 1.5f);
                LayoutParams lp =
                        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                lp.topMargin = p / 2;
                lp.leftMargin = lm;
                addView(title, lp);

                TextView subtitle = new TextView(mContext);
                subtitle.setTextColor(0xff000000);
                subtitle.setTypeface(LIGHT);
                subtitle.setText(values[defaultValue]);
                lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                lp.addRule(BELOW, title.getId());
                lp.leftMargin = lm;
                lp.bottomMargin = p / 2;
                addView(subtitle, lp);
            }
        }

        private class TriggerView extends RelativeLayout {
            public TriggerView(String text) {
                super(ZenModeConfigView.this.mContext);

                setBackgroundColor(0xffffffff);
                final int p = getResources().getDimensionPixelSize(R.dimen.content_margin_left);

                final TextView tv = new TextView(mContext);
                tv.setText(text);
                tv.setTypeface(LIGHT);
                tv.setTextColor(0xff000000);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() * 1.5f);
                LayoutParams lp =
                        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                lp.addRule(CENTER_VERTICAL);
                lp.bottomMargin = p / 2;
                lp.topMargin = p / 2;
                lp.leftMargin = p * 3 / 4;
                addView(tv, lp);
            }
        }
    }

    // Enable indexing of searchable data
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();

                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.zen_mode_settings_title);
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "When on";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "Calls";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "Text & SMS Messages";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "Alarms & Timers";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "Other Interruptions";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "Automatically turn on";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "While driving";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "While in meetings";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = "During a set time period";
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);

                return result;
            }
        };

}
