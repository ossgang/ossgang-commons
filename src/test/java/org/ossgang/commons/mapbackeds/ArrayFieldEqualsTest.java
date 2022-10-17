package org.ossgang.commons.mapbackeds;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

@Ignore("This test does not pass in the current version! Would need a more sophisticated equals method.")
public class ArrayFieldEqualsTest {

    @Test
    public void differentArrayInstancesShouldBeEqual() {
        Assertions.assertThat(anInstance()).isEqualTo(anInstance());
    }

    private ArrayFieldContaining anInstance() {
        return Mapbackeds.builder(ArrayFieldContaining.class) //
                .field(ArrayFieldContaining::anArrayField, new double[]{0.5, 0.2, 0.1})//
                .build();
    }


    private interface ArrayFieldContaining {
        public double[] anArrayField();
    }
}
