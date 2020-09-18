package org.ossgang.commons.mapbackeds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CustomToStringTest {

    @Test
    public void noValue() {
        AnInterface object = Mapbackeds.builder(AnInterface.class).build();
        assertThat(object.toString()).isEqualTo("null");
    }

    @Test
    public void intValue() {
        AnInterface object = Mapbackeds.builder(AnInterface.class).field(AnInterface::intValue, 3).build();
        assertThat(object.toString()).isEqualTo("3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void moreThanOneToStringThrows() {
        Mapbackeds.builder(MoreThanOneToString.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void subToStringWithSameNameAndSignatureIsDetectedAsTwoMethods() {
        Mapbackeds.builder(SubInterfaceWithDefaultToStringOnSameMethod.class).build();
    }

    @Test
    public void overriddenSubToStringMethodWorks() {
        SubInterfaceWithOverriddenToString object = Mapbackeds.builder(SubInterfaceWithOverriddenToString.class)
                .build();
        assertThat(object.toString()).isEqualTo("sub");
    }

    private interface AnInterface {

        @ToString
        int intValue();
    }

    private interface MoreThanOneToString extends AnInterface {

        @ToString
        String name();

    }

    private interface InterfaceWithDefaultToString {

        @ToString
        default String name() {
            return "super";
        }
    }

    public interface SubInterfaceWithDefaultToStringOnSameMethod extends InterfaceWithDefaultToString {
        @Override
        @ToString
        default String name() {
            return "sub";
        }
    }

    public interface SubInterfaceWithOverriddenToString extends InterfaceWithDefaultToString {
        @Override
        /* Do NOT put the @ToString here, as it is already on the super method. */
        default String name() {
            return "sub";
        }
    }
}
