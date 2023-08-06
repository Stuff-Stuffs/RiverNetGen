package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.RiverData;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourChooser;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;

public class OutgoingGrow extends NeighbourChooser<RiverData> {
    @Override
    protected double weight(RiverData val, RiverData center, Hex.Direction direction, Neighbourhood<RiverData> neighbourhood, long seed) {
        if (val.outgoing() != null) {
            return 1;
        }
        return 0;
    }
}
