package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun CustomHorizontalBarChart(
    data: Map<String, Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No analytical data stream", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
        return
    }

    val maxValue = data.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.forEach { (label, value) ->
            var animatedScale by remember { mutableStateOf(0f) }
            val scale by animateFloatAsState(
                targetValue = animatedScale,
                animationSpec = tween(800)
            )
            LaunchedEffect(value) {
                animatedScale = (value / maxValue).toFloat()
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("₹${String.format("%,.0f", value)}", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(scale.coerceIn(0.01f, 1f))
                            .background(
                                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun CustomLineChart(
    data: List<Double>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No trend historical records", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
        return
    }

    val maxVal = data.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height * i / gridLines
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            if (data.size > 1) {
                val stepX = width / (data.size - 1)
                val points = data.mapIndexed { idx, v ->
                    Offset(idx * stepX, height - (v.toFloat() / maxVal.toFloat() * height * 0.85f))
                }

                // Draw solid line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = color,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 4f
                    )
                }

                // Draw circles at points
                points.forEach { pt ->
                    drawCircle(
                        color = color,
                        radius = 8f,
                        center = pt
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 4f,
                        center = pt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun CustomPieChart(
    data: Map<String, Int>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.values.sum() == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No segment distribution metadata", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
        return
    }

    val total = data.values.sum().toFloat()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.values.forEachIndexed { index, value ->
                    val sweepAngle = (value.toFloat() / total) * 360f
                    val color = colors.getOrElse(index) { Color.Gray }
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.entries.forEachIndexed { index, entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.getOrElse(index) { Color.Gray }, RoundedCornerShape(3.dp))
                    )
                    Text(
                        "${entry.key}: ${entry.value} (${String.format("%.1f", (entry.value / total) * 100)}%)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CustomDonutChart(
    data: Map<String, Double>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.values.sum() == 0.0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No segmentation value sets", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
        return
    }

    val total = data.values.sum().toFloat()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeWidth = 24f
                data.values.forEachIndexed { index, value ->
                    val sweepAngle = (value.toFloat() / total) * 360f
                    val color = colors.getOrElse(index) { Color.Gray }
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Total",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
                Text(
                    "₹${String.format("%.0f", total / 100000.0)}L",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.entries.forEachIndexed { index, entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.getOrElse(index) { Color.Gray }, RoundedCornerShape(3.dp))
                    )
                    Text(
                        "${entry.key} (₹${String.format("%,.0f", entry.value)})",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
