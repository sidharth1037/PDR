package `in`.project.enroute.feature.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A pill-shaped button for setting the user's location as the PDR origin point.
 * 
 * @param isSliderVisible Whether the floor slider is currently visible (affects vertical position)
 * @param onClick Callback when the button is clicked
 * @param modifier Modifier for the button container
 */
@Composable
fun SetLocationButton(
    isSliderVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate top padding based on whether slider is visible
    // When slider is visible: position below it (~88dp for slider height + spacing)
    // When slider is hidden (search shown): position at top with minimal padding
    val topPadding by animateDpAsState(
        targetValue = if (isSliderVisible) 96.dp else 58.dp,
        animationSpec = tween(durationMillis = 300),
        label = "SetLocationButtonTopPadding"
    )
    
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
            .padding(top = topPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Set Location",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Set Location",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
