package io.github.stuff_stuffs.river_net_gen.river.neighbour;

import io.github.stuff_stuffs.river_net_gen.util.SHM;
import io.github.stuff_stuffs.river_net_gen.river.impl.neighbour.NeighbourhoodFactoryImpl;

import java.util.function.Function;

public interface NeighbourhoodFactory {
    <T> Neighbourhood<T> build(SHM.Coordinate coordinate, final Function<SHM.Coordinate, T> layer);

    static NeighbourhoodFactory create(final int level) {
        return new NeighbourhoodFactoryImpl(level);
    }
}
