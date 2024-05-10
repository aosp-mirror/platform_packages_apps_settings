/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.applications.appcompat;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * Radio button preference with image at the bottom.
 *
 * <p>Layout should stay the same as
 * {@link com.android.settingslib.widget.SelectorWithWidgetPreference} for consistency.
 */
public class RadioWithImagePreference extends CheckBoxPreference {

    /**
     * Interface definition for a callback to be invoked when the preference is clicked.
     */
    public interface OnClickListener {
        /**
         * Called when a preference has been clicked.
         *
         * @param emiter The clicked preference
         */
        void onRadioButtonClicked(RadioWithImagePreference emiter);
    }

    private OnClickListener mListener = null;

    /**
     * Performs inflation from XML and apply a class-specific base style.
     *
     * @param context  The {@link Context} this is associated with, through which it can
     *                 access the current theme, resources, {@link SharedPreferences}, etc.
     * @param attrs    The attributes of the XML tag that is inflating the preference
     * @param defStyle An attribute in the current theme that contains a reference to a style
     *                 resource that supplies default values for the view. Can be 0 to not
     *                 look for defaults.
     */
    public RadioWithImagePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Performs inflation from XML and apply a class-specific base style.
     *
     * @param context The {@link Context} this is associated with, through which it can
     *                access the current theme, resources, {@link SharedPreferences}, etc.
     * @param attrs   The attributes of the XML tag that is inflating the preference
     */
    public RadioWithImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor to create a preference.
     *
     * @param context The Context this is associated with.
     */
    public RadioWithImagePreference(Context context) {
        this(context, null);
    }

    /**
     * Sets the callback to be invoked when this preference is clicked by the user.
     *
     * @param listener The callback to be invoked
     */
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    /**
     * Processes a click on the preference.
     */
    @Override
    public void onClick() {
        if (mListener != null) {
            mListener.onRadioButtonClicked(this);
        }
    }

    /**
     * Binds the created View to the data for this preference.
     *
     * <p>This is a good place to grab references to custom Views in the layout and set
     * properties on them.
     *
     * <p>Make sure to call through to the superclass's implementation.
     *
     * @param holder The ViewHolder that provides references to the views to fill in. These views
     *               will be recycled, so you should not hold a reference to them after this method
     *               returns.
     */
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View summaryContainer = holder.findViewById(R.id.summary_container);
        if (summaryContainer != null) {
            summaryContainer.setVisibility(
                    TextUtils.isEmpty(getSummary()) ? View.GONE : View.VISIBLE);
        }
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        setLayoutResource(R.layout.radio_with_image_preference);
        setIconSpaceReserved(false);
    }
}
