package org.ossgang.commons.mapbackeds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.mapbackeds.Mapbackeds.builder;

import java.util.List;

import org.junit.Test;

public class ElementChangeTest {

    @Test(expected = IllegalStateException.class)
    public void nonInitializedThrows() {
        Mapbackeds.builder(AnInterface.class).element(AnInterface::integerList, 2, 5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void toShortListFails() {
        Mapbackeds.builder(AnInterface.class).field(AnInterface::integerList, List.of())
                .element(AnInterface::integerList, 2, 5);
    }

    @Test
    public void sufficientLengthListWorks() {
        AnInterface object = builder(AnInterface.class)//
                .field(AnInterface::integerList, List.of(1, 2, 3)) //
                .element(AnInterface::integerList, 2, 5)//
                .build();

        assertThat(object.integerList()).containsExactly(1, 2, 5);
    }

    @Test
    public void changingMoreThanOneElementIsOk() {
        AnInterface object = builder(AnInterface.class)//
                .field(AnInterface::integerList, List.of(1, 2, 3))//
                .element(AnInterface::integerList, 2, 5)//
                .element(AnInterface::integerList, 0, 6)//
                .build();

        assertThat(object.integerList()).containsExactly(6, 2, 5);
    }

    @Test
    public void changingMorOftenTheSameElementIsNotPreventing() {
        /* This is a bit unfortunate, but probably not worth preventing. The last one counts. */
        AnInterface object = builder(AnInterface.class)//
                .field(AnInterface::integerList, List.of(1, 2, 3))//
                .element(AnInterface::integerList, 2, 5)//
                .element(AnInterface::integerList, 2, 6)//
                .build();

        assertThat(object.integerList()).containsExactly(1, 2, 6);
    }

    private interface AnInterface {
        List<Integer> integerList();
    }
}
