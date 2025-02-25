package no.dervis.terminal_games.terminal_chess;

import java.util.function.Function;

public interface TestUtils {
    Function<Long, String> longToString = n -> String
            .format("%64s", Long.toBinaryString(n))
            .replace(' ', '0');

    Function<Integer, String> intToString = n -> String
            .format("%32s", Integer.toBinaryString(n))
            .replace(' ', '0');

    Function<Byte, String> byteToString = n -> String
            .format("%4s", Integer.toBinaryString(n))
            .replace(' ', '0');
}
