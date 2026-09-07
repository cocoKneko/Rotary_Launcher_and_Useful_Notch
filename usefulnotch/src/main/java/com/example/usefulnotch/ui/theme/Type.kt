package com.example.usefulnotch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.usefulnotch.R

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
    // Add: Font(R.font.jetbrains_mono_bold, FontWeight.Bold) if you downloaded the bold weight
)

// Labels: small, wide-tracked, all-caps convention (apply .uppercase() at call sites)
val LabelStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    letterSpacing = 1.5.sp
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelSmall = LabelStyle
)