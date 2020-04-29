/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImeAwareEditText;

/**
 * An EditText that, instead of scrolling to itself when focused, will request scrolling to its
 * parent. This is used in ChooseLockPassword to do make a best effort for not hiding the error
 * messages for why the password is invalid under the keyboard.
 */
public class ScrollToParentEditText extends ImeAwareEditText {

    private Rect mRect = new Rect();

    public ScrollToParentEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        ViewParent parent = getParent();
        if (parent instanceof View) {
            // Request the entire parent view to be shown, which in ChooseLockPassword's case,
            // will include messages for why the password is invalid (if any).
            ((View) parent).getDrawingRect(mRect);
            return ((View) parent).requestRectangleOnScreen(mRect, immediate);
        } else {
            return super.requestRectangleOnScreen(rectangle, immediate);
        }
    }
}
