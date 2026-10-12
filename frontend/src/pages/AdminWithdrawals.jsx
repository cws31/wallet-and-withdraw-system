import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  approveWithdrawal,
  getAdminWithdrawals,
  markWithdrawalProcessing,
  rejectWithdrawal,
} from "../api/adminApi";

const STATUSES = [
  "ALL",
  "PENDING",
  "PROCESSING",
  "APPROVED",
  "REJECTED",
  "CANCELLED",
];

const STATUS_STYLES = {
  PENDING: "bg-amber-500/15 text-amber-300 border-amber-500/30",
  PROCESSING: "bg-blue-500/15 text-blue-300 border-blue-500/30",
  APPROVED: "bg-emerald-500/15 text-emerald-300 border-emerald-500/30",
  REJECTED: "bg-red-500/15 text-red-300 border-red-500/30",
  CANCELLED: "bg-gray-500/15 text-gray-300 border-gray-500/30",
};

function formatDate(value) {
  if (!value) return "—";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? String(value)
    : date.toLocaleString();
}

function formatAmount(value, currency = "INR") {
  const amount = Number(value);

  if (!Number.isFinite(amount)) return "—";

  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: currency || "INR",
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return String(value);
  }
}

function formatVes(value) {
  const amount = Number(value);

  return Number.isFinite(amount)
    ? `${amount.toLocaleString("en-IN")} VEs`
    : "—";
}

function StatusBadge({ status }) {
  return (
    <span
      className={`inline-flex whitespace-nowrap rounded-full border px-3 py-1 text-xs font-semibold ${
        STATUS_STYLES[status] ||
        "border-gray-500/30 bg-gray-500/10 text-gray-300"
      }`}
    >
      {status || "UNKNOWN"}
    </span>
  );
}

function DetailRow({ label, children }) {
  return (
    <div className="flex flex-col gap-2 border-b border-white/10 py-3 sm:flex-row sm:items-start sm:justify-between">
      <span className="shrink-0 text-sm text-slate-400">{label}</span>
      <div className="min-w-0 break-all text-sm font-medium text-white sm:max-w-[65%] sm:text-right">
        {children ?? "—"}
      </div>
    </div>
  );
}

