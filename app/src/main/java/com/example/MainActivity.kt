package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.GeminiRoomScannerService
import com.example.data.model.UiState
import com.example.data.repository.FirestoreOptiCleanDataSource
import com.example.data.repository.LocalOptiCleanDataSource
import com.example.data.repository.OptiCleanRepository
import com.example.ui.OptiCleanTab
import com.example.ui.OptiCleanViewModel
import com.example.ui.components.ShareAppDialog
import com.example.ui.screens.ConsumablesInventoryScreen
import com.example.ui.screens.HousekeeperTasksScreen
import com.example.ui.screens.ManagerDashboardScreen
import com.example.ui.screens.SupervisorInspectorScreen
import com.example.ui.theme.AlertRed500
import com.example.ui.theme.OptiCleanTheme
import com.example.ui.theme.Sky400
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal400
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal500
import com.example.ui.theme.Teal600
import com.example.ui.theme.WarningAmber600
import com.example.ui.util.ShareHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { com.google.firebase.FirebaseApp.initializeApp(this) }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.LTGRAY
            )
        )
        setContent {
            OptiCleanTheme {
                OptiCleanRootNavigation()
            }
        }
    }
}

@Composable
fun OptiCleanRootNavigation() {
    val context = LocalContext.current
    val existingUserId = remember(context) {
        runCatching {
            com.google.firebase.FirebaseApp.initializeApp(context)
            Firebase.auth.currentUser?.uid
        }.getOrNull()
    }

    OptiCleanWorkspaceShell(currentUserId = existingUserId)
}

