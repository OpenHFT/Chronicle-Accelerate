package cash.xcl.api.exch;

import net.openhft.chronicle.core.annotation.NotNull;

import java.util.function.Consumer;

import static cash.xcl.util.Validators.notNaN;
import static java.lang.Math.*;

public enum Side {
    BUY {
        @Override
        public @NotNull
        PriceCompareResult compare(double newPrice, double referencePrice, double precision) {
            double delta = notNaN(Math.abs(newPrice - referencePrice));
            if (delta <= precision) {
                return PriceCompareResult.SAME;
            } else if (newPrice > referencePrice) {
                return PriceCompareResult.BETTER;
            } else {
                return PriceCompareResult.WORSE;
            }
        }

        @Override
        public @CouldBeNaN
        double improveBy(@CouldBeNaN double price, @CouldBeNaN double adjustment) {
            return price + adjustment;
        }

        @Override
        public @CouldBeNaN
        double worsenBy(@CouldBeNaN double price, @CouldBeNaN double adjustment) {
            return price - adjustment;
        }

        @Override
        public double getBetter(double price1, double price2) {
            if (price1 >= price2) {
                return price1;
            } else if (price2 > price1) {
                return price2;
            } else {
                // one of them must be NaN
                throw new IllegalArgumentException("NaN value passed to comparision" + price1 + " vs " + price2);
            }

        }

        @Override
        public double getWorse(double price1, double price2) {
            if (price1 <= price2) {
                return price1;
            } else if (price2 < price1) {
                return price2;
            } else {
                // one of them must be NaN
                throw new IllegalArgumentException("NaN value passed to comparision" + price1 + " vs " + price2);
            }
        }

        @Override
        public @CouldBeNaN
        double roundWorse(@CouldBeNaN double value, @CouldBeNaN double tickSize) {
            return tickSize * Math.floor(value / tickSize);
        }

        @Override
        public @CouldBeNaN
        double roundBetter(@CouldBeNaN double value, @CouldBeNaN double tickSize) {
            return tickSize * Math.ceil(value / tickSize);
        }

        @Override
        public Side other() {
            return SELL;
        }

    },
    SELL {
        @Override
        public PriceCompareResult compare(double newPrice, double referencePrice, double precision) {
            return BUY.compare(referencePrice, newPrice, precision);
        }

        @Override
        public @CouldBeNaN
        double improveBy(@CouldBeNaN double price, @CouldBeNaN double adjustment) {
            return BUY.worsenBy(price, adjustment);
        }

        @Override
        public @CouldBeNaN
        double worsenBy(@CouldBeNaN double price, @CouldBeNaN double adjustment) {
            return BUY.improveBy(price, adjustment);
        }

        @Override
        public double getBetter(double price1, double price2) {
            return BUY.getWorse(price1, price2);
        }

        @Override
        public double getWorse(double price1, double price2) {
            return BUY.getBetter(price1, price2);
        }

        @Override
        public @CouldBeNaN
        double roundWorse(@CouldBeNaN double value, @CouldBeNaN double tickSize) {
            return BUY.roundBetter(value, tickSize);
        }

        @Override
        public @CouldBeNaN
        double roundBetter(@CouldBeNaN double value, @CouldBeNaN double tickSize) {
            return BUY.roundWorse(value, tickSize);
        }

        @Override
        public Side other() {
            return BUY;
        }

    };

    static final double DEFAULT_PRECISION_FACTOR = 1E-7;

    @NotNull
    public abstract PriceCompareResult compare(double newPrice, double referencePrice, double precision);

    public boolean isBetterOrSame(double price, double referencePrice, double precision) {
        return compare(price, referencePrice, precision) != PriceCompareResult.WORSE;
    }

    public boolean isBetter(double price, double referencePrice, double precision) {
        return compare(price, referencePrice, precision) == PriceCompareResult.BETTER;
    }

    public boolean isWorse(double price, double referencePrice, double precision) {
        return compare(price, referencePrice, precision) == PriceCompareResult.WORSE;
    }

    public boolean isWorseOrSame(double price, double referencePrice, double precision) {
        return compare(price, referencePrice, precision) != PriceCompareResult.BETTER;
    }

    public boolean isSame(double price, double referencePrice, double precision) {
        return compare(price, referencePrice, precision) == PriceCompareResult.SAME;
    }

    public abstract double getWorse(double price1, double price2);

    public abstract double getBetter(double price1, double price2);

    public abstract double improveBy(double price, double adjustment);

    public abstract double worsenBy(double price, double adjustment);

    public abstract double roundWorse(double value, double precision);

    public abstract double roundBetter(double value, double precision);

    public abstract Side other();

    private static final Side SIDES[] = {BUY, SELL};

    static {
        assert (BUY.ordinal() == 0) && (SELL.ordinal() == 1);
    }

    public static Side fromId(int ordinal) {
        return SIDES[ordinal];
    }

    public static void forEach(Consumer<Side> consumer) {
        consumer.accept(BUY);
        consumer.accept(SELL);
    }

    public static int ticksBetween(double bestPrice, double worstPrice, double tickSize) {
        return (int) round(abs(bestPrice - worstPrice) / tickSize);
    }

    public static double getDefaultPrecision(double tickSize) {
        return nextUp(tickSize * DEFAULT_PRECISION_FACTOR);
    }

    public static enum PriceCompareResult {
        WORSE, SAME, BETTER;
    }


}
