package com.flowiq.unit.profile;

import com.flowiq.entity.User;
import com.flowiq.profile.entity.FopProfile;
import com.flowiq.profile.entity.TaxSystem;
import com.flowiq.profile.repository.FopProfileRepository;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.repository.UserRepository;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FopProfileService unit tests")
class FopProfileServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FopProfileRepository fopProfileRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FopProfileService fopProfileService;

    @Test
    @DisplayName("findByUserId returns existing profile")
    void findByUserId_success() {
        FopProfile profile = sampleProfile();
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));

        assertThat(fopProfileService.findByUserId(USER_ID)).contains(profile);
    }

    @Test
    @DisplayName("getOrCreateForUser returns existing profile")
    void getOrCreateForUser_existing() {
        User user = SecurityTestSupport.testUser(USER_ID, "fop@test.flowiq");
        FopProfile profile = sampleProfile();
        profile.setUser(user);
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));

        FopProfile result = fopProfileService.getOrCreateForUser(user);

        assertThat(result.getFopGroup()).isEqualTo(2);
        verify(fopProfileRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateForUser creates default profile when missing")
    void getOrCreateForUser_createsDefault() {
        User user = SecurityTestSupport.testUser(USER_ID, "fop@test.flowiq");
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        when(fopProfileRepository.save(any(FopProfile.class))).thenAnswer(invocation -> {
            FopProfile saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        FopProfile result = fopProfileService.getOrCreateForUser(user);

        assertThat(result.getFopGroup()).isEqualTo(2);
        assertThat(result.getTaxSystem()).isEqualTo(TaxSystem.SINGLE_TAX);
        assertThat(result.getMainKved()).isEqualTo("62.01");
        verify(fopProfileRepository).save(any(FopProfile.class));
    }

    @Test
    @DisplayName("resolveEffectiveFopGroup uses stored group when profile exists")
    void resolveEffectiveFopGroup_stored() {
        FopProfile profile = sampleProfile();
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));

        assertThat(fopProfileService.resolveEffectiveFopGroup(USER_ID, new BigDecimal("999999999")))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("resolveEffectiveFopGroup derives from income when profile missing")
    void resolveEffectiveFopGroup_derived() {
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());

        assertThat(fopProfileService.resolveEffectiveFopGroup(USER_ID, new BigDecimal("1000000")))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("resolveEffectiveTaxRate uses profile tax rate when set")
    void resolveEffectiveTaxRate_customRate() {
        FopProfile profile = sampleProfile();
        profile.setTaxRate(new BigDecimal("0.03"));
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));

        assertThat(fopProfileService.resolveEffectiveTaxRate(USER_ID, 3))
                .isEqualByComparingTo("0.03");
    }

    @Test
    @DisplayName("resolveEffectiveTaxRate falls back to default rate")
    void resolveEffectiveTaxRate_defaultRate() {
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());

        assertThat(fopProfileService.resolveEffectiveTaxRate(USER_ID, 2))
                .isEqualByComparingTo("0.05");
    }

    @Test
    @DisplayName("isVatPayer returns false when profile missing")
    void isVatPayer_defaultFalse() {
        when(fopProfileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());

        assertThat(fopProfileService.isVatPayer(USER_ID)).isFalse();
    }

    @Test
    @DisplayName("deriveFopGroupFromIncome maps income to group boundaries")
    void deriveFopGroupFromIncome_boundaries() {
        assertThat(fopProfileService.deriveFopGroupFromIncome(null)).isEqualTo(2);
        assertThat(fopProfileService.deriveFopGroupFromIncome(new BigDecimal("1000000"))).isEqualTo(1);
        assertThat(fopProfileService.deriveFopGroupFromIncome(new BigDecimal("3000000"))).isEqualTo(2);
        assertThat(fopProfileService.deriveFopGroupFromIncome(new BigDecimal("7000000"))).isEqualTo(3);
        assertThat(fopProfileService.deriveFopGroupFromIncome(new BigDecimal("9000000"))).isEqualTo(0);
    }

    @Test
    @DisplayName("incomeLimitForGroup returns configured limits")
    void incomeLimitForGroup_success() {
        assertThat(fopProfileService.incomeLimitForGroup(1))
                .isEqualByComparingTo("1672000");
        assertThat(fopProfileService.incomeLimitForGroup(99))
                .isEqualByComparingTo("0");
    }

    private FopProfile sampleProfile() {
        FopProfile profile = new FopProfile();
        profile.setId(1L);
        profile.setFopGroup(2);
        profile.setTaxSystem(TaxSystem.SINGLE_TAX);
        profile.setTaxRate(new BigDecimal("0.05"));
        return profile;
    }
}
