package com.matriz.testsupport;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TestDatabaseTest {

    @Test
    void onlyADatabaseNamedLikeATestDatabaseMayBeEmptied() {
        assertThatCode(() -> TestDatabase.requireTestDatabase("matriz_test")).doesNotThrowAnyException();
        assertThatThrownBy(() -> TestDatabase.requireTestDatabase("matriz"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TEST_DB_URL");
        assertThatThrownBy(() -> TestDatabase.requireTestDatabase(null)).isInstanceOf(IllegalStateException.class);
    }
}
