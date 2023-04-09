package no.dervis;

import org.junit.jupiter.api.Test;

import static no.dervis.Board.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardTest {

    @Test
    void testConvertIndexToCoordinates() {

        assertEquals(0, a1.file());
        assertEquals(0, a1.rank());
        assertEquals(0, indexFn.apply(a1.rank(), a1.file()));

        assertEquals(4, e5.file());
        assertEquals(4, e5.rank());
        assertEquals(36, indexFn.apply(e5.rank(), e5.file()));

        assertEquals(7, h8.file());
        assertEquals(7, h8.rank());
        assertEquals(63, indexFn.apply(h8.rank(), h8.file()));

    }
}