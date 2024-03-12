/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.remoteauth

import android.R
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

import com.android.settings.Utils
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout

/**
 * Displays a content view with a sticky footer in the SetupDesign style. Implementations
 * must define a primary button, and an optional secondary button.
 *
 * A layout with a [GlifLayout] must be provided, along with the id of the [GlifLayout].
 */
abstract class RemoteAuthEnrollBase(
    @LayoutRes val layoutResId: Int,
    @IdRes private val glifLayoutId: Int
) : Fragment(layoutResId) {
    protected val primaryFooterButton by lazy { initializePrimaryFooterButton() }
    protected val secondaryFooterButton by lazy { initializeSecondaryFooterButton() }

    override fun onCreateView(
        inflater: LayoutInflater,
        viewGroup: ViewGroup?,
        savedInstanceArgs: Bundle?
    ) =
        super.onCreateView(inflater, viewGroup, savedInstanceArgs)!!.also { view ->
            initializeFooterbarMixin(view)
        }

    protected fun getGlifLayout(view: View) = view.findViewById<GlifLayout>(glifLayoutId)

    /**
     * Return a button will be used as the primary footer button.
     */
    abstract fun initializePrimaryFooterButton(): FooterButton

    /** If non-null, returned button will be used as the secondary footer button. */
    abstract fun initializeSecondaryFooterButton(): FooterButton?

    private fun initializeFooterbarMixin(view: View) {
        val footerBarMixin = checkNotNull(getGlifLayout(view)).getMixin(FooterBarMixin::class.java)
        primaryFooterButton.also { footerBarMixin.primaryButton = it }
        secondaryFooterButton?.also { footerBarMixin.secondaryButton = it }
        footerBarMixin.getButtonContainer().setBackgroundColor(getBackgroundColor())
    }

    @ColorInt
    private fun getBackgroundColor(): Int {
        val stateList = Utils.getColorAttr(context, R.attr.windowBackground)
        return stateList?.defaultColor ?: Color.TRANSPARENT
    }

    private companion object{
        const val TAG = "RemoteAuthEnrollBase"
    }
}
