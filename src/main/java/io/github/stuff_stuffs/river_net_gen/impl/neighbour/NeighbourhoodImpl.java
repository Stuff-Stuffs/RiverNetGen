package io.github.stuff_stuffs.river_net_gen.impl.neighbour;

import io.github.stuff_stuffs.river_net_gen.api.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import io.github.stuff_stuffs.river_net_gen.impl.util.SHMImpl;

import java.util.function.Function;

public class NeighbourhoodImpl<T> implements Neighbourhood<T> {
    private static final int[] PACKED_ADDITION_TABLE;

    static {
        PACKED_ADDITION_TABLE = new int[7 * 7];
        final SHM shm = SHM.create();
        for (int i = 0; i < 7; i++) {
            final SHM.Coordinate first = new SHMImpl.CoordinateImpl(i, (byte) SHMImpl.level(i));
            for (int j = 0; j < 7; j++) {
                final SHM.Coordinate second = new SHMImpl.CoordinateImpl(j, (byte) SHMImpl.level(j));
                final SHM.Coordinate add = shm.add(first, second);
                if (add.level() > 2) {
                    throw new IllegalStateException();
                }
                PACKED_ADDITION_TABLE[i + 7 * j] = add.get(0) + add.get(1) * 7;
            }
        }
    }

    private final Function<SHM.Coordinate, T> delegate;
    private final SHM.Coordinate[] offsets;
    private final Object[] cache;
    private final SHM shm;
    private final SHM.Coordinate center;
    private final SHM.MutableCoordinate mutable = SHM.createMutable();

    public NeighbourhoodImpl(final Function<SHM.Coordinate, T> delegate, final SHM.Coordinate[] offsets, final SHM.Coordinate center) {
        this.delegate = delegate;
        this.offsets = offsets;
        this.center = center;
        cache = new Object[7 * 7];
        shm = SHM.create();
    }

    @Override
    public int center() {
        return 0;
    }

    @Override
    public T get(final int s) {
        final Object o = cache[s];
        if (o != null) {
            return (T) o;
        }
        final T ret = delegate.apply(toGlobalMut(s));
        cache[s] = ret;
        return ret;
    }

    private SHM.Coordinate toGlobalMut(final int s) {
        if (s == 0) {
            return center;
        }
        shm.addMutable(center, offsets[s], mutable);
        return mutable;
    }

    @Override
    public int offset(final int s, final Hex.Direction offset) {
        if (s > 7) {
            throw new IllegalStateException("Tried to offset outside neighbourhood!");
        }
        final int offsetId = s + SHMImpl.idFromDirection(offset) * 7;
        if (offsetId >= 7 * 7) {
            throw new IllegalStateException("Tried to offset outside neighbourhood!");
        }
        return PACKED_ADDITION_TABLE[offsetId];
    }

    @Override
    public boolean inCenterCluster(final int s) {
        return s < 7;
    }

    @Override
    public SHM.Coordinate toGlobal(final int s) {
        if (s == 0) {
            return center;
        }
        return shm.add(center, offsets[s]);
    }
}
