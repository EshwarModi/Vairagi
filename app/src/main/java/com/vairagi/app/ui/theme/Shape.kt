package com.vairagi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val HeroCardShape = RoundedCornerShape(28.dp)
val StandardCardShape = RoundedCornerShape(20.dp)
val ComponentShape = RoundedCornerShape(16.dp)

val VairagiShapes = Shapes(
    small = ComponentShape,
    medium = StandardCardShape,
    large = HeroCardShape,
    extraLarge = RoundedCornerShape(32.dp)
)
