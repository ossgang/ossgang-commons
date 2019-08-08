package org.ossgang.commons.property;

import org.junit.Test;
import org.ossgang.commons.observable.ObservableValue;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class ObservableWeakReferenceTest {
    private CompletableFuture<Integer> methodReferenceUpdateValue = new CompletableFuture<>();

    private void handleUpdate(int test) {
        methodReferenceUpdateValue.complete(test);
    }

    @Test
    public void transformationGcTest() throws Exception {
        Property<String> property = Properties.property("2");
        transformationGcTest_subscribe(property);
        System.gc();
        property.set("1");
        assertThat(methodReferenceUpdateValue.get(1, SECONDS)).isEqualTo(1);
    }

    private void transformationGcTest_subscribe(ObservableValue<String> value) {
        value.map(Integer::parseInt).subscribe(this::handleUpdate);
    }
}
