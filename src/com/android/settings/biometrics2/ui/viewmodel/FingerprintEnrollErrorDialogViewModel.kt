package com.android.settings.biometrics2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FingerprintEnrollErrorDialogViewModel(
    application: Application,
    val isSuw: Boolean
): AndroidViewModel(application) {

    private val _isDialogShown: AtomicBoolean = atomic(false)
    val isDialogShown: Boolean
        get() = _isDialogShown.value

    private val _newDialogFlow = MutableSharedFlow<Int>()
    val newDialogFlow: SharedFlow<Int>
        get() = _newDialogFlow.asSharedFlow()

    private val _triggerRetryFlow = MutableSharedFlow<Any>()
    val triggerRetryFlow: SharedFlow<Any>
        get() = _triggerRetryFlow.asSharedFlow()

    private val _setResultFlow = MutableSharedFlow<FingerprintErrorDialogSetResultAction>()
    val setResultFlow: SharedFlow<FingerprintErrorDialogSetResultAction>
        get() = _setResultFlow.asSharedFlow()

    suspend fun newDialog(errorMsgId: Int) {
        _isDialogShown.compareAndSet(expect = false, update = true)
        _newDialogFlow.emit(errorMsgId)
    }

    suspend fun triggerRetry() {
        _isDialogShown.compareAndSet(expect = true, update = false)
        _triggerRetryFlow.emit(Any())
    }

    suspend fun setResultAndFinish(action: FingerprintErrorDialogSetResultAction) {
        _isDialogShown.compareAndSet(expect = true, update = false)
        _setResultFlow.emit(action)
    }
}

enum class FingerprintErrorDialogSetResultAction {
    FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH,
    FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
}
