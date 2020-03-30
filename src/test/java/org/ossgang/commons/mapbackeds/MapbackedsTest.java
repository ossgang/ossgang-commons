package org.ossgang.commons.mapbackeds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.mapbackeds.Mapbackeds.builder;

import java.util.Map;

import org.junit.Test;
import org.ossgang.commons.mapbackeds.Mapbackeds;

public class MapbackedsTest {

    private static final int ANY_INT = 5;

    @Test
    public void builderSimplestHappyPath() {
        AnInterface object = builder(AnInterface.class)//
                .field(AnInterface::integerValue, ANY_INT)//
                .build();

        assertThat(object.integerValue()).isEqualTo(ANY_INT);
    }

    @Test
    public void builderWithPrimitives() {
        AnInterface object = builder(AnInterface.class)//
                .field(AnInterface::intValue, ANY_INT)//
                .build();

        assertThat(object.intValue()).isEqualTo(ANY_INT);
    }

    @Test(expected = NullPointerException.class)
    public void primitiveNotSetThrows() {
        AnInterface object = builder(AnInterface.class).build();
        object.intValue();
    }

    @Test
    public void objectValueNotSetReturnsNull() {
        AnInterface object = builder(AnInterface.class).build();
        assertThat(object.integerValue()).isNull();
    }

    @Test
    public void toStringWorks() {
        AnInterface object = builder(AnInterface.class).build();
        System.out.println(object.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void proxyingObject() {
        Mapbackeds.mapOf("Any Non Mapbacked");
    }

    @Test
    public void mapOfBackedObjectIsCorrect() {
        AnInterface object = builder(AnInterface.class).field(AnInterface::intValue, ANY_INT).build();
        Map<String, Object> map = Mapbackeds.mapOf(object);
        assertThat(map).isEqualTo(Map.of("intValue", ANY_INT));
    }

    private interface AnInterface {
        int intValue();

        Integer integerValue();
    }

}
