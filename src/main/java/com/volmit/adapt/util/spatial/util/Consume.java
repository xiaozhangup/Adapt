/*
 * Spatial is a spatial api for Java...
 * Copyright (c) 2021 Arcane Arts
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

package com.volmit.adapt.util.spatial.util;

import java.io.IOException;

public class Consume {
    @FunctionalInterface
    public interface One<T0> {
        void accept(T0 t0);
    }

    @FunctionalInterface
    public interface Any<T0> {
        void accept(T0... t0);
    }

    @FunctionalInterface
    public interface Two<T0, T1> {
        void accept(T0 t0, T1 t1);
    }

    @FunctionalInterface
    public interface TwoIO<T0, T1> {
        void accept(T0 t0, T1 t1) throws IOException;
    }

    @FunctionalInterface
    public interface Three<T0, T1, T2> {
        void accept(T0 t0, T1 t1, T2 t2);
    }

    @FunctionalInterface
    public interface Four<T0, T1, T2, T3> {
        void accept(T0 t0, T1 t1, T2 t2, T3 t3);
    }

    @FunctionalInterface
    public interface FourIO<T0, T1, T2, T3> {
        void accept(T0 t0, T1 t1, T2 t2, T3 t3) throws IOException;
    }

    @FunctionalInterface
    public interface Five<T0, T1, T2, T3, T4> {
        void accept(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4);
    }

    @FunctionalInterface
    public interface Six<T0, T1, T2, T3, T4, T5> {
        void accept(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }

    @FunctionalInterface
    public interface Seven<T0, T1, T2, T3, T4, T5, T6> {
        void accept(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
    }

    @FunctionalInterface
    public interface Eight<T0, T1, T2, T3, T4, T5, T6, T7> {
        void accept(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);
    }
}
