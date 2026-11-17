package com.example

import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationEngineTest {

    @Test
    fun partialRoomSpecificParasFourAndFiveAdvanceToSixAndSeven() {
        val participants = listOf(
            ParticipantAllocation(
                id = 1L,
                name = "Hamza",
                count = 2,
                displayOrder = 4,
                specificParas = listOf(4)
            )
        )

        val month0 = RotationEngine.calculateAssignments(
            participants = participants,
            startYear = 2026,
            startMonth = 1,
            targetYear = 2026,
            targetMonth = 1
        )
        assertEquals(1, month0.size)
        assertEquals(listOf(4, 5), month0.first().assignedParas)
        assertEquals("Paras 4–5", month0.first().displayRangeText)

        val month1 = RotationEngine.calculateAssignments(
            participants = participants,
            startYear = 2026,
            startMonth = 1,
            targetYear = 2026,
            targetMonth = 2
        )
        assertEquals(1, month1.size)
        assertEquals(listOf(6, 7), month1.first().assignedParas)
        assertEquals("Paras 6–7", month1.first().displayRangeText)

        val month2 = RotationEngine.calculateAssignments(
            participants = participants,
            startYear = 2026,
            startMonth = 1,
            targetYear = 2026,
            targetMonth = 3
        )
        assertEquals(listOf(8, 9), month2.first().assignedParas)
    }

    @Test
    fun partialRoomSpecificParasOneAndTwoAdvanceToThreeAndFour() {
        val participants = listOf(
            ParticipantAllocation(
                id = 10L,
                name = "User",
                count = 2,
                displayOrder = 1,
                specificParas = listOf(1)
            )
        )

        val month0 = RotationEngine.calculateAssignments(participants, 2026, 5, 2026, 5)
        val month1 = RotationEngine.calculateAssignments(participants, 2026, 5, 2026, 6)

        assertEquals(listOf(1, 2), month0.first().assignedParas)
        assertEquals(listOf(3, 4), month1.first().assignedParas)
    }

    @Test
    fun partialRoomWrapsCleanlyFromPara30ToPara1() {
        val participants = listOf(
            ParticipantAllocation(
                id = 2L,
                name = "Amina",
                count = 2,
                displayOrder = 29,
                specificParas = listOf(29)
            )
        )

        val month0 = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, 1)
        val month1 = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, 2)

        assertEquals(listOf(29, 30), month0.first().assignedParas)
        assertEquals(listOf(1, 2), month1.first().assignedParas)
    }

    @Test
    fun partialRoomMultipleMembersWithUnequalCountsStayCollisionFree() {
        val participants = listOf(
            ParticipantAllocation(id = 1L, name = "A", count = 2, displayOrder = 1, specificParas = listOf(1)),
            ParticipantAllocation(id = 2L, name = "B", count = 1, displayOrder = 3, specificParas = listOf(3)),
            ParticipantAllocation(id = 3L, name = "C", count = 3, displayOrder = 10, specificParas = listOf(10))
        )

        for (offset in 0..14) {
            val year = 2026 + (offset / 12)
            val month = (offset % 12) + 1
            val result = RotationEngine.calculateAssignments(participants, 2026, 1, year, month)

            assertEquals(3, result.size)
            assertEquals(2, result.first { it.participantId == 1L }.assignedParas.size)
            assertEquals(1, result.first { it.participantId == 2L }.assignedParas.size)
            assertEquals(3, result.first { it.participantId == 3L }.assignedParas.size)

            val flat = result.flatMap { it.assignedParas }
            assertEquals("No duplicate Paras across members in offset $offset", 6, flat.toSet().size)
            assertTrue(flat.all { it in 1..30 })
        }
    }

    @Test
    fun full30ParaRoomUnequalAllocationsMaintainZeroConsecutiveOverlap() {
        val participants = listOf(
            ParticipantAllocation(id = 1L, name = "M1", count = 4, displayOrder = 1),
            ParticipantAllocation(id = 2L, name = "M2", count = 6, displayOrder = 2),
            ParticipantAllocation(id = 3L, name = "M3", count = 3, displayOrder = 3),
            ParticipantAllocation(id = 4L, name = "M4", count = 7, displayOrder = 4),
            ParticipantAllocation(id = 5L, name = "M5", count = 5, displayOrder = 5),
            ParticipantAllocation(id = 6L, name = "M6", count = 5, displayOrder = 6)
        )

        for (offset in 0 until 12) {
            val cur = RotationEngine.calculateAssignments(participants, 2026, 1, 2026, offset + 1)
            val nextYear = if (offset + 2 > 12) 2027 else 2026
            val nextMonth = if (offset + 2 > 12) (offset + 2) - 12 else offset + 2
            val next = RotationEngine.calculateAssignments(participants, 2026, 1, nextYear, nextMonth)

            assertEquals((1..30).toSet(), cur.flatMap { it.assignedParas }.toSet())
            participants.forEach { p ->
                val curSet = cur.first { it.participantId == p.id }.assignedParas.toSet()
                val nextSet = next.first { it.participantId == p.id }.assignedParas.toSet()
                assertTrue("Member ${p.name} overlapped in month ${offset + 1}", curSet.intersect(nextSet).isEmpty())
            }
        }
    }
}
