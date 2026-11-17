package com.dramaease.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.dramaease.tv.data.AppLanguage
import com.dramaease.tv.data.AppSettings
import com.dramaease.tv.data.TextSize
import com.dramaease.tv.provider.ContentSource
import com.dramaease.tv.ui.SearchViewModel
import com.dramaease.tv.ui.components.BigButton
import com.dramaease.tv.ui.components.MessagePanel
import com.dramaease.tv.ui.components.PosterCard
import com.dramaease.tv.ui.theme.Dimens
import com.dramaease.tv.ui.theme.DramaType
import com.dramaease.tv.ui.theme.LocalDramaColors
import com.dramaease.tv.ui.theme.LocalStrings

@Composable
fun SearchScreen(vm: SearchViewModel, onOpenDrama: (String) -> Unit) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    val state by vm.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val fieldReq = remember { FocusRequester() }
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { fieldReq.requestFocus() } }

    Column(
        Modifier.fillMaxSize().background(c.background).padding(horizontal = Dimens.safeHorizontal, vertical = Dimens.safeVertical),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(s.search, style = DramaType.headline, color = c.text)
        BasicTextField(
            value = state.query,
            onValueChange = vm::search,
            singleLine = true,
            textStyle = DramaType.title.copy(color = c.text),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth().focusRequester(fieldReq)
                .onFocusChanged { focused = it.isFocused }
                // OK on the remote opens the on-screen keyboard.
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { keyboard?.show(); false } else false
                }
                .background(c.surface, RoundedCornerShape(14.dp))
                .border(if (focused) 5.dp else 2.dp, if (focused) c.focusRing else c.textDim, RoundedCornerShape(14.dp))
                .padding(24.dp),
        )
        if (state.query.isBlank()) MessagePanel(s.typeToSearch)
        else when {
            state.failed -> MessagePanel(s.loadFailed)
            state.searched && state.results.isEmpty() -> MessagePanel(s.noResults)
            state.results.isNotEmpty() -> {
                Text(s.searchResultsFor(state.query.trim()), style = DramaType.title, color = c.textDim)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    items(state.results, key = { it.id }) { d -> PosterCard(d, onClick = { onOpenDrama(d.id) }) }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    val firstReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstReq.requestFocus() } }

    fun sizeName(t: TextSize) = when (t) { TextSize.NORMAL -> s.sizeNormal; TextSize.LARGE -> s.sizeLarge; TextSize.EXTRA_LARGE -> s.sizeExtraLarge }
    fun next(t: TextSize) = TextSize.entries[(t.ordinal + 1) % TextSize.entries.size]

    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.safeHorizontal, vertical = Dimens.safeVertical),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(s.settings, style = DramaType.headline, color = c.text)
        Text(s.pressOkToChange, style = DramaType.label, color = c.textDim)
        SettingRow(s.easyMode, if (settings.easyMode) s.on else s.off, Modifier.focusRequester(firstReq)) {
            onChange { it.copy(easyMode = !it.easyMode) }
        }
        SettingRow(s.textSize, sizeName(settings.textSize)) { onChange { it.copy(textSize = next(it.textSize)) } }
        SettingRow(s.highContrast, if (settings.highContrast) s.on else s.off) { onChange { it.copy(highContrast = !it.highContrast) } }
        SettingRow(s.language, s.langName) {
            onChange { it.copy(language = if (it.language == AppLanguage.ENGLISH) AppLanguage.FILIPINO else AppLanguage.ENGLISH) }
        }
        SettingRow(s.subtitleSize, sizeName(settings.subtitleSize)) { onChange { it.copy(subtitleSize = next(it.subtitleSize)) } }
        SettingRow(s.contentSource, if (settings.source == ContentSource.DEMO) s.sourceDemo else s.sourceKisskh) {
            onChange { it.copy(source = if (it.source == ContentSource.DEMO) ContentSource.KISSKH else ContentSource.DEMO) }
        }
        BigButton(s.goBack, onClick = onBack, primary = true, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SettingRow(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalDramaColors.current
    com.dramaease.tv.ui.components.FocusCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = DramaType.title, color = c.text)
            Text(value, style = DramaType.title, color = c.accent)
        }
    }
}
