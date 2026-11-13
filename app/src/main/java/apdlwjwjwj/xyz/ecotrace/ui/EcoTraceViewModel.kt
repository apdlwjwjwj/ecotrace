package apdlwjwjwj.xyz.ecotrace.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import apdlwjwjwj.xyz.ecotrace.data.VehicleRepository
import apdlwjwjwj.xyz.ecotrace.model.DropEventMarker
import apdlwjwjwj.xyz.ecotrace.model.EcoAnalysisEngine
import apdlwjwjwj.xyz.ecotrace.model.TripSummaryReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class EcoTraceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VehicleRepository(application)
    private var tts: TextToSpeech? = null

    private val _isParkedMode = MutableStateFlow(false)
    val isParkedMode: StateFlow<Boolean> = _isParkedMode.asStateFlow()

    val instantFuel = repository.instantFuelKmPerL
    val isBrakePressed = repository.isBrakePressed

    private val _report = MutableStateFlow<TripSummaryReport?>(null)
    val report: StateFlow<TripSummaryReport?> = _report.asStateFlow()

    private val _selectedMarker = MutableStateFlow<DropEventMarker?>(null)
    val selectedMarker: StateFlow<DropEventMarker?> = _selectedMarker.asStateFlow()

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
            }
        }
    }

    fun runDemoAndPark() {
        val snapshots = repository.generateDemoTripSnapshots()
        val analyzed = EcoAnalysisEngine.analyzeTrip(snapshots, fuelPricePerLiter = 1650)
        _report.value = analyzed
        _selectedMarker.value = analyzed.dropEvents.firstOrNull()
        _isParkedMode.value = true
        speakSummary(analyzed)
    }

    fun toggleDrivingMode() {
        _isParkedMode.value = !_isParkedMode.value
        if (_isParkedMode.value && _report.value == null) {
            runDemoAndPark()
        }
    }

    fun selectMarker(marker: DropEventMarker) {
        _selectedMarker.value = marker
    }

    fun speakSummary(summary: TripSummaryReport? = _report.value) {
        summary ?: return
        val distStr = String.format("%.1f", summary.totalDistanceKm)
        val fuelStr = String.format("%.1f", summary.harmonicAvgFuelKmPerL)
        val costComment = if (summary.diffFromEcoWon > 0) {
            "에코 주행 대비 약 ${summary.diffFromEcoWon}원이 더 소모되었습니다."
        } else {
            "에코 주행으로 약 ${-summary.diffFromEcoWon}원을 절약하셨습니다."
        }

        val script = "주행이 종료되었습니다. 총 ${distStr}킬로미터를 주행했으며, " +
                "평균 연비는 리터당 ${fuelStr}킬로미터, 실제 유류비는 ${summary.actualFuelCostWon}원입니다. " +
                costComment
        tts?.speak(script, TextToSpeech.QUEUE_FLUSH, null, "ECO_SUMMARY")
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        repository.release()
    }
}