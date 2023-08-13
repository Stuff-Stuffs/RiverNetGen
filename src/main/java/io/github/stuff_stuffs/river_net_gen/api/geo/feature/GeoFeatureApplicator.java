package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

import io.github.stuff_stuffs.river_net_gen.impl.geo.GeoFeatureApplicatorImpl;

import java.util.Collection;
import java.util.OptionalInt;
import java.util.Set;

public interface GeoFeatureApplicator<T> {
    int apply(int x, int y, int z);

    void setFeatures(Collection<? extends GeoFeature<T>> features);

    Set<T> geoIdentifiers();

    OptionalInt getGeoId(T identifier);

    T get(int geoId);

    interface BaseGeoFeature<T> {
        GeoFeature.Instance setup(GeoFeatureApplicator<T> applicator, GeoFeature.Registry<T> registry);
    }

    static <T> GeoFeatureApplicator<T> create(final BaseGeoFeature<T> base, final int maxDepth) {
        return new GeoFeatureApplicatorImpl<>(base, maxDepth);
    }
}
