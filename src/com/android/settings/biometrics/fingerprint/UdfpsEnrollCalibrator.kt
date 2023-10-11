package com.android.settings.biometrics.fingerprint

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import java.util.UUID

interface UdfpsEnrollCalibrator {

    enum class Status {
        PROCESSING,
        GOT_RESULT,
        FINISHED,
    }

    enum class Result {
        NEED_CALIBRATION,
        NO_NEED_CALIBRATION,
    }

    val uuid: UUID

    val statusLiveData: LiveData<Status>

    val result: Result?

    fun setFinished()

    @get:StringRes
    val calibrationDialogTitleTextId: Int

    @get:StringRes
    val calibrationDialogMessageTextId: Int

    @get:StringRes
    val calibrationDialogDismissButtonTextId: Int
}