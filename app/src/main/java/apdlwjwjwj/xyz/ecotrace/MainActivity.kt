package apdlwjwjwj.xyz.ecotrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import apdlwjwjwj.xyz.ecotrace.model.DropEventMarker
import apdlwjwjwj.xyz.ecotrace.model.TripSummaryReport
import apdlwjwjwj.xyz.ecotrace.ui.EcoTraceViewModel
import kotlin.math.hypot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val vm: EcoTraceViewModel = viewModel()
                EcoTraceApp(vm)
            }
        }
    }
}

@Composable
fun EcoTraceApp(vm: EcoTraceViewModel) {
    val isParked by vm.isParkedMode.collectAsState()
    val instantFuel by vm.instantFuel.collectAsState()
    val isBrakePressed by vm.isBrakePressed.collectAsState()
    val report by vm.report.collectAsState()
    val selectedMarker by vm.selectedMarker.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101418)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ECO TRACE",
                    color = Color(0xFF4ADE80),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { vm.toggleDrivingMode() }) {
                        Text(if (isParked) "주행 모드(D)로 전환" else "주차(P) 리포트 보기")
                    }
                    Button(
                        onClick = { vm.runDemoAndPark() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Text("▶ 샘플 주행 시뮬레이션", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isParked) {
                DrivingMinimalScreen(instantFuel = instantFuel, isBrakePressed = isBrakePressed)
            } else {
                report?.let { summary ->
                    ParkedReportScreen(
                        report = summary,
                        selectedMarker = selectedMarker,
                        onSelectMarker = { vm.selectMarker(it) },
                        onReplayTts = { vm.speakSummary() }
                    )
                }
            }
        }
    }
}

