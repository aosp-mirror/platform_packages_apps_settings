/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.dashboard.suggestions

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.service.settings.suggestions.Suggestion
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.settings.core.InstrumentedFragment
import com.android.settings.homepage.SettingsHomepageActivity
import com.android.settings.homepage.SplitLayoutListener
import com.android.settings.overlay.FeatureFactory
import com.android.settings.R
import com.android.settingslib.suggestions.SuggestionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SUGGESTIONS = "suggestions"
private const val TAG = "ContextualSuggestFrag"
private const val FLAG_IS_DISMISSIBLE = 1 shl 2

/**
 * Fragment to control display and interaction logic for [Suggestion]s
 */
class SuggestionFragment : InstrumentedFragment(),
    SplitLayoutListener, SuggestionController.ServiceConnectionListener {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private lateinit var suggestionController: SuggestionController
    private lateinit var suggestionTile: View
    private var icon: ImageView? = null
    private var iconFrame: View? = null
    private var title: TextView? = null
    private var summary: TextView? = null
    private var dismiss: ImageView? = null
    private var iconVisible = true
    private var startTime: Long = 0
    private var suggestionsRestored = false
    private var splitLayoutSupported = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val component = FeatureFactory.featureFactory
            .suggestionFeatureProvider
            .suggestionServiceComponent
        suggestionController = SuggestionController(context, component, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        suggestionTile = inflater.inflate(R.layout.suggestion_tile, container, true)
        icon = suggestionTile.findViewById(android.R.id.icon)
        iconFrame = suggestionTile.findViewById(android.R.id.icon_frame)
        title = suggestionTile.findViewById(android.R.id.title)
        summary = suggestionTile.findViewById(android.R.id.summary)
        dismiss = suggestionTile.findViewById(android.R.id.closeButton)
        if (!iconVisible) {
            onSplitLayoutChanged(false)
        }
        // Restore the suggestion and skip reloading
        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring suggestions")
            savedInstanceState.getParcelableArrayList(
                SUGGESTIONS,
                Suggestion::class.java
            )?.let { suggestions ->
                suggestionsRestored = true
                startTime = SystemClock.uptimeMillis()
                updateState(suggestions)
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(SUGGESTIONS, currentSuggestions)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        suggestionController.start()
    }

    override fun onStop() {
        suggestionController.stop()
        super.onStop()
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SETTINGS_HOMEPAGE
    }

    override fun setSplitLayoutSupported(supported: Boolean) {
        splitLayoutSupported = supported
    }

    override fun onSplitLayoutChanged(isRegularLayout: Boolean) {
        iconVisible = isRegularLayout
        if (splitLayoutSupported) {
            iconFrame?.visibility = if (iconVisible) View.VISIBLE else View.GONE
        }
    }

    override fun onServiceConnected() {
        loadSuggestions()
    }

    override fun onServiceDisconnected() {
        // no-op
    }

    private fun loadSuggestions() {
        if (suggestionsRestored) {
            // Skip first suggestion loading when restored
            suggestionsRestored = false
            return
        }

        startTime = SystemClock.uptimeMillis()
        scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Start loading suggestions")
            val suggestions = suggestionController.suggestions
            Log.d(TAG, "Loaded suggestions: ${suggestions?.size}")
            withContext(Dispatchers.Main) {
                updateState(suggestions)
            }
        }
    }

    private fun updateState(suggestions: List<Suggestion>?) {
        currentSuggestions.clear()
        if (suggestions.isNullOrEmpty()) {
            Log.d(TAG, "Remove suggestions")
            showSuggestionTile(false)
            return
        }
        currentSuggestions.addAll(suggestions)

        // Only take top suggestion; we assume this is the highest rank.
        val suggestion = suggestions.first()
        icon?.setImageIcon(suggestion.icon)
        suggestion.title?.let {
            title?.text = it
        } ?: run {
            Log.d(TAG, "No suggestion title, removing")
            showSuggestionTile(false)
            return
        }
        val suggestionSummary = suggestion.summary
        if (suggestionSummary.isNullOrEmpty()) {
            summary?.visibility = View.GONE
        } else {
            summary?.visibility = View.VISIBLE
            summary?.text = suggestionSummary
        }
        if (suggestion.flags and FLAG_IS_DISMISSIBLE != 0) {
            dismiss?.let { dismissView ->
                dismissView.visibility = View.VISIBLE
                dismissView.setOnClickListener {
                    scope.launch(Dispatchers.IO) {
                        suggestionController.dismissSuggestions(suggestion)
                    }
                    if (suggestions.size > 1) {
                        dismissView.visibility = View.GONE
                        updateState(suggestions.subList(1, suggestions.size))
                    } else {
                        currentSuggestions.clear()
                        suggestionTile.visibility = View.GONE
                    }
                }
            }
        }
        suggestionTile.setOnClickListener {
            // Notify service that suggestion is being launched. Note that the service does not
            // actually start the suggestion on our behalf, instead simply logging metrics.
            scope.launch(Dispatchers.IO) {
                suggestionController.launchSuggestion(suggestion)
            }
            currentSuggestions.clear()
            try {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                suggestion.pendingIntent.send(options.toBundle())
            } catch (e: PendingIntent.CanceledException) {
                Log.e(TAG, "Failed to start suggestion ${suggestion.title}", e)
            }
        }
        showSuggestionTile(true)
    }

    private fun showSuggestionTile(show: Boolean) {
        val totalTime = SystemClock.uptimeMillis() - startTime
        Log.d(TAG, "Total loading time: $totalTime ms")
        mMetricsFeatureProvider.action(
            context,
            SettingsEnums.ACTION_CONTEXTUAL_HOME_SHOW,
            totalTime.toInt()
        )
        (activity as? SettingsHomepageActivity)?.showHomepageWithSuggestion(show)
    }

    private companion object {
        val currentSuggestions = arrayListOf<Suggestion>()
    }
}