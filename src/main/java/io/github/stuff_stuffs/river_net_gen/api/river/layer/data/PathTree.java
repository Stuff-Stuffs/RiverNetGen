package io.github.stuff_stuffs.river_net_gen.api.river.layer.data;

import io.github.stuff_stuffs.river_net_gen.api.util.Hex;

public final class PathTree {
    public final int start;
    public final Hex.Direction[] outgoing;
    public final int[] depth;
    public final long[] children;

    public PathTree(final int start) {
        this.start = start;
        outgoing = new Hex.Direction[7];
        depth = new int[7];
        children = new long[7];
    }
}
