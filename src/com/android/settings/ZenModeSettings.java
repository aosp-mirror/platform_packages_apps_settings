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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ZenModeSettings extends SettingsPreferenceFragment {
    private static final String TAG = "ZenModeSettings";
    private static final boolean DEBUG = false;

    private ZenModeConfigView mConfig;

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

    public static final class ZenModeConfigView extends LinearLayout {
        private static final Typeface LIGHT =
                Typeface.create("sans-serif-light", Typeface.NORMAL);
        private static final int BG_COLOR = 0xffe7e8e9;
        private final Context mContext;

        private Drawable mOldBackground;
        private Toast mToast;

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
            addView(new TriggerView("While driving"));
            addView(new TriggerView("While in meetings"));
            addView(new TriggerView("During a set time period"));
        }

        private void addBuckets() {
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            BucketView callView = new BucketView(android.R.drawable.ic_menu_call, "Calls", 0,
                    "Block all", "Starred contacts only", "Allow all");
            addView(callView, lp);
            lp.topMargin = 4;
            BucketView msgView = new BucketView(android.R.drawable.ic_menu_myplaces,
                    "Text & SMS Messages", 0,
                    "Block all", "Starred contacts only", "Allow all");
            addView(msgView, lp);
            BucketView alarmView = new BucketView(android.R.drawable.ic_menu_agenda,
                    "Alarms & Timers", 1,
                    "Block all", "Allow all");
            addView(alarmView, lp);
            BucketView otherView = new BucketView(android.R.drawable.ic_menu_info_details,
                    "Other Interruptions", 0,
                    "Block all", "Block all except...");
            addView(otherView, lp);
        }

        private void notImplemented() {
            if (mToast != null) mToast.cancel();
            mToast = Toast.makeText(mContext, "Not implemented", Toast.LENGTH_SHORT);
            mToast.show();
        }

        private class BucketView extends RelativeLayout {
            private final BucketSpinner mSpinner;

            public BucketView(int icon, String category, int defaultValue, String... values) {
                super(ZenModeConfigView.this.mContext);

                setBackgroundColor(0xffffffff);
                final int p = getResources().getDimensionPixelSize(R.dimen.content_margin_left);

                final ImageView iv = new ImageView(mContext);
                iv.setId(android.R.id.icon);
                iv.setImageResource(icon);
                iv.setAlpha(.5f);

                final int size = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.app_icon_size);
                LayoutParams lp = new LayoutParams(size, size);
                lp.addRule(CENTER_VERTICAL);
                lp.leftMargin = 16;
                lp.rightMargin = 16;
                addView(iv, lp);

                TextView tv = new TextView(mContext);
                tv.setPadding(4, 0, 0, 0);
                tv.setId(android.R.id.title);
                tv.setTextColor(0xff000000);
                tv.setText(category);
                tv.setAllCaps(true);
                lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                lp.addRule(RIGHT_OF, iv.getId());
                lp.topMargin = p / 2;
                addView(tv, lp);

                mSpinner = new BucketSpinner(defaultValue, values);
                lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                lp.addRule(RIGHT_OF, iv.getId());
                lp.addRule(BELOW, tv.getId());
                addView(mSpinner, lp);
            }
        }

        private class BucketSpinner extends Spinner {
            private final Bitmap mArrow;

            public BucketSpinner(int defaultValue, String... values) {
                super(ZenModeConfigView.this.mContext);
                setGravity(Gravity.LEFT);
                mArrow = BitmapFactory.decodeResource(getResources(),
                        R.drawable.spinner_default_holo_dark_am_no_underline);
                setPadding(0, 0, getPaddingRight(), getPaddingBottom());
                setBackgroundColor(0x00000000);
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, 0) {
                    @Override
                    public View getView(int position, View convertView,  ViewGroup parent) {
                        return getDropDownView(position, convertView, parent);
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        final TextView tv = convertView != null ? (TextView) convertView
                                : new TextView(ZenModeConfigView.this.mContext);
                        tv.setText(getItem(position));
                        if (convertView == null) {
                            tv.setTypeface(LIGHT);
                            tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                            tv.setTextColor(0xff000000);
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() * 1.5f);
                            final int p = (int) tv.getTextSize() / 2;
                            if (parent instanceof ListView) {
                                final ListView lv = (ListView)parent;
                                lv.setDividerHeight(0);
                                tv.setBackgroundColor(BG_COLOR);
                                tv.setPadding(p, p, p, p);
                            } else {
                                tv.setPadding(0, 0, p, 0);
                            }
                        }
                        return tv;
                    }
                };
                adapter.addAll(values);
                setAdapter(adapter);
                setSelection(defaultValue, true);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                final TextView tv = (TextView)getSelectedView();
                final int w = (int)tv.getLayout().getLineWidth(0);
                final int left = w - mArrow.getWidth() / 4;
                final int top = getHeight() - mArrow.getHeight();
                canvas.drawBitmap(mArrow, left, top, null);
                super.onDraw(canvas);
            }

            @Override
            public void setSelection(int position) {
                if (position != getSelectedItemPosition()) {
                    notImplemented();
                }
            }
        }

        private class TriggerView extends RelativeLayout {
            public TriggerView(String text) {
                super(ZenModeConfigView.this.mContext);

                setBackgroundColor(0xffffffff);
                final int p = getResources().getDimensionPixelSize(R.dimen.content_margin_left);
                final int p2 = p / 4;
                setPadding(p2, p2, p2, p2);

                final CheckBox cb = new CheckBox(mContext);
                cb.setId(android.R.id.checkbox);
                cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            cb.setChecked(false);
                            notImplemented();
                        }
                    }
                });
                LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT);
                lp.addRule(ALIGN_PARENT_RIGHT);
                addView(cb, lp);

                final TextView tv = new TextView(mContext);
                tv.setText(text);
                tv.setTypeface(LIGHT);
                tv.setTextColor(0xff000000);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() * 1.5f);
                final int p3 = p / 2;
                tv.setPadding(p3, 0, p3, 0);
                lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                lp.addRule(LEFT_OF, cb.getId());
                lp.addRule(CENTER_VERTICAL);
                addView(tv, lp);
            }
        }
    }
}
