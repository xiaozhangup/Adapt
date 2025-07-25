/*
 * Amulet is an extension api for Java
 * Copyright (c) 2022 Arcane Arts
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package extensions.java.util.Map;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.Self;
import manifold.ext.rt.api.This;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Extension
public class XMap {
    /**
     * Returns a sorted list of keys from this map, based on the sorting order of
     * the values.
     *
     * @param comparator
     *            the comparator
     * @return the value-sorted key list
     */
    public static <K, V> List<K> sortK(@This Map<K, V> self, Comparator<V> comparator) {
        List<K> k = new ArrayList<K>();
        List<V> v = self.v();
        List<K> sk = self.k();

        v.sort(comparator);

        for (V i : v) {
            for (K j : sk) {
                if (self.get(j).equals(i)) {
                    k.add(j);
                }
            }
        }

        return k;
    }

    public static <K, V> List<K> sortKNumber(@This Map<K, V> self) {
        List<K> k = new ArrayList<>();
        List<V> v = self.v();

        v.sort((v1, t1) -> {
            Number n1 = (Number) v1;
            Number n2 = (Number) t1;
            return (int) ((n1.doubleValue() - n2.doubleValue()) * 1_000);
        });

        for (V i : v) {
            for (K j : self.k()) {
                if (self.get(j).equals(i)) {
                    k.add(j);
                }
            }
        }

        return k.withoutDuplicates();
    }

    public static <K, V> @Self Map<K, V> unmodifiable(@This Map<K, V> self) {
        return Collections.unmodifiableMap(self);
    }

    /**
     * Returns a sorted list of keys from this map, based on the sorting order of
     * the values toString order
     *
     * @return the value-sorted key list
     */
    public static <K, V> List<K> sortK(@This Map<K, V> self) {
        return self.sortK(Comparator.comparing(Object::toString));
    }

    public static <K, V> @Self Map<K, V> plus(@This Map<K, V> self, Map<K, V> map) {
        return self.copy().put(map);
    }

    @Extension
    public static <K, V> ConcurrentHashMap<K, V> concurrent() {
        return new ConcurrentHashMap<>();
    }

    @Extension
    public static <K, V> HashMap<K, V> hash() {
        return new HashMap<>();
    }

    @Extension
    public static <K, V> LinkedHashMap<K, V> linked() {
        return new LinkedHashMap<>();
    }

    @Extension
    public static <K, V> WeakHashMap<K, V> weak() {
        return new WeakHashMap<>();
    }

    @Extension
    public static <K, V> IdentityHashMap<K, V> identityHash() {
        return new IdentityHashMap<>();
    }

    public static <K, V> @Self Map<K, V> minus(@This Map<K, V> self, K v) {
        return self.copy().qremove(v);
    }

    public static <K, V> @Self Map<V, K> unaryMinus(@This Map<K, V> self) {
        return self.copy().flipFlatten();
    }

    /**
     * Put another map's values into this map
     *
     * @param m
     *            the map to insert
     * @return this map (builder)
     */
    public static <K, V> @Self Map<K, V> put(@This Map<K, V> self, Map<K, V> m) {
        self.putAll(m);
        return self;
    }

    /**
     * Return a copy of this map
     *
     * @param factory
     *            the factory to create a new map
     * @return the copied map
     */
    public static <K, V> @Self Map<K, V> copy(@This Map<K, V> self, Supplier<Map<K, V>> factory) {
        return factory.get().put(self);
    }

    /**
     * Return a copy of this map
     *
     * @return the copied map
     */
    public static <K, V> @Self Map<K, V> copy(@This Map<K, V> self) {
        return self.copy(HashMap::new);
    }

    /**
     * Flip the hashmap and flatten the value list even if there are multiple keys
     *
     * @return the flipped and flattened hashmap
     */
    public static <K, V> Map<V, K> flipFlatten(@This Map<K, V> self) {
        Map<V, List<K>> f = self.flip();
        Map<V, K> m = new HashMap<>();

        for (V i : f.k()) {
            m.putNonNull(i, m.isEmpty() ? null : (f.get(i))[0]);
        }

        return m;
    }

    /**
     * Flip the hashmap so keys are now list-keys in the value position
     *
     * @return the flipped hashmap
     */
    public static <K, V> Map<V, List<K>> flip(@This Map<K, V> self) {
        Map<V, List<K>> flipped = new HashMap<>();

        for (K i : self.keySet()) {
            if (i == null) {
                continue;
            }

            flipped.computeIfAbsent(self.get(i), (__) -> new ArrayList<>()).add(i);
        }

        return flipped;
    }

    /**
     * Sort values based on the keys sorting order
     *
     * @param comparator
     *            the comparator to use
     * @return the values (sorted)
     */
    public static <K, V> List<V> sortV(@This Map<K, V> self, Comparator<K> comparator) {
        List<V> v = new ArrayList<>();
        List<K> k = self.k();
        List<V> vs = self.v();
        k.sort(comparator);

        for (K i : k) {
            for (V j : vs) {
                if (self.get(i).equals(j)) {
                    v.add(j);
                }
            }
        }

        return v;
    }

    /**
     * Sort values based on the keys toString sorted order
     *
     * @return the values (sorted)
     */
    public static <K, V> List<V> sortV(@This Map<K, V> self) {
        return self.sortV(Comparator.comparing(Object::toString));
    }

    /**
     * Get a copy of this maps keys
     *
     * @return the keys
     */
    public static <K, V> List<K> k(@This Map<K, V> self) {
        return List.from(self.keySet());
    }

    /**
     * Get a copy of this maps values
     *
     * @return the values
     */
    public static <K, V> List<V> v(@This Map<K, V> self) {
        return List.from(self.values());
    }

    /**
     * Still works as it normally should except it returns itself (builder)
     *
     * @param key
     *            the key
     * @param value
     *            the value (single only supported)
     * @return self
     */
    public static <K, V> @Self Map<K, V> qput(@This Map<K, V> self, K key, V value) {
        self.put(key, value);
        return self;
    }

    public static <K, V> @Self Map<K, V> qremove(@This Map<K, V> self, K key) {
        self.remove(key);
        return self;
    }

    /**
     * Works just like put, except it wont put anything unless the key and value are
     * nonnull
     *
     * @param key
     *            the nonnull key
     * @param value
     *            the nonnull value
     * @return the same map
     */
    public static <K, V> @Self Map<K, V> putNonNull(@This Map<K, V> self, K key, V value) {
        if (key != null && value != null) {
            self.put(key, value);
        }

        return self;
    }

    /**
     * Clear this map and return it
     *
     * @return the cleared map
     */
    public static <K, V> @Self Map<K, V> qclear(@This Map<K, V> self) {
        self.clear();
        return self;
    }
}
