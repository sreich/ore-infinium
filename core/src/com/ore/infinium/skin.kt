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

package com.ore.infinium

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import ktx.assets.file
import ktx.style.*

fun createFont(): BitmapFont {
    val fontGenerator = FreeTypeFontGenerator(file("fonts/Ubuntu-L.ttf"))
    val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
    parameter.size = 13

    return fontGenerator.generateFont(parameter)
}

var bitmapFont = createFont()

val oreSkin = skin(TextureAtlas(file("packed/ui.atlas"))) {

    scrollPane("list") {
        background = getDrawable("grey-panel")
        vScroll = getDrawable("button-blue-up")
        vScrollKnob = getDrawable("bar-horizontal-white")
    }

    scrollPane("default") {
        background = getDrawable("grey-panel")
        vScroll = getDrawable("button-blue-up")
        vScrollKnob = getDrawable("bar-horizontal-white")
    }

    progressBar("default-horizontal") {
        background = getDrawable("bar-horizontal-white")
        knobBefore = getDrawable("bar-horizontal-blue")
    }

    label("default") {
        font = bitmapFont
        fontColor = Color.BLACK
    }

    visTooltip {
        background = getDrawable("grey-panel")
    }

    window("default") {
        background = getDrawable("grey-panel")
        titleFont = bitmapFont
    }

    sizes("default") {
        scaleFactor = 2f

        //spacingLeft
        spacingBottom = 18f
        spacingRight = 26f
        spacingLeft = 100f
        buttonBarSpacing = 20f
        menuItemIconSize = 22f
        borderSize = 20f
        spinnerButtonHeight = 12f
        spinnerFieldSize = 40f
    }

    /*
        //---  from visui
	x2: {scaleFactor: 2, menuItemIconSize: 44, spinnerButtonHeight: 24,
    spinnerFieldSize: 80,
    spacingBottom: 8, spacingRight: 6, buttonBarSpacing: 10, borderSize: 1,
    fileChooserViewModeBigIconsSize: 200,
    fileChooserViewModeMediumIconsSize: 128, fileChooserViewModeSmallIconsSize: 64,
    fileChooserViewModeListWidthSize: 155 }

    com.kotcrab.vis.ui.widget.VisTextField$VisTextFieldStyle: {
	default: {focusBorder: border, errorBorder: border-error, font: default-font, fontColor: white,
	 disabledFontColor: grey, selection: selection, background: textfield,
	 backgroundOver: textfield-over, cursor: cursor },
	textArea: {errorBorder: border-error, font: default-font, fontColor: white, disabledFontColor: grey,
	 selection: selection, background: textfield, cursor: cursor },
	small: {focusBorder: border, errorBorder: border-error, font: small-font, f
	ontColor: white, disabledFontColor: grey,
	selection: selection, background: textfield, backgroundOver: textfield-over, cursor: cursor },
	label: {selection: selection, errorBorder: border-error, font: default-font,
	fontColor: white, disabledFontColor: grey }
	 ------------------------

     */
    visTextField("default") {
        background = getDrawable("bar-horizontal-white")

        font = bitmapFont
        fontColor = Color.BLACK
    }

    visTextButton("default") {
        up = getDrawable("button-blue-up").apply {
            //            up = getDrawable("grey-panel").apply {
            //      minHeight = 35f
            //     minWidth = 50f
        }

        down = getDrawable("button-blue-down")
        over = getDrawable("button-blue-over")
        disabled = getDrawable("glass-panel")
        focusBorder = getDrawable("glass-panel")

        font = bitmapFont
        fontColor = Color.BLACK
        disabledFontColor = Color.GRAY
    }
}
