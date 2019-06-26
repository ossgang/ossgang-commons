package io.github.ossgang.commons.observable;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class SimplePropertyTest {

    private static final String ANY_STRING = "foo";
    private static final String ANY_OTHER_STRING = "bar";
    private static final int ANY_INT = 42;

    @Test
    public void setUpdatesValueAndSubscriptions() throws Exception {
        CompletableFuture<String> updateValue = new CompletableFuture<>();

        Property<String> property = Properties.property();
        property.subscribe(updateValue::complete);
        property.set(ANY_STRING);

        assertThat(property.get()).isEqualTo(ANY_STRING);
        assertThat(updateValue.get(1, SECONDS)).isEqualTo(ANY_STRING);
    }

}