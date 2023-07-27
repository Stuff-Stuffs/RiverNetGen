package io.github.stuff_stuffs.river_net_gen.api.river.neighbour;

import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import io.github.stuff_stuffs.river_net_gen.impl.river.neighbour.NeighbourhoodFactoryImpl;

import java.util.function.Function;

public interface NeighbourhoodFactory {
    <T> Neighbourhood<T> build(SHM.Coordinate coordinate, final Function<SHM.Coordinate, T> layer);

    static NeighbourhoodFactory create(final int level) {
        return new NeighbourhoodFactoryImpl(level);
    }
}
