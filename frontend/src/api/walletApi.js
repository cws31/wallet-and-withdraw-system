
import api from "./client";

export async function getWallet() {
  const response = await api.get("/api/wallet");
  return response.data;
}

export async function getWalletTransactions(
  page = 1,
  limit = 10,
  type = "ALL"
) {
  const params = { page, limit };

  if (type && type !== "ALL") {
    params.type = type;
  }

  const response = await api.get("/api/wallet/transactions", {
    params,
  });

  return response.data;
}
