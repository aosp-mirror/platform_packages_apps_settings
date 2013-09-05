/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.android.settings.R;

public class StreamVolumePreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private boolean mProtectFromCheckedChange = false;

    private CheckBox mCheckBox;

    final static String TAG = "StreamVolumePreference";

    private ProfileConfig.StreamItem mStreamItem;

    private SeekBar mBar;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public StreamVolumePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * @param context
     * @param attrs
     */
    public StreamVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * @param context
     */
    public StreamVolumePreference(Context context) {
        super(context);
        init();
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        View widget = view.findViewById(R.id.profile_checkbox);
        if ((widget != null) && widget instanceof CheckBox) {
            mCheckBox = (CheckBox) widget;
            mCheckBox.setOnCheckedChangeListener(this);

            mProtectFromCheckedChange = true;
            mCheckBox.setChecked(isChecked());
            mProtectFromCheckedChange = false;
        }

        View textLayout = view.findViewById(R.id.text_layout);
        if ((textLayout != null) && textLayout instanceof LinearLayout) {
            textLayout.setOnClickListener(this);
        }

        return view;
    }

    private void init() {
        setLayoutResource(R.layout.preference_streamvolume);
    }

    public boolean isChecked() {
        return mStreamItem != null && mStreamItem.mSettings.isOverride();
    }

    public void setStreamItem(ProfileConfig.StreamItem streamItem) {
        mStreamItem = streamItem;
        
        if (mCheckBox != null) {
            mCheckBox.setChecked(mStreamItem.mSettings.isOverride());
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mProtectFromCheckedChange) {
            return;
        }

        mStreamItem.mSettings.setOverride(isChecked);

        callChangeListener(isChecked);
    }

    protected Dialog createVolumeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        builder.setTitle(mStreamItem.mLabel);
        mBar = new SeekBar(getContext());
        mBar.setPaddingRelative(32, 16, 32, 16); // TODO: confirm appropriate padding
        mBar.setMax(am.getStreamMaxVolume(mStreamItem.mStreamId));
        mBar.setProgress(mStreamItem.mSettings.getValue());
        builder.setView(mBar);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int value = mBar.getProgress();
                mStreamItem.mSettings.setValue(value);
                setSummary(getContext().getString(R.string.volume_override_summary) + " " + value + "/" + mBar.getMax());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        return builder.create();
    }

    public ProfileConfig.StreamItem getStreamItem() {
        return mStreamItem;
    }

    @Override
    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            createVolumeDialog().show();
        }
    }
}
