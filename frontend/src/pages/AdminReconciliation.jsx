import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { reconcileWallet } from "../api/adminApi";

const CURRENCIES = ["VES", "SVES", "GEMS", "TOKENS", "SPINS"];

function formatBalance(value) {
  if (value === null || value === undefined || value === "") return "—";
  const number = Number(value);
  return Number.isFinite(number)
    ? number.toLocaleString("en-IN", { maximumFractionDigits: 8 })
    : String(value);
}

export default function AdminReconciliation() {
  const navigate = useNavigate();
  const [walletId, setWalletId] = useState("");
  const [currency, setCurrency] = useState("VES");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function runReconciliation(event) {
    event.preventDefault();
    if (loading) return;

    setLoading(true);
    setError("");
    setResult(null);

    try {
      const response = await reconcileWallet(walletId.trim(), currency);
      if (response?.success === false) {
        throw new Error(response.message || "Reconciliation failed.");
      }

      const data = response?.data ?? response;
      if (
        !data ||
        typeof data.reconciled !== "boolean" ||
        data.walletBalance === undefined ||
        data.ledgerDerivedBalance === undefined ||
        data.difference === undefined
      ) {
        throw new Error("The backend returned an unexpected reconciliation response.");
      }
      setResult(data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Unable to reconcile this wallet.");
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    localStorage.removeItem("veloop_token");
    localStorage.removeItem("veloop_user");
    navigate("/login", { replace: true });
  }

  return (
    <main className="min-h-screen bg-[#080d1b] text-white">
      <header className="border-b border-white/10 bg-[#0d1426]">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-3 px-4 py-5 sm:px-6">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-400">VELoop Rewards</p>
            <h1 className="mt-1 text-2xl font-bold">Wallet reconciliation</h1>
          </div>
          <div className="flex gap-2">
            <Link to="/admin/withdrawals" className="rounded-xl border border-white/15 px-4 py-2.5 text-sm font-semibold transition hover:bg-white/5">Withdrawals</Link>
            <button type="button" onClick={logout} className="rounded-xl border border-white/15 px-4 py-2.5 text-sm font-semibold transition hover:bg-white/5">Logout</button>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-4xl space-y-5 px-4 py-6 sm:px-6 sm:py-8">
        <section className="rounded-2xl border border-white/10 bg-[#111a2e] p-5 sm:p-6">
          <h2 className="text-lg font-bold">Check wallet balance</h2>
          <form onSubmit={runReconciliation} className="mt-5 grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
            <label className="block text-sm text-slate-300">
              Wallet ID
              <input required inputMode="numeric" pattern="[0-9]+" value={walletId}
                onChange={(event) => setWalletId(event.target.value)} placeholder="Enter wallet ID"
                className="mt-2 w-full rounded-xl border border-white/15 bg-[#080d1b] px-3 py-3 text-white outline-none placeholder:text-slate-500 focus:border-indigo-400" />
            </label>
            <label className="block text-sm text-slate-300">
              Currency
              <select value={currency} onChange={(event) => setCurrency(event.target.value)}
                className="mt-2 w-full rounded-xl border border-white/15 bg-[#080d1b] px-3 py-3 text-white outline-none focus:border-indigo-400">
                {CURRENCIES.map((item) => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>
            <button type="submit" disabled={loading || !walletId.trim()}
              className="rounded-xl bg-indigo-600 px-5 py-3 text-sm font-semibold transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-50">
              {loading ? "Checking…" : "Reconcile"}
            </button>
          </form>
        </section>

        {error && <div role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{error}</div>}

        {result && (
          <section className="rounded-2xl border border-white/10 bg-[#111a2e] p-5 sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-bold">Result</h2>
              <span className={`rounded-full border px-3 py-1 text-sm font-semibold ${result.reconciled ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300" : "border-red-500/30 bg-red-500/10 text-red-300"}`}>
                {result.reconciled ? "Matched" : "Mismatch detected"}
              </span>
            </div>
            <dl className="mt-4 divide-y divide-white/10">
              {[
                ["Wallet ID", result.walletId ?? walletId],
                ["User ID", result.userId ?? "—"],
                ["Currency", result.currency ?? currency],
                ["Wallet balance", formatBalance(result.walletBalance)],
                ["Ledger balance", formatBalance(result.ledgerDerivedBalance)],
                ["Difference", formatBalance(result.difference)],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between gap-4 py-3">
                  <dt className="text-sm text-slate-400">{label}</dt>
                  <dd className={`text-right text-sm font-semibold tabular-nums ${label === "Difference" && !result.reconciled ? "text-red-300" : ""}`}>{value}</dd>
                </div>
              ))}
            </dl>
          </section>
        )}
      </div>
    </main>
  );
}
