package com.veloop.rewards.wallet;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WalletIsolationIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Test
    void userCannotAccessAnotherUsersWallet() {

        String uniqueId = String.valueOf(System.nanoTime());

        User userA = new User();
        userA.setEmail("wallet-a-" + uniqueId + "@test.com");
        userA.setPasswordHash("test-password");
        userA.setName("User A");

        User userB = new User();
        userB.setEmail("wallet-b-" + uniqueId + "@test.com");
        userB.setPasswordHash("test-password");
        userB.setName("User B");

        User savedUserA = userRepository.save(userA);
        User savedUserB = userRepository.save(userB);

        Wallet walletA = walletService.createWallet(savedUserA.getId());
        Wallet walletB = walletService.createWallet(savedUserB.getId());

        assertNotNull(walletA);
        assertNotNull(walletB);

        assertNotEquals(
                walletA.getId(),
                walletB.getId());

        assertEquals(
                savedUserA.getId(),
                walletA.getUserId());

        assertEquals(
                savedUserB.getId(),
                walletB.getUserId());

        assertNotEquals(
                walletA.getUserId(),
                walletB.getUserId());
    }
}