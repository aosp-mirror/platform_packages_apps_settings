/*
 * Copyright (C) 2013 XuiMod
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
package com.android.settings.mahdi.aokp;

import android.content.Context;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class TestKeyboard extends Preference{

       int mHint;

       public TestKeyboard(Context context, AttributeSet attrs) {
           super(context, attrs);
           mHint = attrs.getAttributeResourceValue(null, "hint", 0);
       }

       @Override
       protected View onCreateView(ViewGroup parent){
           EditText tv = new EditText(this.getContext());
           tv.setPadding(0, 15, 0, 15);
           tv.setHint(mHint);
           tv.setGravity(Gravity.CENTER);
           tv.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
           return tv;
       }
}
