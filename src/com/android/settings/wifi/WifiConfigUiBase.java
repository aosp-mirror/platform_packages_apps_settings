/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;

/**
 * Foundation interface glues between Activities and UIs like {@link WifiDialog}.
 */
public interface WifiConfigUiBase {
    public Context getContext();
    public WifiConfigController getController();
    public LayoutInflater getLayoutInflater();
    public boolean isEdit();

    public void dispatchSubmit();

    public void setTitle(int id);
    public void setTitle(CharSequence title);

    public void setSubmitButton(CharSequence text);
    public void setForgetButton(CharSequence text);
    public void setCancelButton(CharSequence text);
    public Button getSubmitButton();
    public Button getForgetButton();
    public Button getCancelButton();
}