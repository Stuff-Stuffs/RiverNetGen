package io.github.stuff_stuffs.river_net_gen.layer;

import io.github.stuff_stuffs.river_net_gen.util.Hex;
import io.github.stuff_stuffs.river_net_gen.util.SHM;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;

import java.util.EnumMap;
import java.util.Map;

public final class RiverLayer {
    public static Layer.Basic<PlateType> base(final int seed, final int level) {
        final SHM shm = new SHM(512);
        final Hash.Strategy<SHM.Coordinate> strategy = SHM.outerStrategy(level);
        return new Layer.Basic<>(coordinate -> {
            final int s = HashCommon.murmurHash3(strategy.hashCode(shm.fromHex(coordinate)) ^ seed);
            return (s & 3) == 0 ? PlateType.CONTINENT : PlateType.OCEAN;
        });
    }

    public static Layer.Basic<PlateType> expand(final int level, final Layer<PlateType> prev) {
        final SHM shm = new SHM(512);
        final Hash.Strategy<SHM.Coordinate> strategy = SHM.outerStrategy(level+1);
        final Map<Hex.Direction, SHM.Coordinate> offsets = new EnumMap<>(Hex.Direction.class);
        for (final Hex.Direction direction : Hex.Direction.values()) {
            offsets.put(direction, SHM.offset(direction, level));
        }
        return new Layer.Basic<>(coordinate -> {
            if (prev.get(coordinate) == PlateType.CONTINENT) {
                return PlateType.CONTINENT;
            }
            final SHM.Coordinate center = shm.fromHex(coordinate);
            int landCount = 0;
            for (final SHM.Coordinate value : offsets.values()) {
                if (prev.get(shm.toHex(shm.add(center, value))) == PlateType.CONTINENT) {
                    landCount++;
                }
            }
            if (landCount < 2) {
                return PlateType.OCEAN;
            }
            if (landCount == 2) {
                return (coordinate.hashCode() & 3) == 0 ? PlateType.CONTINENT : PlateType.OCEAN;
            }
            return PlateType.CONTINENT;
        });
    }

    private RiverLayer() {
    }
}
