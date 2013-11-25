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

package com.android.settings.notificationlight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

public class ApplicationLightPreference extends DialogPreference {

    private static String TAG = "AppLightPreference";
    public static final int DEFAULT_TIME = 1000;
    public static final int DEFAULT_COLOR = 0xFFFFFF; //White

    private ImageView mLightColorView;
    private TextView mOnValueView;
    private TextView mOffValueView;

    private int mColorValue;
    private int mOnValue;
    private int mOffValue;
    private boolean mOnOffChangeable;

    private Resources mResources;
    private ScreenReceiver mReceiver = null;
    private AlertDialog mTestDialog;

    /**
     * @param context
     * @param attrs
     */
    public ApplicationLightPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColorValue = DEFAULT_COLOR;
        mOnValue = DEFAULT_TIME;
        mOffValue = DEFAULT_TIME;
        mOnOffChangeable = true;
        init();
    }

    /**
     * @param context
     * @param color
     * @param onValue
     * @param offValue
     */
    public ApplicationLightPreference(Context context, int color, int onValue, int offValue) {
        super(context, null);
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = true;
        init();
    }

    /**
     * @param context
     * @param onLongClickListener
     * @param color
     * @param onValue
     * @param offValue
     */
    public ApplicationLightPreference(Context context, int color, int onValue, int offValue, boolean onOffChangeable) {
        super(context, null);
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = onOffChangeable;
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_application_light);
        mResources = getContext().getResources();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mLightColorView = (ImageView) view.findViewById(R.id.light_color);
        mOnValueView = (TextView) view.findViewById(R.id.textViewTimeOnValue);
        mOffValueView = (TextView) view.findViewById(R.id.textViewTimeOffValue);

        // Hide the summary text - it takes up too much space on a low res device
        // We use it for storing the package name for the longClickListener
        TextView tView = (TextView) view.findViewById(android.R.id.summary);
        tView.setVisibility(View.GONE);

        updatePreferenceViews();
    }

    private void updatePreferenceViews() {
        final int width = (int) mResources.getDimension(R.dimen.device_memory_usage_button_width);
        final int height = (int) mResources.getDimension(R.dimen.device_memory_usage_button_height);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            mLightColorView.setImageDrawable(createRectShape(width, height, 0xFF000000 + mColorValue));
        }
        if (mOnValueView != null) {
            mOnValueView.setText(mapLengthValue(mOnValue));
        }
        if (mOffValueView != null) {
            if (mOnValue == 1) {
                mOffValueView.setVisibility(View.GONE);
            } else {
                mOffValueView.setVisibility(View.VISIBLE);
            }
            mOffValueView.setText(mapSpeedValue(mOffValue));
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final LightSettingsDialog d = (LightSettingsDialog) getDialog();

        // Intercept the click on the middle button to show the test dialog and prevent the onDismiss
        d.findViewById(android.R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int onTime = d.getPulseSpeedOn();
                int offTime = d.getPulseSpeedOff();

                showTestDialog(d.getColor() - 0xFF000000, onTime, offTime);
            }
        });
    }

    @Override
    protected Dialog createDialog() {
        final LightSettingsDialog d = new LightSettingsDialog(getContext(),
                0xFF000000 + mColorValue, mOnValue, mOffValue, mOnOffChangeable);
        d.setAlphaSliderVisible(false);

        d.setButton(AlertDialog.BUTTON_POSITIVE, mResources.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mColorValue =  d.getColor() - 0xFF000000; // strip alpha, led does not support it
                mOnValue = d.getPulseSpeedOn();
                mOffValue = d.getPulseSpeedOff();
                updatePreferenceViews();
                callChangeListener(this);
            }
        });
        d.setButton(AlertDialog.BUTTON_NEUTRAL, mResources.getString(R.string.dialog_test),
                (DialogInterface.OnClickListener) null);
        d.setButton(AlertDialog.BUTTON_NEGATIVE, mResources.getString(R.string.cancel),
                (DialogInterface.OnClickListener) null);

        return d;
    }

    private void showTestDialog(int color, int speedOn, int speedOff) {
        final Context context = getContext();

        if (mReceiver != null) {
            context.unregisterReceiver(mReceiver);
        }
        if (mTestDialog != null) {
            mTestDialog.dismiss();
        }

        mReceiver = new ScreenReceiver(color, speedOn, speedOff);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(mReceiver, filter);

        mTestDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_test)
                .setMessage(R.string.dialog_test_message)
                .setPositiveButton(R.string.dialog_test_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mReceiver != null) {
                            context.unregisterReceiver(mReceiver);
                            mReceiver = null;
                        }
                    }
                })
                .create();

        mTestDialog.show();
    }

    /**
     * Getters and Setters
     */

    public int getColor() {
        return mColorValue;
    }

    public void setColor(int color) {
        mColorValue = color;
        updatePreferenceViews();
    }

    public void setOnValue(int value) {
        mOnValue = value;
        updatePreferenceViews();
    }

    public int getOnValue() {
        return mOnValue;
    }

    public void setOffValue(int value) {
        mOffValue = value;
        updatePreferenceViews();
    }

    public int getOffValue() {
        return mOffValue;
    }

    public void setAllValues(int color, int onValue, int offValue) {
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = true;
        updatePreferenceViews();
    }

    public void setAllValues(int color, int onValue, int offValue, boolean onOffChangeable) {
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = onOffChangeable;
        updatePreferenceViews();
    }

    public void setOnOffValue(int onValue, int offValue) {
        mOnValue = onValue;
        mOffValue = offValue;
        updatePreferenceViews();
    }

    public void setOnOffChangeable(boolean value) {
        mOnOffChangeable = value;
    }

    /**
     * Utility methods
     */
    public class ScreenReceiver extends BroadcastReceiver {
        protected int timeon;
        protected int timeoff;
        protected int color;

        public ScreenReceiver(int color, int timeon, int timeoff) {
            this.timeon = timeon;
            this.timeoff = timeoff;
            this.color = color;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Notification.Builder builder = new Notification.Builder(context);
                builder.setAutoCancel(true);
                builder.setLights(color, timeon, timeoff);
                Notification n = builder.getNotification();
                n.flags |= Notification.FLAG_SHOW_LIGHTS;
                nm.notify(1, n);
            } else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                nm.cancel(1);
                context.unregisterReceiver(mReceiver);
                mReceiver = null;
                mTestDialog.dismiss();
                mTestDialog = null;
            }
        }
    }

    private static ShapeDrawable createRectShape(int width, int height, int color) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setIntrinsicHeight(height);
        shape.setIntrinsicWidth(width);
        shape.getPaint().setColor(color);
        return shape;
    }

    private String mapLengthValue(Integer time) {
        if (time == DEFAULT_TIME) {
            return getContext().getString(R.string.default_time);
        }

        String[] timeNames = mResources.getStringArray(R.array.notification_pulse_length_entries);
        String[] timeValues = mResources.getStringArray(R.array.notification_pulse_length_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return getContext().getString(R.string.custom_time);
    }

    private String mapSpeedValue(Integer time) {
        if (time == DEFAULT_TIME) {
            return getContext().getString(R.string.default_time);
        }

        String[] timeNames = mResources.getStringArray(R.array.notification_pulse_speed_entries);
        String[] timeValues = mResources.getStringArray(R.array.notification_pulse_speed_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return getContext().getString(R.string.custom_time);
    }
}