@Composable
fun DrivingMinimalScreen(instantFuel: Float, isBrakePressed: Boolean) {
    val statusColor = when {
        instantFuel >= 14f -> Color(0xFF4ADE80)
        instantFuel >= 9f -> Color(0xFFFACC15)
        else -> Color(0xFFF87171)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF181E24), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("● 주행 데이터 실시간 수집 중 (D)", color = Color(0xFF94A3B8), fontSize = 18.sp)
                Surface(
                    color = if (isBrakePressed) Color(0xFFEF4444) else Color(0xFF334155),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isBrakePressed) "BRAKE ON" else "BRAKE OFF",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("현재 순간 연비", color = Color.LightGray, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", instantFuel),
                    color = statusColor,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("km/L", color = Color.Gray, fontSize = 28.sp, modifier = Modifier.padding(bottom = 14.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "안전 운전을 위해 상세 히트맵 리포트는 주차(P) 완료 후 표시됩니다.",
                color = Color(0xFF64748B),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ParkedReportScreen(
    report: TripSummaryReport,
    selectedMarker: DropEventMarker?,
    onSelectMarker: (DropEventMarker) -> Unit,
    onReplayTts: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            modifier = Modifier.weight(0.38f).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181E24)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("주행 요약 영수증", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    HorizontalDivider(color = Color(0xFF2D3748))

                    ReceiptRow("총 주행 거리", "${String.format("%.1f", report.totalDistanceKm)} km")
                    ReceiptRow("조화평균 연비", "${String.format("%.1f", report.harmonicAvgFuelKmPerL)} km/L")
                    ReceiptRow("적용 유가 (오피넷)", "1,650 원/L")

                    HorizontalDivider(color = Color(0xFF2D3748))

                    Text("실제 지출 유류비", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        text = "${report.actualFuelCostWon} 원",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    val diffColor = if (report.diffFromEcoWon > 0) Color(0xFFF87171) else Color(0xFF4ADE80)
                    val diffText = if (report.diffFromEcoWon > 0) {
                        "에코 주행 대비 +${report.diffFromEcoWon}원 추가 소모"
                    } else {
                        "에코 주행으로 ${-report.diffFromEcoWon}원 절약!"
                    }
                    Text(text = diffText, color = diffColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onReplayTts,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("🔊 음성 브리핑 다시 듣기", fontSize = 16.sp, color = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier.weight(0.62f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF181E24), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                InteractiveHeatmapCanvas(
                    report = report,
                    selectedMarker = selectedMarker,
                    onSelectMarker = onSelectMarker
                )
                Text(
                    text = "● 초록: 연비 우수   ● 노랑: 보통   ● 빨강: 급락 (마커 터치 시 원인 확인)",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF23191D)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selectedMarker != null) {
                        Text(
                            text = "⚠️ ${selectedMarker.reasonTitle}  (약 ${selectedMarker.wastedWon}원 손해)",
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = selectedMarker.detailMessage, color = Color.White, fontSize = 15.sp)
                    } else {
                        Text("급가속이나 급제동이 감지되지 않은 완벽한 에코 주행입니다!", color = Color(0xFF4ADE80))
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 16.sp)
        Text(text = value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InteractiveHeatmapCanvas(
    report: TripSummaryReport,
    selectedMarker: DropEventMarker?,
    onSelectMarker: (DropEventMarker) -> Unit
) {
    val snapshots = report.snapshots
    if (snapshots.size < 2) return

    var markerOffsets by remember { mutableStateOf<List<Pair<DropEventMarker, Offset>>>(emptyList()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(report) {
                detectTapGestures { tapOffset ->
                    val closest = markerOffsets.minByOrNull { (_, pos) ->
                        hypot(tapOffset.x - pos.x, tapOffset.y - pos.y)
                    }
                    if (closest != null && hypot(tapOffset.x - closest.second.x, tapOffset.y - closest.second.y) < 60f) {
                        onSelectMarker(closest.first)
                    }
                }
            }
    ) {
        val minLat = snapshots.minOf { it.latitude }
        val maxLat = snapshots.maxOf { it.latitude }
        val minLng = snapshots.minOf { it.longitude }
        val maxLng = snapshots.maxOf { it.longitude }

        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lngRange = (maxLng - minLng).coerceAtLeast(0.0001)

        fun toOffset(lat: Double, lng: Double): Offset {
            val x = ((lng - minLng) / lngRange * size.width * 0.78f + size.width * 0.11f).toFloat()
            val y = ((maxLat - lat) / latRange * size.height * 0.72f + size.height * 0.16f).toFloat()
            return Offset(x, y)
        }

        for (i in 0 until snapshots.size - 1) {
            val p1 = toOffset(snapshots[i].latitude, snapshots[i].longitude)
            val p2 = toOffset(snapshots[i + 1].latitude, snapshots[i + 1].longitude)
            val fuel = snapshots[i + 1].instantFuelKmPerL

            val segColor = when {
                fuel >= 14f -> Color(0xFF22C55E)
                fuel >= 9f -> Color(0xFFEAB308)
                else -> Color(0xFFEF4444)
            }

            drawLine(color = segColor, start = p1, end = p2, strokeWidth = 14f, cap = StrokeCap.Round)
        }

        val startPos = toOffset(snapshots.first().latitude, snapshots.first().longitude)
        val endPos = toOffset(snapshots.last().latitude, snapshots.last().longitude)
        drawCircle(color = Color.White, radius = 12f, center = startPos)
        drawCircle(color = Color(0xFF38BDF8), radius = 14f, center = endPos)

        val computedMarkers = report.dropEvents.map { marker ->
            val pos = toOffset(marker.latitude, marker.longitude)
            val isSelected = selectedMarker?.index == marker.index
            drawCircle(
                color = if (isSelected) Color.White else Color(0xFFEF4444),
                radius = if (isSelected) 22f else 16f,
                center = pos
            )
            drawCircle(
                color = Color(0xFF991B1B),
                radius = 10f,
                center = pos
            )
            marker to pos
        }
        markerOffsets = computedMarkers
    }
}