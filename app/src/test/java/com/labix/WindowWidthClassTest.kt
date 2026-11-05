package com.labix

import androidx.compose.ui.unit.dp
import com.labix.navirom.ui.layout.WindowWidthClass
import com.labix.navirom.ui.layout.windowWidthClass
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowWidthClassTest {
    @Test
    fun classifiesAvailableWidthAtCompactAndExpandedBoundaries() {
        assertEquals(WindowWidthClass.COMPACT, windowWidthClass(599.dp))
        assertEquals(WindowWidthClass.MEDIUM, windowWidthClass(600.dp))
        assertEquals(WindowWidthClass.MEDIUM, windowWidthClass(839.dp))
        assertEquals(WindowWidthClass.EXPANDED, windowWidthClass(840.dp))
        assertEquals(WindowWidthClass.EXPANDED, windowWidthClass(1600.dp))
    }
}
