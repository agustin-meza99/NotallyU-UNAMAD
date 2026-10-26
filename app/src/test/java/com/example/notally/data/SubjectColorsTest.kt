package com.example.notally.data

import androidx.compose.ui.graphics.Color
import com.example.notally.R
import com.example.notally.ui.theme.NotallySubjectSwatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectColorsTest {

    @Test
    fun `hay seis acentos opacos y distintos`() {
        val colors = SubjectColors.all

        assertEquals(6, colors.size)
        assertEquals(6, colors.distinct().size)
        assertTrue(colors.all { (it ushr 24) == 0xFFL })
    }

    @Test
    fun `los valores respetan la tabla del documento`() {
        assertEquals(
            listOf(0xFF3A5A9EL, 0xFF2F6A4EL, 0xFF99482EL, 0xFF5F7300L, 0xFF7A5F87L, 0xFF1D5F6AL),
            SubjectColors.all,
        )
    }

    @Test
    fun `los swatches conservan orden nombres y variantes del documento`() {
        val swatches = NotallySubjectSwatches

        assertEquals(SubjectColors.all, swatches.map { it.argb })
        assertEquals(
            listOf(
                R.string.subject_color_indigo,
                R.string.subject_color_forest,
                R.string.subject_color_terracotta,
                R.string.subject_color_lime,
                R.string.subject_color_lavender,
                R.string.subject_color_petrol,
            ),
            swatches.map { it.nameRes },
        )
        assertEquals(
            listOf(
                Color(0xFF3A5A9E),
                Color(0xFF2F6A4E),
                Color(0xFF99482E),
                Color(0xFF5F7300),
                Color(0xFF7A5F87),
                Color(0xFF1D5F6A),
            ),
            swatches.map { it.light },
        )
        assertEquals(
            listOf(
                Color(0xFFAFC3EE),
                Color(0xFF9FC9B2),
                Color(0xFFE7AE97),
                Color(0xFFD8E5A0),
                Color(0xFFCAA8B8),
                Color(0xFF96CDD6),
            ),
            swatches.map { it.dark },
        )
        assertEquals(
            listOf(
                Color(0xFFE9ECF5),
                Color(0xFFE6EFE8),
                Color(0xFFF4E8E2),
                Color(0xFFF3F5E0),
                Color(0xFFF3EDF5),
                Color(0xFFE3EEEE),
            ),
            swatches.map { it.lightContainer },
        )
        assertEquals(
            listOf(
                Color(0xFF222B3F),
                Color(0xFF1D2C24),
                Color(0xFF33231C),
                Color(0xFF2A2B16),
                Color(0xFF2B2233),
                Color(0xFF182B2E),
            ),
            swatches.map { it.darkContainer },
        )
        swatches.forEach { swatch ->
            assertEquals(swatch.light, swatch.accent(isDark = false))
            assertEquals(swatch.dark, swatch.accent(isDark = true))
            assertEquals(swatch.lightContainer, swatch.container(isDark = false))
            assertEquals(swatch.darkContainer, swatch.container(isDark = true))
            assertEquals(swatch.light, swatch.onContainer(isDark = false))
            assertEquals(swatch.dark, swatch.onContainer(isDark = true))
        }
    }
}
