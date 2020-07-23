package org.ossgang.commons.monads;

/**
 * Represents a function that accepts 3 arguments and produces a result.
 * This is a specialization of standard Java {@link java.util.function.Function}.
 *
 * @param <I1> the first argument type
 * @param <I2> the second argument type
 * @param <I3> the third argument type
 * @param <I4> the fourth argument type
 * @param <I5> the fifth argument type
 * @param <O>  the result type
 */
public interface Function5<I1, I2, I3, I4, I5, O> {

    /**
     * Apply this function to the given arguments.
     *
     * @param input1 the first argument
     * @param input2 the second argument
     * @param input3 the third argument
     * @param input4 the fourth argument
     * @param input5 the fifth argument
     * @return the result of the function
     */
    O apply(I1 input1, I2 input2, I3 input3, I4 input4, I5 input5);
}
