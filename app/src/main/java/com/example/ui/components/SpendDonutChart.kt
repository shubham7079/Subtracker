package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CategorySpend
import com.example.util.CurrencyHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpendDonutChart(
    categorySpends: List<CategorySpend>,
    totalMonthlySpend: Double,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<CategorySpend?>(null) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(categorySpends) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spend_donut_chart"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(176.dp)
                    .testTag("donut_canvas")
            ) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                if (categorySpends.isEmpty() || totalMonthlySpend <= 0.0) {
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    return@Canvas
                }

                var startAngle = -90f
                for (spend in categorySpends) {
                    val sweepAngle = spend.percentage * 360f * animatedProgress.value
                    if (sweepAngle > 0.5f) {
                        val isSelected = selectedCategory?.category == spend.category
                        val drawColor = if (selectedCategory == null || isSelected) {
                            spend.category.composeColor
                        } else {
                            spend.category.composeColor.copy(alpha = 0.35f)
                        }

                        drawArc(
                            color = drawColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f, // subtle gap
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) strokeWidth * 1.15f else strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                    startAngle += spend.percentage * 360f
                }
            }

            // Center Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedCategory != null) {
                    Text(
                        text = selectedCategory!!.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = CurrencyHelper.format(selectedCategory!!.monthlyTotal, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(selectedCategory!!.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = selectedCategory!!.category.composeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Monthly",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyHelper.format(totalMonthlySpend, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categorySpends.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive legend chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (spend in categorySpends) {
                val isSelected = selectedCategory?.category == spend.category
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) {
                        spend.category.composeColor.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .clickable {
                            selectedCategory = if (isSelected) null else spend
                        }
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .then(
                                    Modifier
                                        .padding(0.dp)
                                )
                        ) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = spend.category.composeColor)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = spend.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(spend.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
