package com.motavation.dashboard.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.motavation.dashboard.ui.theme.*

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationScreen() {
    val navController = rememberNavController()
    val navItems = remember {
        listOf(
            NavItem("Home", Icons.Default.Home, "home"),
            NavItem("Rounds", Icons.Default.SportsMartialArts, "rounds"),
            NavItem("Competition", Icons.Default.EmojiEvents, "competition"),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
                )
            )
            .padding(start = 48.dp, end = 48.dp, top = 27.dp, bottom = 27.dp)
    ) {
        NavRail(
            items = navItems,
            currentRoute = currentRoute,
            onItemSelected = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        Spacer(modifier = Modifier.width(32.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            AppNavigation(navController = navController)
        }
    }
}

@Composable
private fun NavRail(
    items: List<NavItem>,
    currentRoute: String?,
    onItemSelected: (String) -> Unit
) {
    val homeFocusRequester = remember { FocusRequester() }
    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        homeFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .animateContentSize(animationSpec = tween(200))
            .onFocusChanged { isExpanded = it.hasFocus },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items.forEachIndexed { index, item ->
            NavRailItem(
                item = item,
                isSelected = currentRoute == item.route,
                isExpanded = isExpanded,
                onClick = { onItemSelected(item.route) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(homeFocusRequester)
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
