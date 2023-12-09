package com.android.settings.biometrics.fingerprint

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle

interface UdfpsEnrollCalibrator {

    val extrasForNextIntent: Bundle

    fun onSaveInstanceState(outState: Bundle)

    fun onFindSensorPage(
            lifecycle: Lifecycle,
            fragmentManager: FragmentManager,
            enableEnrollingRunnable: Runnable
    )
}