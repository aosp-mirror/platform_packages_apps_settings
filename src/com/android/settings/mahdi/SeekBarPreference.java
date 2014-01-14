package com.android.settings.mahdi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.settings.R;

import android.provider.Settings;

public class SeekBarPreference extends Preference
        implements OnSeekBarChangeListener {

    public static int maximum = 100;
    public int interval = 5;

    private String property;

    private TextView monitorBox;
    private SeekBar bar;

    int defaultValue = 60;
    boolean mDisablePercentageValue = false;
    boolean mZeroDefault = false;
    boolean mIsMilliSeconds = false;
    boolean mSameValue = false;

    private OnPreferenceChangeListener changer;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        View layout = View.inflate(getContext(), R.layout.slider_preference, null);

        monitorBox = (TextView) layout.findViewById(R.id.monitor_box);
        bar = (SeekBar) layout.findViewById(R.id.seek_bar);
        int progress;
        try{
            float setting = Settings.System.getFloat(
                    getContext().getContentResolver(), property);
            progress = (int) (mSameValue ? setting : (setting * 100));
        } catch (Exception e) {
            progress = defaultValue;
        }
        bar.setOnSeekBarChangeListener(this);
        bar.setProgress(progress);
        if (progress == 0 && mZeroDefault) {
            monitorBox.setText(R.string.default_string);
        } else {
            if (mIsMilliSeconds) {
                monitorBox.setText(progress + " ms");
            } else if (!mDisablePercentageValue) {
                monitorBox.setText(progress + "%");
            }
        }
        return layout;
    }

    public void setInitValue(int progress) {
        defaultValue = progress;
        if (bar!=null) {
            bar.setProgress(progress);
            if (progress == 0 && mZeroDefault) {
                monitorBox.setText(R.string.default_string);
            } else {
                if (mIsMilliSeconds) {
                    monitorBox.setText(progress + " ms");
                } else if (!mDisablePercentageValue) {
                    monitorBox.setText(progress + "%");
                }
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // TODO Auto-generated method stub
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public void setOnPreferenceChangeListener(
                OnPreferenceChangeListener onPreferenceChangeListener) {
        changer = onPreferenceChangeListener;
        super.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        progress = Math.round(((float) progress) / interval) * interval;
        seekBar.setProgress(progress);

        if (progress == 0 && mZeroDefault) {
            monitorBox.setText(R.string.default_string);
        } else {
            if (mIsMilliSeconds) {
                monitorBox.setText(progress + " ms");
            } else if (!mDisablePercentageValue) {
                monitorBox.setText(progress + "%");
            }
        }
        changer.onPreferenceChange(this, Integer.toString(progress));
    }

    public void setValue(int progress){
        if (bar!=null) {
            bar.setProgress(progress);
            if (progress == 0 && mZeroDefault) {
                monitorBox.setText(R.string.default_string);
            } else {
                if (mIsMilliSeconds) {
                    monitorBox.setText(progress + " ms");
                } else if (!mDisablePercentageValue) {
                    monitorBox.setText(progress + "%");
                }
            }
            changer.onPreferenceChange(this, Integer.toString(progress));
        }
    }

    public void disablePercentageValue(boolean disable) {
        mDisablePercentageValue = disable;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setInterval(int inter) {
        interval = inter;
    }

    public void displaySameValue(boolean same) {
        mSameValue = same;
    }

    public void zeroDefault(boolean displayDefault) {
        mZeroDefault = displayDefault;
    }

    public void isMilliseconds(boolean millis) {
        mIsMilliSeconds = millis;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

}
