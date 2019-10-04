package org.ossgang.commons.event;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.event.EventSources.eventSource;

public class SimpleEventSourceTest {

    @Test
    public void isDispatching() throws Exception {
        CompletableFuture<Integer> subscriber1 = new CompletableFuture<>();
        CompletableFuture<Integer> subscriber2 = new CompletableFuture<>();
        CompletableFuture<Integer> subscriber3 = new CompletableFuture<>();
        EventSource<Integer> eventSource = eventSource();
        eventSource.subscribe(subscriber1::complete);
        eventSource.subscribe(subscriber2::complete);
        eventSource.subscribe(subscriber3::complete);
        assertThat(subscriber1.isDone()).isFalse();
        assertThat(subscriber2.isDone()).isFalse();
        assertThat(subscriber3.isDone()).isFalse();

        eventSource.send(42);

        assertThat(subscriber1.get(1, TimeUnit.SECONDS)).isEqualTo(42);
        assertThat(subscriber2.get(1, TimeUnit.SECONDS)).isEqualTo(42);
        assertThat(subscriber3.get(1, TimeUnit.SECONDS)).isEqualTo(42);

    }

}