package org.ossgang.commons.property;

import org.junit.Test;
import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observables.Observers.withErrorHandling;

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

    @Test
    public void transformationTest() throws Exception {
        CompletableFuture<Integer> mappedValueUpdate = new CompletableFuture<>();
        CompletableFuture<Throwable> mappedValueError = new CompletableFuture<>();
        CompletableFuture<String> filteredValueUpdate = new CompletableFuture<>();

        Property<String> property = Properties.property(ANY_STRING);
        ObservableValue<Integer> mappedValue = property.map(Integer::parseInt);
        mappedValue.subscribe(withErrorHandling(mappedValueUpdate::complete, mappedValueError::complete));
        ObservableValue<String> filteredValue = property.filter(ANY_OTHER_STRING::equals);
        filteredValue.subscribe(filteredValueUpdate::complete);
        assertThat(mappedValue.get()).isNull();
        assertThat(filteredValue.get()).isNull();

        property.set(ANY_OTHER_STRING);
        assertThat(mappedValueError.get(10, SECONDS)).isInstanceOf(NumberFormatException.class);
        assertThat(mappedValueUpdate.isDone()).isFalse();
        assertThat(mappedValue.get()).isNull();
        assertThat(filteredValueUpdate.get(10, SECONDS)).isEqualTo(ANY_OTHER_STRING);
        assertThat(filteredValue.get()).isEqualTo(ANY_OTHER_STRING);

        property.set(Integer.toString(ANY_INT));
        assertThat(mappedValueUpdate.get(10, SECONDS)).isEqualTo(ANY_INT);
        assertThat(filteredValue.get()).isEqualTo(ANY_OTHER_STRING);
        assertThat(mappedValue.get()).isEqualTo(ANY_INT);
    }

}