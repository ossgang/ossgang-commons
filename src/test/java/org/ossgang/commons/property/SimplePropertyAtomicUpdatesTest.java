package org.ossgang.commons.property;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.ossgang.commons.observables.Transition;
import org.ossgang.commons.properties.AtomicProperty;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

public class SimplePropertyAtomicUpdatesTest {

    private static final String INITIAL = "initial";
    private static final String ANOTHER = "another";

    private CompletableFuture<String> updateValue = new CompletableFuture<>();
    private AtomicProperty<String> property = Properties.property();

    @Before
    public void setUp() {
        updateValue = new CompletableFuture<>();
        property = Properties.property(INITIAL);
        property.subscribe(updateValue::complete);
    }

    @Test
    public void getAndSet() {
        String oldVal = property.getAndSet(ANOTHER);
        assertThat(oldVal).isEqualTo(INITIAL);
        assertThat(dispatched()).isEqualTo(ANOTHER);
    }

    @Test
    public void update() {
        Transition<String> tr = property.update(s -> s + "_A");
        assertNewSuffix(tr, "_A");
    }

    @Test
    public void null_updateFunction_throws() {
        assertThatThrownBy(() -> property.update(null))//
                .isInstanceOf(NullPointerException.class)//
                .hasMessageContainingAll("updateFunction");
    }

    @Test
    public void updateAndGet() {
        String v = property.updateAndGet(s -> s + "_A");
        assertThat(v).isEqualTo(INITIAL + "_A");
        assertThat(dispatched()).isEqualTo(INITIAL + "_A");
    }

    @Test
    public void getAndUpdate() {
        String v = property.getAndUpdate(s -> s + "_A");
        assertThat(v).isEqualTo(INITIAL);
        assertThat(dispatched()).isEqualTo(INITIAL + "_A");
    }

    @Test
    public void null_accumulatorFunction_throws() {
        assertThatThrownBy(() -> property.accumulate("", null))//
                .isInstanceOf(NullPointerException.class)//
                .hasMessageContainingAll("accumulatorFunction");
    }

    @Test
    public void null_updateValue_is_allowed() {
        Transition<String> tr = property.accumulate(null, (s, u) -> "A");//
        assertThat(tr.newValue()).isEqualTo("A");
    }

    @Test
    public void accumulation_to_null_is_not_performed() {
        assertThatThrownBy(() -> property.accumulate("A", (s, u) -> null))// 
                .isInstanceOf(NullPointerException.class)//
                .hasMessageContainingAll("updated value");//
        assertThat(property.get()).isEqualTo(INITIAL);
        assertNothingDispatched();
    }

    @Test
    public void update_to_null_is_not_performed() {
        assertThatThrownBy(() -> property.update(s -> null))//
                .isInstanceOf(NullPointerException.class)//
                .hasMessageContainingAll("updated value");//
        assertThat(property.get()).isEqualTo(INITIAL);
        assertNothingDispatched();
    }

    @Test
    public void accumulate() {
        Transition<String> tr = property.accumulate("_A", (s, u) -> s + u);
        assertNewSuffix(tr, "_A");
    }

    @Test
    public void accumulateAndGet() {
        String v = property.accumulateAndGet("_A", (s, u) -> s + u);
        assertThat(v).isEqualTo(INITIAL + "_A");
        assertThat(dispatched()).isEqualTo(INITIAL + "_A");
    }

    @Test
    public void getAndAccumulate() {
        String v = property.getAndAccumulate("_A", (s, u) -> s + u);
        assertThat(v).isEqualTo(INITIAL);
        assertThat(dispatched()).isEqualTo(INITIAL + "_A");
    }

    private void assertNewSuffix(Transition<String> tr, String suffix) {
        assertThat(tr.oldValue()).isEqualTo(INITIAL);
        assertThat(tr.newValue()).isEqualTo(INITIAL + suffix);
        assertThat(dispatched()).isEqualTo(INITIAL + suffix);
    }

    private String dispatched() {
        try {
            return updateValue.get(1, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertNothingDispatched() {
        assertThatThrownBy(() -> updateValue.get(1, SECONDS)).isInstanceOf(TimeoutException.class);
    }

}
