package com.android.settings.battery_history;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

public class GraphableButton extends Button {
    private static final String TAG = "GraphableButton";

    static Paint[] sPaint = new Paint[2];
    static {
        sPaint[0] = new Paint();
        sPaint[0].setStyle(Paint.Style.FILL);
        sPaint[0].setColor(Color.BLUE);
        
        sPaint[1] = new Paint();
        sPaint[1].setStyle(Paint.Style.FILL);
        sPaint[1].setColor(Color.RED);
    }
    
    double[] mValues;
    
    public GraphableButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setValues(double[] values, double maxValue) {
        mValues = values.clone();
        for (int i = 0; i < values.length; i++) {
            mValues[i] /= maxValue;
        }
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        Log.i(TAG, "onDraw: w = " + getWidth() + ", h = " + getHeight());
        
        int xmin = getPaddingLeft();
        int xmax = getWidth() - getPaddingRight();
        int ymin = getPaddingTop();
        int ymax = getHeight() - getPaddingBottom();
        
        int startx = xmin;
        for (int i = 0; i < mValues.length; i++) {
            int endx = xmin + (int) (mValues[i] * (xmax - xmin));
            canvas.drawRect(startx, ymin, endx, ymax, sPaint[i]);
            startx = endx;
        }
        super.onDraw(canvas);
    }
}
