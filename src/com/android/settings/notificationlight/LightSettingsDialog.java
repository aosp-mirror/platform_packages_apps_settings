/*
 * Copyright (C) 2010 Daniel Nilsson
 * Copyright (C) 2012 THe CyanogenMod Project
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

package com.android.settings.notificationlight;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.notificationlight.ColorPickerView.OnColorChangedListener;

public class LightSettingsDialog extends AlertDialog implements
        ColorPickerView.OnColorChangedListener {

    private ColorPickerView mColorPicker;

    private ColorPanelView mOldColor;
    private ColorPanelView mNewColor;
    private Spinner mPulseSpeedOn;
    private Spinner mPulseSpeedOff;
    private LayoutInflater mInflater;

    private OnColorChangedListener mListener;

    /**
     * @param context
     * @param initialColor
     * @param initialSpeedOn
     * @param initialSpeedOff
     */
    protected LightSettingsDialog(Context context, int initialColor, int initialSpeedOn,
            int initialSpeedOff) {
        super(context);

        init(initialColor, initialSpeedOn, initialSpeedOff, true);
    }

    /**
     * @param context
     * @param initialColor
     * @param initialSpeedOn
     * @param initialSpeedOff
     * @param onOffChangeable
     */
    protected LightSettingsDialog(Context context, int initialColor, int initialSpeedOn,
            int initialSpeedOff, boolean onOffChangeable) {
        super(context);

        init(initialColor, initialSpeedOn, initialSpeedOff, onOffChangeable);
    }

    private void init(int color, int speedOn, int speedOff, boolean onOffChangeable) {
        // To fight color banding.
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setUp(color, speedOn, speedOff, onOffChangeable);
    }

    /**
     * This function sets up the dialog with the proper values.  If the speedOff parameters
     * has a -1 value disable both spinners
     *
     * @param color - the color to set
     * @param speedOn - the flash time in ms
     * @param speedOff - the flash length in ms
     */
    private void setUp(int color, int speedOn, int speedOff, boolean onOffChangeable) {
        mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = mInflater.inflate(R.layout.dialog_light_settings, null);

        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mOldColor = (ColorPanelView) layout.findViewById(R.id.old_color_panel);
        mNewColor = (ColorPanelView) layout.findViewById(R.id.new_color_panel);

        ((LinearLayout) mOldColor.getParent()).setPadding(Math
                .round(mColorPicker.getDrawingOffset()), 0, Math
                .round(mColorPicker.getDrawingOffset()), 0);

        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);

        mPulseSpeedOn = (Spinner) layout.findViewById(R.id.on_spinner);
        PulseSpeedAdapter pulseSpeedAdapter = new PulseSpeedAdapter(
                R.array.notification_pulse_length_entries,
                R.array.notification_pulse_length_values,
                speedOn);
        mPulseSpeedOn.setAdapter(pulseSpeedAdapter);
        mPulseSpeedOn.setSelection(pulseSpeedAdapter.getTimePosition(speedOn));
        mPulseSpeedOn.setOnItemSelectedListener(mSelectionListener);

        mPulseSpeedOff = (Spinner) layout.findViewById(R.id.off_spinner);
        pulseSpeedAdapter = new PulseSpeedAdapter(R.array.notification_pulse_speed_entries,
                R.array.notification_pulse_speed_values,
                speedOff);
        mPulseSpeedOff.setAdapter(pulseSpeedAdapter);
        mPulseSpeedOff.setSelection(pulseSpeedAdapter.getTimePosition(speedOff));

        mPulseSpeedOn.setEnabled(onOffChangeable);
        mPulseSpeedOff.setEnabled((speedOn != 1) && onOffChangeable);

        setView(layout);
        setTitle(R.string.edit_light_settings);
    }

    private AdapterView.OnItemSelectedListener mSelectionListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mPulseSpeedOff.setEnabled(getPulseSpeedOn() != 1);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @Override
    public void onColorChanged(int color) {
        mNewColor.setColor(color);

        if (mListener != null) {
            mListener.onColorChanged(color);
        }
    }

    public void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    public int getPulseSpeedOn() {
        return ((Pair<String, Integer>) mPulseSpeedOn.getSelectedItem()).second;
    }

    public int getPulseSpeedOff() {
        // return 0 if 'Always on' is selected
        return getPulseSpeedOn() == 1 ? 0 : ((Pair<String, Integer>) mPulseSpeedOff.getSelectedItem()).second;
    }

    class PulseSpeedAdapter extends BaseAdapter implements SpinnerAdapter {
        private ArrayList<Pair<String, Integer>> times;

        public PulseSpeedAdapter(int timeNamesResource, int timeValuesResource) {
            times = new ArrayList<Pair<String, Integer>>();

            String[] time_names = getContext().getResources().getStringArray(timeNamesResource);
            String[] time_values = getContext().getResources().getStringArray(timeValuesResource);

            for(int i = 0; i < time_values.length; ++i) {
                times.add(new Pair<String, Integer>(time_names[i], Integer.decode(time_values[i])));
            }

        }

        /**
         * This constructor apart from taking a usual time entry array takes the
         * currently configured time value which might cause the addition of a
         * "Custom" time entry in the spinner in case this time value does not
         * match any of the predefined ones in the array.
         *
         * @param timeNamesResource The time entry names array
         * @param timeValuesResource The time entry values array
         * @param customTime Current time value that might be one of the
         *            predefined values or a totally custom value
         */
        public PulseSpeedAdapter(int timeNamesResource, int timeValuesResource, Integer customTime) {
            this(timeNamesResource, timeValuesResource);

            // Check if we also need to add the custom value entry
            if (getTimePosition(customTime) == -1) {
                times.add(new Pair<String, Integer>(getContext().getResources()
                        .getString(R.string.custom_time), customTime));
            }
        }

        /**
         * Will return the position of the spinner entry with the specified
         * time. Returns -1 if there is no such entry.
         *
         * @param time Time in ms
         * @return Position of entry with given time or -1 if not found.
         */
        public int getTimePosition(Integer time) {
            for (int position = 0; position < getCount(); ++position) {
                if (getItem(position).second.equals(time)) {
                    return position;
                }
            }

            return -1;
        }

        @Override
        public int getCount() {
            return times.size();
        }

        @Override
        public Pair<String, Integer> getItem(int position) {
            return times.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mInflater.inflate(R.layout.pulse_time_item, null);
            }

            Pair<String, Integer> entry = getItem(position);
            ((TextView) view.findViewById(R.id.textViewName)).setText(entry.first);

            return view;
        }
    }
}
