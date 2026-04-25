package com.example.einkaufsliste

import com.example.einkaufsliste.data.repository.mergeAmounts
import com.example.einkaufsliste.data.repository.mergeSources
import com.example.einkaufsliste.data.repository.normalizedKey
import org.junit.Test

import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun normalizedKey_collapsesWhitespaceAndCase() {
        assertEquals("passierte tomaten", "  Passierte   Tomaten  ".normalizedKey())
    }

    @Test
    fun mergeAmounts_keepsUsefulQuantityText() {
        assertEquals("500 g", mergeAmounts("500 g", ""))
        assertEquals("500 g", mergeAmounts("500 g", "500 g"))
        assertEquals("500 g + 200 g", mergeAmounts("500 g", "200 g"))
    }

    @Test
    fun mergeSources_deduplicatesRecipeNames() {
        assertEquals("Pasta, Chili", mergeSources("Pasta", "Pasta, Chili"))
    }
}
