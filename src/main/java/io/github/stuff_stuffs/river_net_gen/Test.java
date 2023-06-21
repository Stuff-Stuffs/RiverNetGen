package io.github.stuff_stuffs.river_net_gen;

import io.github.stuff_stuffs.river_net_gen.layer.Layer;
import io.github.stuff_stuffs.river_net_gen.layer.PlateType;
import io.github.stuff_stuffs.river_net_gen.layer.RiverLayer;
import io.github.stuff_stuffs.river_net_gen.util.Hex;
import io.github.stuff_stuffs.river_net_gen.util.ImageOut;

public class Test {
    public static void main(final String[] args) {
        final Layer.Basic<PlateType> base = RiverLayer.base(1, 3);
        final double scale = 1 / 8.0;
        ImageOut.draw((x, y) -> {
            final Hex.Coordinate coordinate = Hex.fromCartesian(x * scale, y * scale);
            return base.get(coordinate) == PlateType.CONTINENT ? 0xFF00 : 0xFF;
        }, 2048, 2048, "test.png");
        Layer.Basic<PlateType> expanded0 = RiverLayer.expand(2, base);
        ImageOut.draw((x, y) -> {
            final Hex.Coordinate coordinate = Hex.fromCartesian(x * scale, y * scale);
            return expanded0.get(coordinate) == PlateType.CONTINENT ? 0xFF00 : 0xFF;
        }, 2048, 2048, "tex0.png");
    }
}
