package org.ossgang.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;

public class GeneralDecimalFormatTest {

    private GeneralDecimalFormat format = new GeneralDecimalFormat(6, 4);

    @org.junit.Test
    public void format_normalDouble_shouldFormatFixed() {
        assertThat(format.format(42.0)).isEqualTo("42");
        assertThat(format.format(42.42)).isEqualTo("42.42");
        assertThat(format.format(0.42)).isEqualTo("0.42");
        assertThat(format.format(0.0)).isEqualTo("0");
        assertThat(format.format(999999.9)).isEqualTo("999999.9");
    }

    @org.junit.Test
    public void format_normalLong_shouldFormatFixed() {
        assertThat(format.format(0)).isEqualTo("0");
        assertThat(format.format(1)).isEqualTo("1");
        assertThat(format.format(42)).isEqualTo("42");
        assertThat(format.format(42000)).isEqualTo("42000");
        assertThat(format.format(999999)).isEqualTo("999999");
    }

    @org.junit.Test
    public void format_tinyDouble_shouldFormatExponential() {
        assertThat(format.format(0.0001)).isEqualTo("1E-4");
        assertThat(format.format(0.00003)).isEqualTo("3E-5");
        assertThat(format.format(42e-9)).isEqualTo("4.2E-8");
        assertThat(format.format(1e-42)).isEqualTo("1E-42");
    }

    @org.junit.Test
    public void format_bigDouble_shouldFormatExponential() {
        assertThat(format.format(1000000.0)).isEqualTo("1E6");
        assertThat(format.format(4242424.42)).isEqualTo("4.2424E6");
    }

    @org.junit.Test
    public void format_bigLong_shouldFormatExponential() {
        assertThat(format.format(1000000)).isEqualTo("1E6");
        assertThat(format.format(4242424)).isEqualTo("4.2424E6");
        assertThat(format.format(123000000)).isEqualTo("1.23E8");
    }


    @org.junit.Test
    public void format_customOptions_shouldFormatAccordingly() {
        assertThat(new GeneralDecimalFormat(1, 1).format(42.42)).isEqualTo("4.2E1");
        assertThat(new GeneralDecimalFormat(11, 1).format(42000000000.0)).isEqualTo("42000000000");
        assertThat(new GeneralDecimalFormat(4, 4).format(9999.9999)).isEqualTo("9999.9999");
    }

}