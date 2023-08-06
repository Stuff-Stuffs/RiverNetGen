package io.github.stuff_stuffs.river_net_gen.api.river.layer.util;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.*;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.NeighbourWeightedWalker;
import io.github.stuff_stuffs.river_net_gen.api.util.GenUtil;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import it.unimi.dsi.fastutil.HashCommon;

public class SplitRivers extends NeighbourWeightedWalker<PathResult, PathTree, Void, CachedExpandData> {
    @Override
    protected PathResult finish(final PathTree tree, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
        final Node[] nodes = data.nodes;
        for (int i = 0; i < 7; i++) {
            nodes[i].depth = tree.depth[i];
            if (tree.start == i) {
                nodes[i].outgoing = data.data.outgoing();
            } else {
                final Hex.Direction direction = tree.outgoing[i];
                nodes[i].outgoing = tree.outgoing[i];
                nodes[neighbourhood.offset(i, direction)].incoming.put(direction.opposite(), tree.children[i]);
            }
            final Hex.Direction edge = neighbourhood.from(i);
            if (edge != null) {
                final RiverData.Incoming incoming = data.data.incoming().get(edge.rotateCC());
                if (incoming != null) {
                    nodes[i].incoming.put(edge.rotateCC(), incoming.tiles() * 7);
                }
            }
        }
        return new PathResult(nodes);
    }

    @Override
    protected PathTree connect(int from, final int to, final Hex.Direction outgoing, final PathTree partialResult, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
        partialResult.depth[to] = partialResult.depth[from] + 1;
        partialResult.outgoing[to] = outgoing;
        while (partialResult.outgoing[from] != null) {
            partialResult.children[from] += 1;
            from = neighbourhood.offset(from, partialResult.outgoing[from]);
        }
        partialResult.children[from] += 1;
        return partialResult;
    }

    @Override
    protected PathTree init(final int start, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
        return new PathTree(start);
    }

    @Override
    protected int start(final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
        return neighbourhood.offset(neighbourhood.center(), data.data.outgoing().rotateC());
    }

    @Override
    protected double weight(final int from, final int to, final Hex.Direction fromDirection, final PathTree tree, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
        final Hex.Direction edge = neighbourhood.from(to);
        double weight = 1;
        if (edge != null) {
            final RiverData.Incoming incoming = data.data.incoming().get(edge.opposite());
            if (incoming != null) {
                weight = weight + weightAngle(fromDirection, edge.opposite());
            }
        }
        if (from != tree.start) {
            weight = weight + weightAngle(tree.outgoing[from], fromDirection.opposite());
        } else {
            weight = weight + weightAngle(data.data.outgoing(), fromDirection.opposite());
        }
        return weight + 70 * GenUtil.randomDoubleFromLong(HashCommon.mix(data.splitSeed ^ HashCommon.mix(seed - 123456789L) ^ HashCommon.mix(from * 63 + 1) ^ HashCommon.mix(to * 127 + 3)));
    }

    private double weightAngle(final Hex.Direction outgoing, final Hex.Direction incoming) {
        if (outgoing == incoming.rotateC() || outgoing == incoming.rotateCC()) {
            return 1;
        }
        if (outgoing == incoming.opposite()) {
            return 100;
        }
        return 50;
    }
}
