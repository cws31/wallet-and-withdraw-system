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

        private static final String INR = "INR";

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

                PayoutMethod paypal = createMethodIfMissing(
                                "PAYPAL",
                                "PayPal",
                                false);

                ensureMethodState(
                                paypal,
                                false);

                createOptionsIfMissing(upi);
                createOptionsIfMissing(amazonGiftCard);
                createOptionsIfMissing(googlePlayGiftCard);
                validateMethodConfiguration(upi);
                validateMethodConfiguration(amazonGiftCard);
                validateMethodConfiguration(googlePlayGiftCard);
                validateInactiveMethodConfiguration(paypal);
        }

        private PayoutMethod createMethodIfMissing(
                        String code,
                        String name,
                        boolean active) {

                return payoutMethodRepository.findByCode(code)
                                .map(existing -> {
                                        if ("PAYPAL".equals(code)) {
                                                ensureMethodState(existing, false);
                                        }

                                        return existing;
                                })
                                .orElseGet(() -> {

                                        PayoutMethod method = new PayoutMethod();

                                        method.setCode(code);
                                        method.setName(name);
                                        method.setActive(active);

                                        return payoutMethodRepository.save(method);
                                });
        }

        private void ensureMethodState(
                        PayoutMethod method,
                        boolean expectedActive) {

                if (!Boolean.valueOf(expectedActive)
                                .equals(method.getActive())) {

                        method.setActive(expectedActive);
                        payoutMethodRepository.save(method);
                }
        }

        private void createOptionsIfMissing(
                        PayoutMethod method) {

                List<PayoutOptionData> options = getStandardInrOptions();

                for (PayoutOptionData data : options) {

                        boolean exists = payoutOptionRepository
                                        .findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
                                                        method.getId())
                                        .stream()
                                        .anyMatch(option -> option.getPayoutAmount() != null
                                                        && option.getCurrencyAmount() != null
                                                        && option.getCurrency() != null
                                                        && option.getPayoutAmount()
                                                                        .compareTo(data.payoutAmount()) == 0
                                                        && option.getCurrencyAmount()
                                                                        .compareTo(data.currencyAmount()) == 0
                                                        && INR.equalsIgnoreCase(
                                                                        option.getCurrency()));

                        if (exists) {
                                continue;
                        }

                        PayoutOption option = new PayoutOption();

                        option.setMethod(method);
                        option.setPayoutAmount(data.payoutAmount());
                        option.setCurrency(INR);
                        option.setCurrencyAmount(data.currencyAmount());
                        option.setActive(true);

                        payoutOptionRepository.save(option);
                }
        }

        private List<PayoutOptionData> getStandardInrOptions() {

                return List.of(
                                new PayoutOptionData("10", "2400"),
                                new PayoutOptionData("25", "5800"),
                                new PayoutOptionData("50", "10000"),
                                new PayoutOptionData("100", "19500"),
                                new PayoutOptionData("150", "28500"),
                                new PayoutOptionData("300", "52500"),
                                new PayoutOptionData("500", "80500"),
                                new PayoutOptionData("1000", "150000"));
        }

        private void validateMethodConfiguration(
                        PayoutMethod method) {

                if (!Boolean.TRUE.equals(method.getActive())) {
                        throw new IllegalStateException(
                                        "Payout method must be active: "
                                                        + method.getCode());
                }

                List<PayoutOption> activeOptions = payoutOptionRepository
                                .findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
                                                method.getId());

                if (activeOptions.isEmpty()) {
                        throw new IllegalStateException(
                                        "Active payout method has no active payout options: "
                                                        + method.getCode());
                }

                for (PayoutOption option : activeOptions) {
                        validatePayoutOption(
                                        method,
                                        option);
                }
        }

        private void validateInactiveMethodConfiguration(
                        PayoutMethod method) {

                if (Boolean.TRUE.equals(method.getActive())) {
                        throw new IllegalStateException(
                                        "PayPal must remain inactive until PayPal integration is enabled");
                }
        }

        private void validatePayoutOption(
                        PayoutMethod method,
                        PayoutOption option) {

                if (option.getMethod() == null
                                || option.getMethod().getId() == null
                                || !option.getMethod().getId()
                                                .equals(method.getId())) {

                        throw new IllegalStateException(
                                        "Payout option does not belong to payout method: "
                                                        + method.getCode());
                }

                if (option.getPayoutAmount() == null
                                || option.getPayoutAmount()
                                                .compareTo(BigDecimal.ZERO) <= 0) {

                        throw new IllegalStateException(
                                        "Payout amount must be greater than zero for method: "
                                                        + method.getCode());
                }

                if (option.getCurrency() == null
                                || option.getCurrency().isBlank()) {

                        throw new IllegalStateException(
                                        "Currency is required for payout method: "
                                                        + method.getCode());
                }

                if (!INR.equalsIgnoreCase(
                                option.getCurrency().trim())) {

                        throw new IllegalStateException(
                                        "Unsupported payout currency for method "
                                                        + method.getCode()
                                                        + ": "
                                                        + option.getCurrency());
                }

                if (option.getCurrencyAmount() == null
                                || option.getCurrencyAmount()
                                                .compareTo(BigDecimal.ZERO) <= 0) {

                        throw new IllegalStateException(
                                        "Required currency amount must be greater than zero "
                                                        + "for method: "
                                                        + method.getCode());
                }
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