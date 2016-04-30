/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                   *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */

package com.ore.infinium.systems.client

/**
 * A marker interface that indicates that this system should only be
 * processed by the render portion of the game loop. Separating the logic
 * and the render ticks, so that we can decide how often to process them (how
 * many ms per frame, etc)
 */
interface RenderSystemMarker

