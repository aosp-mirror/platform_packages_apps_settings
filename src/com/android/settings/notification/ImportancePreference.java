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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.android.settingslib.R;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class ImportancePreference extends Preference {

    boolean mIsBlockable = true;
    boolean mIsConfigurable = true;
    int mImportance;
    ImageButton blockButton;
    ImageButton silenceButton;
    ImageButton alertButton;
    ArrayMap<ImageButton, Integer> mImageButtons = new ArrayMap<>();
    Context mContext;

    public ImportancePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public ImportancePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ImportancePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImportancePreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setLayoutResource(R.layout.notif_importance_preference);
    }

    public void setImportance(int importance) {
        mImportance = importance;
    }

    public void setBlockable(boolean blockable) {
        mIsBlockable = blockable;
    }

    public void setConfigurable(boolean configurable) {
        mIsConfigurable = configurable;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View blockView = holder.itemView.findViewById(R.id.block);
        View alertView = holder.itemView.findViewById(R.id.alert);
        View silenceView = holder.itemView.findViewById(R.id.silence);
        if (!mIsBlockable) {
            blockView.setVisibility(View.GONE);
            if (mImportance == IMPORTANCE_NONE) {
                mImportance = IMPORTANCE_LOW;
                callChangeListener(IMPORTANCE_LOW);
            }

        }
        blockButton = blockView.findViewById(R.id.block_icon);
        silenceButton = silenceView.findViewById(R.id.silence_icon);
        alertButton = alertView.findViewById(R.id.alert_icon);
        mImageButtons.put(blockButton, mContext.getColor(R.color.notification_block_color));
        mImageButtons.put(silenceButton, mContext.getColor(R.color.notification_silence_color));
        mImageButtons.put(alertButton, mContext.getColor(R.color.notification_alert_color));

        switch (mImportance) {
            case IMPORTANCE_NONE:
                colorizeImageButton(blockButton.getId());
                if (!mIsConfigurable) {
                    alertView.setVisibility(View.GONE);
                    silenceView.setVisibility(View.GONE);
                }
                break;
            case IMPORTANCE_MIN:
            case IMPORTANCE_LOW:
                colorizeImageButton(silenceButton.getId());
                if (!mIsConfigurable) {
                    alertView.setVisibility(View.GONE);
                    blockView.setVisibility(View.GONE);
                }
                break;
            case IMPORTANCE_HIGH:
            default:
                colorizeImageButton(alertButton.getId());
                if (!mIsConfigurable) {
                    blockView.setVisibility(View.GONE);
                    silenceView.setVisibility(View.GONE);
                }
                break;
        }

        blockButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_NONE);
            colorizeImageButton(blockButton.getId());
        });
        silenceButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_LOW);
            colorizeImageButton(silenceButton.getId());
        });
        alertButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_DEFAULT);
            colorizeImageButton(alertButton.getId());
        });
    }

    private void colorizeImageButton(int buttonId) {
        if (mImageButtons != null) {
            for (int i = 0; i < mImageButtons.size(); i++) {
                final ImageButton imageButton = mImageButtons.keyAt(i);
                final int color = mImageButtons.valueAt(i);
                if (imageButton != null) {
                    LayerDrawable drawable = (LayerDrawable) imageButton.getDrawable();
                    Drawable foreground = drawable.findDrawableByLayerId(R.id.fore);
                    GradientDrawable background =
                            (GradientDrawable) drawable.findDrawableByLayerId(R.id.back);
                    if (buttonId == imageButton.getId()) {
                        foreground.setTint(Color.WHITE);
                        background.setColor(color);
                    } else {
                        foreground.setTint(color);
                        background.setColor(Color.TRANSPARENT);
                    }
                }
            }
        }
    }
}
