package com.nuani.asobiri.commons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EngineTest {

    @Test
    fun `fromId resolves every declared id`() {
        Engine.entries.forEach { engine ->
            assertEquals(engine, Engine.fromId(engine.id))
        }
    }

    @Test
    fun `fromId returns null for unknown id`() {
        assertNull(Engine.fromId("unreal5"))
    }

    @Test
    fun `parseList handles whitespace case and unknown ids`() {
        val parsed = Engine.parseList(" renpy , RPGM-MV ,unreal5, rpgm-mz ")
        assertEquals(
            listOf(Engine.RENPY, Engine.RPG_MAKER_MV, Engine.RPG_MAKER_MZ),
            parsed,
        )
    }

    @Test
    fun `ids are unique`() {
        val ids = Engine.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
