package io.github.stuff_stuffs.river_net_gen.layer;

import io.github.stuff_stuffs.river_net_gen.util.Hex;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.jetbrains.annotations.Nullable;

public record RiverData(PlateType type, Object2DoubleMap<Hex.Direction> incoming, @Nullable Hex.Direction outgoing,
                        double height) {
    public RiverData {
        if (outgoing != null && incoming.containsKey(outgoing)) {
            throw new IllegalArgumentException();
        }
        if (type == PlateType.CONTINENT) {
            if (height == 0) {
                throw new IllegalArgumentException();
            }
            if (!incoming.isEmpty() && outgoing == null) {
                throw new IllegalArgumentException();
            }
        } else {
            if (height != 0) {
                throw new IllegalArgumentException();
            }
            if (outgoing != null) {
                throw new IllegalArgumentException();
            }
        }
    }
}
