
import api from "./client";

export async function createWithdrawal(request, idempotencyKey) {
  const response = await api.post("/api/withdrawals", request, {
    headers: {
      "Idempotency-Key": idempotencyKey,
    },
  });

  return response.data;
}

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
