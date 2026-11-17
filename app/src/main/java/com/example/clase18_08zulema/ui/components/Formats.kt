package com.example.clase18_08zulema.ui.components

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    private val locale = Locale("es", "CO")
    private val day = DateTimeFormatter.ofPattern("d", locale)
    private val month = DateTimeFormatter.ofPattern("MMM", locale)
    private val weekday = DateTimeFormatter.ofPattern("EEE", locale)
    private val time = DateTimeFormatter.ofPattern("h:mm a", locale)
    private val long = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)

    fun parse(value: String): LocalDateTime? = runCatching { LocalDateTime.parse(value) }.getOrNull()

    fun day(value: String) = parse(value)?.format(day) ?: "—"
    fun month(value: String) = parse(value)?.format(month)?.trimEnd('.')?.uppercase(locale) ?: ""
    fun weekday(value: String) = parse(value)?.format(weekday)?.trimEnd('.')?.replaceFirstChar { it.titlecase(locale) } ?: ""
    fun time(value: String) = parse(value)?.format(time)?.replace("a. m.", "a.m.")?.replace("p. m.", "p.m.") ?: value
    fun longDate(value: String) = parse(value)?.format(long)?.replaceFirstChar { it.titlecase(locale) } ?: value.replace('T', ' ')

    fun slots(count: Long) = if (count == 1L) "1 cupo" else "$count cupos"
    fun people(count: Int) = if (count == 1) "1 persona" else "$count personas"
}
