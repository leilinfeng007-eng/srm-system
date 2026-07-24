package com.srm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SensitiveValuePolicyTest {

    @Test
    void rejectsMissingShortPlaceholderAndWeakValues() {
        assertThatThrownBy(() -> SensitiveValuePolicy.requireSecret("SECRET", null, 12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
        assertThatThrownBy(() -> SensitiveValuePolicy.requireSecret("SECRET", "too-short", 12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12");
        assertThatThrownBy(() -> SensitiveValuePolicy.requireSecret(
                        "SECRET", "CHANGE_ME_WITH_A_LONGER_VALUE", 12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
        assertThatThrownBy(() -> SensitiveValuePolicy.requirePassword(
                        "PASSWORD", "alllowercasepassword2026", 12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("character classes");
    }

    @Test
    void acceptsNonPlaceholderSecretsAndStrongPasswords() {
        assertThat(SensitiveValuePolicy.requireSecret(
                        "SECRET", "r5Kz9Yw2Dp7Ns4Qx8Lm3Vc6Ht1Bj0FgE", 32))
                .isNotBlank();
        assertThat(SensitiveValuePolicy.requirePassword(
                        "PASSWORD", "Strong-Stage0-Only!2026", 12))
                .isNotBlank();
    }

    @Test
    void rejectsSharedBootstrapPasswords() {
        assertThatThrownBy(() -> SensitiveValuePolicy.rejectMatchingPasswords(
                        "ADMIN", "Strong-Stage0-Only!2026",
                        "VIEWER", "Strong-Stage0-Only!2026"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must differ");
    }
}