export default function AdminWithdrawals() {
  const navigate = useNavigate();

  const [withdrawals, setWithdrawals] = useState([]);
  const [page, setPage] = useState(1);

  const [pagination, setPagination] = useState({
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const [status, setStatus] = useState("ALL");
  const [selected, setSelected] = useState(null);
  const [showRecipientDetails, setShowRecipientDetails] = useState(false);

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [rejectModal, setRejectModal] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const [reviewNote, setReviewNote] = useState("");

  let storedUser = null;

  try {
    storedUser = JSON.parse(localStorage.getItem("veloop_user") || "null");
  } catch {
    storedUser = null;
  }

  const normalizedRole = String(storedUser?.role || "")
    .replace(/^ROLE_/i, "")
    .toUpperCase();

  const isAdmin = normalizedRole === "ADMIN";

  const loadWithdrawals = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await getAdminWithdrawals({
        page,
        limit: 20,
        status: status === "ALL" ? "" : status,
      });

      if (response?.success === false) {
        throw new Error(
          response.message || "Unable to load withdrawal requests."
        );
      }

      const result = response?.data ?? response;

      const records = Array.isArray(result?.content)
        ? result.content
        : Array.isArray(result?.items)
          ? result.items
          : [];

      setWithdrawals(records);

      setPagination({
        totalElements: Number(result?.totalElements ?? records.length),
        totalPages: Number(result?.totalPages ?? 0),
        hasNext: Boolean(result?.hasNext),
        hasPrevious: Boolean(result?.hasPrevious),
      });
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.message ||
          "Failed to load withdrawals. Please try again."
      );

      setWithdrawals([]);

      setPagination({
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
      });
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    if (!localStorage.getItem("veloop_token")) {
      navigate("/login", { replace: true });
      return;
    }

    if (!isAdmin) return;

    loadWithdrawals();
  }, [navigate, isAdmin, loadWithdrawals]);

  function logout() {
    localStorage.removeItem("veloop_token");
    localStorage.removeItem("veloop_user");
    navigate("/login", { replace: true });
  }

  function openWithdrawal(item) {
    setSelected(item);
    setShowRecipientDetails(false);
    setRejectModal(false);
    setRejectionReason("");
    setReviewNote("");
    setError("");
    setNotice("");
  }

  function closeWithdrawal() {
    if (actionLoading) return;

    setSelected(null);
    setShowRecipientDetails(false);
    setRejectModal(false);
    setRejectionReason("");
    setReviewNote("");
  }

  async function runAction(action, successMessage) {
    if (!selected || actionLoading) return;

    if (action === "reject" && !rejectionReason.trim()) {
      setError("Please enter a rejection reason.");
      return;
    }

    const withdrawalId = selected.withdrawalId;

    setActionLoading(true);
    setError("");
    setNotice("");

    try {
      let response;

      if (action === "processing") {
        response = await markWithdrawalProcessing(withdrawalId);
      } else if (action === "approve") {
        response = await approveWithdrawal(withdrawalId);
      } else {
        response = await rejectWithdrawal(
          withdrawalId,
          rejectionReason.trim(),
          reviewNote.trim()
        );
      }

      if (response?.success === false) {
        throw new Error(response.message || "The action was unsuccessful.");
      }

      setNotice(successMessage);
      setSelected(null);
      setShowRecipientDetails(false);
      setRejectModal(false);
      setRejectionReason("");
      setReviewNote("");

      await loadWithdrawals();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.message ||
          "The action failed. Please try again."
      );
    } finally {
      setActionLoading(false);
    }
  }

  function changeStatus(nextStatus) {
    setStatus(nextStatus);
    setPage(1);
    setNotice("");
    setError("");
  }

  // Preserve the exact page and ordering returned by the backend.
  // Search is omitted because the backend admin endpoint has no search parameter.
  const visibleWithdrawals = withdrawals;

  if (!localStorage.getItem("veloop_token")) {
    return null;
  }

  if (!isAdmin) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#080d1b] px-4 text-white">
        <div className="w-full max-w-md rounded-2xl border border-white/10 bg-[#111a2e] p-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-500/10 text-2xl text-red-300">
            !
          </div>

          <h1 className="text-2xl font-bold">Admin access required</h1>

          <p className="mt-3 text-sm leading-6 text-slate-400">
            This account does not have the administrator role. Sign in with an
            authorized admin account to manage withdrawals.
          </p>

          <button
            type="button"
            onClick={logout}
            className="mt-6 rounded-xl bg-indigo-600 px-5 py-3 font-semibold transition hover:bg-indigo-500"
          >
            Sign out
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#080d1b] text-white">
      <header className="border-b border-white/10 bg-[#0d1426]">
        <div className="mx-auto flex max-w-[1500px] flex-wrap items-center justify-between gap-4 px-4 py-5 sm:px-6 lg:px-8">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-400">
              VELoop Rewards
            </p>

            <h1 className="mt-1 text-2xl font-bold sm:text-3xl">
              Admin Dashboard
            </h1>

          </div>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => navigate("/admin/reconciliation")}
              className="rounded-xl border border-white/15 px-4 py-2.5 text-sm font-semibold transition hover:bg-white/5"
            >
              Wallet reconciliation
            </button>

            <button
              type="button"
              onClick={loadWithdrawals}
              disabled={loading}
              className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold transition hover:bg-indigo-500 disabled:opacity-50"
            >
              {loading ? "Refreshing..." : "Refresh"}
            </button>

            <button
              type="button"
              onClick={logout}
              className="rounded-xl border border-white/15 px-4 py-2.5 text-sm font-medium transition hover:bg-white/5"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-[1500px] space-y-6 px-4 py-6 sm:px-6 sm:py-8 lg:px-8">
        {error && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200"
          >
            <span>{error}</span>

            <button
              type="button"
              onClick={() => setError("")}
              className="font-semibold underline"
            >
              Dismiss
            </button>
          </div>
        )}

        {notice && (
          <div
            role="status"
            className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200"
          >
            {notice}
          </div>
        )}

        <section className="overflow-hidden rounded-2xl border border-white/10 bg-[#111a2e]">
          <div className="flex flex-col gap-4 border-b border-white/10 p-5">
            <div>
              <h2 className="text-lg font-bold">Withdrawal Requests</h2>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-slate-400">
                {loading
                  ? "Loading requests…"
                  : `${pagination.totalElements.toLocaleString("en-IN")} request${pagination.totalElements === 1 ? "" : "s"}`}
              </p>

              <label className="flex items-center gap-3 text-sm text-slate-400">
                <span>Status</span>
                <select
                  value={status}
                  onChange={(event) => changeStatus(event.target.value)}
                  className="rounded-xl border border-white/15 bg-[#080d1b] px-3 py-2.5 text-white outline-none focus:border-indigo-400"
                >
                  {STATUSES.map((item) => (
                    <option key={item} value={item}>
                      {item === "ALL" ? "All statuses" : item}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>

          {loading ? (
            <div className="flex min-h-48 items-center justify-center gap-3 text-slate-400">
              <span className="h-5 w-5 animate-spin rounded-full border-2 border-indigo-400 border-t-transparent" />
              Loading withdrawal requests...
            </div>
          ) : visibleWithdrawals.length === 0 ? (
            <div className="px-5 py-16 text-center">
              <p className="font-semibold">No withdrawal requests found</p>
              <p className="mt-2 text-sm text-slate-400">
                There are no requests for this status.
              </p>
            </div>
          ) : (
            <>
              <div className="hidden overflow-x-auto lg:block">
                <table className="w-full text-left text-sm">
                  <thead className="bg-white/[0.03] text-xs uppercase tracking-wide text-slate-400">
                    <tr>
                      <th className="px-5 py-4">Withdrawal / User</th>
                      <th className="px-5 py-4">Payout</th>
                      <th className="px-5 py-4">Required</th>
                      <th className="px-5 py-4">Status</th>
                      <th className="px-5 py-4">Requested</th>
                      <th className="px-5 py-4">Action</th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-white/10">
                    {visibleWithdrawals.map((item) => (
                      <tr
                        key={item.withdrawalId}
                        className="transition hover:bg-white/[0.025]"
                      >
                        <td className="px-5 py-4">
                          <p className="font-semibold">{item.userName || "—"}</p>
                          <p className="mt-1 text-xs text-slate-400">
                            {item.userEmail || "—"}
                          </p>
                          <p className="mt-1 break-all font-mono text-xs text-indigo-300">
                            {item.withdrawalId}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          <p className="font-semibold">
                            {formatAmount(item.currencyAmount, item.currency)}
                          </p>
                          <p className="mt-1 text-xs text-slate-400">
                            {item.payoutMethod || "—"}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          <p className="font-semibold">
                            {formatVes(item.payoutAmount)}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          <StatusBadge status={item.status} />
                        </td>

                        <td className="px-5 py-4 text-slate-300">
                          {formatDate(item.requestedAt || item.createdAt)}
                        </td>

                        <td className="px-5 py-4">
                          <button
                            type="button"
                            onClick={() => openWithdrawal(item)}
                            className="rounded-lg border border-indigo-400/40 px-3 py-2 font-semibold text-indigo-300 transition hover:bg-indigo-500/10"
                          >
                            Review
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="space-y-3 p-4 lg:hidden">
                {visibleWithdrawals.map((item) => (
                  <article
                    key={item.withdrawalId}
                    className="rounded-xl border border-white/10 bg-[#0b1222] p-4"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="break-all font-mono text-xs text-indigo-300">
                          {item.withdrawalId}
                        </p>
                        <p className="mt-2 font-semibold">
                          {item.userName || "—"}
                        </p>
                        <p className="mt-1 break-all text-xs text-slate-400">
                          {item.userEmail || "—"}
                        </p>
                      </div>

                      <StatusBadge status={item.status} />
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs text-slate-400">Payout</p>
                        <p className="mt-1 font-semibold">
                          {formatAmount(item.currencyAmount, item.currency)}
                        </p>
                        <p className="mt-1 text-xs text-slate-400">
                          {item.payoutMethod || "—"}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs text-slate-400">Required</p>
                        <p className="mt-1 font-semibold">
                          {formatVes(item.payoutAmount)}
                        </p>
                      </div>
                    </div>

                    <p className="mt-4 text-xs text-slate-400">
                      {formatDate(item.requestedAt || item.createdAt)}
                    </p>

                    <button
                      type="button"
                      onClick={() => openWithdrawal(item)}
                      className="mt-4 w-full rounded-xl border border-indigo-400/40 px-4 py-3 text-sm font-semibold text-indigo-300 transition hover:bg-indigo-500/10"
                    >
                      Review withdrawal
                    </button>
                  </article>
                ))}
              </div>

              <div className="flex flex-wrap items-center justify-between gap-3 border-t border-white/10 px-5 py-4">
                <p className="text-sm text-slate-400">
                  Page {page}
                  {pagination.totalPages > 0
                    ? ` of ${pagination.totalPages}`
                    : ""}
                  <span className="ml-2">
                    ({visibleWithdrawals.length} shown on this page)
                  </span>
                </p>

                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={!pagination.hasPrevious || loading}
                    onClick={() => setPage((current) => Math.max(1, current - 1))}
                    className="rounded-lg border border-white/15 px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Previous
                  </button>

                  <button
                    type="button"
                    disabled={!pagination.hasNext || loading}
                    onClick={() => setPage((current) => current + 1)}
                    className="rounded-lg border border-white/15 px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              </div>
            </>
          )}
        </section>
      </div>

      {selected && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/75 p-0 backdrop-blur-sm sm:items-center sm:p-4"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeWithdrawal();
          }}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="withdrawal-detail-title"
            className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-t-2xl border border-white/10 bg-[#111a2e] p-5 shadow-2xl sm:rounded-2xl sm:p-6"
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2
                  id="withdrawal-detail-title"
                  className="text-xl font-bold"
                >
                  Withdrawal Details
                </h2>
                <p className="mt-2 break-all font-mono text-xs text-indigo-300">
                  {selected.withdrawalId}
                </p>
              </div>

              <button
                type="button"
                disabled={actionLoading}
                onClick={closeWithdrawal}
                aria-label="Close withdrawal details"
                className="rounded-lg px-3 py-2 text-slate-400 hover:bg-white/10 hover:text-white disabled:opacity-50"
              >
                ✕
              </button>
            </div>

            <div className="mt-5">
              <DetailRow label="Status">
                <StatusBadge status={selected.status} />
              </DetailRow>

              <DetailRow label="User ID">{selected.userId}</DetailRow>
              <DetailRow label="User name">{selected.userName}</DetailRow>
              <DetailRow label="Email">{selected.userEmail}</DetailRow>
              <DetailRow label="Payout method">{selected.payoutMethod}</DetailRow>

              <DetailRow label="Payout amount">
                {formatAmount(selected.currencyAmount, selected.currency)}
              </DetailRow>

              <DetailRow label="Required VEs">
                {formatVes(selected.payoutAmount)}
              </DetailRow>

              <DetailRow label="Recipient details">
                <div className="flex flex-wrap items-center gap-3 sm:justify-end">
                  <span className="break-all">
                    {showRecipientDetails
                      ? selected.payoutDetails || "No recipient details supplied"
                      : "••••••••••"}
                  </span>

                  {selected.payoutDetails && (
                    <button
                      type="button"
                      onClick={() =>
                        setShowRecipientDetails((current) => !current)
                      }
                      aria-pressed={showRecipientDetails}
                      className="shrink-0 font-semibold text-indigo-300 underline"
                    >
                      {showRecipientDetails ? "Hide" : "Reveal"}
                    </button>
                  )}
                </div>
              </DetailRow>

              <DetailRow label="Transaction ID">
                {selected.transactionId}
              </DetailRow>

              <DetailRow label="Requested at">
                {formatDate(selected.requestedAt || selected.createdAt)}
              </DetailRow>

              <DetailRow label="Processed at">
                {formatDate(selected.processedAt)}
              </DetailRow>

              <DetailRow label="Review note">
                {selected.reviewNote}
              </DetailRow>

              <DetailRow label="Rejection reason">
                {selected.rejectionReason}
              </DetailRow>
            </div>

            {selected.status === "PENDING" && (
              <div className="mt-6 rounded-xl border border-amber-400/20 bg-amber-500/5 p-4">
                <p className="text-sm leading-6 text-amber-100">
                  Start the review before approving this request. The backend
                  validates the status transition.
                </p>

                <button
                  type="button"
                  disabled={actionLoading}
                  onClick={() => {
                    if (
                      window.confirm(
                        "Move this withdrawal to PROCESSING?"
                      )
                    ) {
                      runAction(
                        "processing",
                        "Withdrawal moved to processing."
                      );
                    }
                  }}
                  className="mt-4 w-full rounded-xl bg-blue-600 px-4 py-3 font-semibold transition hover:bg-blue-500 disabled:opacity-50"
                >
                  {actionLoading ? "Please wait..." : "Move to Processing"}
                </button>
              </div>
            )}

            {selected.status === "PROCESSING" && (
              <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2">
                <button
                  type="button"
                  disabled={actionLoading}
                  onClick={() => {
                    if (
                      window.confirm(
                        "Approve this withdrawal? Verify the payout details first."
                      )
                    ) {
                      runAction(
                        "approve",
                        "Withdrawal approved successfully."
                      );
                    }
                  }}
                  className="rounded-xl bg-emerald-600 px-4 py-3 font-semibold transition hover:bg-emerald-500 disabled:opacity-50"
                >
                  {actionLoading ? "Please wait..." : "Approve Withdrawal"}
                </button>

                <button
                  type="button"
                  disabled={actionLoading}
                  onClick={() => {
                    setRejectionReason("");
                    setReviewNote("");
                    setError("");
                    setRejectModal(true);
                  }}
                  className="rounded-xl border border-red-400/40 px-4 py-3 font-semibold text-red-300 transition hover:bg-red-500/10 disabled:opacity-50"
                >
                  Reject Withdrawal
                </button>
              </div>
            )}

            {!["PENDING", "PROCESSING"].includes(selected.status) && (
              <p className="mt-6 rounded-xl border border-white/10 bg-white/[0.03] p-4 text-sm text-slate-400">
                This request is in a final state. No further action is
                available from this screen.
              </p>
            )}
          </section>
        </div>
      )}

      {rejectModal && selected && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/80 p-4">
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="reject-withdrawal-title"
            className="w-full max-w-lg rounded-2xl border border-white/10 bg-[#111a2e] p-5 sm:p-6"
          >
            <h2 id="reject-withdrawal-title" className="text-xl font-bold">
              Reject Withdrawal
            </h2>

            <p className="mt-2 text-sm leading-6 text-slate-400">
              Enter a reason for rejecting this request. The backend validates
              the action and applies its configured wallet reversal rules.
            </p>

            <form
              className="mt-5 space-y-4"
              onSubmit={(event) => {
                event.preventDefault();

                if (!rejectionReason.trim() || actionLoading) return;

                if (
                  window.confirm(
                    "Confirm rejection of this withdrawal? This action may trigger a wallet reversal."
                  )
                ) {
                  runAction("reject", "Withdrawal rejected.");
                }
              }}
            >
              <div>
                <label
                  htmlFor="rejectionReason"
                  className="mb-2 block text-sm font-medium text-slate-300"
                >
                  Rejection reason *
                </label>

                <textarea
                  id="rejectionReason"
                  required
                  maxLength={500}
                  rows={3}
                  value={rejectionReason}
                  onChange={(event) => setRejectionReason(event.target.value)}
                  placeholder="Enter the reason for rejection"
                  className="w-full resize-y rounded-xl border border-white/15 bg-[#080d1b] px-4 py-3 text-white outline-none placeholder:text-slate-500 focus:border-indigo-400"
                />
              </div>

              <div>
                <label
                  htmlFor="reviewNote"
                  className="mb-2 block text-sm font-medium text-slate-300"
                >
                  Review note (optional)
                </label>

                <textarea
                  id="reviewNote"
                  maxLength={500}
                  rows={2}
                  value={reviewNote}
                  onChange={(event) => setReviewNote(event.target.value)}
                  placeholder="Internal review note"
                  className="w-full resize-y rounded-xl border border-white/15 bg-[#080d1b] px-4 py-3 text-white outline-none placeholder:text-slate-500 focus:border-indigo-400"
                />
              </div>

              <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  disabled={actionLoading}
                  onClick={() => setRejectModal(false)}
                  className="rounded-xl border border-white/15 px-4 py-3 font-semibold disabled:opacity-50"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={actionLoading || !rejectionReason.trim()}
                  className="rounded-xl bg-red-600 px-4 py-3 font-semibold transition hover:bg-red-500 disabled:opacity-50"
                >
                  {actionLoading ? "Submitting..." : "Confirm Rejection"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}
