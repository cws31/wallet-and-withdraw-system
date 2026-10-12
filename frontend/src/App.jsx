
import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./pages/Login";
import Wallet from "./pages/Wallet";
import Payout from "./pages/Payout";
import AdminWithdrawals from "./pages/AdminWithdrawals";

function ProtectedRoute({ children }) {
  const token = localStorage.getItem("veloop_token");

  return token ? children : <Navigate to="/login" replace />;
}

function AdminRoute({ children }) {
  const token = localStorage.getItem("veloop_token");

  let user = null;

  try {
    user = JSON.parse(localStorage.getItem("veloop_user") || "null");
  } catch {
    user = null;
  }

  const role = String(user?.role || "")
    .replace(/^ROLE_/, "")
    .toUpperCase();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (role !== "ADMIN") {
    return <Navigate to="/wallet" replace />;
  }

  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route path="/login" element={<Login />} />

      <Route
        path="/wallet"
        element={
          <ProtectedRoute>
            <Wallet />
          </ProtectedRoute>
        }
      />

      <Route
        path="/payout"
        element={
          <ProtectedRoute>
            <Payout />
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/withdrawals"
        element={
          <AdminRoute>
            <AdminWithdrawals />
          </AdminRoute>
        }
      />

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
