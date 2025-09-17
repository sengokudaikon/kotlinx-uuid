package app.softwork.uuid

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private const val MASK_A12: Int = 0x0FFF
private val MASK_B62: ULong = (1uL shl 62) - 1uL

class Uuidv7MonotonicTest {
    @BeforeTest
    fun reset() {
        Uuidv7Monotonic.resetForTests()
    }

    @Test
    fun seedsNewTimestamps() {
        val timestamp = 1_234L
        val uuid = Uuidv7Monotonic.next(timestamp)

        assertEquals(timestamp, uuid.unixTimeStamp)
        assertEquals(7, uuid.version)
        assertTrue(uuid.variant == 4 || uuid.variant == 5)
    }

    @Test
    fun incrementsRandBWithinSameMillisecond() {
        val timestamp = 4_200L
        val initialRandA = 0x012
        val initialRandB = 1uL
        Uuidv7Monotonic.forceStateForTests(timestamp, initialRandA, initialRandB)

        val uuid = Uuidv7Monotonic.next(timestamp)
        val (randA, randB) = uuid.extractRandParts()

        assertEquals(timestamp, uuid.unixTimeStamp)
        assertEquals(initialRandA and MASK_A12, randA)
        assertEquals(initialRandB + 1u, randB)
    }

    @Test
    fun carriesIntoRandAWhenRandBOverflows() {
        val timestamp = 9_999L
        val initialRandA = 0x001
        val initialRandB = MASK_B62
        Uuidv7Monotonic.forceStateForTests(timestamp, initialRandA, initialRandB)

        val uuid = Uuidv7Monotonic.next(timestamp)
        val (randA, randB) = uuid.extractRandParts()

        assertEquals(timestamp, uuid.unixTimeStamp)
        assertEquals((initialRandA + 1) and MASK_A12, randA)
        assertEquals(0u, randB)
    }

    @Test
    fun advancesTimeWhenCountersExhausted() {
        val timestamp = 55_555L
        val initialRandA = MASK_A12
        val initialRandB = MASK_B62
        Uuidv7Monotonic.forceStateForTests(timestamp, initialRandA, initialRandB)

        val uuid = Uuidv7Monotonic.next(timestamp)
        val (randA, randB) = uuid.extractRandParts()

        assertEquals(timestamp + 1, uuid.unixTimeStamp)
        assertEquals(0, randA)
        assertEquals(0u, randB)
    }
}

private fun Uuid.extractRandParts(): Pair<Int, ULong> = toLongs { msb, lsb ->
    val aHigh = ((msb ushr 8) and 0x0F).toInt()
    val aLow = (msb and 0xFF).toInt()
    val randA = (aHigh shl 8) or aLow

    val lower = lsb.toULong()
    var randB = ((lower shr 56) and 0x3Fu) shl 56
    randB = randB or (((lower shr 48) and 0xFFu) shl 48)
    randB = randB or (((lower shr 40) and 0xFFu) shl 40)
    randB = randB or (((lower shr 32) and 0xFFu) shl 32)
    randB = randB or (((lower shr 24) and 0xFFu) shl 24)
    randB = randB or (((lower shr 16) and 0xFFu) shl 16)
    randB = randB or (((lower shr 8) and 0xFFu) shl 8)
    randB = randB or (lower and 0xFFu)

    randA to randB
}
