/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


public class Display extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.display);

        mFontSize = (Spinner) findViewById(R.id.fontSize);
        mFontSize.setOnItemSelectedListener(mFontSizeChanged);
        String[] states = new String[3];
        Resources r = getResources();
        states[0] = r.getString(R.string.small_font);
        states[1] = r.getString(R.string.medium_font);
        states[2] = r.getString(R.string.large_font);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, states);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mFontSize.setAdapter(adapter);

        mPreview = (TextView) findViewById(R.id.preview);
        mPreview.setText(r.getText(R.string.font_size_preview_text));

        Button save = (Button) findViewById(R.id.save);
        save.setText(r.getText(R.string.font_size_save));
        save.setOnClickListener(this);

        mTextSizeTyped = new TypedValue();
        TypedArray styledAttributes = 
            obtainStyledAttributes(android.R.styleable.TextView);
        styledAttributes.getValue(android.R.styleable.TextView_textSize,
                mTextSizeTyped);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDisplayMetrics = new DisplayMetrics();
        mDisplayMetrics.density = metrics.density;
        mDisplayMetrics.heightPixels = metrics.heightPixels;
        mDisplayMetrics.scaledDensity = metrics.scaledDensity;
        mDisplayMetrics.widthPixels = metrics.widthPixels;
        mDisplayMetrics.xdpi = metrics.xdpi;
        mDisplayMetrics.ydpi = metrics.ydpi;
        
        styledAttributes.recycle();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mCurConfig.updateFrom(
                ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
        }
        if (mCurConfig.fontScale < 1) {
            mFontSize.setSelection(0);
        } else if (mCurConfig.fontScale > 1) {
            mFontSize.setSelection(2);
        } else {
            mFontSize.setSelection(1);
        }
        updateFontScale();
    }

    private void updateFontScale() {
        mDisplayMetrics.scaledDensity = mDisplayMetrics.density *
                mCurConfig.fontScale;

        float size = mTextSizeTyped.getDimension(mDisplayMetrics);
        mPreview.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void onClick(View v) {
        try {
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
        }
        finish();
    }

    private Spinner.OnItemSelectedListener mFontSizeChanged
                                    = new Spinner.OnItemSelectedListener() {
        public void onItemSelected(android.widget.AdapterView av, View v,
                                    int position, long id) {
            if (position == 0) {
                mCurConfig.fontScale = .75f;
            } else if (position == 2) {
                mCurConfig.fontScale = 1.25f;
            } else {
                mCurConfig.fontScale = 1.0f;
            }

            updateFontScale();
        }

        public void onNothingSelected(android.widget.AdapterView av) {
        }
    };

    private Spinner mFontSize;
    private TextView mPreview;
    private TypedValue mTextSizeTyped;
    private DisplayMetrics mDisplayMetrics;
    private Configuration mCurConfig = new Configuration();
}
