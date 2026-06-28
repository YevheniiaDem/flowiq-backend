package com.flowiq.profile.service;

import com.flowiq.profile.entity.FopProfile;
import com.flowiq.profile.entity.TaxSystem;
import com.flowiq.profile.repository.FopProfileRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FopProfileService {

    private static final Map<Integer, BigDecimal> INCOME_LIMITS = Map.of(
            1, new BigDecimal("1672000"),
            2, new BigDecimal("5328000"),
            3, new BigDecimal("7818000")
    );

    private static final Map<Integer, BigDecimal> DEFAULT_SINGLE_TAX_RATES = Map.of(
            1, new BigDecimal("0.10"),
            2, new BigDecimal("0.05"),
            3, new BigDecimal("0.03")
    );

    private final FopProfileRepository fopProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<FopProfile> findByUserId(Long userId) {
        return fopProfileRepository.findByUser_Id(userId);
    }

    @Transactional
    public FopProfile getOrCreateForUser(User user) {
        Long userId = user.getId();
        return fopProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    FopProfile profile = new FopProfile();
                    profile.setUser(user);
                    profile.setFopGroup(2);
                    profile.setTaxSystem(TaxSystem.SINGLE_TAX);
                    profile.setTaxRate(DEFAULT_SINGLE_TAX_RATES.get(2));
                    profile.setKvedCodes(new ArrayList<>(List.of("62.01")));
                    profile.setMainKved("62.01");
                    profile.setMainKvedName("Computer programming");
                    return fopProfileRepository.save(profile);
                });
    }

    /**
     * Returns stored FOP group when profile exists; otherwise derives from annual income.
     */
    @Transactional(readOnly = true)
    public int resolveEffectiveFopGroup(Long userId, BigDecimal annualIncome) {
        return fopProfileRepository.findByUser_Id(userId)
                .map(FopProfile::getFopGroup)
                .orElseGet(() -> deriveFopGroupFromIncome(annualIncome));
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveEffectiveTaxRate(Long userId, int fopGroup) {
        return fopProfileRepository.findByUser_Id(userId)
                .map(profile -> {
                    if (profile.getTaxRate() != null) {
                        return profile.getTaxRate();
                    }
                    return DEFAULT_SINGLE_TAX_RATES.getOrDefault(fopGroup, BigDecimal.ZERO);
                })
                .orElse(DEFAULT_SINGLE_TAX_RATES.getOrDefault(fopGroup, BigDecimal.ZERO));
    }

    @Transactional(readOnly = true)
    public boolean isVatPayer(Long userId) {
        return fopProfileRepository.findByUser_Id(userId)
                .map(FopProfile::isVatPayer)
                .orElse(false);
    }

    public BigDecimal incomeLimitForGroup(int fopGroup) {
        return INCOME_LIMITS.getOrDefault(fopGroup, BigDecimal.ZERO);
    }

    public int deriveFopGroupFromIncome(BigDecimal annualIncome) {
        if (annualIncome == null) {
            return 2;
        }
        BigDecimal income = annualIncome.setScale(2, RoundingMode.HALF_UP);
        if (income.compareTo(INCOME_LIMITS.get(1)) <= 0) {
            return 1;
        }
        if (income.compareTo(INCOME_LIMITS.get(2)) <= 0) {
            return 2;
        }
        if (income.compareTo(INCOME_LIMITS.get(3)) <= 0) {
            return 3;
        }
        return 0;
    }
}
