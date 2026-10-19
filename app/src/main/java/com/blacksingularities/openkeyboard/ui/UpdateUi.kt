package com.blacksingularities.openkeyboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openkeyboard.i18n.tr
import com.blacksingularities.openkeyboard.ui.components.RowIcon
import com.blacksingularities.openkeyboard.ui.theme.GlassLevel
import com.blacksingularities.openkeyboard.ui.theme.Palette
import com.blacksingularities.openkeyboard.ui.theme.glass
import com.blacksingularities.openkeyboard.update.UpdateState
import kotlin.math.roundToInt

/**
 * Baner nowej wersji: jednym dotknięciem aplikacja pobiera APK z GitHub Releases i instaluje go
 * (bez przeglądarki). Od Androida 12 kolejne aktualizacje mogą przejść bez potwierdzenia systemu.
 */
@Composable
fun UpdateBanner(vm: AppViewModel, modifier: Modifier = Modifier) {
    val u = vm.update
    AnimatedVisibility(visible = vm.updatePending && !vm.updateBannerHidden, modifier = modifier) {
        val (title, action) = when (u) {
            is UpdateState.Available -> tr("Nowa wersja {0}", u.info.version) to tr("Aktualizuj")
            is UpdateState.Downloading -> tr("Pobieranie {0} · {1}%", u.info.version, (u.progress * 100).roundToInt()) to null
            is UpdateState.Ready -> tr("Wersja {0} pobrana", u.info.version) to tr("Zainstaluj")
            is UpdateState.Installing -> tr("Instalowanie {0}…", u.info.version) to null
            else -> "" to null
        }
        val notes = (u as? UpdateState.Available)?.info?.notes ?: (u as? UpdateState.Ready)?.info?.notes
        Column(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(20.dp), GlassLevel.Thin)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (u !is UpdateState.Available) tr("Nie zamykaj aplikacji") else tr("Pobierze się i zainstaluje automatycznie"),
                        color = Palette.TextDim,
                        fontSize = 11.sp,
                    )
                }
                if (action != null) {
                    Text(
                        action,
                        color = Palette.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable { vm.updateNow() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                if (u is UpdateState.Available) {
                    Icon(
                        Icons.Rounded.Close,
                        tr("Później"),
                        tint = Palette.TextDim,
                        modifier = Modifier.padding(start = 6.dp).size(32.dp).clip(RoundedCornerShape(50)).clickable { vm.updateBannerHidden = true }.padding(6.dp),
                    )
                }
            }
            if (u is UpdateState.Downloading) {
                LinearProgressIndicator(
                    progress = { u.progress },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
            if (!notes.isNullOrBlank()) {
                Text(notes, color = Palette.TextDim, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 34.dp))
            }
        }
    }
}

@Composable
fun UpdateRow(vm: AppViewModel) {
    val update = vm.update
    val (status, action, handler) = when (update) {
        UpdateState.Idle -> Triple(tr("Wersja {0}", vm.appVersion), tr("Sprawdź"), { vm.checkUpdate() })
        UpdateState.Checking -> Triple(tr("Sprawdzanie…"), null, {})
        UpdateState.UpToDate -> Triple(tr("Wersja {0} jest najnowsza", vm.appVersion), tr("Sprawdź"), { vm.checkUpdate() })
        is UpdateState.Available -> Triple(tr("Dostępna wersja {0}", update.info.version), tr("Aktualizuj"), { vm.updateNow() })
        is UpdateState.Downloading -> Triple(tr("Pobieranie {0} · {1}%", update.info.version, (update.progress * 100).roundToInt()), null, {})
        is UpdateState.Ready -> Triple(tr("Wersja {0} gotowa", update.info.version), tr("Zainstaluj"), { vm.installUpdate() })
        is UpdateState.Installing -> Triple(tr("Instalowanie {0}…", update.info.version), null, {})
        is UpdateState.Failed -> Triple(update.message, tr("Ponów"), { vm.checkUpdate() })
    }
    Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(Icons.Rounded.SystemUpdate)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("OpenKeyboard ${vm.appVersion}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(status, fontSize = 12.sp, color = Palette.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (action != null) {
                Text(
                    action,
                    color = Palette.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Palette.Amber)
                        .clickable(onClick = handler)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        if (update is UpdateState.Downloading) {
            LinearProgressIndicator(
                progress = { update.progress },
                color = Palette.Amber,
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        val notes = (update as? UpdateState.Available)?.info?.notes ?: (update as? UpdateState.Ready)?.info?.notes
        if (!notes.isNullOrBlank()) {
            Text(notes, fontSize = 12.sp, lineHeight = 17.sp, color = Palette.TextDim, modifier = Modifier.padding(top = 8.dp, start = 44.dp))
        }
    }
}
