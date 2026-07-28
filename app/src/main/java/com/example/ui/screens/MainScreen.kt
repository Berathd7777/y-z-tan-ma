package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FaceViewModel
import com.example.utils.ImageUtils
import com.example.utils.SampleFaceGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FaceViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Camera / Scan, 1: Database Directory
    var showAddFriendDialog by remember { mutableStateOf(false) }

    val friends by viewModel.allFriends.collectAsState()
    val queryPhoto by viewModel.queryPhoto.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Activity Launchers for Camera & Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = ImageUtils.readUriBytes(context, it)
            if (bytes != null) {
                viewModel.setQueryPhoto(bytes)
            } else {
                Toast.makeText(context, "Fotoğraf yüklenemedi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val bytes = ImageUtils.compressBitmap(it)
            viewModel.setQueryPhoto(bytes)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yüz Tanıma & Rehber",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Material 3 Navigation Tab Bar
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Kamera ve Tarayıcı", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Camera, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Arkadaş Rehberi", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
            }

            // Tab Views with animated switcher
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabSwitcher"
            ) { tab ->
                when (tab) {
                    0 -> CameraScanTab(
                        queryPhoto = queryPhoto,
                        isScanning = isScanning,
                        scanResult = scanResult,
                        errorMessage = errorMessage,
                        hasFriends = friends.isNotEmpty(),
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onCameraClick = { cameraLauncher.launch(null) },
                        onScanClick = { viewModel.runFaceRecognition() },
                        onResetClick = { viewModel.setQueryPhoto(null) },
                        onSelectSampleQuery = { name, theme, glasses, hat, mustache ->
                            val bytes = SampleFaceGenerator.generateFace(
                                name, AndroidColor.parseColor(theme), glasses, hat, mustache, isQueryImage = true
                            )
                            viewModel.setQueryPhoto(bytes)
                        }
                    )
                    1 -> FriendDatabaseTab(
                        friends = friends,
                        onAddFriendClick = { showAddFriendDialog = true },
                        onLoadSamplesClick = { viewModel.loadSampleFriends() },
                        onDeleteFriend = { id -> viewModel.deleteFriend(id) },
                        onDeletePhoto = { photoId -> viewModel.deletePhoto(photoId) },
                        onAddPhotoToFriend = { id, bytes -> viewModel.addPhotoToFriend(id, bytes) }
                    )
                }
            }
        }
    }

    // Add Friend Dialog Screen Overlay
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onSave = { name, social, photos ->
                viewModel.addFriend(name, social, photos)
                showAddFriendDialog = false
            }
        )
    }
}

// --- TAB 1: CAMERA & SCANNER INTERFACE ---

