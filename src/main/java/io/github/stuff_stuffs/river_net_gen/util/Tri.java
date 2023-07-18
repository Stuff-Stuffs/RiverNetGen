package io.github.stuff_stuffs.river_net_gen.util;

import it.unimi.dsi.fastutil.HashCommon;

import java.util.function.Function;

public final class Tri {
    private static final double SQRT3_3 = Math.sqrt(3) / 3.0;
    private static final double SQRT3_6 = Math.sqrt(3) / 6.0;
    private Tri() {
    }

    public static Coordinate fromCartesian(double x, double y) {
        return new Coordinate((int)Math.ceil( 1 * x - SQRT3_3 * y), (int)Math.floor(2 * SQRT3_3 * y) + 1,(int)Math.ceil(-1 * x - SQRT3_3 * y));
    }

    public static Coordinate[] corners(Coordinate coordinate) {
        if (coordinate.up()) {
            return new Coordinate[]{new Coordinate(coordinate.a + 1, coordinate.b, coordinate.c), new Coordinate(coordinate.a, coordinate.b, coordinate.c + 1), new Coordinate(coordinate.a, coordinate.b + 1, coordinate.c)};
        } else {
            return new Coordinate[]{new Coordinate(coordinate.a - 1, coordinate.b, coordinate.c), new Coordinate(coordinate.a, coordinate.b, coordinate.c - 1), new Coordinate(coordinate.a, coordinate.b - 1, coordinate.c)};
        }
    }

    private static double x(int a, int b, int c) {
        return (a-c) * 0.5;
    }

    private static double y(int a, int b, int c) {
        return -SQRT3_6 * a + SQRT3_3 * b - SQRT3_6 * c;
    }

    public record Coordinate(int a, int b, int c) {
        public double x() {
            return Tri.x(a,b,c);
        }

        public double y() {
            return Tri.y(a,b,c);
        }

        public boolean up() {
            return (a+b+c)==2;
        }
    }

    public static final class Cache<T> {
        private final int mask;
        private final Object[] data;
        private final int[] hashes;
        private final Coordinate[] keys;
        private final Function<Coordinate, T> function;

        public Cache(int sizeLog2, Function<Coordinate, T> function) {
            mask = (1<<sizeLog2)-1;
            data = new Object[sizeLog2<<1];
            hashes = new int[sizeLog2<<1];
            keys = new Coordinate[sizeLog2<<1];
            this.function = function;
        }

        public T get(Coordinate coordinate) {
            int hash = HashCommon.mix(coordinate.hashCode());
            int pos = hash & mask;
            if(hashes[pos]==hash) {
                Coordinate key = keys[pos];
                if(coordinate.equals(key)) {
                    return (T) data[pos];
                }
            }
            T d = function.apply(coordinate);
            keys[pos] = coordinate;
            data[pos] = d;
            hashes[pos] = hash;
            return d;
        }
    }
}
