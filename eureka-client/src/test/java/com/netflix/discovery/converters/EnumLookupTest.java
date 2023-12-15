package com.netflix.discovery.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class EnumLookupTest {
    
    enum TestEnum {
        VAL_ONE("one"), VAL_TWO("two"), VAL_THREE("three");
        private final String name;
        
        private TestEnum(String name) {
            this.name = name;
        }
    }

    @Test
    void lookup() {
        EnumLookup<TestEnum> lookup = new EnumLookup<>(TestEnum.class, v->v.name.toCharArray());
        char[] buffer = "zeroonetwothreefour".toCharArray();
        assertSame(TestEnum.VAL_ONE, lookup.find(buffer, 4, 3));
        assertSame(TestEnum.VAL_TWO, lookup.find(buffer, 7, 3));
        assertSame(TestEnum.VAL_THREE, lookup.find(buffer, 10, 5));
    }

}
