package io.github.stuff_stuffs.river_net_gen.impl.neighbour;

import io.github.stuff_stuffs.river_net_gen.api.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.NeighbourhoodFactory;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import io.github.stuff_stuffs.river_net_gen.impl.util.SHMImpl;

import java.util.function.Function;

public class NeighbourhoodFactoryImpl<T> implements NeighbourhoodFactory<T> {
    private final SHM.Coordinate[] offsets;
    private final int level;

    public NeighbourhoodFactoryImpl(final int level) {
        this.level = level;
        offsets = new SHM.Coordinate[7 * 7];
        final SHM shm = SHM.create();
        for (int i = 0; i < 7; i++) {
            final SHM.Coordinate first = new SHMImpl.CoordinateImpl(i == 0 ? new byte[0] : new byte[]{(byte) i});
            final SHM.Coordinate firstShifted = SHM.shift(first, level);
            for (int j = 0; j < 7; j++) {
                final SHM.Coordinate second = new SHMImpl.CoordinateImpl(j == 0 ? new byte[0] : new byte[]{(byte) j});
                final SHM.Coordinate secondShifted = SHM.shift(second, level);
                offsets[i + 7 * j] = shm.add(firstShifted, secondShifted);
            }
        }
    }

    @Override
    public Neighbourhood<T> build(final SHM.Coordinate coordinate, final Function<SHM.Coordinate, T> layer) {
        return new NeighbourhoodImpl<>(layer, offsets, SHM.outerTruncate(coordinate, level));
    }
}
