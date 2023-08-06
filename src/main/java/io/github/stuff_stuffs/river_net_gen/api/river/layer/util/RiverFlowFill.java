package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.RiverData;
import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.WalkerNode;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourhoodWalker;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import org.jetbrains.annotations.Nullable;

public class RiverFlowFill extends NeighbourhoodWalker<RiverData, WalkerNode, WalkerNode, RiverData> {
    public RiverFlowFill() {
        super(RiverData.class, WalkerNode.class);
    }

    @Override
    protected @Nullable WalkerNode start(final int s, final Neighbourhood<WalkerNode> neighbourhood, final RiverData context) {
        final WalkerNode node = neighbourhood.get(s);
        final Hex.Direction outgoing = node.outgoing;
        if (outgoing == null || !neighbourhood.inCenterCluster(neighbourhood.offset(s, outgoing))) {
            node.flowRate = context.flowRate();
            return node;
        }
        return null;
    }

    @Override
    protected @Nullable WalkerNode process(final int s, final WalkerNode[] states, final Neighbourhood<WalkerNode> neighbourhood, final RiverData context) {
        final WalkerNode node = neighbourhood.get(s);
        final int offset = neighbourhood.offset(s, node.outgoing);
        final WalkerNode parent = states[offset];
        if (parent != null) {
            final double overFlow = Math.max(parent.flowRate - parent.requiredFlowRate, 0) * 1.2;
            node.flowRate = node.requiredFlowRate + overFlow * (node.tiles / (parent.tiles));
            return node;
        }
        return null;
    }

    @Override
    protected RiverData finish(final int s, final WalkerNode val, final Neighbourhood<WalkerNode> neighbourhood, final RiverData context) {
        double humidity = val.flowRate;
        for (final RiverData.Incoming incoming : val.incomingHeights.values()) {
            humidity = humidity - incoming.flowRate();
        }
        return new RiverData(context.type(), val.incomingHeights, val.outgoing, val.height, val.flowRate, val.tiles, context.level() - 1, (humidity + context.rainfall() * 4) * 0.2);
    }
}
