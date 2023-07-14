package io.github.stuff_stuffs.river_net_gen.api.layer;

import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record RiverData(PlateType type, Map<Hex.Direction, Incoming> incoming, @Nullable Hex.Direction outgoing,
                        double height, double flowRate, double tiles, int level, double rainfall) {
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

    public record Incoming(double height, double tiles, double flowRate) {
    }
}
