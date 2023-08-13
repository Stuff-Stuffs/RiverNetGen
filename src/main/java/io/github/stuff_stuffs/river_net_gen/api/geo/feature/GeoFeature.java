package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

public interface GeoFeature<T> {
    double timeStamp();

    Instance setup(Registry<T> registry);

    interface Instance {
        int apply(Context context);
    }

    interface Context {
        int x();

        int y();

        int z();

        int query(int x, int y, int z);
    }

    interface Registry<T> {
        int getGeoId(T feature);
    }
}
