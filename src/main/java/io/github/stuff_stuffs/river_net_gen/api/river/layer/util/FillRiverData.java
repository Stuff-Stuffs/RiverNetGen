package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.Node;
import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.RiverData;
import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.WalkerNode;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourhoodWalker;
import io.github.stuff_stuffs.river_net_gen.api.util.GenUtil;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import it.unimi.dsi.fastutil.HashCommon;
import org.jetbrains.annotations.Nullable;

public class FillRiverData extends NeighbourhoodWalker<WalkerNode, WalkerNode, Node, RiverData> {
    public FillRiverData() {
        super(WalkerNode.class, WalkerNode.class);
    }

    @Override
    protected @Nullable WalkerNode start(final int s, final Neighbourhood<Node> neighbourhood, final RiverData context) {
        final Node node = neighbourhood.get(s);
        if (node.incoming.isEmpty()) {
            final WalkerNode walkerNode = new WalkerNode(context.height() + 1, node.depth, context.height() + 1, node.outgoing);
            final int hash = SHM.outerHash(neighbourhood.toGlobal(s), 0);
            walkerNode.tiles = GenUtil.randomDoubleFromLong(HashCommon.murmurHash3(hash | (long) hash << 32)) * 2 * context.rainfall() + 1;
            walkerNode.requiredFlowRate = 0;
            return walkerNode;
        }
        boolean internal = false;
        for (final Hex.Direction direction : node.incoming.keySet()) {
            if (neighbourhood.inCenterCluster(neighbourhood.offset(s, direction))) {
                internal = true;
                break;
            }
        }
        if (!internal) {
            double height = Double.POSITIVE_INFINITY;
            Hex.Direction incoming = null;
            double tiles = 0;
            double requiredFlowRate = 0;
            for (final Hex.Direction direction : node.incoming.keySet()) {
                if (neighbourhood.offset(neighbourhood.center(), direction.rotateC()) == s) {
                    incoming = direction;
                    final RiverData.Incoming neighbourIncoming = context.incoming().get(direction);
                    height = neighbourIncoming.height();
                    tiles = 7 * neighbourIncoming.tiles();
                    requiredFlowRate = neighbourIncoming.flowRate();
                    break;
                }
            }
            final double interpolated = interpolate(node.depth, node.depth, context.height(), height);
            final WalkerNode walkerNode = new WalkerNode(interpolated, node.depth, interpolated, node.outgoing);
            walkerNode.tiles = tiles + 1;
            walkerNode.incomingHeights.put(incoming, new RiverData.Incoming(height, tiles, requiredFlowRate));
            walkerNode.requiredFlowRate = requiredFlowRate;
            return walkerNode;
        }
        return null;
    }

    @Override
    protected @Nullable WalkerNode process(final int s, final WalkerNode[] states, final Neighbourhood<Node> neighbourhood, final RiverData context) {
        double minHeight = Double.POSITIVE_INFINITY;
        int minDepth = Integer.MAX_VALUE;
        boolean edgeAdjacent = false;
        final Node node = neighbourhood.get(s);
        for (final Hex.Direction direction : node.incoming.keySet()) {
            final int n = neighbourhood.offset(s, direction);
            if (neighbourhood.inCenterCluster(n)) {
                final WalkerNode state = states[n];
                if (state != null) {
                    minHeight = Math.min(minHeight, state.minHeightAlongPath);
                    minDepth = Math.min(minDepth, state.minDepthAlongPath);
                } else {
                    return null;
                }
            } else {
                final RiverData.Incoming incoming = context.incoming().get(direction);
                final double interpolated = interpolate(node.depth, node.depth + 1, context.height(), incoming.height());
                minHeight = Math.min(minHeight, interpolated);
                minDepth = Math.min(minDepth, node.depth);
                edgeAdjacent = true;
            }
        }
        final WalkerNode walkerNode;
        if (edgeAdjacent) {
            walkerNode = new WalkerNode(minHeight, minDepth, minHeight, node.outgoing);
        } else {
            final double interpolated = interpolate(node.depth, minDepth, context.height(), minHeight);
            walkerNode = new WalkerNode(minHeight, minDepth, interpolated, node.outgoing);
        }
        double requiredFlowRate = 0;
        double tiles = 1;
        for (final Hex.Direction direction : node.incoming.keySet()) {
            final int n = neighbourhood.offset(s, direction);
            if (neighbourhood.inCenterCluster(n)) {
                final double flowRate = states[n].requiredFlowRate;
                requiredFlowRate = requiredFlowRate + flowRate;
                tiles = tiles + states[n].tiles;
                walkerNode.incomingHeights.put(direction, new RiverData.Incoming(states[n].height, states[n].tiles, flowRate));
            } else {
                final RiverData.Incoming incoming = context.incoming().get(direction);
                final double expandedTiles = incoming.tiles() * 7;
                walkerNode.incomingHeights.put(direction, new RiverData.Incoming(incoming.height(), expandedTiles, incoming.flowRate()));
                requiredFlowRate = requiredFlowRate + incoming.flowRate();
                tiles = tiles + expandedTiles;
            }
        }
        walkerNode.requiredFlowRate = requiredFlowRate;
        walkerNode.tiles = tiles;
        return walkerNode;
    }

    @Override
    protected WalkerNode finish(final int s, final WalkerNode val, final Neighbourhood<Node> neighbourhood, final RiverData context) {
        return val;
    }

    private static double interpolate(final int depth, final int maxDepth, final double height, final double endHeight) {
        return height + (endHeight - height) * (depth / (maxDepth + 1.0D));
    }
}
