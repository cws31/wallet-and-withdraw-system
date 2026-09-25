package com.veloop.rewards.wallet.service;

import com.veloop.rewards.common.exception.InsufficientBalanceException;
import com.veloop.rewards.common.exception.InvalidAmountException;
import com.veloop.rewards.common.exception.WalletAlreadyExistsException;
import com.veloop.rewards.common.exception.WalletNotFoundException;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veloop.rewards.wallet.enums.Currency;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Wallet createWallet(Long userId) {

        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(userId);
        }

        Wallet wallet = new Wallet();
        wallet.setUserId(userId);

        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(Long userId) {

        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
    }

    @Transactional
    public Wallet creditWallet(
            Long userId,
            Currency currency,
            BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }

        Wallet wallet = getWallet(userId);

        switch (currency) {
            case VES -> wallet.setVes(
                    wallet.getVes().add(amount));

            case SVES -> wallet.setSves(
                    wallet.getSves().add(amount));

            case GEMS -> wallet.setGems(
                    wallet.getGems().add(amount));

            case TOKENS -> wallet.setTokens(
                    wallet.getTokens().add(amount));

            case SPINS -> wallet.setSpins(
                    wallet.getSpins().add(amount));
        }

        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debitWallet(
            Long userId,
            Currency currency,
            BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }

        Wallet wallet = getWallet(userId);

        switch (currency) {

            case VES -> {
                validateBalance(
                        currency,
                        wallet.getVes(),
                        amount);

                wallet.setVes(
                        wallet.getVes().subtract(amount));
            }

            case SVES -> {
                validateBalance(
                        currency,
                        wallet.getSves(),
                        amount);

                wallet.setSves(
                        wallet.getSves().subtract(amount));
            }

            case GEMS -> {
                validateBalance(
                        currency,
                        wallet.getGems(),
                        amount);

                wallet.setGems(
                        wallet.getGems().subtract(amount));
            }

            case TOKENS -> {
                validateBalance(
                        currency,
                        wallet.getTokens(),
                        amount);

                wallet.setTokens(
                        wallet.getTokens().subtract(amount));
            }

            case SPINS -> {
                validateBalance(
                        currency,
                        wallet.getSpins(),
                        amount);

                wallet.setSpins(
                        wallet.getSpins().subtract(amount));
            }
        }

        return walletRepository.save(wallet);
    }

    private void validateBalance(
            Currency currency,
            BigDecimal available,
            BigDecimal requested) {

        if (available.compareTo(requested) < 0) {
            throw new InsufficientBalanceException(
                    currency.name(),
                    available,
                    requested);
        }
    }
}
