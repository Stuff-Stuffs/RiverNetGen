package io.github.stuff_stuffs.river_net_gen.impl.geo;

import io.github.stuff_stuffs.river_net_gen.api.geo.feature.GeoFeature;
import io.github.stuff_stuffs.river_net_gen.api.geo.feature.GeoFeatureApplicator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterators;

import java.util.*;

public class GeoFeatureApplicatorImpl<T> implements GeoFeatureApplicator<T> {
    private static final Comparator<GeoFeature<?>> OLDEST_FIRST = Comparator.comparingDouble(GeoFeature::timeStamp);
    private final BaseGeoFeature<T> base;
    private final int maxDepth;
    private final GeoFeatureContextImpl root;
    private final GeoFeatureContextImpl[] contexts;
    private final GeoFeature<T>[] featureStack;
    private final GeoFeature.Instance[] instances;
    private int features = 0;
    private GeoFeature.Instance baseInstance;
    private RegistryImpl<T> registry;

    public GeoFeatureApplicatorImpl(final BaseGeoFeature<T> base, final int maxDepth) {
        this.base = base;
        this.maxDepth = maxDepth;
        root = new GeoFeatureContextImpl(this, maxDepth);
        contexts = new GeoFeatureContextImpl[maxDepth];
        for (int i = 0; i < maxDepth; i++) {
            contexts[i] = new GeoFeatureContextImpl(this, i);
        }
        featureStack = new GeoFeature[maxDepth];
        instances = new GeoFeature.Instance[maxDepth];
        registry = new RegistryImpl<>();
        baseInstance = base.setup(this, registry);
    }

    @Override
    public int apply(final int x, final int y, final int z) {
        return query(-1, x, y, z);
    }

    @Override
    public void setFeatures(final Collection<? extends GeoFeature<T>> features) {
        final int unwrap = ObjectIterators.unwrap(features.iterator(), featureStack);
        this.features = unwrap;
        Arrays.sort(featureStack, 0, unwrap, OLDEST_FIRST);
        if (unwrap != features.size()) {
            throw new RuntimeException();
        }
        registry = new RegistryImpl<>();
        baseInstance = base.setup(this, registry);
        for (int i = 0; i < unwrap; i++) {
            instances[i] = featureStack[i].setup(registry);
        }
    }

    @Override
    public Set<T> geoIdentifiers() {
        return Collections.unmodifiableSet(registry.geoIds.keySet());
    }

    @Override
    public OptionalInt getGeoId(final T feature) {
        final int i = registry.geoIds.getInt(feature);
        if (i == -1) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(i);
    }

    @Override
    public T get(final int geoId) {
        return (T) registry.featuresById[geoId];
    }

    private int query(final int index, final int x, final int y, final int z) {
        if (index < maxDepth) {
            final GeoFeatureContextImpl context;
            final GeoFeature.Instance feature;
            if (index + 1 < features) {
                context = contexts[index + 1];
                feature = instances[index + 1];
            } else {
                context = root;
                feature = baseInstance;
            }
            context.x = x;
            context.y = y;
            context.z = z;
            return feature.apply(context);
        } else {
            throw new IllegalStateException();
        }
    }

    private static final class GeoFeatureContextImpl implements GeoFeature.Context {
        private final GeoFeatureApplicatorImpl<?> parent;
        private final int index;
        private int x;
        private int y;
        private int z;

        private GeoFeatureContextImpl(final GeoFeatureApplicatorImpl<?> parent, final int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int z() {
            return z;
        }

        @Override
        public int query(final int x, final int y, final int z) {
            return parent.query(index, x, y, z);
        }
    }

    private static final class RegistryImpl<T> implements GeoFeature.Registry<T> {
        private final Object2IntMap<T> geoIds;
        private Object[] featuresById;
        private int nextId = 0;

        public RegistryImpl() {
            geoIds = new Object2IntOpenHashMap<>();
            geoIds.defaultReturnValue(-1);
            featuresById = new Object[0];
        }

        @Override
        public int getGeoId(final T feature) {
            if (geoIds.containsKey(feature)) {
                return geoIds.getInt(feature);
            }
            final int id = nextId++;
            final Object[] arr = Arrays.copyOf(featuresById, featuresById.length + 1);
            arr[featuresById.length] = feature;
            featuresById = arr;
            geoIds.put(feature, id);
            return id;
        }
    }
}
