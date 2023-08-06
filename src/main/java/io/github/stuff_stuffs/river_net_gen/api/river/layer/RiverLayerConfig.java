package io.github.stuff_stuffs.river_net_gen.api.river.layer;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.*;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.*;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

public record RiverLayerConfig<NNode extends Node, WNode extends WalkerNode, PTree extends PathTree, PRes extends PathResult, CData extends CachedExpandData, RData extends RiverData>(
        int continentChanceNumerator, int continentChanceDenominatorLog2, double flowBaseAddend,
        double flowBaseMultiplier, double flowGrowAddend, double flowGrowMultiplier,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourhoodPredicate<PlateType>> enclaveDestroyerFactory,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourChooser<PlateType>> outgoingBaseFactory,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourChooser<RData>> outgoingGrowFactory,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourWeightedWalker<PRes, PTree, Void, CData>> splitRiversFactory,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourhoodWalker<WNode, WNode, NNode, RData>> fillDataFactory,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourhoodWalker<RData, WNode, WNode, RData>> flowFillFactory,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourCounter<RData, Void>> landAdjacentCounterFactory,
        Function<? super RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData>, ? extends NeighbourChooser<RData>> coastlineGrowFactory,
        IntFunction<CData> cacheDataFactory,
        RDataBaseFactory<RData> baseOceanFactory,
        RDataBaseFactory<RData> baseContinentFactory,
        RDataGrowFactory<RData>
        int levelCount
) {

    public interface RDataBaseFactory<RData extends RiverData> {
        RData create(Map<Hex.Direction, RiverData.Incoming> incoming, @Nullable Hex.Direction outgoing,
                     double height, double flowRate, double tiles, int level, double rainfall, Layer<PlateType> prev, int seed);
    }

    public interface RDataGrowFactory<RData extends RiverData> {
        RData create(Map<Hex.Direction, RiverData.Incoming> incoming, @Nullable Hex.Direction outgoing,
                     double height, double flowRate, double tiles, int level, double rainfall, Layer<RData> prev, int seed);
    }
}
