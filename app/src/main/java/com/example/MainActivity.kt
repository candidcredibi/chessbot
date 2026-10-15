package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BoardAnalysisScreen
import com.example.ui.screens.ChessComLiveScreen
import com.example.ui.screens.GameReviewScreen
import com.example.ui.screens.TacticsTrainerScreen
import com.example.ui.theme.ChessBlueGM
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessMasterTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ChessViewModel

enum class MainNavTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    LIVE(R.string.nav_live, Icons.Filled.Language, Icons.Outlined.Language, "nav_tab_live"),
    BOARD(R.string.nav_board, Icons.Filled.GridOn, Icons.Outlined.GridOn, "nav_tab_board"),
    GAMES(R.string.nav_games, Icons.Filled.History, Icons.Outlined.History, "nav_tab_games"),
    TACTICS(R.string.nav_tactics, Icons.Filled.School, Icons.Outlined.School, "nav_tab_tactics")
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessMasterTheme {
                val viewModel: ChessViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                var currentTab by remember { mutableStateOf(MainNavTab.LIVE) }
                var showHelpDialog by remember { mutableStateOf(false) }

                // BackHandler to return to LIVE tab when in other tabs
                BackHandler(enabled = currentTab != MainNavTab.LIVE) {
                    currentTab = MainNavTab.LIVE
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "ChessMaster",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showHelpDialog = true },
                                    modifier = Modifier.testTag("help_dialog_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "О приложении",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = DarkSurface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_navigation_bar"),
                            containerColor = DarkSurface,
                            tonalElevation = 8.dp
                        ) {
                            MainNavTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.titleRes),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.titleRes),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = ChessGreenPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = ChessGreenPrimary
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(DarkBackground)
                    ) {
                        when (currentTab) {
                            MainNavTab.LIVE -> {
                                ChessComLiveScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    onNavigateToSandbox = { currentTab = MainNavTab.BOARD }
                                )
                            }
                            MainNavTab.BOARD -> {
                                BoardAnalysisScreen(
                                    viewModel = viewModel,
                                    uiState = uiState
                                )
                            }
                            MainNavTab.GAMES -> {
                                GameReviewScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    onGameSelected = { currentTab = MainNavTab.BOARD }
                                )
                            }
                            MainNavTab.TACTICS -> {
                                TacticsTrainerScreen(
                                    viewModel = viewModel,
                                    uiState = uiState
                                )
                            }
                        }
                    }

                    if (showHelpDialog) {
                        AlertDialog(
                            onDismissRequest = { showHelpDialog = false },
                            title = {
                                Text("Как пользоваться ChessMaster", fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Text(
                                    "1. Во вкладке 'Chess.com Live' играйте против ботов или игроков на Chess.com.\n\n" +
                                    "2. Приложение автоматически отслеживает расстановку фигур и отображает лучший ход зеленой стрелкой, а также дает совет гроссмейстера.\n\n" +
                                    "3. Если позиция изменилась, нажмите иконку синхронизации 🔄.\n\n" +
                                    "4. Во вкладке 'Анализ' тренируйтесь на интерактивной доске со стрелками и оценкой Stockfish.\n\n" +
                                    "5. Во вкладке 'Архив партий' разбирайте свои прошлые игры по никнейму.",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showHelpDialog = false }) {
                                    Text("Понятно", color = ChessGreenPrimary, fontWeight = FontWeight.Bold)
                                }
                            },
                            containerColor = DarkSurface,
                            titleContentColor = TextPrimary,
                            textContentColor = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
