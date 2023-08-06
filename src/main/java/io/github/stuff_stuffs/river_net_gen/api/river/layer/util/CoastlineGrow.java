package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.RiverData;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourChooser;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;

public class CoastlineGrow extends NeighbourChooser<RiverData> {
    @Override
    protected double weight(final RiverData val, final RiverData center, final Hex.Direction direction, final Neighbourhood<RiverData> neighbourhood, final long seed) {
        return val.outgoing() != null && center.level() == val.level() ? val.tiles() : 0;
    }
}
