package com.quantum_prof.phantalandwaittimes.ui.theme.main

import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AttractionSortingTest {

    private fun attraction(
        code: String,
        name: String,
        wait: Int = 0,
        status: String = "opened"
    ) = AttractionWaitTime(code = code, name = name, waitTimeMinutes = wait, status = status)

    private val taron = attraction("1", "Taron", wait = 60)
    private val blackMamba = attraction("2", "Black Mamba", wait = 30)
    private val chiapas = attraction("3", "Chiapas", wait = 10)
    private val closedRide = attraction("4", "Anubis", wait = 90, status = "closed")

    private val all = listOf(taron, blackMamba, chiapas, closedRide)

    @Test
    fun `name sort is ascending and case insensitive`() {
        val sorted = sortAttractions(all, SortType.NAME, SortDirection.ASCENDING, emptySet())

        assertEquals(listOf("Anubis", "Black Mamba", "Chiapas", "Taron"), sorted.map { it.name })
    }

    @Test
    fun `name sort reverses with the direction`() {
        val sorted = sortAttractions(all, SortType.NAME, SortDirection.DESCENDING, emptySet())

        assertEquals(listOf("Taron", "Chiapas", "Black Mamba", "Anubis"), sorted.map { it.name })
    }

    @Test
    fun `closed attractions sort last when ordering by wait time ascending`() {
        val sorted = sortAttractions(all, SortType.WAIT_TIME, SortDirection.ASCENDING, emptySet())

        assertEquals(listOf("Chiapas", "Black Mamba", "Taron", "Anubis"), sorted.map { it.name })
    }

    @Test
    fun `favourites stay on top in ascending order`() {
        val sorted = sortAttractions(all, SortType.NAME, SortDirection.ASCENDING, setOf("1"))

        assertEquals("Taron", sorted.first().name)
    }

    @Test
    fun `favourites also stay on top when the direction is reversed`() {
        val sorted = sortAttractions(all, SortType.NAME, SortDirection.DESCENDING, setOf("3"))

        assertEquals("Chiapas", sorted.first().name)
        // The remainder is still reversed by name.
        assertEquals(listOf("Taron", "Black Mamba", "Anubis"), sorted.drop(1).map { it.name })
    }

    @Test
    fun `multiple favourites are ordered among themselves by the active criterion`() {
        val sorted = sortAttractions(all, SortType.WAIT_TIME, SortDirection.ASCENDING, setOf("1", "2"))

        assertEquals(listOf("Black Mamba", "Taron"), sorted.take(2).map { it.name })
    }

    @Test
    fun `the open filter removes everything that is not operating`() {
        val filtered = filterAttractions(all, onlyOpen = true)

        assertEquals(listOf("Taron", "Black Mamba", "Chiapas"), filtered.map { it.name })
    }

    @Test
    fun `the open filter is a no-op when disabled`() {
        assertEquals(all, filterAttractions(all, onlyOpen = false))
    }
}
