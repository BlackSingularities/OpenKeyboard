package com.blacksingularities.openkeyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openkeyboard.ui.theme.Palette
import com.blacksingularities.openkeyboard.ui.theme.reactive
import kotlin.math.roundToInt

/** Nagłówek sekcji ustawień: ikona, nazwa i cienka linia do krawędzi. */
@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
    }
}

/** Kafelek-przełącznik: ikona i krótki podpis; pełny opis po przytrzymaniu. */
class Tile(
    val icon: ImageVector,
    val short: String,
    val full: String,
    val value: Boolean,
    val onChange: (Boolean) -> Unit,
)

@Composable
fun TileGrid(tiles: List<Tile>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t ->
                    Column(
                        Modifier
                            .weight(1f)
                            .reactive(t.full) { t.onChange(!t.value) }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (t.value) Color.White else Color.White.copy(alpha = 0.06f))
                            .padding(vertical = 9.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(t.icon, null, tint = if (t.value) Palette.Ink else Palette.TextDim, modifier = Modifier.size(20.dp))
                        Text(
                            t.short,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            color = if (t.value) Palette.Ink else Palette.TextDim,
                            fontWeight = if (t.value) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Suwak w wierszu ustawień: ikona, nazwa, wartość. Podgląd zmienia się na żywo,
 * zapis następuje po puszczeniu suwaka.
 */
@Composable
fun SliderRow(
    icon: ImageVector,
    title: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    format: (Int) -> String = { it.toString() },
    onChange: (Int) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Text(format(snap(local, range, step)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(snap(local, range, step)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.padding(start = 40.dp).height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
            ),
        )
    }
}

private fun snap(v: Float, range: IntRange, step: Int): Int =
    ((v / step).roundToInt() * step).coerceIn(range.first, range.last)

/** Kafelek z liczbą (statystyki). */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        Text(label, fontSize = 10.sp, color = Palette.TextDim, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}
