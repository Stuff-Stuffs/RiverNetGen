package io.github.stuff_stuffs.river_net_gen.river.neighbour;

import io.github.stuff_stuffs.river_net_gen.util.Hex;
import io.github.stuff_stuffs.river_net_gen.util.SHM;
import org.jetbrains.annotations.Nullable;

public interface Neighbourhood<T> {
    int center();

    int centerHash();

    T get(int s);

    default T get(final int s, final Hex.Direction offset) {
        return get(offset(s, offset));
    }

    @Nullable Hex.Direction from(int s);

    int offset(int s, Hex.Direction offset);

    boolean inCenterCluster(int s);

    SHM.Coordinate toGlobal(int s);
}
