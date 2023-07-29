package io.github.stuff_stuffs.river_net_gen.api.util;

public abstract class CachingSpreadGrid<T> extends SpreadGrid<T> {
    private final int cacheSizeLog2;
    private final long[] keys;
    private final Object[] cache;

    public CachingSpreadGrid(final int seed, final int log2) {
        super(seed);
        cacheSizeLog2 = log2;
        keys = new long[1 << cacheSizeLog2];
        cache = new Object[1 << cacheSizeLog2];
    }

    protected abstract T createCacheObject(long seed);

    @Override
    protected final T create(final long seed) {
        final int pos = (int) (seed & (((long) 1 << cacheSizeLog2) - 1));
        if (keys[pos] == seed) {
            final Object o = cache[pos];
            if (o != null) {
                return (T) o;
            }
        }
        final T o = createCacheObject(seed);
        keys[pos] = seed;
        cache[pos] = o;
        return o;
    }
}
