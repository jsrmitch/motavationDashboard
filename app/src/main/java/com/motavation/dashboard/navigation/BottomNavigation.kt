package com.motavation.dashboard.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.motavation.dashboard.ui.theme.*
import kotlinx.coroutines.delay

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

// Horizontal space reserved on the left for the nav rail. Content is inset
// by this amount so it never sits underneath the rail when it's visible,
// and the layout stays stable when the rail shows/hides.
private val NAV_RAIL_RESERVED_WIDTH = 96.dp

private const val MENU_AUTO_HIDE_MS = 4000L
private const val HOME_AUTO_RETURN_MS = 300_000L
private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"

@Composable
fun BottomNavigationScreen() {
    val navController = rememberNavController()
    val navItems = remember {
        listOf(
            NavItem("Home", Icons.Default.Home, "home"),
            NavItem("Rounds", Icons.Default.SportsMartialArts, "rounds"),
            NavItem("Competition", Icons.Default.EmojiEvents, "competition"),
            NavItem("Settings", Icons.Default.Settings, "settings"),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var menuVisible by remember { mutableStateOf(true) }
    val interactionTickState = remember { mutableIntStateOf(0) }
    var interactionTick by interactionTickState
    val rootFocusRequester = remember { FocusRequester() }
    val navRailFocusRequester = remember { FocusRequester() }
    val idleController = remember { IdleController() }

    // Stable lambda so providing it through CompositionLocalProvider does
    // not invalidate descendants on every recomposition of this shell.
    val reportInteraction = remember(interactionTickState) {
        { interactionTickState.intValue += 1 }
    }

    // Auto-hide menu after inactivity; resets whenever interactionTick changes.
    // Any reported interaction (D-pad key, IME keystroke in Settings, mute
    // toggle, …) also re-shows the menu — otherwise typing on the on-screen
    // keyboard, which doesn't surface as hardware key events to our preview
    // handler, would let the menu hide mid-type and stay hidden.
    LaunchedEffect(interactionTick) {
        menuVisible = true
        delay(MENU_AUTO_HIDE_MS)
        menuVisible = false
    }

    // When menu hides, pull focus to the root so we keep receiving key events.
    // Exception: on the Settings screen we must leave focus on the TextField
    // (otherwise the soft keyboard dismisses and typing is interrupted).
    LaunchedEffect(menuVisible, currentRoute) {
        if (!menuVisible && currentRoute != SETTINGS_ROUTE) {
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    // When a screen becomes busy (e.g. the rounds timer just started), the
    // focused action button (START) disappears and Compose drops focus into
    // the void — after which no key event reaches our preview handler and
    // arrow keys appear dead until the user presses Enter (which causes
    // Compose to restore focus as a side-effect). Proactively collapse the
    // menu and yank focus back to the root so the very next arrow press is
    // intercepted and re-opens the nav rail.
    LaunchedEffect(idleController.busy) {
        if (idleController.busy) {
            menuVisible = false
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    // Auto-return to Home after prolonged inactivity on any non-home screen,
    // unless the current screen reports itself as busy (e.g. timer running).
    LaunchedEffect(currentRoute, idleController.busy, interactionTick) {
        if (currentRoute != null && currentRoute != HOME_ROUTE && !idleController.busy) {
            delay(HOME_AUTO_RETURN_MS)
            navController.navigate(HOME_ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
                )
            )
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val wasHidden = !menuVisible
                    menuVisible = true
                    interactionTick++
                    // On Settings we must never steal focus away from the
                    // TextField or swallow keystrokes — just reveal the menu
                    // and let the key reach the focused field.
                    if (currentRoute == SETTINGS_ROUTE) {
                        return@onPreviewKeyEvent false
                    }
                    if (wasHidden) {
                        // Reveal menu and move focus into it; consume the key
                        // so it doesn't also trigger navigation on first press.
                        runCatching { navRailFocusRequester.requestFocus() }
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .padding(start = 48.dp, end = 48.dp, top = 27.dp, bottom = 27.dp)
    ) {
        // Main content always occupies the full area so showing/hiding the
        // nav rail never reflows or resizes anything on screen. We reserve
        // a fixed left inset equal to the nav rail's footprint so content
        // never sits beneath the rail when it's visible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = NAV_RAIL_RESERVED_WIDTH)
        ) {
            CompositionLocalProvider(
                LocalIdleController provides idleController,
                LocalReportInteraction provides reportInteraction
            ) {
                AppNavigation(navController = navController)
            }
        }

        // Nav rail floats over the content on the left edge.
        AnimatedVisibility(
            visible = menuVisible,
            enter = slideInHorizontally(animationSpec = tween(200)) { -it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(animationSpec = tween(200)) { -it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            NavRail(
                items = navItems,
                currentRoute = currentRoute,
                focusRequester = navRailFocusRequester,
                // On Settings the user may be interacting with the banner
                // field or mute toggle — do NOT let the nav rail grab
                // focus just because it re-appeared after auto-hiding.
                autoFocusOnAppear = currentRoute != SETTINGS_ROUTE,
                onItemSelected = { route ->
                    interactionTick++
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun NavRail(
    items: List<NavItem>,
    currentRoute: String?,
    focusRequester: FocusRequester,
    autoFocusOnAppear: Boolean,
    onItemSelected: (String) -> Unit
) {
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    LaunchedEffect(Unit) {
        if (autoFocusOnAppear) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items.forEachIndexed { index, item ->
            NavRailItem(
                item = item,
                isSelected = currentRoute == item.route,
                isExpanded = true,
                onClick = { onItemSelected(item.route) },
                modifier = if (index == selectedIndex) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )

            if (index < items.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(150),
        label = "navItemScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> AccentCyan.copy(alpha = 0.2f)
            isSelected -> AccentCyan.copy(alpha = 0.1f)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "navItemBg"
    )

    val contentColor = when {
        isFocused -> AccentCyan
        isSelected -> AccentCyan
        else -> TextTertiary
    }

    Column(
        modifier = modifier
            .scale(scale)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        AnimatedVisibility(visible = isExpanded) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}
