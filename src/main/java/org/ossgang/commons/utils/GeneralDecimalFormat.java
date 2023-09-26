package org.ossgang.commons.utils;

import static java.lang.Math.pow;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * A Decimal Format that behaves similar to the format string "%g" ("general"): if the number is representable with
 * a small number of digits, using fixed-point formatting, use fixed point. If it is too small or too big, fall back to
 * scientific format.
 */
public class GeneralDecimalFormat extends NumberFormat {
    private final DecimalFormat fixedFormat;
    private final DecimalFormat exponentialFormat;
    private final double lowExponentialThreshold;
    private final double highExponentialThreshold;

    public GeneralDecimalFormat(DecimalFormat fixedFormat, DecimalFormat exponentialFormat,
            double lowExponentialThreshold, double highExponentialThreshold) {
        if (lowExponentialThreshold <= 0 || highExponentialThreshold <= 0) {
            throw new IllegalArgumentException("Thresholds must be > 0!");
        }
        this.fixedFormat = fixedFormat;
        this.exponentialFormat = exponentialFormat;
        this.lowExponentialThreshold = lowExponentialThreshold;
        this.highExponentialThreshold = highExponentialThreshold;
    }

    public GeneralDecimalFormat(String pattern, double lowExponentialThreshold, double highExponentialThreshold) {
        this(new DecimalFormat(pattern), new DecimalFormat(pattern + "E0"), lowExponentialThreshold,
                highExponentialThreshold);
    }

    public GeneralDecimalFormat(int integerDigits, int fractionDigits) {
        this(buildDefaultPattern(fractionDigits), pow(10, -fractionDigits), pow(10, integerDigits));
    }

    public GeneralDecimalFormat() {
        this(6, 3);
    }

    private static String buildDefaultPattern(int numberOfDigits) {
        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < numberOfDigits; i++) {
            pattern.append('#');
        }
        return pattern.toString();
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        double val = Math.abs(number);
        if (val >= highExponentialThreshold || (val <= lowExponentialThreshold && val > 0.0)) {
            return exponentialFormat.format(number, toAppendTo, pos);
        } else {
            return fixedFormat.format(number, toAppendTo, pos);
        }
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        long val = Math.abs(number);
        if (val >= highExponentialThreshold) {
            return exponentialFormat.format(number, toAppendTo, pos);
        } else {
            return fixedFormat.format(number, toAppendTo, pos);
        }
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        return exponentialFormat.parse(source, parsePosition);
    }
}
