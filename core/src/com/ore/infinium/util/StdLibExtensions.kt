package com.ore.infinium.util

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
 *
 * returns result of lambda, so we can e.g. return an element outward, by doing
 *
 * list.mapFirstNotNull { it.mapFirstNotNull { it.firstOrNull { it.predicate() } } }
 *
 * allowing us to get the element that matches that predicate, outside of the nesting
 * lambdas.
 */
inline fun <T, R : Any> Iterable<T>.firstNotNull(selector: (T) -> R?): R? {
    forEach { selector(it)?.let { return it } }
    return null
}

