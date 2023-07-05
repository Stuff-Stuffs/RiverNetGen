package io.github.stuff_stuffs.river_net_gen.api.neighbour;

import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;

public interface Neighbourhood<T> {
    int center();

    T get(int s);

    default T get(final int s, final Hex.Direction offset) {
        return get(offset(s, offset));
    }

    int offset(int s, Hex.Direction offset);

    boolean inCenterCluster(int s);

    SHM.Coordinate toGlobal(int s);
}
