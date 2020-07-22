package org.ossgang.commons.monads;

public interface Function4<I1, I2, I3, I4, O> {
    O apply(I1 input1, I2 input2, I3 input3, I4 input4);
}
