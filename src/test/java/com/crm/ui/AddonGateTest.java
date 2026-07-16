package com.crm.ui;

import com.crm.service.AddonService;
import com.crm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddonGateTest {

    @Mock SecurityService securityService;
    @Mock UserService userService;
    @Mock AddonService addonService;

    private AddonGate addonGate;

    @BeforeEach
    void setUp() {
        addonGate = new AddonGate(securityService, userService, addonService);
    }

    @Test
    void adminUser_alwaysHasBillingDocuments() {
        when(securityService.hasRole("SUPER_ADMIN")).thenReturn(true);

        assertThat(addonGate.currentUserHasBillingDocuments()).isTrue();
    }

    @Test
    void regularUser_gatedByAddonRecord() {
        when(securityService.hasRole("SUPER_ADMIN")).thenReturn(false);
        when(securityService.hasRole("ADMIN")).thenReturn(false);
        when(securityService.getUsername()).thenReturn("jdoe");
        when(userService.getAccountIdByUsername("jdoe")).thenReturn(Optional.of(7L));
        when(addonService.accountHasActiveAddon(7L, "Billing & Documents")).thenReturn(false);

        assertThat(addonGate.currentUserHasBillingDocuments()).isFalse();

        when(addonService.accountHasActiveAddon(7L, "Billing & Documents")).thenReturn(true);
        assertThat(addonGate.currentUserHasBillingDocuments()).isTrue();
    }
}
