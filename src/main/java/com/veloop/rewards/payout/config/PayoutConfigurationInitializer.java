package com.veloop.rewards.payout.config;

import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PayoutConfigurationInitializer implements CommandLineRunner {

    private final PayoutMethodRepository payoutMethodRepository;
    private final PayoutOptionRepository payoutOptionRepository;

    public PayoutConfigurationInitializer(
            PayoutMethodRepository payoutMethodRepository,
            PayoutOptionRepository payoutOptionRepository) {

        this.payoutMethodRepository = payoutMethodRepository;
        this.payoutOptionRepository = payoutOptionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        PayoutMethod upi = createMethodIfMissing(
                "UPI",
                "UPI",
                true);

        PayoutMethod amazonGiftCard = createMethodIfMissing(
                "AMAZON_GIFT_CARD",
                "Amazon Gift Card",
                true);

        PayoutMethod googlePlayGiftCard = createMethodIfMissing(
                "GOOGLE_PLAY_GIFT_CARD",
                "Google Play Gift Card",
                true);

        createMethodIfMissing(
                "PAYPAL",
                "PayPal",
                false);

        createUpiOptions(upi);

        ensureMethodHasNoMissingConfiguration(
                amazonGiftCard);

        ensureMethodHasNoMissingConfiguration(
                googlePlayGiftCard);
    }

    private PayoutMethod createMethodIfMissing(
            String code,
            String name,
            boolean active) {

        return payoutMethodRepository.findByCode(code)
                .orElseGet(() -> {

                    PayoutMethod method = new PayoutMethod();

                    method.setCode(code);
                    method.setName(name);
                    method.setActive(active);

                    return payoutMethodRepository.save(method);
                });
    }

    private void createUpiOptions(PayoutMethod upi) {

        List<PayoutOptionData> options = List.of(
                new PayoutOptionData("10", "2400"),
                new PayoutOptionData("25", "5800"),
                new PayoutOptionData("50", "10000"),
                new PayoutOptionData("100", "19500"),
                new PayoutOptionData("150", "28500"),
                new PayoutOptionData("300", "52500"),
                new PayoutOptionData("500", "80500"),
                new PayoutOptionData("1000", "150000"));

        for (PayoutOptionData data : options) {

            boolean exists = payoutOptionRepository
                    .findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
                            upi.getId())
                    .stream()
                    .anyMatch(option -> option.getPayoutAmount()
                            .compareTo(data.payoutAmount()) == 0
                            && option.getCurrencyAmount()
                                    .compareTo(data.currencyAmount()) == 0);

            if (exists) {
                continue;
            }

            PayoutOption option = new PayoutOption();

            option.setMethod(upi);
            option.setPayoutAmount(data.payoutAmount());
            option.setCurrency("INR");
            option.setCurrencyAmount(data.currencyAmount());
            option.setActive(true);

            payoutOptionRepository.save(option);
        }
    }

    private void ensureMethodHasNoMissingConfiguration(
            PayoutMethod method) {
    }

    private record PayoutOptionData(
            BigDecimal payoutAmount,
            BigDecimal currencyAmount) {

        private PayoutOptionData(
                String payoutAmount,
                String currencyAmount) {

            this(
                    new BigDecimal(payoutAmount),
                    new BigDecimal(currencyAmount));
        }
    }
}