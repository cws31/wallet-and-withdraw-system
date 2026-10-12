import api from "./client";

export async function getPayoutConfiguration() {
  const response = await api.get("/api/payouts/configuration");
  return response.data;
}