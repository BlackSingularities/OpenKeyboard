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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openkeyboard.ui.theme.Palette
import com.blacksingularities.openkeyboard.ui.theme.reactive
import kotlin.math.roundToInt

/** Pozycje sekcji (do przewijania z paska skrótów u góry ustawień). */
val LocalSectionAnchors = androidx.compose.runtime.staticCompositionLocalOf<MutableMap<String, Int>?> { null }

/**
 * Nagłówek sekcji ustawień: ikona, nazwa i cienka linia do krawędzi, pod spodem
 * jedno zdanie wyjaśnienia, czego dotyczy sekcja.
 */
@Composable
fun SectionHeader(icon: ImageVector, title: String, intro: String? = null) {
    val anchors = LocalSectionAnchors.current
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (anchors != null) Modifier.onGloballyPositioned { anchors[title] = it.positionInParent().y.toInt() } else Modifier,
            )
            .padding(top = 18.dp, bottom = 2.dp, start = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        }
        if (intro != null) {
            Text(intro, color = Palette.TextDim, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp, end = 4.dp))
        }
    }
}

/**
 * Przełącznik ustawienia: ikona, nazwa i opis, co dokładnie robi. [enabled] = false – opcja
 * nie ma teraz znaczenia (zależy od innej opcji); [requires] – zdanie, co trzeba zmienić, żeby działała.
 */
class Tile(
    val icon: ImageVector,
    val short: String,
    val full: String,
    val value: Boolean,
    val enabled: Boolean = true,
    val requires: String? = null,
    val onChange: (Boolean) -> Unit,
)

/** Ile przygaszona jest opcja, która teraz nie działa. */
const val DISABLED_ALPHA = 0.38f

/**
 * Lista przełączników z pełnymi opisami – każdy wiersz mówi wprost, co włącza.
 * Stan widać po przełączniku i po rozjaśnionej ikonie.
 */
@Composable
fun TileGrid(tiles: List<Tile>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(vertical = 2.dp),
    ) {
        tiles.forEach { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (t.enabled) Modifier.reactive(null) { t.onChange(!t.value) } else Modifier.alpha(DISABLED_ALPHA))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (t.value) Color.White else Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(t.icon, null, tint = if (t.value) Palette.Ink else Palette.TextDim, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(t.short, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(t.full, fontSize = 11.sp, color = Palette.TextDim, lineHeight = 14.sp)
                    if (!t.enabled) t.requires?.let { Text(it, fontSize = 11.sp, color = Palette.Amber, lineHeight = 14.sp) }
                }
                androidx.compose.material3.Switch(
                    checked = t.value,
                    onCheckedChange = { t.onChange(it) },
                    enabled = t.enabled,
                    modifier = Modifier.scale(0.82f),
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
                )
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
    enabled: Boolean = true,
    requires: String? = null,
    onChange: (Int) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().then(if (enabled) Modifier else Modifier.alpha(DISABLED_ALPHA)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Text(format(snap(local, range, step)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        if (!enabled) requires?.let { Text(it, fontSize = 11.sp, color = Palette.Amber, modifier = Modifier.padding(start = 40.dp)) }
        Slider(
            value = local,
            enabled = enabled,
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
