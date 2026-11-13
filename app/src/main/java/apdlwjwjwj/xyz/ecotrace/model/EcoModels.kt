package apdlwjwjwj.xyz.ecotrace.model

data class DrivingSnapshot(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val instantFuelKmPerL: Float,
    val isBrakePressed: Boolean
)

data class DropEventMarker(
    val index: Int,
    val latitude: Double,
    val longitude: Double,
    val reasonTitle: String,
    val detailMessage: String,
    val wastedWon: Int
)

data class TripSummaryReport(
    val totalDistanceKm: Float,
    val harmonicAvgFuelKmPerL: Float,
    val actualFuelCostWon: Int,
    val diffFromEcoWon: Int,
    val snapshots: List<DrivingSnapshot>,
    val dropEvents: List<DropEventMarker>
)

object EcoAnalysisEngine {

    fun analyzeTrip(
        snapshots: List<DrivingSnapshot>,
        fuelPricePerLiter: Int = 1650
    ): TripSummaryReport {
        if (snapshots.size < 2) {
            return TripSummaryReport(0f, 0f, 0, 0, snapshots, emptyList())
        }

        var totalDistanceKm = 0.0
        var totalFuelLiters = 0.0
        val dropEvents = mutableListOf<DropEventMarker>()

        val ecoTargetFuelKmPerL = 15.0

        for (i in 1 until snapshots.size) {
            val prev = snapshots[i - 1]
            val curr = snapshots[i]

            val dtSec = ((curr.timestamp - prev.timestamp) / 1000.0).coerceIn(0.5, 5.0)
            val distKm = curr.speedKmh * (dtSec / 3600.0)

            if (distKm > 0.0001 && curr.instantFuelKmPerL > 0.5f) {
                totalDistanceKm += distKm
                totalFuelLiters += (distKm / curr.instantFuelKmPerL)
            }

            val accelMps2 = ((curr.speedKmh - prev.speedKmh) / 3.6) / dtSec

            if (accelMps2 >= 2.5) {
                dropEvents.add(
                    DropEventMarker(
                        index = i,
                        latitude = curr.latitude,
                        longitude = curr.longitude,
                        reasonTitle = "급가속 감지 (+${String.format("%.1f", accelMps2)} m/s²)",
                        detailMessage = "급가속으로 순간 연비가 ${String.format("%.1f", curr.instantFuelKmPerL)} km/L까지 급락했습니다.",
                        wastedWon = (45 * (3.0 / curr.instantFuelKmPerL.coerceAtLeast(2f))).toInt()
                    )
                )
            } else if (accelMps2 <= -3.0 && curr.isBrakePressed) {
                dropEvents.add(
                    DropEventMarker(
                        index = i,
                        latitude = curr.latitude,
                        longitude = curr.longitude,
                        reasonTitle = "급제동 감지 (${String.format("%.1f", accelMps2)} m/s²)",
                        detailMessage = "불필요한 급감속으로 운동 에너지 손실 및 재가속 연료가 소모되었습니다.",
                        wastedWon = 80
                    )
                )
            }
        }

        val harmonicAvgFuel = if (totalFuelLiters > 0) {
            (totalDistanceKm / totalFuelLiters).toFloat()
        } else 0f

        val actualCost = (totalFuelLiters * fuelPricePerLiter).toInt()
        val ecoIdealCost = ((totalDistanceKm / ecoTargetFuelKmPerL) * fuelPricePerLiter).toInt()
        val diffWon = actualCost - ecoIdealCost

        return TripSummaryReport(
            totalDistanceKm = totalDistanceKm.toFloat(),
            harmonicAvgFuelKmPerL = harmonicAvgFuel,
            actualFuelCostWon = actualCost,
            diffFromEcoWon = diffWon,
            snapshots = snapshots,
            dropEvents = dropEvents
        )
    }
}