package com.example.fitjournal_capstone_leandro.ui.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StopwatchViewModel : ViewModel() {
    private val _timer = MutableStateFlow(0L)
    val timer: StateFlow<Long> = _timer

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private var timerJob: Job? = null
    private var startTimeMillis = 0L

    fun startTimer() {
        if (_isActive.value == false) {
            _isActive.value = true
            startTimeMillis = System.currentTimeMillis() - (_timer.value * 1000)

            timerJob = viewModelScope.launch {
                while (_isActive.value) {
                    delay(1000)
                    val currentTime = System.currentTimeMillis()
                    _timer.value = (currentTime - startTimeMillis) / 1000
                }
            }
        }
    }

    fun pauseTimer() {
        _isActive.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isActive.value = false
        timerJob?.cancel()
        _timer.value = 0L
        startTimeMillis = 0L
    }
}