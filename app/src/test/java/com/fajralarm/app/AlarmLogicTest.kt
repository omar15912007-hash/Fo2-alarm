package com.fajralarm.app

import com.fajralarm.app.data.AlarmHistoryRecord
import com.fajralarm.app.data.AlarmModel
import com.fajralarm.app.data.MissionType
import com.fajralarm.app.receiver.AlarmScheduler
import com.fajralarm.app.ui.ringing.canSnoozeAlarm
import com.fajralarm.app.ui.ringing.missions.generateMathProblem
import com.fajralarm.app.ui.screens.computeActiveStreakDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmLogicTest {

    @Test
    fun computeNextTriggerMillis_oneShotFutureToday_schedulesToday() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 8, 30, 45)
            set(Calendar.MILLISECOND, 123)
        }
        val trigger = AlarmScheduler.computeNextTriggerMillis(
            hour24 = 9,
            minute = 15,
            daysOfWeek = emptyList(),
            nowMillis = nowCal.timeInMillis
        )
        val resultCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(nowCal.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(9, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
    }

    @Test
    fun computeNextTriggerMillis_oneShotPastToday_schedulesTomorrow() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val trigger = AlarmScheduler.computeNextTriggerMillis(
            hour24 = 7,
            minute = 30,
            daysOfWeek = emptyList(),
            nowMillis = nowCal.timeInMillis
        )
        val expectedCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
        }
        assertEquals(expectedCal.timeInMillis, trigger)
    }

    @Test
    fun computeNextTriggerMillis_repeatingSkipsNonMatchingDays() {
        // Oct 24, 2026 is Saturday (Calendar.SATURDAY = 7). Schedule for Monday (Calendar.MONDAY = 2).
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val trigger = AlarmScheduler.computeNextTriggerMillis(
            hour24 = 6,
            minute = 0,
            daysOfWeek = listOf(Calendar.MONDAY),
            nowMillis = nowCal.timeInMillis
        )
        val resultCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(Calendar.MONDAY, resultCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(6, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun canSnoozeAlarm_enforcesMaxCountAndPreviewGuard() {
        val base = AlarmModel(isSnoozeEnabled = true, maxSnoozeCount = 2, currentSnoozeCount = 0)
        assertFalse(canSnoozeAlarm(base, isPreview = true))
        assertTrue(canSnoozeAlarm(base, isPreview = false))
        assertTrue(canSnoozeAlarm(base.copy(currentSnoozeCount = 1), isPreview = false))
        assertFalse(canSnoozeAlarm(base.copy(currentSnoozeCount = 2), isPreview = false))
        assertTrue(canSnoozeAlarm(base.copy(maxSnoozeCount = 999, currentSnoozeCount = 50), isPreview = false))
    }

    @Test
    fun computeActiveStreakDays_calculatesRealConsecutiveDays() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 14, 0, 0)
        }
        assertEquals(0, computeActiveStreakDays(emptyList(), nowCal.timeInMillis))

        fun recordDaysAgo(daysAgo: Int, success: Boolean = true, snoozed: Boolean = false): AlarmHistoryRecord {
            val cal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            return AlarmHistoryRecord(
                timestamp = cal.timeInMillis,
                alarmId = "a1",
                alarmLabel = "Fajr",
                missionType = MissionType.MATH,
                timeToSolveSeconds = 15,
                wasSnoozed = snoozed,
                isSuccess = success
            )
        }

        // Solved yesterday, 2 days ago, and 3 days ago (today's alarm hasn't fired yet) -> 3d streak
        val streakThree = listOf(recordDaysAgo(1), recordDaysAgo(2), recordDaysAgo(3))
        assertEquals(3, computeActiveStreakDays(streakThree, nowCal.timeInMillis))

        // Solved today as well -> 4d streak
        val streakFour = streakThree + recordDaysAgo(0)
        assertEquals(4, computeActiveStreakDays(streakFour, nowCal.timeInMillis))

        // Gap yesterday & today -> 0d streak
        val broken = listOf(recordDaysAgo(2), recordDaysAgo(3))
        assertEquals(0, computeActiveStreakDays(broken, nowCal.timeInMillis))
    }

    @Test
    fun generateMathProblem_neverProducesNegativeAnswers() {
        for (difficulty in 1..5) {
            repeat(200) {
                val problem = generateMathProblem(difficulty)
                assertTrue("Expression blank at difficulty $difficulty", problem.expression.isNotBlank())
                assertTrue("Negative answer ${problem.answer} at difficulty $difficulty", problem.answer >= 0)
            }
        }
    }
}
