package io.github.stuff_stuffs.river_net_gen.util;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public interface ProceduralRandom {
    int nextInt();

    int nextInt(int bound);

    long nextLong();

    long nextLong(int bound);

    boolean nextBoolean();

    double nextDouble();

    double nextGaussian();

    interface Factory {
        Factory XOROSHIRO = fromGeneratorFactory(RandomGeneratorFactory.of("Xoroshiro128PlusPlus"));

        ProceduralRandom create(long seed);

        static Factory fromGeneratorFactory(final RandomGeneratorFactory<?> factory) {
            return seed -> fromGenerator(factory.create(seed));
        }
    }

    static ProceduralRandom fromGenerator(final RandomGenerator generator) {
        return new ProceduralRandom() {
            @Override
            public int nextInt() {
                return generator.nextInt();
            }

            @Override
            public int nextInt(final int bound) {
                return generator.nextInt(bound);
            }

            @Override
            public long nextLong() {
                return generator.nextLong();
            }

            @Override
            public long nextLong(final int bound) {
                return generator.nextLong(bound);
            }

            @Override
            public boolean nextBoolean() {
                return generator.nextBoolean();
            }

            @Override
            public double nextDouble() {
                return generator.nextDouble();
            }

            @Override
            public double nextGaussian() {
                return generator.nextGaussian();
            }
        };
    }
}
