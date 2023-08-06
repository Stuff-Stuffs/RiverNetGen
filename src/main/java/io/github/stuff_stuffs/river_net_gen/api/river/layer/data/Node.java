package io.github.stuff_stuffs.river_net_gen.api.river.layer.data;

import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.jetbrains.annotations.Nullable;

public final class Node {
    public Hex.@Nullable Direction outgoing;
    public final Object2DoubleMap<Hex.Direction> incoming = new Object2DoubleOpenHashMap<>();
    public int depth = -1;
}
