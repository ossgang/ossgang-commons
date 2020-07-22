package org.ossgang.commons.monads;

public interface Function5<I1, I2, I3, I4, I5, O> {
    O apply(I1 input1, I2 input2, I3 input3, I4 input4, I5 input5);
}
