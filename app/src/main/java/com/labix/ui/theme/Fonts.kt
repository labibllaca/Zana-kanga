package com.labix.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.labix.R

@OptIn(ExperimentalTextApi::class)
private fun lexend(weight: Int, fontWeight: FontWeight): Font =
    Font(
        resId = R.font.lexend_variable,
        weight = fontWeight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight))
    )

/** Display / editorial font. */
@OptIn(ExperimentalTextApi::class)
val PoppinsFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/** UI / labels / headings in dense surfaces. */
@OptIn(ExperimentalTextApi::class)
val LexendFamily = FontFamily(
    lexend(400, FontWeight.Normal),
    lexend(500, FontWeight.Medium),
    lexend(600, FontWeight.SemiBold),
    lexend(700, FontWeight.Bold)
)

/** Body copy and readable metadata. */
val LatoFamily = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.lato_medium, FontWeight.Medium),
    Font(R.font.lato_bold, FontWeight.Bold)
)
