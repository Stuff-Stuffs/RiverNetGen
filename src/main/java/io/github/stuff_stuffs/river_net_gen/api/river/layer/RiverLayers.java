package io.github.stuff_stuffs.river_net_gen.api.river.layer;

import io.github.stuff_stuffs.river_net_gen.api.river.layer.data.*;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.NeighbourhoodFactory;
import io.github.stuff_stuffs.river_net_gen.api.river.neighbour.base.*;
import io.github.stuff_stuffs.river_net_gen.api.util.GenUtil;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class RiverLayers<NNode extends Node, WNode extends WalkerNode, PTree extends PathTree, PRes extends PathResult, CData extends CachedExpandData, RData extends RiverData> {
    public static final double SQRT3_3 = Math.sqrt(3.0) / 3.0;
    public static final Hex.Direction[] DIRECTIONS = Hex.Direction.values();
    public final NeighbourhoodPredicate<PlateType> ENCLAVE_DESTROYER;
    public final NeighbourChooser<PlateType> OUTGOING_BASE;
    public final NeighbourChooser<RData> OUTGOING_GROW;
    public final NeighbourWeightedWalker<PRes, PTree, Void, CData> SPLIT_RIVERS;
    public final NeighbourhoodWalker<WNode, WNode, NNode, RData> FILL_DATA;
    public final NeighbourhoodWalker<RData, WNode, WNode, RData> FLOW_FILL;
    public final NeighbourCounter<RData, Void> LAND_ADJACENT_COUNTER;
    public final NeighbourChooser<RData> COASTLINE_GROW;
    private final Object[] cacheData;
    private final RiverLayerConfig.RDataBaseFactory<RData> baseOceanFactory;
    private final RiverLayerConfig.RDataBaseFactory<RData> baseContinentFactory;

    public RiverLayers(final RiverLayerConfig<NNode, WNode, PTree, PRes, CData, RData> config) {
        ENCLAVE_DESTROYER = config.enclaveDestroyerFactory().apply(config);
        OUTGOING_BASE = config.outgoingBaseFactory().apply(config);
        OUTGOING_GROW = config.outgoingGrowFactory().apply(config);
        SPLIT_RIVERS = config.splitRiversFactory().apply(config);
        FILL_DATA = config.fillDataFactory().apply(config);
        FLOW_FILL = config.flowFillFactory().apply(config);
        LAND_ADJACENT_COUNTER = config.landAdjacentCounterFactory().apply(config);
        COASTLINE_GROW = config.coastlineGrowFactory().apply(config);
        cacheData = new Object[config.levelCount()];
        for (int i = 0; i < cacheData.length; i++) {
            cacheData[i] = config.cacheDataFactory().apply(i);
        }
        baseOceanFactory = config.baseOceanFactory();
        baseContinentFactory = config.baseContinentFactory();
    }

    public Layer.Basic<PlateType> base(final int seed, final int level) {
        final SHM.LevelCache cache = SHM.createCache(level);
        final Hash.Strategy<SHM.Coordinate> strategy = cache.outer();
        return new Layer.Basic<>(coordinate -> plateOfCoordinate(coordinate, seed, strategy));
    }

    public Layer.Basic<PlateType> destroyEnclaves(final int level, final Layer<PlateType> prev) {
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        return new Layer.Basic<>(coordinate -> {
            final PlateType type = prev.get(coordinate);
            if (type == PlateType.CONTINENT) {
                if (ENCLAVE_DESTROYER.test(factory.build(coordinate, prev))) {
                    return PlateType.CONTINENT;
                } else {
                    return PlateType.OCEAN;
                }
            }
            return PlateType.OCEAN;
        });
    }

    private PlateType plateOfCoordinate(final SHM.Coordinate coordinate, final int seed, final Hash.Strategy<SHM.Coordinate> strategy) {
        return (HashCommon.mix(strategy.hashCode(coordinate) ^ seed) & 31) < 14 ? PlateType.CONTINENT : PlateType.OCEAN;
    }

    public Layer.Basic<RData> riverBase(final int seed, final int level, final Layer<PlateType> prev) {
        final SHM.LevelCache cache = SHM.createCache(level);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        final SHM.MutableCoordinate mutable = SHM.createMutable();
        final Hash.Strategy<SHM.Coordinate> outer = cache.outer();
        return new Layer.Basic<>(coordinate -> {
            final PlateType type = prev.get(coordinate);
            if (type == PlateType.OCEAN) {
                final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
                double flowSum = 0;
                for (final Hex.Direction direction : DIRECTIONS) {
                    SHM.addMutable(coordinate, cache.offset(direction), mutable);
                    final Hex.Direction adjacentOutgoing = outgoingBase(mutable, factory.build(mutable, prev), seed, cache);
                    if (adjacentOutgoing != null && adjacentOutgoing.opposite() == direction) {
                        final double flowBase = flowBase(mutable, seed, outer);
                        incoming.put(direction, new RiverData.Incoming(1, 1, flowBase));
                        flowSum = flowSum + flowBase;
                    }
                }
                return baseOceanFactory.create(incoming, null, 0, flowSum, 1, level, -1, prev, seed);
            }
            final Hex.@Nullable Direction chosen = outgoingBase(coordinate, factory.build(coordinate, prev), seed, cache);
            return baseContinentFactory.create(Collections.emptyMap(), chosen, 1, flowBase(coordinate, seed, outer), 1, level, 1, prev, seed);
        });
    }

    private double flowBase(final SHM.Coordinate coordinate, final int seed, final Hash.Strategy<SHM.Coordinate> strategy) {
        final int hashCode = strategy.hashCode(coordinate);
        final int start = HashCommon.mix(seed ^ hashCode) ^ HashCommon.murmurHash3(hashCode);
        final long data = HashCommon.murmurHash3(HashCommon.mix((long) start | (((long) start) << 32L)) + 123456);
        return 4 + 4 * GenUtil.randomDoubleFromLong(data);
    }

    private double flowCoastline(final SHM.Coordinate coordinate, final int seed, final Hash.Strategy<SHM.Coordinate> strategy) {
        final int hashCode = strategy.hashCode(coordinate);
        final int start = HashCommon.mix(seed ^ hashCode) ^ HashCommon.murmurHash3(hashCode);
        final long data = HashCommon.murmurHash3(HashCommon.mix((long) start | (((long) start) << 32L)) + 123456);
        return 0.1 + 0.05 * GenUtil.randomDoubleFromLong(data);
    }

    public Layer.Basic<RData> growBase(final int seed, final int level, final Layer<RData> prev) {
        final SHM.LevelCache cache = SHM.createCache(level);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        return new Layer.Basic<>(coordinate -> {
            final RData data = prev.get(coordinate);
            if (data.type() == PlateType.OCEAN || data.outgoing()!=null) {
                return data;
            }
            final Neighbourhood<RData> neighbourhood = factory.build(coordinate, prev);
            final Hex.@Nullable Direction chosen = outgoingGrow(coordinate, neighbourhood, cache.outer(), seed);
            if (chosen != null) {
                final RiverData neighbourData = neighbourhood.get(neighbourhood.center(), chosen);
                return new RiverData(PlateType.CONTINENT, Collections.emptyMap(), chosen, neighbourData.height() + 1, flowBase(coordinate, seed, cache.outer()), 1, level, 1);
            }
            return data;
        });
    }

    public Layer.Basic<RiverData> propagateBase(final int seed, final int level, final Layer<RiverData> prev) {
        final SHM.LevelCache cache = SHM.createCache(level);
        final SHM.MutableCoordinate mutable = SHM.createMutable();
        return new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            double base = 0;
            double tiles = 1;
            if (data.incoming().isEmpty()) {
                return data;
            }
            if (data.type() == PlateType.CONTINENT) {
                base = flowBase(coordinate, seed, cache.outer());
            }
            final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
            for (final Map.Entry<Hex.Direction, RiverData.Incoming> entry : data.incoming().entrySet()) {
                SHM.addMutable(coordinate, cache.offset(entry.getKey()), mutable);
                final RiverData riverData = prev.get(mutable);
                incoming.put(entry.getKey(), new RiverData.Incoming(riverData.height(), riverData.tiles(), riverData.flowRate()));
                base = base + riverData.flowRate();
                tiles = tiles + riverData.tiles();
            }
            if (tiles != data.tiles()) {
                return new RiverData(data.type(), incoming, data.outgoing(), data.height(), base, tiles, level, data.rainfall());
            }
            return data;
        });
    }

    public Layer.Basic<RiverData> zoom(final int level, final int seed, final Layer<RiverData> prev) {
        final SHM.LevelCache cache = SHM.createCache(level);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        final CachedExpandData cachedExpandData = new CachedExpandData(level);
        final Layer<SubRiverData> dataLayer = new Layer.CachingOuter<>(new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            return zoomInternal(coordinate, data, level, cache, seed, factory, cachedExpandData);
        }), 8, level + 1);
        return new Layer.Basic<>(coordinate -> dataLayer.get(coordinate).data[coordinate.get(level)]);
    }

    private SubRiverData zoomInternal(final SHM.Coordinate coordinate, final RiverData parentData, final int level, final SHM.LevelCache cache, final int seed, final NeighbourhoodFactory factory, final CachedExpandData cachedExpandData) {
        if (parentData.type() == PlateType.OCEAN) {
            if ((parentData.flowRate() - cachedExpandData.maxFlowRate) > 0) {
                return new SubRiverData(new RiverData[]{parentData, parentData, parentData, parentData, parentData, parentData, parentData});
            }
            if (parentData.incoming().isEmpty()) {
                final RiverData lowered = new RiverData(PlateType.OCEAN, Collections.emptyMap(), null, 0, 0, 1, level, 0);
                return new SubRiverData(new RiverData[]{lowered, lowered, lowered, lowered, lowered, lowered, lowered});
            }
            final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
            final RiverData[] dataArr = new RiverData[7];
            final RiverData empty = new RiverData(PlateType.OCEAN, Collections.emptyMap(), null, 0, 0, 0, level, -1);
            dataArr[truncated.get(level)] = empty;
            for (final Hex.Direction direction : DIRECTIONS) {
                final int offsetIndex = SHM.offsetPartial(truncated, level, direction.rotateC());
                if (parentData.incoming().containsKey(direction)) {
                    final RiverData.Incoming incoming = parentData.incoming().get(direction);
                    final RiverData riverData = new RiverData(PlateType.OCEAN, Map.of(direction, incoming), null, 0, incoming.flowRate(), 1 + incoming.tiles() * 7, level, -1);
                    dataArr[offsetIndex] = riverData;
                } else {
                    dataArr[offsetIndex] = empty;
                }
            }
            return new SubRiverData(dataArr);
        }
        if (parentData.outgoing() == null || (parentData.flowRate() - cachedExpandData.maxFlowRate) > -0.0001) {
            return new SubRiverData(new RiverData[]{parentData, parentData, parentData, parentData, parentData, parentData, parentData});
        }
        cachedExpandData.reset();
        final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
        final int outerHash = SHM.outerHash(truncated, level + 1);
        cachedExpandData.splitSeed = HashCommon.mix(outerHash + 4321);
        cachedExpandData.data = parentData;
        cachedExpandData.nodeGetter.nodes = SPLIT_RIVERS.walk(factory.build(truncated, i -> null), cachedExpandData, seed ^ outerHash).nodes;
        final NeighbourhoodWalker.Result<WalkerNode> walk = FILL_DATA.walk(factory.build(truncated, cachedExpandData.nodeGetter), parentData);
        cachedExpandData.walkerNodeGetter.nodes = walk.raw();
        final NeighbourhoodWalker.Result<RiverData> flowWalk = FLOW_FILL.walk(factory.build(truncated, cachedExpandData.walkerNodeGetter), parentData);
        return new SubRiverData(flowWalk.raw());
    }

    public Layer.Basic<RiverData> coastlineGrow(final int level, final int seed, final Layer<RiverData> prev) {
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        final SHM.LevelCache levelCache = SHM.createCache(level);
        final SHM.MutableCoordinate mutable = SHM.createMutable();
        return new Layer.Basic<>(coordinate -> {
            final RiverData parentData = prev.get(coordinate);
            if (parentData.type() == PlateType.OCEAN) {
                if (!parentData.incoming().isEmpty()) {
                    return parentData;
                }
                final int mixedSeed = HashCommon.murmurHash3(SHM.outerHash(coordinate, level) + seed);
                final Neighbourhood<RiverData> neighbourhood = factory.build(coordinate, prev);
                final int growChance = (7 - LAND_ADJACENT_COUNTER.count(neighbourhood, null)) / 2 + 1;
                if (mixedSeed % growChance != 0) {
                    return parentData;
                }
                final Hex.@Nullable Direction chosen = COASTLINE_GROW.choose(neighbourhood, mixedSeed);
                if (chosen == null) {
                    return parentData;
                }
                SHM.addMutable(coordinate, levelCache.offset(chosen), mutable);
                final RiverData downStream = prev.get(mutable);
                return new RiverData(PlateType.CONTINENT, Collections.emptyMap(), chosen, downStream.height() + 1, Math.min(downStream.flowRate(), flowCoastline(coordinate, seed, levelCache.outer())), 1, level, 0);
            }
            if (parentData.outgoing() == null) {
                return parentData;
            }
            Map<Hex.Direction, RiverData.Incoming> incoming = null;
            double tiles = 0;
            for (final Hex.Direction direction : DIRECTIONS) {
                if (parentData.outgoing() == direction || parentData.incoming().containsKey(direction)) {
                    continue;
                }
                SHM.addMutable(coordinate, levelCache.offset(direction), mutable);
                final RiverData maybeUpStream = prev.get(mutable);
                if (maybeUpStream.type() == PlateType.CONTINENT || !maybeUpStream.incoming().isEmpty()) {
                    continue;
                }
                final Neighbourhood<RiverData> neighbourhood = factory.build(mutable, prev);
                final int growChance = (7 - LAND_ADJACENT_COUNTER.count(neighbourhood, null)) / 2 + 1;
                final int mixedSeed = HashCommon.murmurHash3(SHM.outerHash(mutable, level) + seed);
                if (mixedSeed % growChance != 0) {
                    continue;
                }
                final Hex.Direction chosen = COASTLINE_GROW.choose(neighbourhood, mixedSeed);
                if (direction.opposite() == chosen) {
                    if (incoming == null) {
                        incoming = new EnumMap<>(Hex.Direction.class);
                    }
                    tiles = tiles + 1;
                    incoming.put(direction, new RiverData.Incoming(parentData.height() + 1, 1, Math.min(parentData.flowRate(), flowCoastline(mutable, seed, levelCache.outer()))));
                }
            }
            if (incoming == null) {
                return parentData;
            }
            incoming.putAll(parentData.incoming());
            return new RiverData(PlateType.CONTINENT, incoming, parentData.outgoing(), parentData.height(), parentData.flowRate(), parentData.tiles() + tiles, level, parentData.rainfall());
        });
    }

    public final class NodeGetter implements Function<SHM.Coordinate, NNode> {
        public final int level;
        public Node[] nodes;

        public NodeGetter(final int level) {
            this.level = level;
        }

        @Override
        public NNode apply(final SHM.Coordinate coordinate) {
            return (NNode) nodes[coordinate.get(level)];
        }
    }

    public static final class WalkerNodeGetter implements Function<SHM.Coordinate, WalkerNode> {
        public final int level;
        public WalkerNode[] nodes;

        public WalkerNodeGetter(final int level) {
            this.level = level;
        }

        @Override
        public WalkerNode apply(final SHM.Coordinate coordinate) {
            return nodes[coordinate.get(level)];
        }
    }


    private Hex.@Nullable Direction outgoingBase(final SHM.Coordinate coordinate, final Neighbourhood<PlateType> neighbourhood, final int seed, final SHM.LevelCache cache) {
        return OUTGOING_BASE.choose(neighbourhood, seed + cache.outer().hashCode(coordinate));
    }

    private Hex.@Nullable Direction outgoingGrow(final SHM.Coordinate coordinate, final Neighbourhood<RData> neighbourhood, final Hash.Strategy<SHM.Coordinate> strategy, final int seed) {
        return OUTGOING_GROW.choose(neighbourhood, strategy.hashCode(coordinate) + seed);
    }

    public record SubRiverData(RiverData[] data) {
    }
}
