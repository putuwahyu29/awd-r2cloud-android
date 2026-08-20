package com.awd.r2cloud.ui.screen.filebrowser

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.awd.r2cloud.R
import com.awd.r2cloud.domain.model.FileItem
import com.awd.r2cloud.ui.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FilePreviewDialog(
    fileItem: FileItem,
    url: String,
    allFiles: List<FileItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDownload: (FileItem) -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { allFiles.size }
    var isZoomModeActive by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
        isZoomModeActive = false
    }

    var showShareSheet by remember { mutableStateOf(false) }
    
    val currentFile = allFiles.getOrNull(pagerState.currentPage) ?: fileItem
    val ext = currentFile.name.lowercase().substringAfterLast(".", "")
    val isMedia = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "mp4", "mkv", "mov", "webm", "avi", "3gp")
    val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
    
    val containerBgColor = if (isMedia) Color.Black else MaterialTheme.colorScheme.background

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false // CRITICAL: This allows Insets to work inside Dialog
        )
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerBgColor,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    windowInsets = TopAppBarDefaults.windowInsets,
                    title = {
                        Column {
                            Text(currentFile.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                text = "${FileUtils.formatSize(currentFile.size)} • ${pagerState.currentPage + 1} / ${allFiles.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMedia) Color.LightGray else MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = stringResource(R.string.close),
                                tint = if (isMedia) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onDownload(currentFile) }) {
                            Icon(
                                imageVector = Icons.Default.Download, 
                                contentDescription = stringResource(R.string.download),
                                tint = if (isMedia) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showShareSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Share, 
                                contentDescription = stringResource(R.string.share_file),
                                tint = if (isMedia) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isImage) {
                            IconToggleButton(
                                checked = isZoomModeActive,
                                onCheckedChange = { isZoomModeActive = it }
                            ) {
                                Icon(
                                    imageVector = if (isZoomModeActive) Icons.Default.ZoomIn else Icons.Default.Search,
                                    contentDescription = stringResource(R.string.zoom_mode_active),
                                    tint = if (isZoomModeActive) MaterialTheme.colorScheme.primary else if (isMedia) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = if (isMedia) {
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            titleContentColor = Color.White
                        )
                    } else {
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isZoomModeActive
                ) { page ->
                    val item = allFiles[page]
                    val currentUrl = if (page == pagerState.currentPage) url else null

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (currentUrl != null) {
                            PreviewContent(
                                fileItem = item, 
                                url = currentUrl, 
                                isZoomEnabled = isZoomModeActive
                            )
                        } else {
                            CircularProgressIndicator(color = if (isMedia) Color.White else MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (showShareSheet) {
                    ShareSheet(
                        url = url,
                        fileName = currentFile.name,
                        onDismiss = { showShareSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewContent(
    fileItem: FileItem, 
    url: String, 
    isZoomEnabled: Boolean
) {
    val ext = fileItem.name.lowercase().substringAfterLast(".", "")
    val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
    val isVideo = ext in listOf("mp4", "mkv", "mov", "webm", "avi", "3gp")
    val isAudio = ext in listOf("mp3", "wav", "flac", "ogg", "aac", "m4a")
    val isPdf = ext == "pdf"
    val isText = ext in listOf(
        "txt", "log", "md", "json", "js", "kt", "java", "gradle", "xml", "html",
        "sh", "py", "sql", "csv", "yaml", "yml", "ini", "conf", "prop", "properties"
    )

    when {
        isImage -> ZoomableImage(url = url, isEnabled = isZoomEnabled)
        isVideo -> VideoPlayerPreview(url = url)
        isAudio -> AudioPlayerPreview(url = url, fileName = fileItem.name)
        isPdf -> PdfViewer(url = url)
        isText -> TextPreview(url = url)
        else -> GenericFileInfoView(fileItem = fileItem)
    }
}

@Composable
fun ZoomableImage(url: String, isEnabled: Boolean) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(isEnabled) {
        if (!isEnabled) {
            scale = 1f
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    
                    if (scale > 1f) {
                        val extraWidth = (containerSize.width * (scale - 1)) / 2f
                        val extraHeight = (containerSize.height * (scale - 1)) / 2f
                        
                        offset = androidx.compose.ui.geometry.Offset(
                            x = (offset.x + pan.x).coerceIn(-extraWidth, extraWidth),
                            y = (offset.y + pan.y).coerceIn(-extraHeight, extraHeight)
                        )
                    } else {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                }
            }
    ) {
        Image(
            painter = rememberAsyncImagePainter(url),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun VideoPlayerPreview(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    var isBuffering by remember { mutableStateOf(true) }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f)
        )
        if (isBuffering) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun AudioPlayerPreview(url: String, fileName: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }
        })

        while (isActive) {
            currentPosition = exoPlayer.currentPosition
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.size(200.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = Color.White
                )
                if (isBuffering) {
                    CircularProgressIndicator(modifier = Modifier.size(180.dp), strokeWidth = 2.dp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = fileName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.White,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { exoPlayer.seekTo(it.toLong()) },
            valueRange = 0f..maxOf(duration.toFloat(), 1f),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { exoPlayer.seekTo(currentPosition - 10000) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Replay10, contentDescription = "-10s", modifier = Modifier.size(32.dp), tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            LargeFloatingActionButton(
                onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { exoPlayer.seekTo(currentPosition + 10000) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Forward10, contentDescription = "+10s", modifier = Modifier.size(32.dp), tint = Color.White)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
fun TextPreview(url: String) {
    var text by remember { mutableStateOf("") }
    val loadingText = stringResource(R.string.loading_content)
    val errorLabel = stringResource(R.string.error_label)
    val isDark = isSystemInDarkTheme()
    
    LaunchedEffect(url) {
        text = loadingText
        text = withContext(Dispatchers.IO) {
            try {
                URL(url).readText()
            } catch (e: Exception) {
                "$errorLabel: ${e.message}"
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        color = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF5F5F5),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun PdfViewer(url: String) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val errorPrefix = stringResource(R.string.pdf_load_error, "")

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "preview.pdf")
                URL(url).openStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                isLoading = false
            } catch (e: Exception) {
                error = "$errorPrefix ${e.message}"
                isLoading = false
            }
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (error != null) {
        Text(error!!, color = MaterialTheme.colorScheme.error)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(pageCount) { index ->
                PdfPageItem(renderer = pdfRenderer!!, pageIndex = index)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
        }
    }
}

@Composable
fun PdfPageItem(renderer: PdfRenderer, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            val page = renderer.openPage(pageIndex)
            val b = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap = b
            page.close()
        }
    }

    bitmap?.let {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Halaman ${pageIndex + 1}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
    } ?: Box(
        modifier = Modifier.fillMaxWidth().height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
fun GenericFileInfoView(fileItem: FileItem) {
    val extension = fileItem.name.substringAfterLast(".", "").lowercase()
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    
    val icon = when {
        mimeType.startsWith("image/") -> Icons.Default.Image
        mimeType.startsWith("video/") -> Icons.Default.VideoFile
        mimeType.startsWith("audio/") -> Icons.Default.AudioFile
        extension in listOf("zip", "rar", "7z", "tar", "gz") -> Icons.Default.FolderZip
        extension == "pdf" -> Icons.Default.PictureAsPdf
        extension in listOf("doc", "docx", "txt", "rtf") -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = fileItem.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = mimeType.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(label = stringResource(R.string.size_label), value = FileUtils.formatSize(fileItem.size))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                InfoRow(label = stringResource(R.string.modified_label), value = fileItem.lastModified?.take(16) ?: "-")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
