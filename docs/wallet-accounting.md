VELoop Rewards Backend — Wallet Accounting
Model
The accounting model contains:
Current Wallet Balance
+
Historical Wallet Ledger
Supported currencies:
VES
SVES
GEMS
TOKENS
SPINS
Credit
request
 -> authenticate/authorize
 -> validate
 -> load wallet
 -> apply credit
 -> persist wallet
 -> create ledger entry
 -> audit
A successful credit records balance_before, amount, and balance_after.
Debit
request
 -> authenticate/authorize
 -> validate
 -> load wallet
 -> check balance
 -> apply debit
 -> persist wallet
 -> create ledger entry
 -> audit
Insufficient balance must not create a successful debit.
Ledger transaction types
Credit examples:
REWARD, BONUS, REFERRAL, DAILY_REWARD, AD_REWARD, GAME_REWARD, ADMIN_CREDIT, EXCHANGE_CREDIT
Debit/correction examples:
WITHDRAWAL, EXCHANGE_DEBIT, ADMIN_DEBIT, CORRECTION
The source enum remains authoritative.
Withdrawal accounting
Withdrawal creation uses immediate VES deduction.
Example:
10,000 VES
-2,400 VES withdrawal
=7,600 VES
The withdrawal references the corresponding WITHDRAWAL ledger transaction.
Reversal
Rejected/cancelled withdrawals do not delete the original debit.
WITHDRAWAL debit
+
CORRECTION credit
=
net zero effect
Atomicity
Wallet mutation and ledger creation are coordinated transactionally so a successful financial operation is not exposed without its ledger record.
Concurrency
Optimistic locking prevents stale concurrent wallet updates from silently overwriting each other.
Accounting invariant
Conceptually:
Current Balance
= Initial Balance
+ Credits
- Debits
+ Corrections
Reconciliation verifies the stored wallet against the ledger-derived result.
Rules
1. Never trust frontend balances.
2. Never trust frontend withdrawal amounts.
3. Never delete financial history to reverse an operation.
4. Every successful wallet mutation needs a ledger record.
5. Every reversal needs a compensating record.
6. Use precise numeric values.
7. Preserve references between business operations and ledger entries.