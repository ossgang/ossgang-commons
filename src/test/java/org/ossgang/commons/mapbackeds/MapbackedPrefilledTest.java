package org.ossgang.commons.mapbackeds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.mapbackeds.Mapbackeds.builder;
import static org.ossgang.commons.mapbackeds.Mapbackeds.mapOf;

import org.junit.Before;
import org.junit.Test;

public class MapbackedPrefilledTest {

    private static final int ORIGINAL_INTEGER = 5;
    private static final int ORIGINAL_INT = 4;
    private static final String ORIGINAL_STRING = "hello";

    private AnInterface first;

    @Before
    public void setUp() {
        first = Mapbackeds.builder(AnInterface.class)//
                .field(AnInterface::intValue, ORIGINAL_INT)//
                .field(AnInterface::integerValue, ORIGINAL_INTEGER)//
                .field(AnInterface::stringValue, ORIGINAL_STRING)//
                .build();
    }

    @Test
    public void overWritingOfFieldWorks() {
        AnInterface second = Mapbackeds.builder(AnInterface.class).from(first)//
                .field(AnInterface::intValue, 7)//
                .build();

        assertThat(second.intValue()).isEqualTo(7);
        assertThat(second.integerValue()).isEqualTo(ORIGINAL_INTEGER);
        assertThat(second.stringValue()).isEqualTo(ORIGINAL_STRING);

        /* Also check the first object to be shure of immutability */
        assertThat(first.intValue()).isEqualTo(ORIGINAL_INT);
        assertThat(first.integerValue()).isEqualTo(ORIGINAL_INTEGER);
        assertThat(first.stringValue()).isEqualTo(ORIGINAL_STRING);
    }

    @Test
    public void overwritingTwiceWorks() {
        AnInterface second = Mapbackeds.builder(AnInterface.class).from(first)//
                .field(AnInterface::intValue, 7)//
                .field(AnInterface::intValue, 8).build();

        /* last one counts */
        assertThat(second.intValue()).isEqualTo(8);
    }

    @Test
    public void initializingSmallerInterfaceWorks() {
        AnotherInterface second = builder(AnotherInterface.class).from(first).build();

        assertThat(second.intValue()).isEqualTo(ORIGINAL_INT);
        assertThat(second.integerValue()).isEqualTo(ORIGINAL_INTEGER);

        assertThat(mapOf(second)).containsOnlyKeys("intValue", "integerValue");
    }

    @Test
    public void initializingOverlappingInterfaceWorks() {
        ThirdInterface second = builder(ThirdInterface.class).from(first).build();

        assertThat(second.intValue()).isEqualTo(ORIGINAL_INT);
        assertThat(second.integerValue()).isEqualTo(ORIGINAL_INTEGER);
        assertThat(second.longValue()).isNull();
    }

    @Test
    public void mismapedInterfaceBuilds() {
        builder(MismatchedInterface.class).from(first).build();
    }

    @Test(expected = ClassCastException.class)
    public void mismapedInterfaceThrowsOnGet() {
        /* This is still annoying and should be fixed at some point (throw already at build time) */
        MismatchedInterface second = builder(MismatchedInterface.class).from(first).build();
        second.stringValue();
    }

    private interface AnInterface {
        int intValue();

        Integer integerValue();

        String stringValue();

    }

    private interface AnotherInterface {
        int intValue();

        Integer integerValue();
    }

    private interface ThirdInterface {
        int intValue();

        Integer integerValue();

        Long longValue();
    }

    private interface MismatchedInterface {
        Long stringValue();
    }

}
