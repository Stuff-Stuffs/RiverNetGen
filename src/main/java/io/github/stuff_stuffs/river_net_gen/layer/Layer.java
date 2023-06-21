package io.github.stuff_stuffs.river_net_gen.layer;

import io.github.stuff_stuffs.river_net_gen.util.Hex;

import java.util.function.Function;

public sealed interface Layer<T> {
    T get(Hex.Coordinate coordinate);

    final class Basic<T> implements Layer<T> {
        private final Function<Hex.Coordinate, T> func;

        public Basic(final Function<Hex.Coordinate, T> func) {
            this.func = func;
        }

        @Override
        public T get(final Hex.Coordinate coordinate) {
            return func.apply(coordinate);
        }
    }

    final class Caching<T> implements Layer<T> {
        private final Layer<T> delegate;
        private final int mask;
        private final Hex.Coordinate[] keys;
        private final Object[] values;

        public Caching(final Layer<T> delegate, final int sizeLog2) {
            this.delegate = delegate;
            mask = (1 << sizeLog2) - 1;
            keys = new Hex.Coordinate[1 << sizeLog2];
            values = new Object[1 << sizeLog2];
        }

        @Override
        public T get(final Hex.Coordinate coordinate) {
            final int hash = coordinate.hashCode();
            final int index = hash & mask;
            if (coordinate.equals(keys[index])) {
                return (T) values[index];
            }
            final T val = delegate.get(coordinate);
            keys[index] = coordinate;
            values[index] = val;
            return val;
        }
    }
}
