package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Global reactive flag for instant Dark / Light theme switching across all screens
var isDarkThemeActive by mutableStateOf(true)

// Global reactive flag for instant Indonesian / English language switching across all screens & dialogs
var isEnglishLanguageActive by mutableStateOf(false)

/**
 * Reactive bilingual string selector. Automatically triggers Compose recomposition when language changes.
 */
fun tr(id: String, en: String): String = if (isEnglishLanguageActive) en else id

/**
 * Translates canonical Indonesian route day names ("Semua", "Senin".."Minggu") to English for UI display
 * while preserving the underlying database route_day keys.
 */
fun trDay(day: String, short: Boolean = false): String {
    if (!isEnglishLanguageActive) {
        return if (short && day != "Semua") day.take(3) else day
    }
    return when (day.trim().lowercase()) {
        "semua", "all" -> "All"
        "senin" -> if (short) "Mon" else "Monday"
        "selasa" -> if (short) "Tue" else "Tuesday"
        "rabu" -> if (short) "Wed" else "Wednesday"
        "kamis" -> if (short) "Thu" else "Thursday"
        "jumat", "jum'at" -> if (short) "Fri" else "Friday"
        "sabtu" -> if (short) "Sat" else "Saturday"
        "minggu" -> if (short) "Sun" else "Sunday"
        else -> if (short) day.take(3) else day
    }
}

// Supabase-inspired Color Palette
// Signature Emerald
val SupabaseGreen: Color
    get() = if (isDarkThemeActive) Color(0xFF3ECF8E) else Color(0xFF10B981)
val SupabaseGreenDark = Color(0xFF24B47E)
val SupabaseGreenLight = Color(0xFF6EE7B7)
val SupabaseGreenMuted = Color(0xFF0F3929)

// Light Theme Palette (Clean charcoal & slate on crisp white)
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF64748B)

// Dynamic Surfaces & Borders (adapts to Dark / Light theme automatically)
val CharcoalBg: Color
    get() = if (isDarkThemeActive) Color(0xFF121316) else LightBg
val CharcoalSurface: Color
    get() = if (isDarkThemeActive) Color(0xFF181A1F) else LightSurface
val CharcoalSurfaceElevated: Color
    get() = if (isDarkThemeActive) Color(0xFF22252B) else LightSurfaceElevated
val CharcoalBorder: Color
    get() = if (isDarkThemeActive) Color(0xFF2D3139) else LightBorder
val CharcoalBorderSubtle: Color
    get() = if (isDarkThemeActive) Color(0xFF23262D) else Color(0xFFCBD5E1)

// Dynamic Text & Monochromes
val TextPrimaryDark: Color
    get() = if (isDarkThemeActive) Color(0xFFF4F4F5) else TextPrimaryLight
val TextSecondaryDark: Color
    get() = if (isDarkThemeActive) Color(0xFFA1A1AA) else TextSecondaryLight
val TextMutedDark: Color
    get() = if (isDarkThemeActive) Color(0xFF71717A) else TextMutedLight

// Status Accents
val AmberWarning = Color(0xFFF59E0B)
val AmberWarningMuted = Color(0xFF3B2506)
val RoseError = Color(0xFFF43F5E)
val RoseErrorMuted = Color(0xFF4C0519)
val BlueInfo = Color(0xFF38BDF8)
