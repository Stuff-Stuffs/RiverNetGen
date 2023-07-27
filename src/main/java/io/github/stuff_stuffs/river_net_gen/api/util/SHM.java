package io.github.stuff_stuffs.river_net_gen.api.util;

import io.github.stuff_stuffs.river_net_gen.impl.util.SHMImpl;
import it.unimi.dsi.fastutil.Hash;

public interface SHM {
    static Coordinate fromHex(Hex.Coordinate coordinate, int level) {
        return SHMImpl.fromHex(coordinate, level);
    }

    static Hex.Coordinate toHex(Coordinate coordinate) {
        return SHMImpl.toHex(coordinate);
    }

    static Coordinate add(Coordinate first, Coordinate second) {
        return SHMImpl.add(first,second);
    }

    static void fromHexMutable(Hex.Coordinate coordinate, int level, MutableCoordinate result) {
        SHMImpl.fromHexMutable(coordinate, level, result);
    }

    static void addMutable(Coordinate first, Coordinate second, MutableCoordinate result) {
        SHMImpl.addMutable(first, second, result);
    }

    static int offsetPartial(Coordinate coordinate, int level, Hex.Direction direction) {
        return SHMImpl.offsetPartial(coordinate, level, direction);
    }

    static LevelCache createCache(final int level) {
        return new SHMImpl.LevelCacheImpl(level);
    }

    static Coordinate offset(final Hex.Direction direction, final int level) {
        return SHMImpl.offset(direction, level);
    }

    static Coordinate outerTruncate(final Coordinate coordinate, final int level) {
        return SHMImpl.outerTruncate(coordinate, level);
    }

    static Coordinate shift(final SHM.Coordinate coordinate, final int shift) {
        return SHMImpl.shift(coordinate, shift);
    }

    static int outerHash(final Coordinate truncated, final int level) {
        return SHMImpl.outerHash(truncated, level);
    }

    static boolean outerEquals(final Coordinate first, final Coordinate second, final int level) {
        return SHMImpl.outerEquals(first, second, level);
    }

    static MutableCoordinate createMutable() {
        return new SHMImpl.MutableCoordinateImpl();
    }

    interface Coordinate {
        int level();

        byte get(int level);

        Coordinate toImmutable();
    }

    interface MutableCoordinate extends Coordinate {
    }

    interface LevelCache {
        int level();

        SHM.Coordinate offset(Hex.Direction direction);

        Hash.Strategy<Coordinate> inner();

        Hash.Strategy<Coordinate> outer();

        Hash.Strategy<Coordinate> full();
    }
}
