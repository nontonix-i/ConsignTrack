package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AppMenuDrawerSheet
import com.example.ui.screens.BackupRestoreDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinancialScreen
import com.example.ui.screens.FloatingAiChatDialog
import com.example.ui.screens.FloatingAiCsButton
import com.example.ui.screens.GraphicsAnalysisScreen
import com.example.ui.screens.ProductCatalogScreen
import com.example.ui.screens.ReconciliationScreen
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ConsignTrackTheme
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.ProductViewModel
import com.example.ui.viewmodel.ReconciliationViewModel
import kotlinx.coroutines.launch

enum class MainNavTab(
    val titleId: String,
    val titleEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ROUTES("Rute", "Routes", Icons.Filled.Store, Icons.Outlined.Store),
    PRODUCTS("Produk", "Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    FINANCE("Buku Kas", "Cashbook", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    MENU("Menu", "Menu", Icons.Filled.Menu, Icons.Outlined.Menu)
}

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val reconciliationViewModel: ReconciliationViewModel by viewModels()
    private val financialViewModel: FinancialViewModel by viewModels()
    private val productViewModel: ProductViewModel by viewModels()
    private val analyticsViewModel: AnalyticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (analyticsState.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> systemDark
            }

            ConsignTrackTheme(darkTheme = useDarkTheme) {
                MainApp(
                    dashboardViewModel = dashboardViewModel,
                    reconciliationViewModel = reconciliationViewModel,
                    financialViewModel = financialViewModel,
                    productViewModel = productViewModel,
                    analyticsViewModel = analyticsViewModel
                )
            }
        }
    }
}

@Composable
fun MainApp(
    dashboardViewModel: DashboardViewModel,
    reconciliationViewModel: ReconciliationViewModel,
    financialViewModel: FinancialViewModel,
    productViewModel: ProductViewModel,
    analyticsViewModel: AnalyticsViewModel
) {
    val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val isEnglish = analyticsState.language == AppLanguage.EN

    var selectedTab by rememberSaveable { mutableStateOf(MainNavTab.ROUTES) }
    var showGraphicsAnalysis by rememberSaveable { mutableStateOf(false) }
    var activeReconciliationCustomerId by rememberSaveable { mutableLongStateOf(-1L) }
    var showAiChatModal by rememberSaveable { mutableStateOf(false) }
    var showBackupRestoreModal by rememberSaveable { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    if (drawerState.isOpen) {
        BackHandler {
            coroutineScope.launch { drawerState.close() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = activeReconciliationCustomerId <= 0L,
            drawerContent = {
                AppMenuDrawerSheet(
                    viewModel = analyticsViewModel,
                    onNavigateTab = { tabKey ->
                        when (tabKey) {
                            "GRAPHICS" -> {
                                showGraphicsAnalysis = true
                            }
                            "ROUTES" -> {
                                showGraphicsAnalysis = false
                                selectedTab = MainNavTab.ROUTES
                            }
                            "PRODUCTS" -> {
                                showGraphicsAnalysis = false
                                selectedTab = MainNavTab.PRODUCTS
                            }
                            "FINANCE" -> {
                                showGraphicsAnalysis = false
                                selectedTab = MainNavTab.FINANCE
                            }
                        }
                    },
                    onOpenAiChat = {
                        showAiChatModal = true
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = activeReconciliationCustomerId,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScreenTransition"
                ) { customerId ->
                    if (customerId > 0L) {
                        ReconciliationScreen(
                            customerId = customerId,
                            viewModel = reconciliationViewModel,
                            onNavigateBack = { activeReconciliationCustomerId = -1L }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = CharcoalBg,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier
                                        .border(1.dp, CharcoalBorder)
                                        .navigationBarsPadding(),
                                    containerColor = CharcoalSurface,
                                    contentColor = SupabaseGreen
                                ) {
                                    MainNavTab.entries.forEach { tab ->
                                        val isDrawerOpen = drawerState.isOpen
                                        val isSelected = if (tab == MainNavTab.MENU) {
                                            isDrawerOpen || showGraphicsAnalysis
                                        } else {
                                            !isDrawerOpen && !showGraphicsAnalysis && selectedTab == tab
                                        }
                                        val tabTitle = if (isEnglish) tab.titleEn else tab.titleId

                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (tab == MainNavTab.MENU) {
                                                    coroutineScope.launch {
                                                        if (drawerState.isOpen) drawerState.close()
                                                        else drawerState.open()
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        if (drawerState.isOpen) drawerState.close()
                                                    }
                                                    showGraphicsAnalysis = false
                                                    selectedTab = tab
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tabTitle,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tabTitle,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFF042114),
                                                selectedTextColor = SupabaseGreen,
                                                indicatorColor = SupabaseGreen,
                                                unselectedIconColor = TextSecondaryDark,
                                                unselectedTextColor = TextMutedDark
                                            )
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                if (showGraphicsAnalysis) {
                                    GraphicsAnalysisScreen(
                                        viewModel = analyticsViewModel,
                                        onNavigateBack = { showGraphicsAnalysis = false },
                                        onOpenDrawer = {
                                            coroutineScope.launch { drawerState.open() }
                                        },
                                        onOpenAiChat = {
                                            showAiChatModal = true
                                        }
                                    )
                                } else {
                                    when (selectedTab) {
                                        MainNavTab.ROUTES -> {
                                            DashboardScreen(
                                                viewModel = dashboardViewModel,
                                                onStartVisit = { targetCustId ->
                                                    activeReconciliationCustomerId = targetCustId
                                                },
                                                onOpenBackupRestore = {
                                                    showBackupRestoreModal = true
                                                }
                                            )
                                        }
                                        MainNavTab.PRODUCTS -> {
                                            ProductCatalogScreen(
                                                viewModel = productViewModel
                                            )
                                        }
                                        MainNavTab.FINANCE -> {
                                            FinancialScreen(
                                                viewModel = financialViewModel
                                            )
                                        }
                                        MainNavTab.MENU -> {
                                            DashboardScreen(
                                                viewModel = dashboardViewModel,
                                                onStartVisit = { targetCustId ->
                                                    activeReconciliationCustomerId = targetCustId
                                                },
                                                onOpenBackupRestore = {
                                                    showBackupRestoreModal = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating AI CS Chat Button at Bottom-Right (above the bottom nav & screen FAB)
                if (analyticsState.showFloatingAiChat && activeReconciliationCustomerId <= 0L) {
                    FloatingAiCsButton(
                        isEnglish = isEnglish,
                        onClick = { showAiChatModal = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 16.dp, bottom = 148.dp)
                    )
                }
            }
        }

        // Floating CS AI Chat Modal Window
        if (showAiChatModal) {
            FloatingAiChatDialog(
                viewModel = analyticsViewModel,
                onDismiss = { showAiChatModal = false }
            )
        }

        // Backup & Restore (.ZIP + Photos) Modal Dialog
        if (showBackupRestoreModal) {
            BackupRestoreDialog(
                viewModel = analyticsViewModel,
                onDismiss = { showBackupRestoreModal = false }
            )
        }
    }
}
