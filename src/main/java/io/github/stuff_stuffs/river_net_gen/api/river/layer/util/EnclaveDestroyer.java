package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.PlateType;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourhoodPredicate;

public class EnclaveDestroyer extends NeighbourhoodPredicate<PlateType> {
    public EnclaveDestroyer() {
        super(Mode.OR, false);
    }

    @Override
    protected boolean testElement(final PlateType val, final Neighbourhood<PlateType> neighbourhood, final int s) {
        return val == PlateType.OCEAN;
    }
}
