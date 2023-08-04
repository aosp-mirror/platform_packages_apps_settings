/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.os.UserManager;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

public class StorageWizardInit extends StorageWizardBase {

    private boolean mIsPermittedToAdopt;
    private boolean mPortable;

    private ViewFlipper mFlipper;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("IS_PORTABLE", mPortable);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }

        mIsPermittedToAdopt = UserManager.get(this).isAdminUser()
            && !ActivityManager.isUserAMonkey();

        if (!mIsPermittedToAdopt) {
            //Notify guest users as to why formatting is disallowed
            Toast.makeText(getApplicationContext(),
                R.string.storage_wizard_guest, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.storage_wizard_init);
        setupHyperlink();
        mPortable = true;

        mFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        if (savedInstanceState != null) {
            mPortable = savedInstanceState.getBoolean("IS_PORTABLE");
        }
        if(mPortable) {
            mFlipper.setDisplayedChild(0);
            setHeaderText(R.string.storage_wizard_init_v2_external_title,
                getDiskShortDescription());
            setNextButtonText(R.string.storage_wizard_init_v2_external_action);
            setBackButtonText(R.string.wizard_back_adoptable);
            setNextButtonVisibility(View.VISIBLE);
            if (!mDisk.isAdoptable()) {
                setBackButtonVisibility(View.GONE);
            }
        }
        else {
            mFlipper.setDisplayedChild(1);
            setHeaderText(R.string.storage_wizard_init_v2_internal_title,
                getDiskShortDescription());
            setNextButtonText(R.string.storage_wizard_init_v2_internal_action);
            setBackButtonText(R.string.wizard_back_adoptable);
            setNextButtonVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNavigateBack(View v) {
        if (!mIsPermittedToAdopt) {
            // TODO: Show a message about why this is disabled for guest and
            // that only an admin user can adopt an sd card.

            v.setEnabled(false);
        } else if (mPortable == false) {
            mFlipper.showNext();
            setHeaderText(R.string.storage_wizard_init_v2_external_title,
                getDiskShortDescription());
            setNextButtonText(R.string.storage_wizard_init_v2_external_action);
            setBackButtonText(R.string.wizard_back_adoptable);
            setBackButtonVisibility(View.VISIBLE);
            mPortable = true;
        } else {
            mFlipper.showNext();
            setHeaderText(R.string.storage_wizard_init_v2_internal_title,
                getDiskShortDescription());
            setNextButtonText(R.string.storage_wizard_init_v2_internal_action);
            setBackButtonText(R.string.wizard_back_adoptable);
            setBackButtonVisibility(View.VISIBLE);
            mPortable = false;
        }
    }

    @Override
    public void onNavigateNext(View v) {
        if (mPortable) {
            onNavigateExternal(v);
        } else {
            onNavigateInternal(v);
        }
    }

    public void onNavigateExternal(View view) {
        if (view != null) {
            // User made an explicit choice for external
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(this,
                SettingsEnums.ACTION_STORAGE_INIT_EXTERNAL);
        }
        StorageWizardFormatConfirm.showPublic(this, mDisk.getId());
    }

    public void onNavigateInternal(View view) {
        if (view != null) {
            // User made an explicit choice for internal
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(this,
                SettingsEnums.ACTION_STORAGE_INIT_INTERNAL);
        }
        StorageWizardFormatConfirm.showPrivate(this, mDisk.getId());
    }

    private void setupHyperlink() {
        TextView external_storage_textview = findViewById(R.id.storage_wizard_init_external_text);
        TextView internal_storage_textview = findViewById(R.id.storage_wizard_init_internal_text);
        String external_storage_text = getResources().getString(R.string.
            storage_wizard_init_v2_external_summary);
        String internal_storage_text = getResources().getString(R.string.
            storage_wizard_init_v2_internal_summary);

        Spannable external_storage_spannable = styleFont(external_storage_text);
        Spannable internal_storage_spannable = styleFont(internal_storage_text);
        external_storage_textview.setText(external_storage_spannable);
        internal_storage_textview.setText(internal_storage_spannable);

        external_storage_textview.setMovementMethod(LinkMovementMethod.getInstance());
        internal_storage_textview.setMovementMethod(LinkMovementMethod.getInstance());
        external_storage_textview.setOnTouchListener(listener);
        internal_storage_textview.setOnTouchListener(listener);
    }

    private Spannable styleFont(String text) {
        Spannable s = (Spannable) Html.fromHtml(text);
        for (URLSpan span : s.getSpans(0, s.length(), URLSpan.class)) {
            TypefaceSpan typefaceSpan = new TypefaceSpan("sans-serif-medium");
            s.setSpan(typefaceSpan, s.getSpanStart(span), s.getSpanEnd(span), 0);
        }
        return s;
    }
    private View.OnTouchListener listener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                if (isInside(v, event)) {
                    return false;
                }
                return true;
            }
            return false;
        }

        private boolean isInside(View v, MotionEvent event) {
            return !(event.getX() < 0 || event.getY() < 0
                || event.getX() > v.getMeasuredWidth()
                || event.getY() > v.getMeasuredHeight());
        }
    };
}