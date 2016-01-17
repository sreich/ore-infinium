package com.ore.infinium.util

import com.artemis.Component
import com.artemis.ComponentMapper

/******************************************************************************
 *   Copyright (C) 2016 by Shaun Reich <sreich02@gmail.com>                *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or            *
 *   modify it under the terms of the GNU General Public License as           *
 *   published by the Free Software Foundation; either version 2 of           *
 *   the License, or (at your option) any later version.                      *
 *                                                                            *
 *   This program is distributed in the hope that it will be useful,          *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 *   GNU General Public License for more details.                             *
 *                                                                            *
 *   You should have received a copy of the GNU General Public License        *
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 *****************************************************************************/

/**
 * Gets the component that this mapper is for, for this entity
 * @returns null if the component does not exist for this entity,
 * or the requested component
 */
fun <T : Component?> ComponentMapper<T>.getNullable(entityId: Int): T? {
    return this.getSafe(entityId)
}
