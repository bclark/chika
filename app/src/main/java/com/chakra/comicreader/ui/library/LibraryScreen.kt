package com.chakra.comicreader.ui.library

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chakra.comicreader.data.db.ComicEntity
import com.chakra.comicreader.data.smb.SmbEntry
import com.chakra.comicreader.ui.brand.ChikaWordmark
import com.chakra.comicreader.ui.brand.OchreBadge
import com.chakra.comicreader.ui.brand.StarburstShape
import com.chakra.comicreader.ui.brand.comicShadow
import com.chakra.comicreader.ui.brand.halftone
import com.chakra.comicreader.ui.theme.Anton
import com.chakra.comicreader.ui.theme.Archivo
import com.chakra.comicreader.ui.theme.Cream
import com.chakra.comicreader.ui.theme.CreamMuted
import com.chakra.comicreader.ui.theme.Crimson
import com.chakra.comicreader.ui.theme.Ink
import com.chakra.comicreader.ui.theme.InkSoft
import com.chakra.comicreader.ui.theme.Ochre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenComic: (Long) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenDrive: () -> Unit,
) {
    val comics by viewModel.comics.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val defaultRtl by viewModel.defaultRightToLeft.collectAsStateWithLifecycle()
    val libraryColumns by viewModel.libraryColumns.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<ComicEntity?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchOpen) {
        isSearchOpen = false
        viewModel.clearSearch()
        focusManager.clearFocus()
    }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            focusRequester.requestFocus()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val availableColumns = remember(screenWidthDp) {
        when {
            screenWidthDp >= 600.dp -> listOf(2, 3, 4, 6)
            screenWidthDp >= 380.dp -> listOf(2, 3, 4)
            else -> listOf(2, 3)
        }
    }
    val actualColumns = libraryColumns.coerceIn(2, availableColumns.last())
    val gridState = rememberLazyGridState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) viewModel.importComics(uris)
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) viewModel.importFolder(treeUri)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val horizontalSpacing = if (actualColumns >= 4) 12.dp else 16.dp
    val verticalSpacing = if (actualColumns >= 4) 14.dp else 18.dp

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Subtle full-bleed halftone wash + an action-burst behind the header.
        Box(Modifier.matchParentSize().halftone(Crimson, alpha = 0.05f))
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp)
                .size(180.dp)
                .clip(StarburstShape)
                .background(Crimson.copy(alpha = 0.14f)),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(actualColumns),
            state = gridState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 4.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChikaWordmark()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSearchOpen) Ochre else Cream.copy(alpha = 0.10f))
                                    .clickable {
                                        isSearchOpen = !isSearchOpen
                                        if (!isSearchOpen) {
                                            viewModel.clearSearch()
                                            focusManager.clearFocus()
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearchOpen) Ink else Cream,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Cream.copy(alpha = 0.10f))
                                    .clickable(onClick = onOpenMenu),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Cream,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSearchOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp)
                                .comicShadow(offset = 3.dp, color = Color(0xB3000000))
                                .clip(RoundedCornerShape(6.dp))
                                .background(InkSoft)
                                .border(
                                    width = 2.dp,
                                    color = if (isSearchFocused) Ochre else Ink,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isSearchFocused) Ochre else CreamMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { isSearchFocused = it.isFocused },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = Archivo,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = Cream,
                                    ),
                                    cursorBrush = SolidColor(Ochre),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "SEARCH COMICS BY TITLE…",
                                                fontFamily = Archivo,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = CreamMuted.copy(alpha = 0.55f),
                                            )
                                        }
                                        innerTextField()
                                    },
                                )
                                if (searchQuery.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Cream.copy(alpha = 0.12f))
                                            .clickable { viewModel.clearSearch() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = Cream,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (searchQuery.isNotBlank()) {
                        OchreBadge("FOUND ${comics.size} ${if (comics.size == 1) "COMIC" else "COMICS"}")
                    } else {
                        OchreBadge("YOUR LIBRARY")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Columns / Density toggle
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "COLUMNS",
                                fontFamily = Archivo,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 7.sp,
                                color = CreamMuted,
                            )
                            Row(
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(InkSoft)
                                    .border(1.dp, Ink, RoundedCornerShape(3.dp)),
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                availableColumns.forEach { count ->
                                    val isSelected = actualColumns == count
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isSelected) Ochre else Color.Transparent)
                                            .clickable { viewModel.setLibraryColumns(count) }
                                            .padding(horizontal = 7.dp, vertical = 3.5.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$count",
                                            fontFamily = Archivo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = if (isSelected) Ink else CreamMuted,
                                        )
                                    }
                                }
                            }
                        }

                        // Global default direction for new comics; each comic remembers its own once opened.
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "NEW COMICS OPEN",
                                fontFamily = Archivo,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 7.sp,
                                color = CreamMuted,
                            )
                            Text(
                                if (defaultRtl) "RTL" else "LTR",
                                fontFamily = Archivo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Ink,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Ochre)
                                    .clickable { viewModel.toggleDefaultDirection() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            if (comics.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (searchQuery.isNotBlank()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "NO MATCHES",
                                fontFamily = Anton,
                                fontSize = 22.sp,
                                color = Cream,
                            )
                            Text(
                                "No comics found matching \"$searchQuery\"",
                                fontFamily = Archivo,
                                fontSize = 13.sp,
                                color = CreamMuted,
                            )
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Ochre)
                                    .clickable { viewModel.clearSearch() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    "CLEAR SEARCH",
                                    fontFamily = Anton,
                                    fontSize = 12.sp,
                                    color = Ink,
                                )
                            }
                        }
                    } else {
                        Text(
                            "No comics yet. Tap LOAD COMIC to add a CBZ, CBR, or PDF.",
                            fontFamily = Archivo,
                            fontSize = 13.sp,
                            color = CreamMuted,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }
            items(comics, key = { it.id }) { comic ->
                ComicCard(
                    comic = comic,
                    columns = actualColumns,
                    onClick = { onOpenComic(comic.id) },
                    onLongClick = { pendingDelete = comic },
                )
            }
            if (searchQuery.isBlank()) {
                item {
                    AddTile(
                        importing = importing,
                        progress = importProgress,
                        status = importStatus,
                        columns = actualColumns,
                        onClick = { if (!importing) showSourceDialog = true },
                    )
                }
            }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Load comic", fontFamily = Anton, color = Cream) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Crimson)
                            .clickable {
                                showSourceDialog = false
                                picker.launch(arrayOf("*/*"))
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("FROM DEVICE (FILES)", fontFamily = Anton, fontSize = 14.sp, color = Cream)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Ink)
                            .border(1.5.dp, Crimson, RoundedCornerShape(4.dp))
                            .clickable {
                                showSourceDialog = false
                                folderPicker.launch(null)
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("FROM DEVICE (FOLDER)", fontFamily = Anton, fontSize = 14.sp, color = Cream)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Ochre)
                            .clickable {
                                showSourceDialog = false
                                if (viewModel.hasSmbConfig) {
                                    onOpenDrive()
                                } else {
                                    viewModel.showMessage("Configure your SMB drive in Menu first.")
                                    onOpenMenu()
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("FROM SMB DRIVE", fontFamily = Anton, fontSize = 14.sp, color = Ink)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("CANCEL", color = CreamMuted) }
            },
            containerColor = InkSoft,
        )
    }

    pendingDelete?.let { comic ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove comic?", fontFamily = Anton) },
            text = { Text("This removes the imported copy. The original file is untouched.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteComic(comic.id); pendingDelete = null }) {
                    Text("DELETE", color = Crimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("CANCEL") } },
            containerColor = InkSoft,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComicCard(
    comic: ComicEntity,
    columns: Int = 2,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pct = if (comic.pageCount > 0) ((comic.lastPage + 1f) / comic.pageCount).coerceIn(0f, 1f) else 0f
    val started = comic.lastPage > 0
    val borderWidth = if (columns >= 4) 2.dp else 3.dp
    val shadowOffset = if (columns >= 4) 3.dp else 5.dp
    val progressHeight = if (columns >= 4) 5.dp else 7.dp

    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .comicShadow(offset = shadowOffset, color = Color(0xB3000000))
                .clip(RoundedCornerShape(4.dp))
                .background(InkSoft)
                .border(borderWidth, Ink, RoundedCornerShape(4.dp)),
        ) {
            val cover = rememberCover(comic.coverPath)
            if (cover != null) {
                Image(cover, comic.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                GeneratedCover(comic.title, columns = columns)
            }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(progressHeight).background(Ink)) {
                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().background(Ochre))
            }
        }
        Text(
            comic.title.uppercase(),
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (columns >= 5) 10.sp else if (columns >= 4) 11.sp else 12.sp,
            color = Cream,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (columns >= 4) 6.dp else 9.dp),
        )
        Text(
            if (started) "${(pct * 100).toInt()}% · pg ${comic.lastPage + 1}/${comic.pageCount}"
            else "${comic.pageCount} pages",
            fontFamily = Archivo,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (columns >= 4) 8.5.sp else 9.5.sp,
            color = CreamMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun GeneratedCover(title: String, columns: Int = 2) {
    val fontSize = if (columns >= 5) 13.sp else if (columns >= 4) 15.sp else if (columns == 3) 18.sp else 22.sp
    val padding = if (columns >= 4) 8.dp else 12.dp
    Box(Modifier.fillMaxSize().background(InkSoft).halftone(Crimson, alpha = 0.18f)) {
        Text(
            title.uppercase(),
            fontFamily = Anton,
            fontSize = fontSize,
            color = Cream,
            modifier = Modifier.align(Alignment.BottomStart).padding(padding),
        )
    }
}

@Composable
fun SmbDriveScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    val entries by viewModel.smbEntries.collectAsStateWithLifecycle()
    val loading by viewModel.smbLoading.collectAsStateWithLifecycle()
    val path by viewModel.smbPath.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPaths by remember(path, entries) { mutableStateOf(emptySet<String>()) }
    val fileEntries = remember(entries) { entries.filter { !it.isDirectory } }
    val selectedCount = selectedPaths.size
    val totalFiles = fileEntries.size
    val allSelected = totalFiles > 0 && selectedCount == totalFiles

    BackHandler(enabled = selectedCount > 0 && !importing) {
        selectedPaths = emptySet()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.matchParentSize().halftone(Crimson, alpha = 0.05f))

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Cream.copy(alpha = 0.12f))
                        .clickable {
                            if (selectedCount > 0) {
                                selectedPaths = emptySet()
                            } else if (path.isNotEmpty()) {
                                viewModel.loadSmbDirectory(path.substringBeforeLast('\\', ""))
                            } else {
                                onBack()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Cream,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    OchreBadge("SMB DRIVE")
                    if (path.isNotEmpty()) {
                        Text(
                            path,
                            fontFamily = Archivo,
                            fontSize = 11.sp,
                            color = CreamMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            if (fileEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (selectedCount > 0) "$selectedCount OF $totalFiles SELECTED" else "$totalFiles ${if (totalFiles == 1) "COMIC" else "COMICS"}",
                        fontFamily = Archivo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = if (selectedCount > 0) Ochre else CreamMuted,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectedCount > 0 && !allSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(InkSoft)
                                    .border(1.dp, Ink, RoundedCornerShape(3.dp))
                                    .clickable { selectedPaths = emptySet() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "CLEAR",
                                    fontFamily = Anton,
                                    fontSize = 11.sp,
                                    color = CreamMuted,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (allSelected) Ochre else Cream.copy(alpha = 0.12f))
                                .clickable {
                                    selectedPaths = if (allSelected) {
                                        emptySet()
                                    } else {
                                        fileEntries.map { it.path }.toSet()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (allSelected) "DESELECT ALL" else "SELECT ALL",
                                fontFamily = Anton,
                                fontSize = 11.sp,
                                color = if (allSelected) Ink else Cream,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(12.dp))

            if (importing) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(InkSoft)
                        .border(2.dp, Ochre, RoundedCornerShape(6.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            color = Ochre,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = importStatus?.uppercase()
                                ?: if (importProgress > 0f) "LOADING COMIC… ${(importProgress * 100).toInt()}%"
                                else "LOADING COMIC…",
                            fontFamily = Anton,
                            fontSize = 13.sp,
                            color = Cream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (importProgress > 0f) importProgress else 0.05f },
                        color = Ochre,
                        trackColor = Ink,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                }
                Spacer(Modifier.size(12.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Ochre,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        if (path.isNotEmpty()) {
                            item {
                                val parent = path.substringBeforeLast('\\', "")
                                SmbRow(
                                    name = ".. (Up)",
                                    isDirectory = true,
                                    onClick = {
                                        selectedPaths = emptySet()
                                        viewModel.loadSmbDirectory(parent)
                                    },
                                )
                            }
                        }
                        if (entries.isEmpty()) {
                            item {
                                Text(
                                    "No folders or comics found here.",
                                    fontFamily = Archivo,
                                    fontSize = 13.sp,
                                    color = CreamMuted,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                        }
                        items(entries, key = { it.path }) { entry ->
                            val isSelected = entry.path in selectedPaths
                            SmbRow(
                                name = entry.name,
                                isDirectory = entry.isDirectory,
                                isSelected = isSelected,
                                onClick = {
                                    if (entry.isDirectory) {
                                        selectedPaths = emptySet()
                                        viewModel.loadSmbDirectory(entry.path)
                                    } else if (!importing) {
                                        selectedPaths = if (isSelected) {
                                            selectedPaths - entry.path
                                        } else {
                                            selectedPaths + entry.path
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedCount > 0 && !importing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .comicShadow(offset = 3.dp, color = Color(0xB3000000))
                        .clip(RoundedCornerShape(6.dp))
                        .background(InkSoft)
                        .border(2.dp, Ochre, RoundedCornerShape(6.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "$selectedCount ${if (selectedCount == 1) "COMIC" else "COMICS"} SELECTED",
                                fontFamily = Anton,
                                fontSize = 15.sp,
                                color = Cream,
                            )
                            Text(
                                text = "Ready to download into your library",
                                fontFamily = Archivo,
                                fontSize = 11.sp,
                                color = CreamMuted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Ochre)
                                .clickable {
                                    val toImport = fileEntries.filter { it.path in selectedPaths }
                                    viewModel.importSmbComics(toImport) {
                                        onBack()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (selectedCount == 1) "IMPORT COMIC" else "IMPORT ALL ($selectedCount)",
                                fontFamily = Anton,
                                fontSize = 13.sp,
                                color = Ink,
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddTile(
    importing: Boolean,
    progress: Float,
    status: String? = null,
    columns: Int = 2,
    onClick: () -> Unit,
) {
    val starburstSize = if (columns >= 5) 42.dp else if (columns >= 4) 50.dp else 64.dp
    val iconSize = if (columns >= 5) 18.dp else if (columns >= 4) 22.dp else 26.dp
    val fontSize = if (columns >= 5) 11.sp else if (columns >= 4) 12.sp else 14.sp
    val spacing = if (columns >= 4) 6.dp else 10.dp
    val contentPadding = if (columns >= 4) 8.dp else 16.dp
    val strokeWidth = if (columns >= 4) 2.dp else 3.dp

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .drawBehind {
                drawRoundRect(
                    color = CreamMuted,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
            .halftone(Ochre, alpha = 0.10f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            Box(
                Modifier.size(starburstSize).clip(StarburstShape).background(Crimson),
                contentAlignment = Alignment.Center,
            ) {
                if (importing) CircularProgressIndicator(color = Cream, modifier = Modifier.size(iconSize))
                else Icon(Icons.Default.Add, contentDescription = "Load comic", tint = Cream, modifier = Modifier.size(iconSize))
            }
            Text(
                if (importing && status != null) status.uppercase()
                else if (importing && progress > 0f) "LOADING ${(progress * 100).toInt()}%"
                else if (importing) "LOADING…"
                else "LOAD COMIC",
                fontFamily = Anton,
                fontSize = fontSize,
                color = Cream,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            if (importing) {
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Ochre,
                        trackColor = Ink,
                        modifier = Modifier.fillMaxWidth().height(if (columns >= 4) 4.dp else 6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                } else {
                    LinearProgressIndicator(
                        color = Ochre,
                        trackColor = Ink,
                        modifier = Modifier.fillMaxWidth().height(if (columns >= 4) 4.dp else 6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmbRow(
    name: String,
    isDirectory: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) Ochre else Color.Transparent
    val bgColor = if (isSelected) Ochre.copy(alpha = 0.14f) else Ink

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (isDirectory) Ochre else if (isSelected) Ochre else Cream,
            modifier = Modifier.size(18.dp),
        )
        Text(
            name,
            fontFamily = Archivo,
            fontWeight = if (isDirectory || isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = Cream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isDirectory) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = CreamMuted.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer(rotationZ = 180f),
            )
        } else {
            // Retro comic checkbox
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isSelected) Ochre else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) Ochre else CreamMuted.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(3.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Ink,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCover(path: String?): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        }
    }
    return state
}
