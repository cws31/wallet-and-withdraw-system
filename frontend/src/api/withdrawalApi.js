
import api from "./client";

/**
 * Create a withdrawal request.
 * The backend validates the request and calculates the required amount.
 */
export async function createWithdrawal(request, idempotencyKey) {
  const response = await api.post("/api/withdrawals", request, {
    headers: {
      "Idempotency-Key": idempotencyKey,
    },
  });

  return response.data;
}

/**
 * Fetch the authenticated user's withdrawal history.
 */
export async function getWithdrawalHistory(
  page = 1,
  limit = 10,
  status = "ALL"
) {
  const params = { page, limit };

  if (status && status !== "ALL") {
    params.status = status;
  }

  const response = await api.get("/api/withdrawals", {
    params,
  });

  return response.data;
}


export async function getWithdrawalById(withdrawalId) {
  if (!withdrawalId) {
    throw new Error("A withdrawal request ID is required.");
  }

  const response = await api.get(
    `/api/withdrawals/${encodeURIComponent(withdrawalId)}`
  );

  return response.data;
}


export async function cancelWithdrawal(withdrawalId) {
  if (!withdrawalId) {
    throw new Error("A withdrawal request ID is required.");
  }

  const response = await api.patch(
    `/api/withdrawals/${encodeURIComponent(withdrawalId)}/cancel`
  );

  return response.data;
}
