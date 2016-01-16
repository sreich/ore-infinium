package com.ore.infinium

import java.time.LocalTime

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich @gmail.com>                    *
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
class WorldTime {
    private var m_time: LocalTime? = null

    fun setTime(hour: Int, minute: Int, second: Int) {
        m_time = LocalTime.of(hour, minute, second)
    }

    fun tick(elapsedTime: Double) {

        /*
            uint8_t interval = 1;
    m_minute += interval;

    if (m_second >= 60) {
        ++m_minute;
        m_second -= 60;
    }

    if (m_minute >= 60) {
        ++m_hour;
        m_minute -= 60;
    }

    if (m_hour >= 24) {
        m_hour -= 24;
    }

    Q_ASSERT(m_minute <= 59 && m_second <= 59);
    Q_ASSERT(m_hour <= 23);
}

QString Time::toString() const
{
    QTime time(m_hour, m_minute, m_second);

    return time.toString("hh:mm:ss");
}
         */

    }

    fun timeString(): String {
        return m_time!!.toString()
    }
}
