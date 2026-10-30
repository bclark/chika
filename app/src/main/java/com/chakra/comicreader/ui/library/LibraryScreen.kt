package com.chakra.comicreader.ui.library

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val defaultRtl by viewModel.defaultRightToLeft.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<ComicEntity?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importComic(uri)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

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
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    ChikaWordmark()
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OchreBadge("YOUR LIBRARY")
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
            if (comics.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No comics yet. Tap LOAD COMIC to add a CBZ, CBR, or PDF.",
                        fontFamily = Archivo,
                        fontSize = 13.sp,
                        color = CreamMuted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            items(comics, key = { it.id }) { comic ->
                ComicCard(
                    comic = comic,
                    onClick = { onOpenComic(comic.id) },
                    onLongClick = { pendingDelete = comic },
                )
            }
            item {
                AddTile(
                    importing = importing,
                    progress = importProgress,
                    onClick = { if (!importing) showSourceDialog = true },
                )
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
                        Text("FROM DEVICE", fontFamily = Anton, fontSize = 14.sp, color = Cream)
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
                        Text("FROM DRIVE", fontFamily = Anton, fontSize = 14.sp, color = Ink)
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
private fun ComicCard(comic: ComicEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    val pct = if (comic.pageCount > 0) ((comic.lastPage + 1f) / comic.pageCount).coerceIn(0f, 1f) else 0f
    val started = comic.lastPage > 0
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .comicShadow(offset = 5.dp, color = Color(0xB3000000))
                .clip(RoundedCornerShape(4.dp))
                .background(InkSoft)
                .border(3.dp, Ink, RoundedCornerShape(4.dp)),
        ) {
            val cover = rememberCover(comic.coverPath)
            if (cover != null) {
                Image(cover, comic.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                GeneratedCover(comic.title)
            }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(7.dp).background(Ink)) {
                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().background(Ochre))
            }
        }
        Text(
            comic.title.uppercase(),
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = Cream,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        Text(
            if (started) "${(pct * 100).toInt()}% · pg ${comic.lastPage + 1}/${comic.pageCount}"
            else "${comic.pageCount} pages",
            fontFamily = Archivo,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            color = CreamMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun GeneratedCover(title: String) {
    Box(Modifier.fillMaxSize().background(InkSoft).halftone(Crimson, alpha = 0.18f)) {
        Text(
            title.uppercase(),
            fontFamily = Anton,
            fontSize = 22.sp,
            color = Cream,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
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
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                            if (path.isNotEmpty()) {
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

            Spacer(Modifier.size(18.dp))

            if (importing) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(InkSoft)
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
                            if (importProgress > 0f) "LOADING COMIC… ${(importProgress * 100).toInt()}%"
                            else "LOADING COMIC…",
                            fontFamily = Anton,
                            fontSize = 14.sp,
                            color = Cream,
                        )
                    }
                    if (importProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { importProgress },
                            color = Ochre,
                            trackColor = Ink,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        )
                    } else {
                        LinearProgressIndicator(
                            color = Ochre,
                            trackColor = Ink,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
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
                                    onClick = { viewModel.loadSmbDirectory(parent) },
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
                            SmbRow(
                                name = entry.name,
                                isDirectory = entry.isDirectory,
                                onClick = {
                                    if (entry.isDirectory) {
                                        viewModel.loadSmbDirectory(entry.path)
                                    } else if (!importing) {
                                        viewModel.importSmbComic(entry)
                                        onBack()
                                    }
                                },
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
private fun AddTile(importing: Boolean, progress: Float, onClick: () -> Unit) {
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
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
            .halftone(Ochre, alpha = 0.10f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(StarburstShape).background(Crimson),
                contentAlignment = Alignment.Center,
            ) {
                if (importing) CircularProgressIndicator(color = Cream, modifier = Modifier.size(26.dp))
                else Icon(Icons.Default.Add, contentDescription = "Load comic", tint = Cream)
            }
            Text(
                if (importing && progress > 0f) "LOADING ${(progress * 100).toInt()}%"
                else if (importing) "LOADING…"
                else "LOAD COMIC",
                fontFamily = Anton,
                fontSize = 14.sp,
                color = Cream,
            )
            if (importing) {
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Ochre,
                        trackColor = Ink,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                } else {
                    LinearProgressIndicator(
                        color = Ochre,
                        trackColor = Ink,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmbRow(name: String, isDirectory: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Ink)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (isDirectory) Ochre else Cream,
            modifier = Modifier.size(18.dp),
        )
        Text(
            name,
            fontFamily = Archivo,
            fontWeight = if (isDirectory) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = Cream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
