/*------------------------------------------------------------------------------
-   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
-   Copyright (c) 2022 Arcane Arts (Volmit Software)
-
-   This program is free software: you can redistribute it and/or modify
-   it under the terms of the GNU General Public License as published by
-   the Free Software Foundation, either version 3 of the License, or
-   (at your option) any later version.
-
-   This program is distributed in the hope that it will be useful,
-   but WITHOUT ANY WARRANTY; without even the implied warranty of
-   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-   GNU General Public License for more details.
-
-   You should have received a copy of the GNU General Public License
-   along with this program.  If not, see <https://www.gnu.org/licenses/>.
-----------------------------------------------------------------------------*/

package com.volmit.adapt.api.xp;

import com.volmit.adapt.AdaptConfig;

@FunctionalInterface
public interface NewtonCurve {
    double getXPForLevel(double level);

    default double computeLevelForXP(double xp, double maxError) {
        double div = 2;
        int iterations = 0;
        double jumpSize = 100;
        double cursor = 0;
        double test;
        boolean last = false;

        while (jumpSize > maxError && iterations < 100) {
            iterations++;
            test = getXPForLevel(cursor);
            if (test < xp) {
                if (last) {
                    jumpSize /= div;
                }
                last = false;
                cursor += jumpSize;
            } else {
                if (!last) {
                    jumpSize /= div;
                }

                last = true;
                cursor -= jumpSize;
            }
            // Check if the level has exceeded the maximum allowed (1000)
            if (cursor > AdaptConfig.get().experienceMaxLevel) {
                cursor = AdaptConfig.get().experienceMaxLevel;
                break;
            }
        }
        return cursor;
    }
}
