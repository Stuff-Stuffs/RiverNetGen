package io.github.stuff_stuffs.river_net_gen.layer;

import io.github.stuff_stuffs.river_net_gen.util.SHM;

import java.util.function.Function;

public sealed interface Layer<T> {
    T get(SHM.Coordinate coordinate);

    final class Basic<T> implements Layer<T> {
        private final Function<SHM.Coordinate, T> func;

        public Basic(final Function<SHM.Coordinate, T> func) {
            this.func = func;
        }

        @Override
        public T get(final SHM.Coordinate coordinate) {
            return func.apply(coordinate);
        }
    }

    final class CachingOuter<T> implements Layer<T> {
        private final Basic<T> delegate;
        private final int level;
        private final int mask;
        private final int[] hashes;
        private final SHM.Coordinate[] keys;
        private final Object[] values;

        public CachingOuter(final Basic<T> delegate, final int sizeLog2, final int level) {
            this.delegate = delegate;
            this.level = level;
            final int size = 1 << sizeLog2;
            mask = size - 1;
            hashes = new int[size];
            keys = new SHM.Coordinate[size];
            values = new Object[size];
        }

        @Override
        public T get(final SHM.Coordinate coordinate) {
            final int hash = SHM.outerHash(coordinate, level);
            final int pos = hash & mask;
            if (hashes[pos] == hash) {
                final SHM.Coordinate key = keys[pos];
                if (key != null && SHM.outerEquals(coordinate, key, level)) {
                    return (T) values[pos];
                }
            }
            final T val = delegate.get(coordinate);
            keys[pos] = coordinate;
            values[pos] = val;
            hashes[pos] = hash;
            return val;
        }
    }

    final class CachingInner<T> implements Layer<T> {
        private final Basic<T> delegate;
        private final int level;
        private final int mask;
        private final int[] hashes;
        private final SHM.Coordinate[] keys;
        private final Object[] values;

        public CachingInner(final Basic<T> delegate, final int sizeLog2, final int level) {
            this.delegate = delegate;
            this.level = level;
            final int size = 1 << sizeLog2;
            mask = size - 1;
            hashes = new int[size];
            keys = new SHM.Coordinate[size];
            values = new Object[size];
        }

        @Override
        public T get(final SHM.Coordinate coordinate) {
            final int hash = SHM.innerHash(coordinate, level);
            final int pos = hash & mask;
            if (hashes[pos] == hash) {
                final SHM.Coordinate key = keys[pos];
                if (key != null && SHM.innerEquals(coordinate, key, level)) {
                    return (T) values[pos];
                }
            }
            final T val = delegate.get(coordinate);
            keys[pos] = coordinate;
            values[pos] = val;
            hashes[pos] = hash;
            return val;
        }
    }
}
