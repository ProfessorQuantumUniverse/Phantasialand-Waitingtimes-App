package com.quantum_prof.phantalandwaittimes.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttractionWaitTimeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `status strings map to the matching enum regardless of case and padding`() {
        assertEquals(AttractionStatus.OPENED, AttractionStatus.fromRaw("  Opened "))
        assertEquals(AttractionStatus.CLOSED, AttractionStatus.fromRaw("CLOSED"))
        assertEquals(AttractionStatus.MAINTENANCE, AttractionStatus.fromRaw("maintenance"))
        assertEquals(AttractionStatus.VIRTUAL_QUEUE, AttractionStatus.fromRaw("virtualqueue"))
    }

    @Test
    fun `an unrecognised status is not treated as operating`() {
        val attraction = AttractionWaitTime(code = "1", name = "Test", status = "sonstwas")

        assertEquals(AttractionStatus.UNKNOWN, attraction.attractionStatus)
        assertFalse(attraction.isOpen)
    }

    @Test
    fun `implausible wait times are clamped for display`() {
        val negative = AttractionWaitTime(code = "1", name = "Test", waitTimeMinutes = -5)
        val absurd = AttractionWaitTime(code = "2", name = "Test", waitTimeMinutes = 99_999)

        assertEquals(0, negative.displayWaitTime)
        assertEquals(AttractionWaitTime.MAX_PLAUSIBLE_WAIT_MINUTES, absurd.displayWaitTime)
    }

    @Test
    fun `entries without a code or a name are rejected as unusable`() {
        assertFalse(AttractionWaitTime(code = "", name = "Taron").isUsable)
        assertFalse(AttractionWaitTime(code = "3136", name = "  ").isUsable)
        assertTrue(AttractionWaitTime(code = "3136", name = "Taron").isUsable)
    }

    @Test
    fun `a null wait time falls back to the default instead of throwing`() {
        val payload = """[{"code":"3136","name":"Taron","waitingtime":null,"status":"opened"}]"""

        val parsed = json.decodeFromString<List<AttractionWaitTime>>(payload)

        assertEquals(1, parsed.size)
        assertEquals(0, parsed.single().waitTimeMinutes)
        assertTrue(parsed.single().isOpen)
    }

    @Test
    fun `missing fields and unknown fields both survive parsing`() {
        val payload = """[{"code":"3136","name":"Taron","somethingNew":"ignored"}]"""

        val parsed = json.decodeFromString<List<AttractionWaitTime>>(payload)

        assertEquals("Taron", parsed.single().name)
        assertEquals(AttractionStatus.UNKNOWN, parsed.single().attractionStatus)
    }

    @Test
    fun `a numeric wait time sent as a string is accepted in lenient mode`() {
        val payload = """[{"code":"3136","name":"Taron","waitingtime":"45","status":"opened"}]"""

        val parsed = json.decodeFromString<List<AttractionWaitTime>>(payload)

        assertEquals(45, parsed.single().waitTimeMinutes)
    }
}
