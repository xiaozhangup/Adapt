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

package extensions.java.util.stream.Stream;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.Self;
import manifold.ext.rt.api.This;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.*;

@Extension
public class XStream {
    public static <T> Set<T> toSet(@This Stream<T> thiz) {
        return thiz.collect(Collectors.toSet());
    }

    public static <T> IntStream ints(@This Stream<T> thiz) {
        return thiz.mapToInt(i -> (int) i);
    }

    public static <T> DoubleStream doubles(@This Stream<T> thiz) {
        return thiz.mapToDouble(i -> (double) i);
    }

    public static <T> LongStream longs(@This Stream<T> thiz) {
        return thiz.mapToLong(i -> (long) i);
    }

    public static <T> @Self Stream<T> and(@This Stream<T> thiz, Stream<T> add) {
        return Stream.concat(thiz, add);
    }

    public static <T> @Self Stream<T> where(@This Stream<T> thiz, Predicate<T> pred) {
        return thiz.filter(pred);
    }

    public static <T> @Self Stream<T> without(@This Stream<T> thiz, Predicate<T> pred) {
        return thiz.filter(pred.negate());
    }

    public static <T> @Self Stream<T> and(@This Stream<T> thiz, T add) {
        return thiz.and(Stream.of(add));
    }

    public static <T> @Self Stream<T> plus(@This Stream<T> thiz, Stream<T> add) {
        return Stream.concat(thiz, add);
    }
}