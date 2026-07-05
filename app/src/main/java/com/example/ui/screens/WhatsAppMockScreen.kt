package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// WhatsApp Exact Colors
val WaDarkGreen = Color(0xFF075E54)
val WaTeal = Color(0xFF128C7E)
val WaLightGreen = Color(0xFFD9FDD3) // Outgoing bubble light theme
val WaIncomingWhite = Color(0xFFFFFFFF) // Incoming bubble light theme
val WaBackgroundBeige = Color(0xFFEFEAE2) // Standard chat background
val WaTextGrey = Color(0xFF667781)
val WaBlueTick = Color(0xFF53BDEB)
val WaFailedRed = Color(0xFFEA0038)
val WaSystemYellow = Color(0xFFFFF2CC)
val WaSystemYellowText = Color(0xFF54656F)
val WaOnlineGreen = Color(0xFF00A884)
val WaBarWhite = Color(0xFFFFFFFF)
val WaTextDark = Color(0xFF111B21)

// High-fidelity Custom shapes for bubbles with real pointer tails
class OutgoingBubbleShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val r = with(density) { 8.dp.toPx() } // Corner radius
            val tailW = with(density) { 6.dp.toPx() } // Tail width
            val tailH = with(density) { 8.dp.toPx() } // Tail height

            moveTo(r, 0f)
            lineTo(width - tailW - r, 0f)
            // Tail beak on top-right
            lineTo(width, 0f)
            lineTo(width - tailW, tailH)
            
            lineTo(width - tailW, height - r)
            quadraticBezierTo(width - tailW, height, width - tailW - r, height)
            lineTo(r, height)
            quadraticBezierTo(0f, height, 0f, height - r)
            lineTo(0f, r)
            quadraticBezierTo(0f, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

class IncomingBubbleShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val r = with(density) { 8.dp.toPx() } // Corner radius
            val tailW = with(density) { 6.dp.toPx() } // Tail width
            val tailH = with(density) { 8.dp.toPx() } // Tail height

            moveTo(tailW + r, 0f)
            // Tail beak on top-left
            lineTo(0f, 0f)
            lineTo(tailW, tailH)
            
            lineTo(tailW, height - r)
            quadraticBezierTo(tailW, height, tailW + r, height)
            lineTo(width - r, height)
            quadraticBezierTo(width, height, width, height - r)
            lineTo(width, r)
            quadraticBezierTo(width, 0f, width - r, 0f)
            lineTo(tailW + r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppMockScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()

    // Screen-level state
    var typedText by remember { mutableStateOf("") }
    var isNextMessageOutgoing by remember { mutableStateOf(true) }
    var showChatSelector by remember { mutableStateOf(false) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showMessageEditor by remember { mutableStateOf<Message?>(null) }
    var showCreateChatDialog by remember { mutableStateOf(false) }
    var showDoodleBackground by remember { mutableStateOf(true) }

    // Mock Status Bar Settings
    var showMockStatusBar by remember { mutableStateOf(true) }
    var mockTime by remember { mutableStateOf("10:49 AM") }
    var mockBattery by remember { mutableStateOf("40") }
    var mockNetworkType by remember { mutableStateOf("4G") }
    var mockWifiSignal by remember { mutableStateOf(4) } // 1-4 scale
    var mockCellSignal by remember { mutableStateOf(4) } // 1-4 scale
    var showMockStatusBarEditor by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) } // Default false to match Brian screenshot
    var showNetworkSpeed by remember { mutableStateOf(false) } // Default false to remove rest notifications
    var showVoLte by remember { mutableStateOf(false) } // Default false to remove rest notifications
    var showNotificationIcon by remember { mutableStateOf(false) } // Default false to remove rest notifications

    // Reference to the main layout for screenshot capture
    var captureViewRef by remember { mutableStateOf<android.view.View?>(null) }

    // Image Picker for profile photo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            activeChat?.let { chat ->
                viewModel.updateActiveChatProfile(
                    name = chat.profileName,
                    photoUri = it.toString(),
                    isOnline = chat.isOnline,
                    lastSeenText = chat.lastSeenText
                )
            }
        }
    }

    // Media launchers for chat messages
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val mediaString = uris.joinToString(",") { it.toString() }
            viewModel.addMessageToActiveChat(
                text = "",
                isOutgoing = isNextMessageOutgoing,
                mediaUris = mediaString
            )
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size)
            }
        }
    }

    val cameraPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addMessageToActiveChat(
                text = "",
                isOutgoing = isNextMessageOutgoing,
                mediaUris = it.toString()
            )
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size)
            }
        }
    }

    var showAttachOptionsDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F2F5))
            ) {
                // Sender Switch Panel (Visible in-app for editing context)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sender mode for next message:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = WaTextGrey
                    )
                    Row {
                        FilterChip(
                            selected = !isNextMessageOutgoing,
                            onClick = { isNextMessageOutgoing = false },
                            label = { Text("Incoming", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White,
                                selectedLabelColor = WaTeal
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = isNextMessageOutgoing,
                            onClick = { isNextMessageOutgoing = true },
                            label = { Text("Outgoing", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WaTeal,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Capture Container
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WaBackgroundBeige)
        ) {
            // AndroidView renders exact high res frame for screen captures
            AndroidView(
                factory = { ctx ->
                    androidx.compose.ui.platform.ComposeView(ctx).apply {
                        setContent {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WaBackgroundBeige)
                            ) {
                                // Full page doodle wallpaper
                                if (showDoodleBackground) {
                                    Image(
                                        painter = painterResource(id = R.drawable.whatsapp_background_doodle),
                                        contentDescription = "Background pattern",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.5f
                                    )
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    // 1. Simulated Status Bar
                                    if (showMockStatusBar) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(WaBarWhite)
                                                .clickable { showMockStatusBarEditor = true }
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left side: Time, WhatsApp notification icon
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = mockTime,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WaTextDark
                                                )
                                                if (showNotificationIcon) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.ChatBubble,
                                                        contentDescription = "WhatsApp notification",
                                                        tint = WaTextDark,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }

                                            // Right side: Network speed/type, Signal, Wifi, Battery
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (showNetworkSpeed) {
                                                    Text(
                                                        text = "15.2 K/S",
                                                        fontSize = 10.sp,
                                                        color = WaTextDark,
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    )
                                                }
                                                if (showVoLte) {
                                                    Text(
                                                        text = "Vo\nLTE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        lineHeight = 8.sp,
                                                        color = WaTextDark,
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    )
                                                }
                                                Text(
                                                    text = mockNetworkType,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WaTextDark,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                
                                                // High Fidelity Custom Cellular Signal
                                                Row(
                                                    verticalAlignment = Alignment.Bottom,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    repeat(4) { index ->
                                                        val barHeight = (3 + index * 2.5).dp
                                                        val isFilled = index < mockCellSignal
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(horizontal = 0.5.dp)
                                                                .width(2.2.dp)
                                                                .height(barHeight)
                                                                .background(if (isFilled) WaTextDark else WaTextDark.copy(alpha = 0.3f))
                                                        )
                                                    }
                                                }

                                                // High Fidelity Custom Wifi Wave Arc Drawing on Canvas
                                                Box(
                                                    modifier = Modifier
                                                        .size(15.dp)
                                                        .padding(end = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        val w = size.width
                                                        val h = size.height
                                                        
                                                        // Dot at bottom
                                                        drawCircle(
                                                            color = if (mockWifiSignal >= 1) WaTextDark else WaTextDark.copy(alpha = 0.3f),
                                                            radius = 1.2.dp.toPx(),
                                                            center = androidx.compose.ui.geometry.Offset(w / 2, h - 1.5.dp.toPx())
                                                        )
                                                        // Arc 1
                                                        drawArc(
                                                            color = if (mockWifiSignal >= 2) WaTextDark else WaTextDark.copy(alpha = 0.3f),
                                                            startAngle = -135f,
                                                            sweepAngle = 90f,
                                                            useCenter = false,
                                                            topLeft = androidx.compose.ui.geometry.Offset(w / 2 - 3.dp.toPx(), h - 5.dp.toPx()),
                                                            size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 6.dp.toPx()),
                                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                        )
                                                        // Arc 2
                                                        drawArc(
                                                            color = if (mockWifiSignal >= 3) WaTextDark else WaTextDark.copy(alpha = 0.3f),
                                                            startAngle = -135f,
                                                            sweepAngle = 90f,
                                                            useCenter = false,
                                                            topLeft = androidx.compose.ui.geometry.Offset(w / 2 - 6.dp.toPx(), h - 9.dp.toPx()),
                                                            size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 12.dp.toPx()),
                                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                        )
                                                        // Arc 3
                                                        drawArc(
                                                            color = if (mockWifiSignal >= 4) WaTextDark else WaTextDark.copy(alpha = 0.3f),
                                                            startAngle = -135f,
                                                            sweepAngle = 90f,
                                                            useCenter = false,
                                                            topLeft = androidx.compose.ui.geometry.Offset(w / 2 - 9.dp.toPx(), h - 13.dp.toPx()),
                                                            size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 18.dp.toPx()),
                                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(2.dp))

                                                // Battery Icon
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(Color.Transparent)
                                                        .padding(horizontal = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .shadow(0.5.dp, shape = RoundedCornerShape(2.dp))
                                                            .background(Color.Transparent)
                                                            .padding(1.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .border(1.dp, WaTextDark, RoundedCornerShape(2.dp))
                                                                .padding(1.dp)
                                                                .width(20.dp)
                                                                .height(10.dp)
                                                        ) {
                                                            val batteryLevel = ((mockBattery.toFloatOrNull() ?: 100f) / 100f).coerceIn(0f, 1f)
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxHeight()
                                                                    .fillMaxWidth(batteryLevel)
                                                                    .background(WaTextDark)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = mockBattery,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = WaTextDark
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 2. Top Custom WhatsApp App Bar (Perfect White background theme with dark elements)
                                    TopAppBar(
                                        title = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clickable { showProfileEditor = true }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                // Profile Avatar with dynamic light colors
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(activeChat?.profileInitialsColor ?: 0xFFEC407A.toInt())),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (activeChat?.profilePhotoUri != null) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(activeChat?.profilePhotoUri),
                                                            contentDescription = "Profile Photo",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Text(
                                                            text = activeChat?.profileName?.firstOrNull()?.toString()?.uppercase() ?: "B",
                                                            color = Color.White,
                                                            fontSize = 17.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = activeChat?.profileName ?: "Brian",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = WaTextDark,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (showSubtitle) {
                                                        Text(
                                                            text = if (activeChat?.isOnline == true) "online" else (activeChat?.lastSeenText ?: ""),
                                                            fontSize = 12.sp,
                                                            color = WaTextGrey,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = { showChatSelector = true }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back to list",
                                                    tint = WaTextDark
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { /* video call */ }) {
                                                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = WaTextDark)
                                            }
                                            IconButton(onClick = { /* voice call */ }) {
                                                Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call", tint = WaTextDark)
                                            }
                                            
                                            var showMenu by remember { mutableStateOf(false) }
                                            IconButton(onClick = { showMenu = true }) {
                                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options", tint = WaTextDark)
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Saved Mock Chats") },
                                                    onClick = {
                                                        showMenu = false
                                                        showChatSelector = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Edit Chat Profile") },
                                                    onClick = {
                                                        showMenu = false
                                                        showProfileEditor = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Edit Custom Status Bar") },
                                                    onClick = {
                                                        showMenu = false
                                                        showMockStatusBarEditor = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(if (showSubtitle) "Hide Status Subtitle" else "Show Status Subtitle") },
                                                    onClick = {
                                                        showMenu = false
                                                        showSubtitle = !showSubtitle
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Toggle Doodle Background") },
                                                    onClick = {
                                                        showMenu = false
                                                        showDoodleBackground = !showDoodleBackground
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Clear Messages") },
                                                    onClick = {
                                                        showMenu = false
                                                        viewModel.clearActiveChatMessages()
                                                    }
                                                )
                                                Divider()
                                                DropdownMenuItem(
                                                    text = { Text("EXPORT MOCK SCREENSHOT", fontWeight = FontWeight.Bold, color = WaTeal) },
                                                    onClick = {
                                                        showMenu = false
                                                        captureViewRef?.let { view ->
                                                            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                                                            val canvas = android.graphics.Canvas(bitmap)
                                                            view.draw(canvas)
                                                            val savedUri = saveBitmapToGallery(context, bitmap, "WhatsApp_Mock_${System.currentTimeMillis()}")
                                                            if (savedUri != null) {
                                                                Toast.makeText(context, "Mock exported successfully!", Toast.LENGTH_LONG).show()
                                                                shareImageIntent(context, savedUri)
                                                            } else {
                                                                Toast.makeText(context, "Failed to save mock", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } ?: run {
                                                            Toast.makeText(context, "Layout render engine is warming up. Try again.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = WaBarWhite,
                                            titleContentColor = WaTextDark,
                                            navigationIconContentColor = WaTextDark,
                                            actionIconContentColor = WaTextDark
                                        ),
                                        modifier = Modifier.shadow(1.dp)
                                    )

                                    // 3. Messages List Area
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                                        ) {
                                            // Custom editable Date Header
                                            item {
                                                var showDateDialog by remember { mutableStateOf(false) }
                                                var dateStr by remember { mutableStateOf("June 13, 2026") }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                                                            .clickable { showDateDialog = true }
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                            .shadow(0.5.dp, shape = RoundedCornerShape(8.dp))
                                                    ) {
                                                        Text(
                                                            text = dateStr,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = WaTextGrey
                                                        )
                                                    }
                                                }

                                                if (showDateDialog) {
                                                    var textInput by remember { mutableStateOf(dateStr) }
                                                    AlertDialog(
                                                        onDismissRequest = { showDateDialog = false },
                                                        title = { Text("Edit Chat Date Header") },
                                                        text = {
                                                            OutlinedTextField(
                                                                value = textInput,
                                                                onValueChange = { textInput = it },
                                                                label = { Text("Date Banner Text") }
                                                            )
                                                        },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                dateStr = textInput
                                                                showDateDialog = false
                                                            }) { Text("Apply") }
                                                        },
                                                        dismissButton = {
                                                            TextButton(onClick = { showDateDialog = false }) { Text("Cancel") }
                                                        }
                                                    )
                                                }
                                            }

                                            // Encryption system box
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .background(WaSystemYellow, shape = RoundedCornerShape(10.dp))
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lock,
                                                            contentDescription = "Encrypted",
                                                            tint = WaSystemYellowText,
                                                            modifier = Modifier
                                                                .size(13.dp)
                                                                .padding(top = 1.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = "Messages and calls are end-to-end encrypted. Only people in this chat can read, listen to, or share them. Learn more",
                                                            fontSize = 11.sp,
                                                            color = WaSystemYellowText,
                                                            textAlign = TextAlign.Center,
                                                            lineHeight = 15.sp
                                                        )
                                                    }
                                                }
                                            }

                                            // List of mock message bubbles
                                            items(messages) { message ->
                                                WhatsAppMessageBubble(
                                                    message = message,
                                                    onTap = { showMessageEditor = message }
                                                )
                                            }
                                        }
                                    }

                                    // 4. WhatsApp floaty message input bottom row (Sitting directly over wallpaper!)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // White rounded pill container (rounded CircleShape)
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .shadow(1.dp, shape = CircleShape)
                                                .background(Color.White, shape = CircleShape)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.EmojiEmotions,
                                                contentDescription = "Emojis",
                                                tint = Color(0xFF8696A0),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable {
                                                        Toast.makeText(context, "Emojis panel mock", Toast.LENGTH_SHORT).show()
                                                    }
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            
                                            BasicTextField(
                                                value = typedText,
                                                onValueChange = { typedText = it },
                                                textStyle = TextStyle(
                                                    color = Color(0xFF111B21),
                                                    fontSize = 17.sp
                                                ),
                                                cursorBrush = SolidColor(Color(0xFF00A884)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("message_input_field"),
                                                decorationBox = { innerTextField ->
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        if (typedText.isEmpty()) {
                                                            Text(
                                                                text = "Message",
                                                                color = Color(0xFF8696A0),
                                                                fontSize = 17.sp
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(
                                                    imeAction = ImeAction.Send
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onSend = {
                                                        if (typedText.isNotBlank()) {
                                                            viewModel.addMessageToActiveChat(typedText, isNextMessageOutgoing)
                                                            typedText = ""
                                                            coroutineScope.launch {
                                                                listState.animateScrollToItem(messages.size)
                                                            }
                                                        }
                                                    }
                                                )
                                            )
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { showAttachOptionsDialog = true }, 
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachFile, 
                                                    contentDescription = "Attach", 
                                                    tint = Color(0xFF8696A0),
                                                    modifier = Modifier.rotate(-45f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            IconButton(
                                                onClick = { cameraPickerLauncher.launch("image/*") }, 
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera, 
                                                    contentDescription = "Camera", 
                                                    tint = Color(0xFF8696A0)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Green Action Button
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .shadow(1.dp, CircleShape)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00A884))
                                                .clickable {
                                                    if (typedText.isNotBlank()) {
                                                        viewModel.addMessageToActiveChat(typedText, isNextMessageOutgoing)
                                                        typedText = ""
                                                        coroutineScope.launch {
                                                            listState.animateScrollToItem(messages.size)
                                                        }
                                                    } else {
                                                        isNextMessageOutgoing = !isNextMessageOutgoing
                                                        Toast.makeText(context, "Switched message sender!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .testTag("send_message_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (typedText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                                contentDescription = "Send or Record",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        captureViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // --- Saved Chats list manager ---
    if (showChatSelector) {
        AlertDialog(
            onDismissRequest = { showChatSelector = false },
            title = { Text("Mock Conversations") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showChatSelector = false
                            showCreateChatDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WaTeal)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Mock Conversation")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(chats) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectChat(chat.id)
                                        showChatSelector = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(chat.profileInitialsColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chat.profilePhotoUri != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(chat.profilePhotoUri),
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = chat.profileName.firstOrNull()?.toString()?.uppercase() ?: "B",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = chat.profileName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = if (chat.isOnline) "online" else chat.lastSeenText, fontSize = 12.sp, color = WaTextGrey)
                                }
                                if (chats.size > 1) {
                                    IconButton(
                                        onClick = {
                                            viewModel.selectChat(chat.id)
                                            viewModel.deleteActiveChat()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatSelector = false }) { Text("Dismiss") }
            }
        )
    }

    // --- Attachment Options dialog ---
    if (showAttachOptionsDialog) {
        var dateHeaderTextInput by remember { mutableStateOf("Today") }
        var showCustomDateInput by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAttachOptionsDialog = false },
            title = { Text("Attachment Simulator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select media or mock components to append to your chat:", fontSize = 13.sp, color = Color.Gray)
                    
                    Button(
                        onClick = {
                            mediaPickerLauncher.launch("image/*")
                            showAttachOptionsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WaTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Collections, contentDescription = "Gallery", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery (Select multiple images)")
                    }

                    Button(
                        onClick = {
                            cameraPickerLauncher.launch("image/*")
                            showAttachOptionsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WaTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera (Select single image)")
                    }

                    Button(
                        onClick = {
                            viewModel.addMessageToActiveChat(
                                text = "",
                                isOutgoing = isNextMessageOutgoing,
                                mediaUris = "img_deal_1,img_deal_2,img_deal_3,img_deal_4"
                            )
                            showAttachOptionsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WaOnlineGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.GridView, contentDescription = "4-Deals Grid", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mock 4-Deals Grid (Screenshot layouts)")
                    }

                    Button(
                        onClick = {
                            viewModel.addMessageToActiveChat(
                                text = "You deleted this message",
                                isOutgoing = isNextMessageOutgoing,
                                isDeleted = true,
                                timeInput = "11:01 AM"
                            )
                            showAttachOptionsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90A4AE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = "Deleted Message", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insert Mock Deleted Message")
                    }

                    HorizontalDivider()

                    if (!showCustomDateInput) {
                        Button(
                            onClick = { showCustomDateInput = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Date Sep", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Insert Center Date Banner...")
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dateHeaderTextInput,
                                onValueChange = { dateHeaderTextInput = it },
                                label = { Text("Banner text (e.g. Today, Yesterday)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showCustomDateInput = false }) {
                                    Text("Back")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        viewModel.addMessageToActiveChat(
                                            text = dateHeaderTextInput,
                                            isOutgoing = false,
                                            isDateHeader = true
                                        )
                                        showAttachOptionsDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WaTeal)
                                ) {
                                    Text("Insert")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachOptionsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Create Mock Chat dialog ---
    if (showCreateChatDialog) {
        var newChatName by remember { mutableStateOf("") }
        var newChatOnlineText by remember { mutableStateOf("online") }
        var isOnlineVal by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreateChatDialog = false },
            title = { Text("Create New Conversation") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newChatName,
                        onValueChange = { newChatName = it },
                        label = { Text("Contact Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isOnlineVal, onCheckedChange = { isOnlineVal = it })
                        Text("Show contact as 'online'")
                    }
                    if (!isOnlineVal) {
                        OutlinedTextField(
                            value = newChatOnlineText,
                            onValueChange = { newChatOnlineText = it },
                            label = { Text("Custom status text (e.g. last seen...)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newChatName.isNotBlank()) {
                            val colors = listOf(
                                0xFFEC407A, 0xFFAB47BC, 0xFF7E57C2, 0xFF5C6BC0,
                                0xFF42A5F5, 0xFF26A69A, 0xFF66BB6A, 0xFFFFA726, 0xFF8D6E63
                            )
                            val randomColor = colors.random().toInt()
                            viewModel.createNewChat(
                                name = newChatName,
                                initialsColor = randomColor,
                                isOnline = isOnlineVal,
                                lastSeenText = if (isOnlineVal) "online" else newChatOnlineText
                            )
                            showCreateChatDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateChatDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Mock Status Bar Editor ---
    if (showMockStatusBarEditor) {
        var editedTime by remember { mutableStateOf(mockTime) }
        var editedBattery by remember { mutableStateOf(mockBattery) }
        var editedNetworkType by remember { mutableStateOf(mockNetworkType) }
        var editedWifiSignal by remember { mutableStateOf(mockWifiSignal) }
        var editedCellSignal by remember { mutableStateOf(mockCellSignal) }
        var editedShowNetworkSpeed by remember { mutableStateOf(showNetworkSpeed) }
        var editedShowVoLte by remember { mutableStateOf(showVoLte) }
        var editedShowNotificationIcon by remember { mutableStateOf(showNotificationIcon) }

        AlertDialog(
            onDismissRequest = { showMockStatusBarEditor = false },
            title = { Text("Simulated Status Bar Editor") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showMockStatusBar, onCheckedChange = { showMockStatusBar = it })
                        Text("Include mock Status Bar in screenshots")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedTime,
                        onValueChange = { editedTime = it },
                        label = { Text("Simulated Clock Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedBattery,
                        onValueChange = { editedBattery = it },
                        label = { Text("Battery Percentage (0-100)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedNetworkType,
                        onValueChange = { editedNetworkType = it },
                        label = { Text("Network Type Badge (e.g. 4G, 5G)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Wifi Strength selection
                    Text("Wifi Signal Strength (0-4 bars)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WaTextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0..4).forEach { level ->
                            FilterChip(
                                selected = editedWifiSignal == level,
                                onClick = { editedWifiSignal = level },
                                label = { Text("$level bars", fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cell Signal strength selection
                    Text("Cellular Signal Strength (0-4 bars)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WaTextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0..4).forEach { level ->
                            FilterChip(
                                selected = editedCellSignal == level,
                                onClick = { editedCellSignal = level },
                                label = { Text("$level bars", fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Toggle Other Indicators", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WaTextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editedShowNetworkSpeed = !editedShowNetworkSpeed }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = editedShowNetworkSpeed, onCheckedChange = { editedShowNetworkSpeed = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Network Speed (15.2 K/S)", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editedShowVoLte = !editedShowVoLte }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = editedShowVoLte, onCheckedChange = { editedShowVoLte = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show VoLTE Badge", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editedShowNotificationIcon = !editedShowNotificationIcon }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = editedShowNotificationIcon, onCheckedChange = { editedShowNotificationIcon = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Notification Icon", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mockTime = editedTime
                        mockBattery = editedBattery
                        mockNetworkType = editedNetworkType
                        mockWifiSignal = editedWifiSignal
                        mockCellSignal = editedCellSignal
                        showNetworkSpeed = editedShowNetworkSpeed
                        showVoLte = editedShowVoLte
                        showNotificationIcon = editedShowNotificationIcon
                        showMockStatusBarEditor = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showMockStatusBarEditor = false }) { Text("Cancel") }
            }
        )
    }

    // --- Profile Editor Dialog ---
    if (showProfileEditor && activeChat != null) {
        val chat = activeChat!!
        var editedName by remember { mutableStateOf(chat.profileName) }
        var editedLastSeen by remember { mutableStateOf(chat.lastSeenText) }
        var editedIsOnline by remember { mutableStateOf(chat.isOnline) }

        AlertDialog(
            onDismissRequest = { showProfileEditor = false },
            title = { Text("Edit Mock Profile Details") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Contact Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { editedIsOnline = !editedIsOnline }
                    ) {
                        Checkbox(checked = editedIsOnline, onCheckedChange = { editedIsOnline = it })
                        Text("Show Status")
                    }
                    if (!editedIsOnline) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editedLastSeen,
                            onValueChange = { editedLastSeen = it },
                            label = { Text("Last Seen Custom Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WaTeal)
                    ) {
                        Icon(imageVector = Icons.Default.Photo, contentDescription = "Select photo")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Custom Profile Photo")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateActiveChatProfile(
                            name = editedName,
                            photoUri = chat.profilePhotoUri,
                            isOnline = editedIsOnline,
                            lastSeenText = if (editedIsOnline) "online" else editedLastSeen
                        )
                        showProfileEditor = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditor = false }) { Text("Cancel") }
            }
        )
    }

    // --- Message Editor Dialog ---
    showMessageEditor?.let { message ->
        var editedText by remember { mutableStateOf(message.text) }
        var editedTime by remember { mutableStateOf(message.time) }
        var editedIsOutgoing by remember { mutableStateOf(message.isOutgoing) }
        var editedStatus by remember { mutableStateOf(message.status) }
        var editedReactions by remember { mutableStateOf(message.reactions ?: "") }
        var editedIsDeleted by remember { mutableStateOf(message.isDeleted) }
        var editedIsDateHeader by remember { mutableStateOf(message.isDateHeader) }
        var editedMediaUris by remember { mutableStateOf(message.mediaUris ?: "") }

        AlertDialog(
            onDismissRequest = { showMessageEditor = null },
            title = { Text("Edit Mock Message") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        label = { Text("Message Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedTime,
                        onValueChange = { editedTime = it },
                        label = { Text("Message Timestamp (e.g. 11:01 AM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editedIsDeleted = !editedIsDeleted }
                    ) {
                        Checkbox(checked = editedIsDeleted, onCheckedChange = { editedIsDeleted = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("This message is deleted (italic style)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editedIsDateHeader = !editedIsDateHeader }
                    ) {
                        Checkbox(checked = editedIsDateHeader, onCheckedChange = { editedIsDateHeader = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Render as Date Separation Banner")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedMediaUris,
                        onValueChange = { editedMediaUris = it },
                        label = { Text("Media images (comma-separated drawables/URIs)") },
                        placeholder = { Text("e.g. img_deal_1,img_deal_2") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sender Direction:")
                        Row {
                            FilterChip(
                                selected = !editedIsOutgoing,
                                onClick = { editedIsOutgoing = false },
                                label = { Text("Incoming") }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = editedIsOutgoing,
                                onClick = { editedIsOutgoing = true },
                                label = { Text("Outgoing") }
                            )
                        }
                    }
                    if (editedIsOutgoing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Delivery Status Indicator:")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("PENDING", "SENT", "DELIVERED", "READ", "FAILED").forEach { statusOption ->
                                FilterChip(
                                    selected = editedStatus == statusOption,
                                    onClick = { editedStatus = statusOption },
                                    label = {
                                        Text(
                                            text = when (statusOption) {
                                                "PENDING" -> "⏰"
                                                "SENT" -> "✓"
                                                "DELIVERED" -> "✓✓"
                                                "READ" -> "✓✓(B)"
                                                "FAILED" -> "⚠ (Failed)"
                                                else -> statusOption
                                              },
                                              fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedReactions,
                        onValueChange = { editedReactions = it },
                        label = { Text("Emoji Reactions (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select quick reaction shortcuts:", fontSize = 11.sp, color = WaTextGrey)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable {
                                        val currentList = editedReactions.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                        if (currentList.contains(emoji)) {
                                            currentList.remove(emoji)
                                        } else {
                                            currentList.add(emoji)
                                        }
                                        editedReactions = currentList.joinToString(", ")
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.editMessageInActiveChat(
                            messageId = message.id,
                            text = editedText,
                            isOutgoing = editedIsOutgoing,
                            time = editedTime,
                            status = editedStatus,
                            reactions = if (editedReactions.isBlank()) null else editedReactions,
                            isDeleted = editedIsDeleted,
                            mediaUris = if (editedMediaUris.isBlank()) null else editedMediaUris,
                            isDateHeader = editedIsDateHeader
                        )
                        showMessageEditor = null
                    }
                ) { Text("Save Changes") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(message.id)
                        showMessageEditor = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Delete Message") }
            }
        )
    }
}

// --- Custom Image Loader for drawable names or file URIs ---
@Composable
fun WhatsAppImage(
    uri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val drawableId = remember(uri) {
        context.resources.getIdentifier(uri, "drawable", context.packageName)
    }

    if (drawableId != 0) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = "Media item",
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Media item",
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

// --- WhatsApp Custom Message Bubble with tail paths and reactions ---
@Composable
fun WhatsAppMessageBubble(
    message: Message,
    onTap: () -> Unit
) {
    if (message.isDateHeader) {
        // Render Center-aligned Date Banner (e.g. Today, June 13, 2026)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE1F3FC), shape = RoundedCornerShape(8.dp)) // Nice soft blue date badge from WhatsApp
                    .clickable { onTap() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .shadow(0.5.dp, shape = RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF50626E)
                )
            }
        }
        return
    }

    val bubbleShape = if (message.isOutgoing) OutgoingBubbleShape() else IncomingBubbleShape()
    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isOutgoing) WaLightGreen else WaIncomingWhite

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
        ) {
            // Forward/Share arrow button next to outgoing media bubbles
            if (message.isOutgoing && !message.mediaUris.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.05f), shape = CircleShape)
                        .clickable { /* mock share action */ }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Forward",
                        tint = WaTextGrey,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start) {
                // Message bubble wrapper with customizable tail
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(bubbleColor, shape = bubbleShape)
                        .clickable { onTap() }
                        .padding(
                            start = if (message.isOutgoing) 8.dp else 14.dp, 
                            end = if (message.isOutgoing) 14.dp else 8.dp, 
                            top = 4.dp, 
                            bottom = 4.dp
                        )
                ) {
                    if (message.isDeleted) {
                        // Deleted Message bubble representation
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Deleted",
                                tint = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (message.text.isNotBlank()) message.text else "You deleted this message",
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.Black.copy(alpha = 0.45f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.time,
                                fontSize = 10.sp,
                                color = WaTextGrey,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else if (!message.mediaUris.isNullOrBlank()) {
                        // Media Grid layout
                        val uris = remember(message.mediaUris) {
                            message.mediaUris.split(",").filter { it.isNotEmpty() }
                        }
                        Box(
                            modifier = Modifier
                                .width(250.dp)
                                .padding(2.dp)
                        ) {
                            Column {
                                when {
                                    uris.size == 1 -> {
                                        WhatsAppImage(
                                            uri = uris[0],
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                    uris.size == 2 -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            WhatsAppImage(
                                                uri = uris[0],
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            WhatsAppImage(
                                                uri = uris[1],
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                        }
                                    }
                                    uris.size == 3 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            WhatsAppImage(
                                                uri = uris[0],
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                WhatsAppImage(
                                                    uri = uris[1],
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(90.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                                WhatsAppImage(
                                                    uri = uris[2],
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(90.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        // 4 or more images -> 2x2 grid
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                WhatsAppImage(
                                                    uri = uris[0],
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(110.dp)
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                                                )
                                                WhatsAppImage(
                                                    uri = uris[1],
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(110.dp)
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                WhatsAppImage(
                                                    uri = uris[2],
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(110.dp)
                                                        .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(110.dp)
                                                ) {
                                                    WhatsAppImage(
                                                        uri = uris[3],
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                                    )
                                                    if (uris.size > 4) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(bottomEnd = 6.dp))
                                                                .padding(4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "+ ${uris.size - 4}",
                                                                color = Color.White,
                                                                fontSize = 24.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!message.text.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.text,
                                        fontSize = 15.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Legible Overlay Pill for Timestamp + Checkmarks inside media message
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = message.time,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                    if (message.isOutgoing && message.status != "FAILED") {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        when (message.status) {
                                            "PENDING" -> Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Pending", tint = Color.White, modifier = Modifier.size(10.dp))
                                            "SENT" -> Icon(imageVector = Icons.Default.Check, contentDescription = "Sent", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(10.dp))
                                            "DELIVERED" -> Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Delivered", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(10.dp))
                                            "READ" -> Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Read", tint = WaBlueTick, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Text Bubble
                        Column {
                            // Text body
                            Text(
                                text = message.text,
                                fontSize = 15.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            // Time & ticks row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = message.time,
                                    fontSize = 10.sp,
                                    color = WaTextGrey,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                if (message.isOutgoing && message.status != "FAILED") {
                                    when (message.status) {
                                        "PENDING" -> Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Pending", tint = WaTextGrey, modifier = Modifier.size(11.dp))
                                        "SENT" -> Icon(imageVector = Icons.Default.Check, contentDescription = "Sent", tint = WaTextGrey, modifier = Modifier.size(11.dp))
                                        "DELIVERED" -> Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Delivered", tint = WaTextGrey, modifier = Modifier.size(11.dp))
                                        "READ" -> Icon(imageVector = Icons.Default.DoneAll, contentDescription = "Read", tint = WaBlueTick, modifier = Modifier.size(11.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Reactions box beautifully overlapping bubble
                if (!message.reactions.isNullOrBlank() && !message.isDeleted) {
                    val reactionsList = message.reactions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    Row(
                        modifier = Modifier
                            .offset(y = (-4).dp, x = if (message.isOutgoing) (-12).dp else 12.dp)
                            .shadow(1.dp, shape = RoundedCornerShape(10.dp))
                            .background(Color.White, shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        reactionsList.forEach { emoji ->
                            Text(text = emoji, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 1.dp))
                        }
                    }
                }
            }

            // Exclamation Red Badge for FAILED messages (Exactly matching user's image)
            if (message.isOutgoing && message.status == "FAILED") {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(WaFailedRed, shape = CircleShape)
                        .clickable { onTap() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// --- Utilities for saving to external Storage / MediaStore ---
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$title.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WhatsAppMocks")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            e.printStackTrace()
        }
    }
    return null
}

private fun shareImageIntent(context: Context, uri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Mock Chat Screenshot"))
}
