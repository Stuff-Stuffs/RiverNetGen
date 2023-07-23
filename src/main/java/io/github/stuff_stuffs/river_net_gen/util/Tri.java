package io.github.stuff_stuffs.river_net_gen.util;

import it.unimi.dsi.fastutil.HashCommon;

import java.util.function.Function;

public final class Tri {
    private static final double SQRT3_3 = Math.sqrt(3) / 3.0;
    private static final double SQRT3_6 = Math.sqrt(3) / 6.0;

    private Tri() {
    }

    public static Coordinate fromCartesian(final double x, final double y) {
        return new Coordinate((int) Math.ceil(x - SQRT3_3 * y), (int) Math.floor(2 * SQRT3_3 * y) + 1, (int) Math.ceil(-x - SQRT3_3 * y));
    }

    public static Coordinate[] corners(final Coordinate coordinate) {
        final Coordinate f;
        final Coordinate s;
        final Coordinate t;
        if (coordinate.up()) {
            f = new Coordinate(coordinate.a + 1, coordinate.b, coordinate.c);
            s = new Coordinate(coordinate.a, coordinate.b, coordinate.c + 1);
            t = new Coordinate(coordinate.a, coordinate.b + 1, coordinate.c);
        } else {
            f = new Coordinate(coordinate.a, coordinate.b + 1, coordinate.c + 1);
            s = new Coordinate(coordinate.a + 1, coordinate.b + 1, coordinate.c);
            t = new Coordinate(coordinate.a + 1, coordinate.b, coordinate.c + 1);
        }
        return new Coordinate[]{f, s, t};
    }

    public static Coordinate[] sortedCorners(final Coordinate coordinate) {
        if (coordinate.up()) {
            final Coordinate f = new Coordinate(coordinate.a + 1, coordinate.b, coordinate.c);
            final Coordinate s = new Coordinate(coordinate.a, coordinate.b, coordinate.c + 1);
            final Coordinate t = new Coordinate(coordinate.a, coordinate.b + 1, coordinate.c);
            int fIndex = (coordinate.a * 2 + coordinate.b) % 3;
            if (fIndex < 0) {
                fIndex = fIndex + 3;
            }
            int sIndex = (coordinate.a * 2 + coordinate.b + 1) % 3;
            if (sIndex < 0) {
                sIndex = sIndex + 3;
            }
            int tIndex = (coordinate.a * 2 + coordinate.b + 2) % 3;
            if (tIndex < 0) {
                tIndex = tIndex + 3;
            }
            final Coordinate[] coordinates = new Coordinate[3];
            coordinates[fIndex] = f;
            coordinates[sIndex] = s;
            coordinates[tIndex] = t;
            return coordinates;
        } else {
            int fIndex = (coordinate.a * 2 + coordinate.b + 2) % 3;
            if (fIndex < 0) {
                fIndex = fIndex + 3;
            }
            int sIndex = (coordinate.a * 2 + coordinate.b + 1) % 3;
            if (sIndex < 0) {
                sIndex = sIndex + 3;
            }
            int tIndex = (coordinate.a * 2 + coordinate.b) % 3;
            if (tIndex < 0) {
                tIndex = tIndex + 3;
            }
            final Coordinate f = new Coordinate(coordinate.a, coordinate.b + 1, coordinate.c + 1);
            final Coordinate s = new Coordinate(coordinate.a + 1, coordinate.b + 1, coordinate.c);
            final Coordinate t = new Coordinate(coordinate.a + 1, coordinate.b, coordinate.c + 1);
            final Coordinate[] coordinates = new Coordinate[3];
            coordinates[fIndex] = f;
            coordinates[sIndex] = s;
            coordinates[tIndex] = t;
            return coordinates;
        }
    }

    private static double x(final int a, final int b, final int c) {
        return a * 0.5 - c * 0.5;
    }

    private static double y(final int a, final int b, final int c) {
        return -SQRT3_6 * a + SQRT3_3 * b - SQRT3_6 * c;
    }

    public record Coordinate(int a, int b, int c) {
        public double x() {
            return Tri.x(a, b, c);
        }

        public double y() {
            return Tri.y(a, b, c);
        }

        public boolean up() {
            return (a + b + c) == 2;
        }

        public boolean center() {
            final int sum = a + b + c;
            return sum == 1 | sum == 2;
        }

        public boolean vertex() {
            final int sum = a + b + c;
            return sum == 3;
        }

        @Override
        public int hashCode() {
            return HashCommon.murmurHash3(a + HashCommon.murmurHash3(b + HashCommon.murmurHash3(c)));
        }
    }

    public static final class Cache<T> {
        private final int mask;
        private final Object[] data;
        private final int[] hashes;
        private final Coordinate[] keys;
        private final Function<Coordinate, T> function;

        public Cache(final int sizeLog2, final Function<Coordinate, T> function) {
            mask = (1 << sizeLog2) - 1;
            data = new Object[1 << sizeLog2];
            hashes = new int[1 << sizeLog2];
            keys = new Coordinate[1 << sizeLog2];
            this.function = function;
        }

        public T get(final Coordinate coordinate) {
            final int hash = HashCommon.mix(coordinate.hashCode());
            final int pos = hash & mask;
            if (hashes[pos] == hash) {
                final Coordinate key = keys[pos];
                if (coordinate.equals(key)) {
                    return (T) data[pos];
                }
            }
            final T d = function.apply(coordinate);
            keys[pos] = coordinate;
            data[pos] = d;
            hashes[pos] = hash;
            return d;
        }
    }
}
