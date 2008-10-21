package com.android.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Map;

/**
 * This class extends Preference to display bluetooth status icons. One
 * icon specifies the connection/pairing status that is right-aligned.
 * An optional headset icon can be added to its left as well.
 */
public class BluetoothListItem extends Preference {

    private boolean mIsHeadset;
    private int mWeight;
    
    public BluetoothListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_btdevice_status);
    }

    private void updateIcons(View view) {
        ImageView headsetView = (ImageView) view.findViewById(R.id.device_headset);
        headsetView.setVisibility(mIsHeadset ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        updateIcons(view);
    }

    /**
     * Set whether the device is of headset type
     * @param headset whether or not the headset icon should be shown
     */
    public void setHeadset(boolean headset) {
        mIsHeadset = headset;
        notifyChanged();
    }

    /**
     * Sets the weight for ordering by signal strength or importance
     * @param weight the ordering weight
     */
    public void setWeight(int weight) {
        mWeight = weight;
    }

    /**
     * Returns the currently set ordering weight
     * @return the current ordering weight
     */
    public int getWeight() {
        return mWeight;
    }
    
    @Override
    public int compareTo(Preference another) {
        int diff = ((BluetoothListItem)another).mWeight - mWeight;
        // Let the new one be after the old one, if they are the same weight
        // TODO: Implement a more reliable way to consistently order items of
        // the same weight
        if (diff == 0) diff = 1;
        return diff;
    }
}