@Composable
fun CameraScanTab(
    queryPhoto: ByteArray?,
    isScanning: Boolean,
    scanResult: com.example.data.api.FaceMatchResult?,
    errorMessage: String?,
    hasFriends: Boolean,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onScanClick: () -> Unit,
    onResetClick: () -> Unit,
    onSelectSampleQuery: (String, String, Boolean, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Scanner Title & Subtext
            Text(
                text = "Yapay Zekâ ile Anında Yüz Tanıma",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Çektiğiniz veya yüklediğiniz fotoğrafı rehberinizdeki kayıtlı kişilerle karşılaştırır, sosyal medya hesaplarını bulur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Image Selection & Display Box
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (queryPhoto != null) {
                        val bitmap = remember(queryPhoto) {
                            BitmapFactory.decodeByteArray(queryPhoto, 0, queryPhoto.size)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Sorgulanan fotoğraf",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Close Button overlay
                            IconButton(
                                onClick = onResetClick,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Sıfırla", tint = Color.White)
                            }
                        }
                    } else {
                        // Empty State Display inside Image Frame
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onGalleryClick() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Fotoğraf Çekin veya Yükleyin",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Kamerayı başlatmak veya galeriden seçmek için dokunun",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Scan Overlay Animating bar
                    if (isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Image Selection Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("capture_photo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kamera", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pick_gallery_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galeri", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Demo Testing Helpers Suite
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Simülatör ve Hızlı Test Seçenekleri",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Kameranız yoksa, yapay zekayı anında denemek için aşağıdan bir örnek sorgu karakteri seçebilirsiniz:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SuggestionChip(
                                onClick = { onSelectSampleQuery("Ahmet", "#1976D2", true, false, false) },
                                label = { Text("Ahmet (Gözlüklü)") }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = { onSelectSampleQuery("Ayşe", "#C2185B", false, true, false) },
                                label = { Text("Ayşe (Şapkalı)") }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = { onSelectSampleQuery("Can", "#FF8F00", false, false, true) },
                                label = { Text("Can (Bıyıklı)") }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = { onSelectSampleQuery("Yabancı", "#388E3C", true, true, true) },
                                label = { Text("Unregistered (Kayıtsız)") }
                            )
                        }
                    }
                }
            }
        }

        // SCAN & MATCH SUBMIT ACTION BUTTON
        item {
            Button(
                onClick = onScanClick,
                enabled = queryPhoto != null && !isScanning && hasFriends,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("run_scan_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Yüz Özellikleri Analiz Ediliyor...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Face, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("YÜZÜ REHBERDE ARA VE TANI", fontWeight = FontWeight.Bold)
                }
            }

            if (!hasFriends) {
                Text(
                    text = "⚠️ Tarama yapabilmek için önce 'Arkadaş Rehberi' sekmesinden bir arkadaş eklemeli veya hazır örnek arkadaşları yüklemelisiniz.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // SCANNING LOGS & ERRS STATE
        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Hata", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // RECOGNITION MATCH RESULTS SHEET
        if (scanResult != null) {
            item {
                val matched = scanResult.matchedFriendId != null
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (matched) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (matched) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (matched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (matched) "EŞLEŞME BULUNDU! 🎉" else "BİLİNMEYEN YÜZ / KAYITSIZ ⚠️",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (matched) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (matched) {
                                    Text(
                                        text = "Güven Oranı: %${(scanResult.confidence * 100).toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = if (matched) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f)
                        )

                        if (matched) {
                            Text(
                                text = "Kişi:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = scanResult.matchName ?: "Bilinmeyen",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Social Media Handle chip
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Sosyal Medya", scanResult.reason)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Detaylar kopyalandı!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sosyal Medya Bilgisini Kopyala",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text(
                            text = "Yapay Zekâ Karşılaştırma Analizi:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (matched) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = scanResult.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (matched) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 2: FRIEND DATABASE DIRECTORY INTERFACE ---

@Composable
fun FriendDatabaseTab(
    friends: List<com.example.data.db.FriendWithPhotos>,
    onAddFriendClick: () -> Unit,
    onLoadSamplesClick: () -> Unit,
    onDeleteFriend: (Long) -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onAddPhotoToFriend: (Long, ByteArray) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Database Control Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Arkadaş Veritabanı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kayıtlı ${friends.size} kişi klasörü bulunuyor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddFriendClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_friend_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekle")
                }
            }
        }

        // Empty State Handler
        if (friends.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Henüz Arkadaş Eklenmemiş",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Yüz tanıma yapabilmek için arkadaşlarınızın profillerini ve referans fotoğraflarını kaydetmelisiniz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onLoadSamplesClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("load_samples_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Örnek Karakterleri Otomatik Yükle")
                        }
                    }
                }
            }
        } else {
            // Display registered friends with reference image scroll rows
            items(friends, key = { it.friend.id }) { item ->
                var expanded by remember { mutableStateOf(false) }

                val pickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        val bytes = ImageUtils.readUriBytes(context, it)
                        if (bytes != null) {
                            onAddPhotoToFriend(item.friend.id, bytes)
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.friend.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Sosyal Medya: ${item.friend.socialMedia}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { expanded = !expanded }
                                ) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Detayları Göster"
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteFriend(item.friend.id) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Profili Sil", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Expanded View showing photo grid
                        if (expanded || true) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Referans Fotoğrafları (${item.photos.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Add New Reference Photo Button
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                            .clickable { pickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Ekle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                // Loaded photos list
                                items(item.photos, key = { it.id }) { photo ->
                                    val photoBitmap = remember(photo.photoBytes) {
                                        BitmapFactory.decodeByteArray(photo.photoBytes, 0, photo.photoBytes.size)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        if (photoBitmap != null) {
                                            Image(
                                                bitmap = photoBitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Delete Photo overlay button
                                        IconButton(
                                            onClick = { onDeletePhoto(photo.id) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Fotoğrafı Sil",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENT: DIALOG FOR ADDING NEW FRIEND PROFILE ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, socialMedia: String, photos: List<ByteArray>) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var socialMedia by remember { mutableStateOf("") }
    val selectedPhotos = remember { mutableStateListOf<ByteArray>() }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = ImageUtils.readUriBytes(context, it)
            if (bytes != null) {
                selectedPhotos.add(bytes)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Arkadaş Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim Soyisim") },
                    placeholder = { Text("Örn. Ahmet Yılmaz") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_friend_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = socialMedia,
                    onValueChange = { socialMedia = it },
                    label = { Text("Sosyal Medya Hesabı") },
                    placeholder = { Text("Örn. @ahmet_ylmz veya link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_friend_social_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Referans Fotoğrafları (${selectedPhotos.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(75.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    .clickable { photoPicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Fotoğraf Ekle", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        items(selectedPhotos) { bytes ->
                            val bitmap = remember(bytes) {
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                            Box(
                                modifier = Modifier
                                    .size(75.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                IconButton(
                                    onClick = { selectedPhotos.remove(bytes) },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Kaldır",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && socialMedia.isNotBlank() && selectedPhotos.isNotEmpty()) {
                        onSave(name, socialMedia, selectedPhotos.toList())
                    } else {
                        Toast.makeText(context, "Lütfen tüm alanları doldurun ve en az bir fotoğraf ekleyin.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = name.isNotBlank() && socialMedia.isNotBlank() && selectedPhotos.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_friend_button")
            ) {
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("İptal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
