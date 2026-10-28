package com.payments.orchestrator.domain.service;

import com.payments.orchestrator.adapter.persistence.MerchantJpaEntity;
import com.payments.orchestrator.adapter.persistence.MerchantJpaRepository;
import com.payments.orchestrator.domain.exception.PaymentException;
import com.payments.orchestrator.domain.model.Merchant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

/**
 * Service for merchant registration and lookup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantJpaRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new merchant on the platform.
     */
    @Transactional
    public Merchant registerMerchant(
            String businessName,
            String email,
            String country,
            String currency,
            String rawPassword) {

        if (merchantRepository.existsByEmail(email)) {
            throw new PaymentException(
                    "MERCHANT_ALREADY_EXISTS",
                    "Merchant with email already exists: " + email);
        }

        // Encode password before storing
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Merchant merchant = Merchant.create(
                businessName, email, country,
                currency, encodedPassword);

        MerchantJpaEntity entity = toEntity(merchant);
        merchantRepository.save(entity);

        log.info("Merchant registered. merchantId={}",
                merchant.getMerchantId());

        return merchant;
    }

    /**
     * Returns merchant by merchantId.
     */
    @Transactional(readOnly = true)
    public Optional<Merchant> findByMerchantId(String merchantId) {
        return merchantRepository
                .findByMerchantId(merchantId)
                .map(this::toDomain);
    }

    /**
     * Returns display name for a merchantId.
     * Falls back to merchantId if not found.
     */
    @Transactional(readOnly = true)
    public String getDisplayName(String merchantId) {
        return merchantRepository
                .findByMerchantId(merchantId)
                .map(MerchantJpaEntity::getBusinessName)
                .orElse(merchantId);
    }

    private MerchantJpaEntity toEntity(Merchant merchant) {
        MerchantJpaEntity entity = new MerchantJpaEntity();
        entity.setId(merchant.getId());
        entity.setMerchantId(merchant.getMerchantId());
        entity.setBusinessName(merchant.getBusinessName());
        entity.setEmail(merchant.getEmail());
        entity.setCountry(merchant.getCountry());
        entity.setCurrency(merchant.getCurrency());
        entity.setStatus(merchant.getStatus());
        entity.setPassword(merchant.getPassword());
        entity.setCreatedAt(merchant.getCreatedAt());
        entity.setUpdatedAt(merchant.getUpdatedAt());
        return entity;
    }

    private Merchant toDomain(MerchantJpaEntity entity) {
        return Merchant.reconstitute(
                entity.getId(),
                entity.getMerchantId(),
                entity.getBusinessName(),
                entity.getEmail(),
                entity.getCountry(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getPassword(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public Optional<Merchant> findByEmail(String email) {
        return merchantRepository.findByEmail(email)
                .map(this::toDomain);
    }
}