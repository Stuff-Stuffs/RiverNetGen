package io.github.stuff_stuffs.river_net_gen.api.util;

import io.github.stuff_stuffs.river_net_gen.impl.util.SHMImpl;
import it.unimi.dsi.fastutil.Hash;

public interface SHM {
    Coordinate fromHex(Hex.Coordinate coordinate);

    Coordinate fromHex(Hex.Coordinate coordinate, int level);

    Hex.Coordinate toHex(Coordinate coordinate);

    Coordinate add(Coordinate first, Coordinate second);

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

    static int outerHash(final Coordinate truncated, final int level) {
        return SHMImpl.outerHash(truncated, level);
    }

    static boolean outerEquals(final Coordinate first, final Coordinate second, final int level) {
        return SHMImpl.outerEquals(first, second, level);
    }

    interface Coordinate {
        int level();

        byte get(int level);
    }

    interface LevelCache {
        int level();

        SHM.Coordinate offset(Hex.Direction direction);

        Hash.Strategy<Coordinate> inner();

        Hash.Strategy<Coordinate> outer();

        Hash.Strategy<Coordinate> full();
    }
}
