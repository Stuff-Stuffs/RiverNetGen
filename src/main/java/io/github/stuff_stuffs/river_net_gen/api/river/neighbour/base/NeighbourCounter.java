package io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base;

import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;

public abstract class NeighbourCounter<T, C> {
    private final boolean includeCenter;

    public NeighbourCounter(boolean includeCenter) {
        this.includeCenter = includeCenter;
    }

    protected abstract boolean test(Neighbourhood<T> neighbourhood, int tile, C context);

    public int count(Neighbourhood<T> neighbourhood, C context) {
        int count = 0;
        for (int i = 1; i < 7; i++) {
            if(test(neighbourhood, i, context)) {
                count++;
            }
        }
        if(includeCenter && test(neighbourhood, 0, context)) {
            count++;
        }
        return count;
    }
}
