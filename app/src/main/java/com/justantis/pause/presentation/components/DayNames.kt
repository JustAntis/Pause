package com.justantis.pause.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.justantis.pause.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Weekday names in the language of the app texts (not the device), e.g. "Mon" / "pon.".
 */
@Composable
fun dayName(day: DayOfWeek, style: TextStyle = TextStyle.SHORT): String =
    day.getDisplayName(style, appLocale())

@Composable
fun appLocale(): Locale = Locale.forLanguageTag(stringResource(R.string.app_locale_tag))
