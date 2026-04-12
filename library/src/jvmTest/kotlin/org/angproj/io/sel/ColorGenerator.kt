/**
 * Copyright (c) 2026 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
 *
 * This software is available under the terms of the MIT license. Parts are licensed
 * under different terms if stated. The legal terms are attached to the LICENSE file
 * and are made available on:
 *
 *      https://opensource.org/licenses/MIT
 *
 * SPDX-License-Identifier: MIT
 *
 * Contributors:
 *      Kristoffer Paulsson - initial implementation
 */
package org.angproj.io.sel

import kotlin.test.Test
import kotlin.test.assertEquals

public class ColorGenerator {
    internal fun lightnessLoop(action: (Int, Float) -> Unit) {
        (0 until 7 step 1).forEach { action(it, lightnessPlane(it)) }
    }

    internal fun lightnessPlane(plane: Int): Float = (6.0-plane).toFloat() / 6.0f

    internal fun lightnessLevel(plane: Int, levelCount: Int, action: (Int, Float) -> Unit) {
        val levels = mapOf<Int, Pair<Float, Float>>(
            0 to Pair(7/7f, 7/7f),
            1 to Pair(6/7f, 5/7f),
            2 to Pair(5/7f, 4/7f),
            3 to Pair(4/7f, 3/7f),
            4 to Pair(3/7f, 2/7f),
            5 to Pair(2/7f, 1/7f),
            6 to Pair(/*7/1f*/ 0f, 0f)
            )
        val level = levels[plane]!!
        val distance = level.first - level.second

        (0 until levelCount step 1).forEach { action(it, level.first - (it.toFloat() / levelCount * distance)) }
    }

    internal fun colorCountLoop(action: (Int) -> Unit) {
        listOf(1, 28, 56, 84, 56, 28, 1).forEach { action(it) }
    }

    internal fun colorCount(slice: Int): Int {
        val colorCount = listOf(1, 28, 56, 84, 56, 28, 1)
        return colorCount[slice]
    }

    internal fun hue(colorCount: Int, slice: Int): Float = slice.toFloat() / colorCount.toFloat()

    internal fun hueSliceLoop(colorCount: Int, action: (Float) -> Unit) {
        (0 until colorCount step 1).forEach { action(hue(colorCount, it)) }
    }

    fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
        if (s == 0f) {
            return Triple(l, l, l)
        }

        val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
        val p = 2 * l - q

        fun hue2rgb(t: Float): Float {
            var tt = t
            if (tt < 0f) tt += 1f
            if (tt > 1f) tt -= 1f

            return when {
                tt < 1f / 6f -> p + (q - p) * 6f * tt
                tt < 1f / 2f -> q
                tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
                else -> p
            }
        }

        val r = hue2rgb(h + 1f / 3f)
        val g = hue2rgb(h)
        val b = hue2rgb(h - 1f / 3f)

        return Triple(r, g, b)
    }
}

fun Triple<Float, Float, Float>.toHexString(): String {
    val r = (this.first * 255).toInt().coerceIn(0, 255)
    val g = (this.second * 255).toInt().coerceIn(0, 255)
    val b = (this.third * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

class ColorGeneratorTest {
    @Test
    fun generatePalette() {
        val cg = ColorGenerator()
        var cnt = 0
        cg.lightnessLoop { p, u ->
            val c = cg.colorCount(p)
            cg.lightnessLevel(p, c) { f, ll ->
                val h = cg.hue(c, f)
                println("'${cg.hslToRgb(h, 1.0f, ll).toHexString()}',")
                cnt++
            }
        }
    }

    @Test
    fun testLightnessPlane() {
        val cg = ColorGenerator()

        println(cg.lightnessPlane(6))
    }

    @Test
    fun testLightnessLevel() {
        var cnt = 0
        /*ColorGenerator().lightnessLevel(1f, 0f, 100) { f, ll ->
            //assertEquals((100-f).toFloat() / 100.0f, ll )
            cnt++
        }*/
    }

    @Test
    fun testLightnessLoop() {
        var cnt = 0
        ColorGenerator().lightnessLoop { c, l ->
            assertEquals(cnt, c)
            cnt++
            assertEquals((7-cnt).toFloat() / 6.0f, l )
        }
    }

    @Test
    fun testColorCountLoop() {
        var sum = 0
        ColorGenerator().colorCountLoop { l -> sum += l }
        assertEquals(254, sum)
    }

    @Test
    fun testHueSliceLoop() {
        var cnt = 0
        ColorGenerator().hueSliceLoop(100) { h ->
            assertEquals((cnt).toFloat() / 100f, h )
            cnt++
        }
    }
}