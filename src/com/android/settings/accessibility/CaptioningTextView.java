/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.os.Parcel;
import android.support.v4.view.ViewCompat;
import android.text.Editable;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.util.AttributeSet;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.widget.TextView;

public class CaptioningTextView extends TextView {
    private MutableBackgroundColorSpan mBackgroundSpan;
    private ColorStateList mOutlineColorState;
    private float mOutlineWidth;
    private int mOutlineColor;

    private int mEdgeType = CaptionStyle.EDGE_TYPE_NONE;
    private int mEdgeColor = Color.TRANSPARENT;
    private float mEdgeWidth = 0;

    private boolean mHasBackground = false;

    public CaptioningTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CaptioningTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CaptioningTextView(Context context) {
        super(context);
    }

    public void applyStyleAndFontSize(int styleId) {
        final Context context = mContext;
        final ContentResolver cr = context.getContentResolver();
        final CaptionStyle style;
        if (styleId == CaptionStyle.PRESET_CUSTOM) {
            style = CaptionStyle.getCustomStyle(cr);
        } else {
            style = CaptionStyle.PRESETS[styleId];
        }

        setTextColor(style.foregroundColor);
        setBackgroundColor(style.backgroundColor);
        setTypeface(style.getTypeface());

        // Clears all outlines.
        applyEdge(style.edgeType, style.edgeColor, 4.0f);

        final float fontSize = CaptioningManager.getFontSize(cr);
        if (fontSize != 0) {
            setTextSize(fontSize);
        }
    }

    /**
     * Applies an edge preset using a combination of {@link #setOutlineLayer}
     * and {@link #setShadowLayer}. Any subsequent calls to either of these
     * methods will invalidate the applied preset.
     *
     * @param type Type of edge to apply, one of:
     *            <ul>
     *            <li>{@link CaptionStyle#EDGE_TYPE_NONE}
     *            <li>{@link CaptionStyle#EDGE_TYPE_OUTLINE}
     *            <li>{@link CaptionStyle#EDGE_TYPE_DROP_SHADOW}
     *            </ul>
     * @param color Edge color as a packed 32-bit ARGB color.
     * @param width Width of the edge in pixels.
     */
    public void applyEdge(int type, int color, float width) {
        if (mEdgeType != type || mEdgeColor != color || mEdgeWidth != width) {
            final int textColor = getTextColors().getDefaultColor();
            switch (type) {
                case CaptionStyle.EDGE_TYPE_DROP_SHADOW:
                    setOutlineLayer(0, 0);
                    super.setShadowLayer(width, width, width, color);
                    break;
                case CaptionStyle.EDGE_TYPE_OUTLINE:
                    setOutlineLayer(width, color);
                    super.setShadowLayer(0, 0, 0, 0);
                    break;
                default:
                    super.setShadowLayer(0, 0, 0, 0);
                    setOutlineLayer(0, 0);
            }

            mEdgeType = type;
            mEdgeColor = color;
            mEdgeWidth = width;
        }
    }

    @Override
    public void setShadowLayer(float radius, float dx, float dy, int color) {
        mEdgeType = CaptionStyle.EDGE_TYPE_NONE;

        super.setShadowLayer(radius, dx, dy, color);
    }

    /**
     * Gives the text an outline of the specified pixel width and color.
     */
    public void setOutlineLayer(float width, int color) {
        width *= 2.0f;

        mEdgeType = CaptionStyle.EDGE_TYPE_NONE;

        if (mOutlineColor != color || mOutlineWidth != width) {
            mOutlineColorState = ColorStateList.valueOf(color);
            mOutlineColor = color;
            mOutlineWidth = width;
            invalidate();

            // TODO: Remove after display list bug is fixed.
            if (width > 0 && Color.alpha(color) != 0) {
                setLayerType(ViewCompat.LAYER_TYPE_SOFTWARE, null);
            } else {
                setLayerType(ViewCompat.LAYER_TYPE_HARDWARE, null);
            }
        }
    }

    /**
     * @return the color of the outline layer
     * @see #setOutlineLayer(float, int)
     */
    public int getOutlineColor() {
        return mOutlineColor;
    }

    /**
     * @return the width of the outline layer
     * @see #setOutlineLayer(float, int)
     */
    public float getOutlineWidth() {
        return mOutlineWidth;
    }

    @Override
    public Editable getEditableText() {
        final CharSequence text = getText();
        if (text instanceof Editable) {
            return (Editable) text;
        }

        setText(text, BufferType.EDITABLE);
        return (Editable) getText();
    }

    @Override
    public void setBackgroundColor(int color) {
        if (Color.alpha(color) == 0) {
            if (mHasBackground) {
                mHasBackground = false;
                getEditableText().removeSpan(mBackgroundSpan);
            }
        } else {
            if (mBackgroundSpan == null) {
                mBackgroundSpan = new MutableBackgroundColorSpan(color);
            } else {
                mBackgroundSpan.setColor(color);
            }

            if (mHasBackground) {
                invalidate();
            } else {
                mHasBackground = true;
                getEditableText().setSpan(mBackgroundSpan, 0, length(), 0);
            }
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        if (mBackgroundSpan != null) {
            getEditableText().setSpan(mBackgroundSpan, 0, lengthAfter, 0);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (mOutlineWidth > 0 && Color.alpha(mOutlineColor) > 0) {
            final TextPaint textPaint = getPaint();
            final Paint.Style previousStyle = textPaint.getStyle();
            final ColorStateList previousColors = getTextColors();
            textPaint.setStyle(Style.STROKE);
            textPaint.setStrokeWidth(mOutlineWidth);
            textPaint.setStrokeCap(Cap.ROUND);
            textPaint.setStrokeJoin(Join.ROUND);

            setTextColor(mOutlineColorState);

            // Remove the shadow.
            final float shadowRadius = getShadowRadius();
            final float shadowDx = getShadowDx();
            final float shadowDy = getShadowDy();
            final int shadowColor = getShadowColor();
            if (shadowRadius > 0) {
                setShadowLayer(0, 0, 0, 0);
            }

            // Draw outline and background only.
            super.onDraw(c);

            // Restore the shadow.
            if (shadowRadius > 0) {
                setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
            }

            // Restore original settings.
            textPaint.setStyle(previousStyle);
            setTextColor(previousColors);

            // Remove the background.
            final int color;
            if (mBackgroundSpan != null) {
                color = mBackgroundSpan.getBackgroundColor();
                mBackgroundSpan.setColor(Color.TRANSPARENT);
            } else {
                color = 0;
            }

            // Draw foreground only.
            super.onDraw(c);

            // Restore the background.
            if (mBackgroundSpan != null) {
                mBackgroundSpan.setColor(color);
            }
        } else {
            super.onDraw(c);
        }
    }

    public static class MutableBackgroundColorSpan extends CharacterStyle
            implements UpdateAppearance, ParcelableSpan {
        private int mColor;

        public MutableBackgroundColorSpan(int color) {
            mColor = color;
        }

        public MutableBackgroundColorSpan(Parcel src) {
            mColor = src.readInt();
        }

        public void setColor(int color) {
            mColor = color;
        }

        @Override
        public int getSpanTypeId() {
            return TextUtils.BACKGROUND_COLOR_SPAN;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mColor);
        }

        public int getBackgroundColor() {
            return mColor;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.bgColor = mColor;
        }
    }
}
