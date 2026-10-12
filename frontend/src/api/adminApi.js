
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
