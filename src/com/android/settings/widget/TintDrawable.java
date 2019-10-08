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
 * limitations under the License
 */

package com.android.settings.widget;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.util.Log;

import com.android.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * A Drawable that tints a contained Drawable, overriding the existing tint specified in the
 * underlying drawable. This class should only be used in XML.
 *
 * @attr ref android.R.styleable#DrawableWrapper_drawable
 * @attr ref R.styleable#TintDrawable_tint
 */
public class TintDrawable extends DrawableWrapper {
    private ColorStateList mTint;
    private int[] mThemeAttrs;

    /** No-arg constructor used by drawable inflation. */
    public TintDrawable() {
        super(null);
    }

    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.TintDrawable);

        super.inflate(r, parser, attrs, theme);

        mThemeAttrs = a.extractThemeAttrs();
        updateStateFromTypedArray(a);
        a.recycle();

        applyTint();
    }

    @Override
    public void applyTheme(Theme t) {
        super.applyTheme(t);

        if (mThemeAttrs != null) {
            final TypedArray a = t.resolveAttributes(mThemeAttrs, R.styleable.TintDrawable);
            updateStateFromTypedArray(a);
            a.recycle();
        }

        // Ensure tint is reapplied after applying the theme to ensure this drawables'
        // tint overrides the underlying drawables' tint.
        applyTint();
    }

    @Override
    public boolean canApplyTheme() {
        return (mThemeAttrs != null && mThemeAttrs.length > 0) || super.canApplyTheme();
    }

    private void updateStateFromTypedArray(@NonNull TypedArray a) {
        if (a.hasValue(R.styleable.TintDrawable_android_drawable)) {
            setDrawable(a.getDrawable(R.styleable.TintDrawable_android_drawable));
        }
        if (a.hasValue(R.styleable.TintDrawable_android_tint)) {
            mTint = a.getColorStateList(R.styleable.TintDrawable_android_tint);
        }
    }

    private void applyTint() {
        if (getDrawable() != null && mTint != null) {
            getDrawable().mutate().setTintList(mTint);
        }
    }
}
