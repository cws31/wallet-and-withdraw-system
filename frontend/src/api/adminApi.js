import api from "./client";

export async function getAdminWithdrawals({
  page = 1,
  limit = 20,
  status = "",
} = {}) {
  const response = await api.get("/api/withdrawals/admin", {
    params: {
      page,
      limit,
      ...(status ? { status } : {}),
    },
  });

  return response.data;
}

export async function markWithdrawalProcessing(withdrawalId) {
  const response = await api.patch(
    `/api/withdrawals/${encodeURIComponent(withdrawalId)}/processing`
  );

  return response.data;
}

export async function approveWithdrawal(withdrawalId) {
  const response = await api.patch(
    `/api/withdrawals/${encodeURIComponent(withdrawalId)}/approve`
  );

  return response.data;
}

export async function rejectWithdrawal(
  withdrawalId,
  rejectionReason,
  reviewNote = ""
) {
  const response = await api.patch(
    `/api/withdrawals/${encodeURIComponent(withdrawalId)}/reject`,
    null,
    {
      params: {
        rejectionReason,
        ...(reviewNote.trim() ? { reviewNote: reviewNote.trim() } : {}),
      },
    }
  );

  return response.data;
}

// Reconcile a wallet balance against its ledger-derived balance.
export async function reconcileWallet(walletId, currency) {
  const normalizedWalletId = String(walletId ?? "").trim();

  if (!/^\d+$/.test(normalizedWalletId)) {
    throw new Error("Enter a valid numeric wallet ID.");
  }

  const supportedCurrencies = ["VES", "SVES", "GEMS", "TOKENS", "SPINS"];

  if (!supportedCurrencies.includes(currency)) {
    throw new Error("Select a supported wallet currency.");
  }

  const response = await api.get(
    `/api/admin/reconciliation/wallet/${encodeURIComponent(
      normalizedWalletId
    )}`,
    {
      params: { currency },
    }
  );

  return response.data;
}