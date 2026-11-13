package apdlwjwjwj.xyz.ecotrace.data

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.FuelEconomyListener
import apdlwjwjwj.xyz.ecotrace.model.DrivingSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

class VehicleRepository(context: Context) {

    private var vehicle: Vehicle? = null
    private var isSdkConnected = false

    private val _instantFuelKmPerL = MutableStateFlow(14.5f)
    val instantFuelKmPerL: StateFlow<Float> = _instantFuelKmPerL.asStateFlow()

    private val _isBrakePressed = MutableStateFlow(false)
    val isBrakePressed: StateFlow<Boolean> = _isBrakePressed.asStateFlow()

    private val fuelListener = object : FuelEconomyListener {
        override fun onFuelEconomyUpdated(efficiency: Float?) {
            val rawLPer100Km = efficiency ?: return
            val kmPerL = if (rawLPer100Km > 0.1f) 100f / rawLPer100Km else 0f
            _instantFuelKmPerL.value = kmPerL
        }

        override fun onFailed(e: Exception) {
            Log.e("EcoTrace", "FuelEconomyListener failed: ${e.message}", e)
        }
    }

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) {
            _isBrakePressed.value = status ?: false
        }

        override fun onFailed(e: Exception) {
            Log.e("EcoTrace", "BrakePedalStatusListener failed: ${e.message}", e)
        }
    }

    init {
        try {
            vehicle = Vehicle(context).apply {
                initialize()
                getFuel().registerInstantaneousFuelEconomy(fuelListener)
                getBrake().registerBrakePedalStatus(brakeListener)
            }
            isSdkConnected = true
            Log.d("EcoTrace", "Pleos Vehicle SDK connected.")
        } catch (e: Exception) {
            Log.w("EcoTrace", "Fallback to standalone mode: ${e.message}")
        }
    }

    fun release() {
        if (isSdkConnected) {
            try {
                vehicle?.getFuel()?.unregisterInstantaneousFuelEconomy(fuelListener)
                vehicle?.getBrake()?.unregisterBrakePedalStatus(brakeListener)
                vehicle?.release()
            } catch (e: Exception) {
                Log.e("EcoTrace", "Error releasing Vehicle SDK", e)
            }
        }
    }

    fun generateDemoTripSnapshots(): List<DrivingSnapshot> {
        val list = mutableListOf<DrivingSnapshot>()
        val baseTime = System.currentTimeMillis() - 600_000L
        var lat = 37.2829
        var lng = 127.0435

        for (step in 0 until 120) {
            val t = baseTime + step * 5000L
            lat += 0.00045 * cos(step * 0.04)
            lng += 0.00055 * sin(step * 0.03) + 0.0003

            val (speed, fuel, brake) = when (step) {
                in 0..25 -> Triple(55f, 16.2f, false)
                26 -> Triple(72f, 5.1f, false)
                in 27..45 -> Triple(85f, 14.8f, false)
                in 46..60 -> Triple(40f, 10.5f, false)
                61 -> Triple(18f, 6.2f, true)
                in 62..85 -> Triple(65f, 15.8f, false)
                86 -> Triple(88f, 4.3f, false)
                in 87..119 -> Triple(50f, 13.9f, false)
                else -> Triple(0f, 0f, true)
            }

            list.add(
                DrivingSnapshot(
                    timestamp = t,
                    latitude = lat,
                    longitude = lng,
                    speedKmh = speed,
                    instantFuelKmPerL = fuel,
                    isBrakePressed = brake
                )
            )
        }
        return list
    }
}