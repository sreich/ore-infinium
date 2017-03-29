package com.ore.infinium

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import ktx.assets.file
import ktx.log.debug
import ktx.style.*

/**
MIT License

Copyright (c) 2017 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

fun createFont(): BitmapFont {
    val fontGenerator = FreeTypeFontGenerator(file("fonts/kenvector-future-thin.ttf"))
    val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
    parameter.size = 13

    debug { "created font for skin." }

    return fontGenerator.generateFont(parameter)
}

var bitmapFont = createFont()

val oreSkin = skin(TextureAtlas(file("packed/ui.atlas"))) {

    scrollPane("list") {
        background = getDrawable("grey-panel")
    }

    scrollPane("default") {
        background = getDrawable("grey-panel")
    }

    progressBar("default-horizontal") {
        background = getDrawable("bar-horizontal-white-mid")
        knobBefore = getDrawable("bar-horizontal-blue-combined")
    }

    label("default") {
        font = bitmapFont
        fontColor = Color.BLACK
    }

    visTooltip {

    }

    window("default") {
        titleFont = bitmapFont
    }

    sizes("default") {
    }

    visTextField("default") {
        font = bitmapFont
        fontColor = Color.BLACK
    }

    visTextButton {
        font = bitmapFont
        fontColor = Color.BLACK
    }
}
