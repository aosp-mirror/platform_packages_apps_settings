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

package com.android.settings.remoteauth.introduction

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2

import com.airbnb.lottie.LottieAnimationView

import com.android.settings.R
import com.android.settingslib.widget.LottieColorUtils

class IntroductionImageCarousel : ConstraintLayout {
    private val carousel: ViewPager2 by lazy { requireViewById<ViewPager2>(R.id.image_carousel) }
    private val progressIndicator: RecyclerView by lazy {
        requireViewById<RecyclerView>(R.id.carousel_progress_indicator)
    }
    private val backArrow: ImageView by lazy {
        requireViewById<ImageView>(R.id.carousel_back_arrow)
    }
    private val forwardArrow: ImageView by lazy {
        requireViewById<ImageView>(R.id.carousel_forward_arrow)
    }
    private val progressIndicatorAdapter = ProgressIndicatorAdapter()
    // The index of the current animation we are on
    private var currentPage = 0
        set(value) {
            val pageRange = 0..(ANIMATION_LIST.size - 1)
            field = value.coerceIn(pageRange)
            backArrow.isEnabled = field > pageRange.start
            forwardArrow.isEnabled = field < pageRange.endInclusive
            carousel.setCurrentItem(field)
            progressIndicatorAdapter.currentIndex = field
        }

    private val onPageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
            }
        }
    constructor(context: Context) : super(context)
    constructor(context: Context, attrSet: AttributeSet?) : super(context, attrSet)

    init {
        LayoutInflater.from(context).inflate(R.layout.remote_auth_introduction_image_carousel, this)

        with(carousel) {
            setPageTransformer(
                MarginPageTransformer(
                    context.resources.getDimension(R.dimen.remoteauth_introduction_fragment_padding_horizontal).toInt()
                )
            )
            adapter = ImageCarouselAdapter()
            registerOnPageChangeCallback(onPageChangeCallback)
        }

        with(progressIndicator) {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = progressIndicatorAdapter
        }

        backArrow.setOnClickListener { currentPage-- }
        forwardArrow.setOnClickListener { currentPage++ }
    }

    fun unregister() {
        carousel.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    private class AnimationViewHolder(val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val animationView = itemView.requireViewById<LottieAnimationView>(R.id.explanation_animation)
        val descriptionText = itemView.requireViewById<TextView>(R.id.carousel_text)
    }

    /** Adapter for the onboarding animations. */
    private class ImageCarouselAdapter : RecyclerView.Adapter<AnimationViewHolder>() {

        override fun getItemCount() = ANIMATION_LIST.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AnimationViewHolder(parent.context, LayoutInflater.from(parent.context).inflate(R.layout.remote_auth_introduction_image_carousel_item, parent, false))

        override fun onBindViewHolder(holder: AnimationViewHolder, position: Int) {
            with(holder.animationView) {
                setAnimation(ANIMATION_LIST[position].first)
                LottieColorUtils.applyDynamicColors(holder.context, this)
            }
            holder.descriptionText.setText(ANIMATION_LIST[position].second)
            with(holder.itemView) {
                // This makes sure that the proper description text instead of a generic "Page" label is
                // verbalized by Talkback when switching to a new page on the ViewPager2.
                contentDescription = context.getString(ANIMATION_LIST[position].second)
            }
        }
    }

    /** Adapter for icons indicating carousel progress. */
    private class ProgressIndicatorAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var currentIndex: Int = 0
            set(value) {
                val previousIndex = field
                field = value.coerceIn(0, getItemCount() - 1)
                notifyItemChanged(previousIndex)
                notifyItemChanged(field)
            }

        override fun getItemCount() = ANIMATION_LIST.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object :
                RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.remote_auth_introduction_image_carousel_progress_icon, parent, false)) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.isSelected = position == currentIndex
        }
    }
    companion object {
        @VisibleForTesting
        val ANIMATION_LIST =
            listOf(
                Pair(
                    R.raw.remoteauth_explanation_swipe_animation,
                    R.string.security_settings_remoteauth_enroll_introduction_animation_swipe_up
                ),
                Pair(
                    R.raw.remoteauth_explanation_notification_animation,
                    R.string.security_settings_remoteauth_enroll_introduction_animation_tap_notification
                ),
            )
        const val TAG = "RemoteAuthCarousel"
    }
}
