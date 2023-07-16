package io.github.stuff_stuffs.river_net_gen.river.neighbour.base;

import io.github.stuff_stuffs.river_net_gen.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.util.Hex;

public abstract class NeighbourhoodPredicate<T> {
    private static final Hex.Direction[] DIRECTIONS = Hex.Direction.values();
    private final Mode mode;
    private final boolean includeCenter;

    public NeighbourhoodPredicate(final Mode mode, final boolean includeCenter) {
        this.mode = mode;
        this.includeCenter = includeCenter;
    }

    protected abstract boolean testElement(T val, Neighbourhood<T> neighbourhood, int s);

    public boolean test(final Neighbourhood<T> neighbourhood) {
        final int center = neighbourhood.center();
        if (includeCenter) {
            final T val = neighbourhood.get(center);
            final boolean state = testElement(val, neighbourhood, center);
            if (state && mode == Mode.OR) {
                return true;
            } else if (!state && mode == Mode.AND) {
                return false;
            }
        }
        for (final Hex.Direction direction : DIRECTIONS) {
            final int offset = neighbourhood.offset(center, direction);
            final T val = neighbourhood.get(offset);
            final boolean state = testElement(val, neighbourhood, offset);
            if (state && mode == Mode.OR) {
                return true;
            } else if (!state && mode == Mode.AND) {
                return false;
            }
        }
        return mode == Mode.AND;
    }

    public enum Mode {
        AND,
        OR
    }
}
