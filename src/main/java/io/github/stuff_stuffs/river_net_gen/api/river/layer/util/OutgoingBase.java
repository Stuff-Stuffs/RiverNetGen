package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.PlateType;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourChooser;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;

public class OutgoingBase extends NeighbourChooser<PlateType> {
    @Override
    protected double weight(PlateType val, PlateType center, Hex.Direction direction, Neighbourhood<PlateType> neighbourhood, long seed) {
        if (center == PlateType.CONTINENT && val == PlateType.OCEAN) {
            return 1;
        }
        return 0;
    }
}
