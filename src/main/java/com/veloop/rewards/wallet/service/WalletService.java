package com.veloop.rewards.wallet.service;

import com.veloop.rewards.common.exception.WalletAlreadyExistsException;
import com.veloop.rewards.common.exception.WalletNotFoundException;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}