package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.PlateType;
import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.RiverData;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourCounter;

public class LandAdjacentCounter extends NeighbourCounter<RiverData, Void> {
    public LandAdjacentCounter() {
        super(false);
    }

    @Override
    protected boolean test(final Neighbourhood<RiverData> neighbourhood, final int tile, final Void context) {
        return neighbourhood.get(tile).type() == PlateType.CONTINENT;
    }
}
