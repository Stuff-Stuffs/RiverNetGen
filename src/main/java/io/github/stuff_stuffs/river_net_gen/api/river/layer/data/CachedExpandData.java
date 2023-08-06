package io.github.stuff_stuffs.river_net_gen.api.river.layer.data;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.RiverLayers;

public final class CachedExpandData {
    public final Node[] nodes = new Node[7];
    public final RiverLayers.NodeGetter nodeGetter;
    public final RiverLayers.WalkerNodeGetter walkerNodeGetter;
    public final double maxFlowRate;
    public int splitSeed;
    public RiverData data;

    public CachedExpandData(final int level) {
        for (int i = 0; i < 7; i++) {
            nodes[i] = new Node();
        }
        nodeGetter = new RiverLayers.NodeGetter(level);
        walkerNodeGetter = new RiverLayers.WalkerNodeGetter(level);
        maxFlowRate = Math.pow(RiverLayers.SQRT3_3 * Math.pow(2.6457513, level), 3);
    }

    public void reset() {
        for (final Node node : nodes) {
            node.outgoing = null;
            node.incoming.clear();
            node.depth = -1;
        }
    }
}
