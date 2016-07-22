package com.ore.infinium

/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

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

class GeneratorInventory(slotCount: Int) : Inventory(slotCount) {
    /**
     * fuel source currently being burnt.
     * once a fuel source is started burning, it cannot
     * be recovered
     */
    var fuelSource: Int? = null

    //when a fuel source is initially burned, it is set to 100
    //over time it will decrease until 0, at which point the fuel
    //source is consumed
    var fuelSourceHealth = 0

    /**
     * the generator that this generator applies to.
     * while this instance will always exist, this
     * field will be set/cleared when the window is shown/closed
     */
    var owningGeneratorEntityId: Int? = null

    init {
        inventoryType = Network.Shared.InventoryType.Generator
    }

    companion object {
        val MAX_SLOTS = 32
    }
}

