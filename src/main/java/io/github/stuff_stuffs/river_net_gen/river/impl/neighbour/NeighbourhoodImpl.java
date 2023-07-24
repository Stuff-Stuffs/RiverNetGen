package io.github.stuff_stuffs.river_net_gen.river.impl.neighbour;

import io.github.stuff_stuffs.river_net_gen.river.impl.util.SHMImpl;
import io.github.stuff_stuffs.river_net_gen.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.util.Hex;
import io.github.stuff_stuffs.river_net_gen.util.SHM;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class NeighbourhoodImpl<T> implements Neighbourhood<T> {
    private static final int[] PACKED_ADDITION_TABLE;
    private static final Hex.Direction[] FROM = new Hex.Direction[]{Hex.Direction.DOWN_RIGHT, Hex.Direction.DOWN, Hex.Direction.DOWN_LEFT, Hex.Direction.UP_LEFT, Hex.Direction.UP, Hex.Direction.UP_RIGHT};

    static {
        PACKED_ADDITION_TABLE = new int[7 * 7];
        for (int i = 0; i < 7; i++) {
            final SHM.Coordinate first = new SHMImpl.CoordinateImpl(i, (byte) SHMImpl.level(i));
            for (int j = 0; j < 7; j++) {
                final SHM.Coordinate second = new SHMImpl.CoordinateImpl(j, (byte) SHMImpl.level(j));
                final SHM.Coordinate add = SHM.add(first, second);
                if (add.level() > 2) {
                    throw new IllegalStateException();
                }
                PACKED_ADDITION_TABLE[i + 7 * j] = add.get(0) + add.get(1) * 7;
            }
        }
    }

    private final Function<SHM.Coordinate, T> delegate;
    private final SHM.Coordinate[] offsets;
    private final SHM.Coordinate center;
    private final SHM.MutableCoordinate mutable = SHM.createMutable();
    private boolean hasCenterHash = false;
    private int centerHash = 0;

    public NeighbourhoodImpl(final Function<SHM.Coordinate, T> delegate, final SHM.Coordinate[] offsets, final SHM.Coordinate center) {
        this.delegate = delegate;
        this.offsets = offsets;
        this.center = center;
    }

    @Override
    public int center() {
        return 0;
    }

    @Override
    public int centerHash() {
        if(!hasCenterHash) {
            centerHash = SHM.outerHash(center, 0);
            hasCenterHash = true;
        }
        return centerHash;
    }

    @Override
    public T get(final int s) {
        return delegate.apply(toGlobalMut(s));
    }

    @Override
    public Hex.@Nullable Direction from(final int s) {
        if (s <= 0 | s > 7) {
            return null;
        }
        return FROM[s - 1];
    }

    private SHM.Coordinate toGlobalMut(final int s) {
        if (s == 0) {
            return center;
        }
        SHM.addMutable(center, offsets[s], mutable);
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
        return SHM.add(center, offsets[s]);
    }
}
