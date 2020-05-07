package org.ossgang.commons.observables;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ossgang.commons.observables.testing.TestObserver;

public class PeriodicObservableValueTest {

    @Test
    public void test() {
        ObservableValue<Instant> source = Observables.periodicEvery(1, SECONDS);
        TestObserver<Instant> obs = new TestObserver<>();
        source.subscribe(obs);

        obs.awaitForValueCountToBe(3);

        List<Duration> diffs = diffs(obs.receivedValues());

        assertThat(diffs).allSatisfy(d -> {
            assertThat(d.toMillis()).isCloseTo(1000L, offset(2L));
        });

    }

    private static List<Duration> diffs(List<Instant> values) {
        if (values.size() < 2) {
            return Collections.emptyList();
        }

        List<Duration> diffs = new ArrayList<>();
        for (int i = 0; i < (values.size() - 1); i++) {
            Duration diff = Duration.between(values.subList(0, values.size() - 1).get(i),
                    values.subList(1, values.size()).get(i));
            diffs.add(diff);
        }
        return diffs;
    }

}
