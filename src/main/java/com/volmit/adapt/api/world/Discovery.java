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

package com.volmit.adapt.api.world;

import com.volmit.adapt.util.collection.KList;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Discovery<T> {
    @Getter
    private final KList<T> seen = new KList<>();
    private transient Set<T> seenIndex;
    private transient int indexedListSize = -1;

    public synchronized boolean isNewDiscovery(T t) {
        if (!index().add(t)) {
            return false;
        }
        seen.add(t);
        indexedListSize = seen.size();
        return true;
    }

    public synchronized boolean isNewDiscovery(T current, Collection<T> legacyValues) {
        Set<T> index = index();
        boolean known = index.contains(current);
        boolean migrated = seen.removeAll(legacyValues);
        if (migrated) {
            index.removeAll(legacyValues);
            indexedListSize = seen.size();
        }
        if (known) {
            return false;
        }
        if (migrated) {
            if (index.add(current)) {
                seen.add(current);
                indexedListSize = seen.size();
            }
            return false;
        }
        if (!index.add(current)) {
            return false;
        }
        seen.add(current);
        indexedListSize = seen.size();
        return true;
    }

    private Set<T> index() {
        if (seenIndex == null || indexedListSize != seen.size()) {
            seenIndex = new HashSet<>(seen);
            indexedListSize = seen.size();
        }
        return seenIndex;
    }
}