@Composable
fun OptiCleanWorkspaceShell(currentUserId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showShareDialog by remember { mutableStateOf(false) }

    val viewModel: OptiCleanViewModel = viewModel(
        key = currentUserId ?: "direct_local_workspace",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource = if (!currentUserId.isNullOrBlank()) {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    FirestoreOptiCleanDataSource(OptiCleanRepository(db), currentUserId)
                } else {
                    LocalOptiCleanDataSource(app)
                }
                OptiCleanViewModel(
                    dataSource = dataSource,
                    scannerService = GeminiRoomScannerService()
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val roomTasksState by viewModel.roomTasksState.collectAsStateWithLifecycle()
    val inspectionsState by viewModel.inspectionsState.collectAsStateWithLifecycle()
    val consumablesState by viewModel.consumablesState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedInventoryCategory by viewModel.selectedInventoryCategory.collectAsStateWithLifecycle()

    val rooms = (roomTasksState as? UiState.Success)?.data.orEmpty()
    val inspections = (inspectionsState as? UiState.Success)?.data.orEmpty()
    val consumables = (consumablesState as? UiState.Success)?.data.orEmpty()

    val urgentTasksCount = rooms.count {
        it.status != "COMPLETED" && (it.alertActive || it.priority == "HIGH" || it.status == "NEEDS_FIX")
    }
    val lowStockCount = consumables.count { it.isLowStock }

    BackHandler(enabled = selectedTab != OptiCleanTab.SUPERVISOR) {
        viewModel.selectTab(OptiCleanTab.SUPERVISOR)
    }

    LaunchedEffect(scanState.snackbarMessage) {
        val msg = scanState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Slate50,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                OptiCleanTopBar(
                    lowStockCount = lowStockCount,
                    onLowStockPillClicked = { viewModel.selectTab(OptiCleanTab.INVENTORY) },
                    onShareWorkspaceClicked = { showShareDialog = true }
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    OptiCleanBottomBar(
                        selectedTab = selectedTab,
                        urgentTasksCount = urgentTasksCount,
                        lowStockCount = lowStockCount,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    OptiCleanNavigationRail(
                        selectedTab = selectedTab,
                        urgentTasksCount = urgentTasksCount,
                        lowStockCount = lowStockCount,
                        onSelectTab = viewModel::selectTab
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 680.dp)) {
                        when (selectedTab) {
                            OptiCleanTab.SUPERVISOR -> {
                                SupervisorInspectorScreen(
                                    scanState = scanState,
                                    rooms = rooms,
                                    onSelectRoom = viewModel::selectRoomForInspection,
                                    onChangeViewportMode = viewModel::setViewportMode,
                                    onRunAiScan = viewModel::runAiRoomScan,
                                    onPhotoPicked = viewModel::onPhotoSelectedAndScan,
                                    onSendHousekeeperAlert = viewModel::sendReportToHousekeeper,
                                    onApproveRoomReady = viewModel::approveSelectedRoomDirectly
                                )
                            }
                            OptiCleanTab.HOUSEKEEPER -> {
                                HousekeeperTasksScreen(
                                    roomTasksState = roomTasksState,
                                    selectedFilter = selectedFilter,
                                    latestAlertBanner = scanState.notificationSentBanner,
                                    onSelectFilter = viewModel::setTaskFilter,
                                    onInspectRoomAi = viewModel::selectRoomForInspection,
                                    onCompleteRoomTask = viewModel::completeRoomTask,
                                    onAddNewRoom = viewModel::addNewRoomTask
                                )
                            }
                            OptiCleanTab.INVENTORY -> {
                                ConsumablesInventoryScreen(
                                    consumablesState = consumablesState,
                                    selectedCategory = selectedInventoryCategory,
                                    onSelectCategory = viewModel::setInventoryCategoryFilter,
                                    onAdjustStock = viewModel::adjustConsumableStock,
                                    onRestockFull = viewModel::restockConsumableToCapacity,
                                    onNotifyManagementForItem = viewModel::sendManagementSupplyAlert,
                                    onNotifyManagementAllLow = viewModel::notifyManagementForAllLowStock,
                                    onAddNewConsumable = viewModel::addNewConsumable
                                )
                            }
                            OptiCleanTab.DASHBOARD -> {
                                ManagerDashboardScreen(
                                    roomTasksState = roomTasksState,
                                    inspectionsState = inspectionsState,
                                    consumablesState = consumablesState,
                                    onOpenInventoryTab = { viewModel.selectTab(OptiCleanTab.INVENTORY) },
                                    onShowCopiedSnackbar = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.snackbar_link_copied)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showShareDialog) {
            ShareAppDialog(
                appUrl = stringResource(R.string.shared_app_url),
                onDismiss = { showShareDialog = false },
                onShareAppLink = {
                    showShareDialog = false
                    ShareHelper.shareAppInvitation(context)
                },
                onCopyAppLink = {
                    if (ShareHelper.copyAppLinkToClipboard(context)) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.snackbar_link_copied)
                            )
                        }
                    }
                },
                onShareHotelSummary = {
                    showShareDialog = false
                    val reportBody = ShareHelper.buildWorkspaceOverviewText(
                        rooms = rooms,
                        consumables = consumables,
                        inspections = inspections
                    )
                    ShareHelper.shareTextReport(
                        context = context,
                        subject = "ملخص حالة الفندق — OptiClean AI",
                        body = reportBody
                    )
                }
            )
        }
    }
}

@Composable
private fun OptiCleanTopBar(
    lowStockCount: Int,
    onLowStockPillClicked: () -> Unit,
    onShareWorkspaceClicked: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "onlinePulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Teal500, Sky400))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OptiClean ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Teal400
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (lowStockCount > 0) {
                    Surface(
                        onClick = onLowStockPillClicked,
                        color = AlertRed500.copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .border(1.dp, AlertRed500.copy(alpha = 0.45f), CircleShape)
                            .testTag("top_low_stock_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .alpha(dotAlpha)
                                    .clip(CircleShape)
                                    .background(AlertRed500)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "نقص ($lowStockCount)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Surface(
                    onClick = onShareWorkspaceClicked,
                    color = Teal500.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .border(1.dp, Teal500.copy(alpha = 0.42f), CircleShape)
                        .testTag("top_share_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.top_share_cd),
                            tint = Teal400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.top_share_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = Teal400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate800)
                .padding(vertical = 0.5.dp)
        )
    }
}

@Composable
private fun OptiCleanBottomBar(
    selectedTab: OptiCleanTab,
    urgentTasksCount: Int,
    lowStockCount: Int,
    onSelectTab: (OptiCleanTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 6.dp,
        modifier = Modifier
            .border(1.dp, Slate200)
            .testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = selectedTab == OptiCleanTab.SUPERVISOR,
            onClick = { onSelectTab(OptiCleanTab.SUPERVISOR) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == OptiCleanTab.SUPERVISOR) {
                        Icons.Default.CenterFocusStrong
                    } else {
                        Icons.Outlined.CenterFocusWeak
                    },
                    contentDescription = stringResource(R.string.nav_supervisor)
                )
            },
            label = { Text(stringResource(R.string.nav_supervisor)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Teal600,
                selectedTextColor = Teal600,
                indicatorColor = Teal50,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_supervisor")
        )

        NavigationBarItem(
            selected = selectedTab == OptiCleanTab.HOUSEKEEPER,
            onClick = { onSelectTab(OptiCleanTab.HOUSEKEEPER) },
            icon = {
                BadgedBox(
                    badge = {
                        if (urgentTasksCount > 0) {
                            Badge(containerColor = AlertRed500, contentColor = Color.White) {
                                Text(urgentTasksCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == OptiCleanTab.HOUSEKEEPER) {
                            Icons.Default.Checklist
                        } else {
                            Icons.Outlined.Checklist
                        },
                        contentDescription = stringResource(R.string.nav_housekeeper)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_housekeeper)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Teal600,
                selectedTextColor = Teal600,
                indicatorColor = Teal50,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_housekeeper")
        )

        NavigationBarItem(
            selected = selectedTab == OptiCleanTab.INVENTORY,
            onClick = { onSelectTab(OptiCleanTab.INVENTORY) },
            icon = {
                BadgedBox(
                    badge = {
                        if (lowStockCount > 0) {
                            Badge(containerColor = AlertRed500, contentColor = Color.White) {
                                Text(lowStockCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == OptiCleanTab.INVENTORY) {
                            Icons.Default.Inventory2
                        } else {
                            Icons.Outlined.Inventory2
                        },
                        contentDescription = stringResource(R.string.nav_inventory)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_inventory)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Teal600,
                selectedTextColor = Teal600,
                indicatorColor = Teal50,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_inventory")
        )

        NavigationBarItem(
            selected = selectedTab == OptiCleanTab.DASHBOARD,
            onClick = { onSelectTab(OptiCleanTab.DASHBOARD) },
            icon = {
                BadgedBox(
                    badge = {
                        if (lowStockCount > 0) {
                            Badge(containerColor = WarningAmber600, contentColor = Color.White)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == OptiCleanTab.DASHBOARD) {
                            Icons.Default.PieChart
                        } else {
                            Icons.Outlined.PieChart
                        },
                        contentDescription = stringResource(R.string.nav_dashboard)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_dashboard)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Teal600,
                selectedTextColor = Teal600,
                indicatorColor = Teal50,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_dashboard")
        )
    }
}

@Composable
private fun OptiCleanNavigationRail(
    selectedTab: OptiCleanTab,
    urgentTasksCount: Int,
    lowStockCount: Int,
    onSelectTab: (OptiCleanTab) -> Unit
) {
    NavigationRail(
        containerColor = Color.White,
        modifier = Modifier.border(1.dp, Slate200)
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        NavigationRailItem(
            selected = selectedTab == OptiCleanTab.SUPERVISOR,
            onClick = { onSelectTab(OptiCleanTab.SUPERVISOR) },
            icon = {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = stringResource(R.string.nav_supervisor))
            },
            label = { Text(stringResource(R.string.nav_supervisor)) }
        )
        NavigationRailItem(
            selected = selectedTab == OptiCleanTab.HOUSEKEEPER,
            onClick = { onSelectTab(OptiCleanTab.HOUSEKEEPER) },
            icon = {
                BadgedBox(
                    badge = {
                        if (urgentTasksCount > 0) {
                            Badge(containerColor = AlertRed500) { Text(urgentTasksCount.toString()) }
                        }
                    }
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = stringResource(R.string.nav_housekeeper))
                }
            },
            label = { Text(stringResource(R.string.nav_housekeeper)) }
        )
        NavigationRailItem(
            selected = selectedTab == OptiCleanTab.INVENTORY,
            onClick = { onSelectTab(OptiCleanTab.INVENTORY) },
            icon = {
                BadgedBox(
                    badge = {
                        if (lowStockCount > 0) {
                            Badge(containerColor = AlertRed500) { Text(lowStockCount.toString()) }
                        }
                    }
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = stringResource(R.string.nav_inventory))
                }
            },
            label = { Text(stringResource(R.string.nav_inventory)) }
        )
        NavigationRailItem(
            selected = selectedTab == OptiCleanTab.DASHBOARD,
            onClick = { onSelectTab(OptiCleanTab.DASHBOARD) },
            icon = {
                Icon(Icons.Default.PieChart, contentDescription = stringResource(R.string.nav_dashboard))
            },
            label = { Text(stringResource(R.string.nav_dashboard)) }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
