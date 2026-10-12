import api from "./client";

export async function loginUser(credentials) {
  const response = await api.post("/api/auth/login", credentials);
  return response.data;
}