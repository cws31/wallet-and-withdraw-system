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

                BigDecimal currentBalance = getBalance(wallet, currency);

                BigDecimal newBalance = currentBalance.add(amount);

                setBalance(wallet, currency, newBalance);

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

                BigDecimal currentBalance = getBalance(wallet, currency);

                validateBalance(
                                currency,
                                currentBalance,
                                amount);

                BigDecimal newBalance = currentBalance.subtract(amount);

                setBalance(wallet, currency, newBalance);

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

        private BigDecimal getBalance(
                        Wallet wallet,
                        Currency currency) {

                return switch (currency) {
                        case VES -> wallet.getVes();
                        case SVES -> wallet.getSves();
                        case GEMS -> wallet.getGems();
                        case TOKENS -> wallet.getTokens();
                        case SPINS -> wallet.getSpins();
                };
        }

        private void setBalance(
                        Wallet wallet,
                        Currency currency,
                        BigDecimal balance) {

                switch (currency) {
                        case VES -> wallet.setVes(balance);
                        case SVES -> wallet.setSves(balance);
                        case GEMS -> wallet.setGems(balance);
                        case TOKENS -> wallet.setTokens(balance);
                        case SPINS -> wallet.setSpins(balance);
                }
        }
}
