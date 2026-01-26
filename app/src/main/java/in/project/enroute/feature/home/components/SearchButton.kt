package `in`.project.enroute.feature.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Search button that adapts its shape based on floor slider visibility.
 * 
 * When floor slider is visible: tall rounded rectangle matching slider height (~76dp)
 * When floor slider is hidden: compact circle (48dp)
 * 
 * @param isSliderVisible Whether the floor slider is currently visible
 * @param modifier Modifier for the component
 */
@Composable
fun SearchButton(
    isSliderVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // Animate width
    val buttonWidth by animateDpAsState(
        targetValue = if (isSliderVisible) 52.dp else 48.dp,
        animationSpec = tween(durationMillis = 300),
        label = "search_button_width"
    )
    
    // Animate height - match FloorSlider height when visible
    // FloorSlider: 10dp padding + ~20dp text + 4dp spacing + 32dp slider + 10dp padding = ~76dp
    val buttonHeight by animateDpAsState(
        targetValue = if (isSliderVisible) 80.dp else 48.dp,
        animationSpec = tween(durationMillis = 300),
        label = "search_button_height"
    )
    
    // Animate corner radius based on slider visibility - matches FloorSlider's 28dp radius
    val cornerRadius by animateDpAsState(
        targetValue = if (isSliderVisible) 26.dp else 24.dp,
        animationSpec = tween(durationMillis = 300),
        label = "search_button_corner"
    )
    
    Box(
        modifier = modifier
            .size(width = buttonWidth, height = buttonHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                // TODO: Implement search functionality
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
