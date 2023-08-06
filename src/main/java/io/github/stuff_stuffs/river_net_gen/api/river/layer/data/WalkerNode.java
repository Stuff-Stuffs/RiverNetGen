package io.github.stuff_stuffs.river_net_gen.api.river.layer.data;

import io.github.stuff_stuffs.river_net_gen.api.util.Hex;

import java.util.EnumMap;
import java.util.Map;

public final class WalkerNode {
    public final Map<Hex.Direction, RiverData.Incoming> incomingHeights = new EnumMap<>(Hex.Direction.class);
    public final double minHeightAlongPath;
    public final int minDepthAlongPath;
    public final double height;
    public final Hex.Direction outgoing;
    public double tiles;
    public double requiredFlowRate = Double.NaN;
    public double flowRate = Double.NaN;

    public WalkerNode(final double minHeightAlongPath, final int minDepthAlongPath, final double height, final Hex.Direction outgoing) {
        this.minHeightAlongPath = minHeightAlongPath;
        this.minDepthAlongPath = minDepthAlongPath;
        this.height = height;
        this.outgoing = outgoing;
    }
}
