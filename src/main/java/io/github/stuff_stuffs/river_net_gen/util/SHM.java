package io.github.stuff_stuffs.river_net_gen.util;

import io.github.stuff_stuffs.river_net_gen.river.impl.util.SHMImpl;
import it.unimi.dsi.fastutil.Hash;

public interface SHM {
    SHM MAX_LEVEL = SHM.create();

    Coordinate fromHex(Hex.Coordinate coordinate);

    Coordinate fromHex(Hex.Coordinate coordinate, int level);

    Hex.Coordinate toHex(Coordinate coordinate);

    Coordinate add(Coordinate first, Coordinate second);

    void fromHexMutable(Hex.Coordinate coordinate, MutableCoordinate result);

    void fromHexMutable(Hex.Coordinate coordinate, int level, MutableCoordinate result);

    void addMutable(Coordinate first, Coordinate second, MutableCoordinate result);

    int offsetPartial(Coordinate coordinate, int level, Hex.Direction direction);

    static SHM create(final int level) {
        return new SHMImpl(level);
    }

    static SHM create() {
        return new SHMImpl();
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
