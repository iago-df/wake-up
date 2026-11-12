package com.iagodf.wakeup.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** un bloque del horario. dia 1 = luns ... 5 = venres. todos duran 1 hora */
data class Block(val day: Int, val start: LocalTime, val code: String, val room: String) {
    val end: LocalTime get() = start.plusMinutes(MINUTES.toLong())
    fun key(date: LocalDate) = "$date|$start"

    companion object {
        const val MINUTES = 60
    }
}

class Semester(private val es: String, private val en: String, val from: LocalDate, val to: LocalDate, val blocks: List<Block>) {
    val name: String get() = tr(es, en)
}

object Schedule {
    private fun b(day: Int, start: String, code: String, room: String) = Block(day, LocalTime.parse(start), code, room)

    // los codigos, grupos y aulas van tal cual salen en el horario oficial, no tocar
    private val c1 = listOf(
        b(1, "08:30", "1.AXO.2...", "L.0.1"),
        b(1, "09:30", "1.AXO.2...", "L.0.1"),

        b(2, "08:30", "1.MD.23", "L.0.1"),
        b(2, "09:30", "1.MD.23", "L.0.1"),
        b(2, "10:30", "1.Cal.2", "A.3.0"),
        b(2, "11:30", "1.AXO.2", "A.3.0"),

        b(3, "10:30", "1.MD.2", "A.3.0"),
        b(3, "17:30", "2.DS.4", "A.3.0"),

        b(4, "10:30", "1.Cal.2", "A.3.0"),
        b(4, "12:30", "1.Cal.2...", "L.0.1"),
        b(4, "13:30", "1.Cal.2...", "L.0.1"),
        b(4, "15:30", "2.DS.41", "L.0.3"),
        b(4, "16:30", "2.DS.41", "L.0.3"),

        b(5, "10:30", "1.AXO.2", "A.3.0"),
        b(5, "11:30", "1.MD.2", "A.3.0"),
        b(5, "18:30", "2.DS.4", "A.3.0"),
    )

    private val c2 = listOf(
        b(1, "10:30", "1.Alx.2", "A.3.0"),
        b(1, "11:30", "1.TE.2", "A.3.0"),
        b(1, "18:30", "2.PS.4", "A.3.0"),

        b(2, "08:30", "1.Est.2...", "A.2.11"),
        b(2, "09:30", "1.Est.2...", "A.2.11"),
        b(2, "10:30", "1.FC.2", "A.3.0"),

        b(3, "10:30", "1.Est.2", "A.3.0"),
        b(3, "11:30", "1.TE.2", "A.3.0"),
        b(3, "12:30", "1.TE.23", "L.0.6"),
        b(3, "13:30", "1.TE.23", "L.0.6"),
        b(3, "17:30", "2.Red.4", "A.3.0"),
        b(3, "18:30", "2.PS.4", "A.3.0"),

        b(4, "08:30", "1.FC.23", "L.0.1"),
        b(4, "09:30", "1.FC.23", "L.0.1"),
        b(4, "10:30", "1.FC.2", "A.3.0"),
        b(4, "11:30", "1.Alx.2", "A.3.0"),
        b(4, "15:30", "2.Red.4...", "L.0.3"),
        b(4, "16:30", "2.Red.4...", "L.0.3"),

        b(5, "11:30", "1.Est.2", "A.3.0"),
        b(5, "12:30", "1.Alx.2...", "L.0.1"),
        b(5, "13:30", "1.Alx.2...", "L.0.1"),
        b(5, "15:30", "2.PS.41", "L.0.3"),
        b(5, "16:30", "2.PS.41", "L.0.3"),
        b(5, "18:30", "2.Red.4", "A.3.0"),
    )

    val semesters = listOf(
        Semester("1º cuatrimestre", "1st semester", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 12, 19), c1),
        // el segundo acaba en junio, no se el dia exacto
        Semester("2º cuatrimestre", "2nd semester", LocalDate.of(2027, 1, 6), LocalDate.of(2027, 6, 30), c2),
    )

    fun semesterOn(date: LocalDate): Semester? = semesters.firstOrNull { !date.isBefore(it.from) && !date.isAfter(it.to) }

    fun blocksOn(date: LocalDate): List<Block> {
        val s = semesterOn(date) ?: return emptyList()
        val dow = date.dayOfWeek.value
        return s.blocks.filter { it.day == dow }.sortedBy { it.start }
    }

    /**
     * minutos de clase que cuentan como tiempo ocupado ese dia:
     * las que marque como "fui" y las que todavia no han pasado
     */
    fun classMinutes(date: LocalDate, attended: Set<String>, now: LocalDateTime): Int =
        blocksOn(date).count { counts(it, date, attended, now) } * Block.MINUTES

    fun counts(block: Block, date: LocalDate, attended: Set<String>, now: LocalDateTime): Boolean =
        block.key(date) in attended || isPending(block, date, now)

    fun isPending(block: Block, date: LocalDate, now: LocalDateTime): Boolean =
        LocalDateTime.of(date, block.end).isAfter(now)
}
