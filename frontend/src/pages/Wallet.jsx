import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getWallet, getWalletTransactions } from "../api/walletApi";
import { cancelWithdrawal, getWithdrawalById, getWithdrawalHistory } from "../api/withdrawalApi";

const PAGE_SIZE = 10;

const secondaryCurrencies = [
  { key: "sves", label: "SVEs", color: "#69A7FF", icon: "◆" },
  { key: "gems", label: "Gems", color: "#D58AFF", icon: "✦" },
  { key: "tokens", label: "Tokens", color: "#F1B95B", icon: "●" },
  { key: "spins", label: "Spins", color: "#35D6A0", icon: "↻" },
];

const transactionFilters = [
  { value: "ALL", label: "All activity" },
  { value: "REWARDS", label: "Rewards" },
  { value: "WITHDRAWALS", label: "Withdrawals" },
];

const withdrawalStatusFilters = [
  { value: "ALL", label: "All statuses" },
  { value: "PENDING", label: "Pending" },
  { value: "PROCESSING", label: "Processing" },
  { value: "APPROVED", label: "Approved" },
  { value: "REJECTED", label: "Rejected" },
  { value: "CANCELLED", label: "Cancelled" },
];

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "—";
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "—";
  return new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 }).format(amount);
}

function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getErrorMessage(error) {
  const status = error.response?.status;
  if (!error.response) return "Unable to connect to the server. Check your connection and try again.";
  if (status === 401) return "Your session has expired. Please log in again.";
  if (status === 403) return "You do not have permission to view this information.";
  if (status === 429) return "Too many requests. Please wait and try again.";
  if (status >= 500) return "The server is temporarily unavailable. Please try again.";
  return error.response?.data?.message || error.message || "Unable to load the requested information.";
}

function unwrapData(response) {
  return response?.data ?? response;
}

// Withdrawal endpoints return ApiResponse<T> ({ success, message, data }),
// while callers may receive either that envelope or an Axios response around it.
function unwrapWithdrawal(response) {
  let data = unwrapData(response);

  if (data?.success === false) {
    throw new Error(data.message || "Unable to load withdrawal status.");
  }

  // Handle an API envelope whether it is returned directly or nested in Axios data.
  if (data?.data && typeof data.data === "object" && !Array.isArray(data.data)) {
    data = data.data;
  }

  return data;
}

function normalizeWallet(response) {
  const data = unwrapData(response);
  if (data?.success === false) {
    throw new Error(data.message || "Unable to load wallet.");
  }
  // Support both { ves, sves, ... } and { success: true, wallet: { ... } } API shapes.
  const wallet = data?.wallet ?? data;
  if (!wallet || typeof wallet !== "object" || Array.isArray(wallet)) {
    throw new Error("The server returned an invalid wallet response.");
  }
  return wallet;
}

function normalizeList(response) {
  const data = unwrapData(response);
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.transactions)) return data.transactions;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.data)) return data.data;
  return [];
}

function normalizePage(response) {
  // Backend responses may be wrapped as { success, message, data: PageResponse }.
  let data = unwrapData(response);
  if (data?.data && typeof data.data === "object" && !Array.isArray(data.data)) {
    data = data.data;
  }
  const pageNumber = data?.page ?? (data?.number != null ? Number(data.number) + 1 : 1);
  const totalElements = Number(data?.totalElements ?? data?.total ?? 0);
  const totalPages = Number(data?.totalPages ?? 0);
  const page = Math.max(1, Number(pageNumber) || 1);
  return {
    items: normalizeList(data),
    page,
    totalElements,
    totalPages,
    hasNext: data?.hasNext ?? (totalPages > 0 ? page < totalPages : false),
    hasPrevious: data?.hasPrevious ?? page > 1,
  };
}

function getTransactionLabel(transaction) {
  return transaction.description || transaction.source || transaction.transactionType || "Wallet transaction";
}

function isWithdrawalTransaction(transaction) {
  return String(transaction.transactionType || transaction.type || "").toUpperCase() === "WITHDRAWAL";
}

function getDisplayedTransactionStatus(transaction) {
  if (isWithdrawalTransaction(transaction)) {
    return getWithdrawalStatusLabel(transaction.withdrawalStatus || "STATUS_UNAVAILABLE");
  }

  return getGeneralStatusLabel(transaction.status);
}

function getGeneralStatusLabel(status) {
  const value = String(status || "").toUpperCase();
  const labels = {
    PENDING: "Pending",
    PROCESSING: "Processing",
    APPROVED: "Approved",
    REJECTED: "Rejected",
    CANCELLED: "Cancelled",
    CANCELED: "Cancelled",
    COMPLETED: "Completed",
    SUCCESS: "Completed",
    SUCCESSFUL: "Completed",
    FAILED: "Failed",
    REVERSED: "Reversed",
    STATUS_UNAVAILABLE: "Request status unavailable",
  };
  return labels[value] || "Status unavailable";
}

function getWithdrawalStatusMessage(status) {
  const value = String(status || "").toUpperCase();
  const messages = {
    PENDING: "Your withdrawal request is waiting for review.",
    PROCESSING: "Your withdrawal request is being processed.",
    APPROVED: "Your withdrawal request has been approved.",
    REJECTED: "Your withdrawal request was rejected.",
    CANCELLED: "This withdrawal request was cancelled.",
    STATUS_UNAVAILABLE: "The withdrawal status could not be loaded. Refresh history to try again.",
  };
  return messages[value] || "The withdrawal status is not available.";
}

function getAmountPrefix(transaction) {
  const type = String(transaction.transactionType || transaction.type || "").toUpperCase();
  if (["CREDIT", "REWARD", "BONUS", "REFERRAL", "DAILY_REWARD", "AD_REWARD", "GAME_REWARD", "ADMIN_CREDIT", "EXCHANGE_CREDIT"].includes(type)) return "+";
  if (["DEBIT", "WITHDRAWAL", "EXCHANGE_DEBIT", "ADMIN_DEBIT"].includes(type)) return "−";
  return "";
}

function getStatusStyle(status) {
  const value = String(status || "").toUpperCase();
  if (["SUCCESS", "COMPLETED", "APPROVED", "SUCCESSFUL"].includes(value)) return "border-emerald-400/25 bg-emerald-400/10 text-emerald-300";
  if (["PENDING", "PROCESSING", "IN_PROGRESS"].includes(value)) return "border-amber-400/25 bg-amber-400/10 text-amber-200";
  if (["FAILED", "REJECTED", "CANCELLED", "CANCELED"].includes(value)) return "border-red-400/25 bg-red-400/10 text-red-300";
  return "border-white/10 bg-white/5 text-[#C1C8DA]";
}

function WalletHeader({ onLogout, onRefresh, refreshing }) {
  return (
    <header className="flex flex-col gap-4 rounded-[22px] border border-white/[0.07] bg-[#141625] p-4 sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:py-5">
      <div className="flex min-w-0 items-center gap-3">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#292442] text-xl text-[#B7AAFF]" aria-hidden="true">◈</div>
        <div>
          <h1 className="text-xl font-bold tracking-tight sm:text-2xl">VELoop <span className="text-[#B7AAFF]">Wallet</span></h1>
          <p className="mt-0.5 text-xs text-[#8F9BB5] sm:text-sm">Your rewards at a glance</p>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-2 sm:flex">
        <button type="button" onClick={onRefresh} disabled={refreshing} className="min-h-11 rounded-xl border border-[#343B56] px-4 py-2 text-sm font-medium transition hover:bg-white/5 disabled:opacity-60">
          {refreshing ? "Refreshing…" : "↻ Refresh"}
        </button>
        <button type="button" onClick={onLogout} className="min-h-11 rounded-xl bg-[#6855D9] px-4 py-2 text-sm font-semibold transition hover:bg-[#7968E8]">Logout</button>
      </div>
    </header>
  );
}

function LoadingState({ label }) {
  return (
    <div role="status" aria-live="polite" className="rounded-2xl border border-[#343B56] bg-[#202438] px-5 py-10 text-center text-sm text-[#A7B1C8]">
      <span className="mx-auto mb-3 block h-6 w-6 animate-spin rounded-full border-2 border-[#53617D] border-t-[#9275FF]" aria-hidden="true" />
      {label}
    </div>
  );
}

function ErrorState({ message, onRetry }) {
  return (
    <div className="rounded-2xl border border-red-400/25 bg-[#202438] p-5">
      <p role="alert" className="break-words text-sm text-red-200">{message}</p>
      <button type="button" onClick={onRetry} className="mt-4 min-h-10 rounded-xl bg-[#5144C8] px-4 py-2 text-sm font-semibold transition hover:bg-[#6254DB]">Try again</button>
    </div>
  );
}

function CurrencyCard({ currency, value, loading, error }) {
  return (
    <article className="min-w-0 rounded-[20px] border border-[#343B56] bg-[#202438] p-4 transition hover:border-[#575078] sm:p-5">
      <div className="flex items-center gap-3">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-lg" style={{ color: currency.color, backgroundColor: `${currency.color}18` }} aria-hidden="true">{currency.icon}</span>
        <h3 className="text-sm font-semibold text-[#C7CDE0]">{currency.label}</h3>
      </div>
      <p className="mt-4 break-words text-2xl font-bold sm:text-3xl" aria-live="polite">{loading || error ? "—" : formatAmount(value)}</p>
      <p className="mt-1 text-xs text-[#8F9BB5]">Available balance</p>
    </article>
  );
}

function FilterButtons({ value, onChange }) {
  return (
    <div className="flex flex-wrap gap-2" role="group" aria-label="Filter wallet activity">
      {transactionFilters.map((filter) => (
        <button key={filter.value} type="button" aria-pressed={value === filter.value} onClick={() => onChange(filter.value)} className={`min-h-10 rounded-xl border px-3 py-2 text-sm font-medium transition ${value === filter.value ? "border-[#7567FF] bg-[#5144C8]/25 text-[#D1C8FF]" : "border-[#343B56] text-[#A7B1C8] hover:bg-white/5 hover:text-white"}`}>
          {filter.label}
        </button>
      ))}
    </div>
  );
}

function WithdrawalStatusFilter({ value, onChange }) {
  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
      <label htmlFor="withdrawal-status-filter" className="text-sm text-[#A7B1C8]">Request status</label>
      <select
        id="withdrawal-status-filter"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="min-h-10 w-full rounded-xl border border-[#343B56] bg-[#202438] px-3 py-2 text-sm text-[#F5F6FF] outline-none focus:border-[#8B7BFF] focus:ring-2 focus:ring-[#8B7BFF]/30 sm:w-auto"
      >
        {withdrawalStatusFilters.map((filter) => (
          <option key={filter.value} value={filter.value} className="bg-[#202438]">{filter.label}</option>
        ))}
      </select>
    </div>
  );
}

function getWithdrawalMethodLabel(withdrawal) {
  const method = String(withdrawal.payoutMethod || withdrawal.method || "Payout").replaceAll("_", " ").trim();
  return `${method.charAt(0).toUpperCase()}${method.slice(1)} withdrawal`;
}

function getWithdrawalStatusLabel(status) {
  const value = String(status || "").toUpperCase();
  const labels = {
    PENDING: "Pending",
    PROCESSING: "Processing",
    APPROVED: "Approved",
    REJECTED: "Rejected",
    CANCELLED: "Cancelled",
    STATUS_UNAVAILABLE: "Request status unavailable",
  };
  return labels[value] || "Status unavailable";
}

function WithdrawalHistory({ withdrawals, onCancelWithdrawal, cancellingWithdrawalId }) {
  return (
    <>
      <div className="divide-y divide-[#343B56] md:hidden">
        {withdrawals.map((withdrawal, index) => (
          <article key={withdrawal.withdrawalId ?? index} className="space-y-3 px-4 py-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="break-words text-sm font-semibold">{getWithdrawalMethodLabel(withdrawal)}</p>
                <p className="mt-1 text-xs text-[#A7B1C8]">{formatDate(withdrawal.requestedAt || withdrawal.createdAt || withdrawal.updatedAt)}</p>
                {withdrawal.withdrawalId && <p className="mt-1 break-all text-xs text-[#7F8AA6]">Request ID: {withdrawal.withdrawalId}</p>}
              </div>
              <p className="shrink-0 text-right font-semibold">₹{formatAmount(withdrawal.payoutAmount)}</p>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="text-xs text-[#A7B1C8]">{formatAmount(withdrawal.currencyAmount)} {withdrawal.currency || "VEs"} requested</span>
              <span className={`rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(withdrawal.status)}`}>{getWithdrawalStatusLabel(withdrawal.status)}</span>
            </div>
            {String(withdrawal.status || "").toUpperCase() === "REJECTED" && withdrawal.rejectionReason && (
              <p className="rounded-xl border border-red-400/20 bg-red-400/5 px-3 py-2 text-xs leading-5 text-red-200">Rejection reason: {withdrawal.rejectionReason}</p>
            )}
            {withdrawal.reviewNote && (
              <p className="rounded-xl border border-[#343B56] bg-[#171827]/60 px-3 py-2 text-xs leading-5 text-[#A7B1C8]">Review note: {withdrawal.reviewNote}</p>
            )}
            {String(withdrawal.status || "").toUpperCase() === "PENDING" && withdrawal.withdrawalId && (
              <button
                type="button"
                onClick={() => onCancelWithdrawal(withdrawal.withdrawalId)}
                disabled={cancellingWithdrawalId === withdrawal.withdrawalId}
                className="min-h-10 w-full rounded-xl border border-red-400/30 px-4 py-2 text-sm font-semibold text-red-200 transition hover:bg-red-400/10 disabled:cursor-wait disabled:opacity-60"
              >
                {cancellingWithdrawalId === withdrawal.withdrawalId ? "Cancelling…" : "Cancel request"}
              </button>
            )}
          </article>
        ))}
      </div>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="bg-[#282E48] text-[#A7B1C8]"><tr>
            <th scope="col" className="px-5 py-4 font-medium">Payout request</th>
            <th scope="col" className="px-5 py-4 font-medium">VEs used</th>
            <th scope="col" className="px-5 py-4 font-medium">Payout</th>
            <th scope="col" className="px-5 py-4 font-medium">Status</th>
            <th scope="col" className="px-5 py-4 font-medium">Requested</th>
          </tr></thead>
          <tbody className="divide-y divide-[#343B56]">
            {withdrawals.map((withdrawal, index) => (
              <tr key={withdrawal.withdrawalId ?? index} className="transition hover:bg-white/[0.025]">
                <td className="max-w-[260px] px-5 py-4"><p className="break-words font-medium">{getWithdrawalMethodLabel(withdrawal)}</p>{withdrawal.withdrawalId && <p className="mt-1 break-all text-xs text-[#7F8AA6]">ID: {withdrawal.withdrawalId}</p>}{String(withdrawal.status || "").toUpperCase() === "REJECTED" && withdrawal.rejectionReason && <p className="mt-2 break-words text-xs text-red-200">Rejection reason: {withdrawal.rejectionReason}</p>}{withdrawal.reviewNote && <p className="mt-1 break-words text-xs text-[#A7B1C8]">Review note: {withdrawal.reviewNote}</p>}</td>
                <td className="whitespace-nowrap px-5 py-4">{formatAmount(withdrawal.currencyAmount)} {withdrawal.currency || "VEs"}</td>
                <td className="whitespace-nowrap px-5 py-4 font-semibold">₹{formatAmount(withdrawal.payoutAmount)}</td>
                <td className="px-5 py-4"><span className={`rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(withdrawal.status)}`}>{getWithdrawalStatusLabel(withdrawal.status)}</span></td>
                <td className="whitespace-nowrap px-5 py-4 text-[#A7B1C8]">{formatDate(withdrawal.requestedAt || withdrawal.createdAt || withdrawal.updatedAt)}</td>
                <td className="px-5 py-4">
                  {String(withdrawal.status || "").toUpperCase() === "PENDING" && withdrawal.withdrawalId ? (
                    <button
                      type="button"
                      onClick={() => onCancelWithdrawal(withdrawal.withdrawalId)}
                      disabled={cancellingWithdrawalId === withdrawal.withdrawalId}
                      className="min-h-9 whitespace-nowrap rounded-lg border border-red-400/30 px-3 py-2 text-xs font-semibold text-red-200 transition hover:bg-red-400/10 disabled:cursor-wait disabled:opacity-60"
                    >
                      {cancellingWithdrawalId === withdrawal.withdrawalId ? "Cancelling…" : "Cancel request"}
                    </button>
                  ) : <span className="text-xs text-[#7F8AA6]">—</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

function EmptyState({ isWithdrawals = false }) {
  return (
    <div className="px-5 py-12 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#292442] text-xl text-[#B7AAFF]" aria-hidden="true">◈</div>
      <h3 className="mt-4 font-semibold">{isWithdrawals ? "No withdrawal requests found" : "No transactions found"}</h3>
      <p className="mt-2 text-sm text-[#A7B1C8]">{isWithdrawals ? "Requests matching this status will appear here." : "Your wallet activity will appear here when available."}</p>
    </div>
  );
}

function Pagination({ pagination, page, onPageChange, loading }) {
  const firstItem = pagination.totalElements === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;
  const lastItem = Math.min(page * PAGE_SIZE, pagination.totalElements);
  return (
    <div className="flex flex-col gap-3 border-t border-[#343B56] px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
      <p className="text-sm text-[#A7B1C8]" aria-live="polite">Showing {firstItem}–{lastItem} of {pagination.totalElements}</p>
      <div className="flex items-center gap-2">
        <button type="button" disabled={loading || !pagination.hasPrevious} onClick={() => onPageChange(page - 1)} className="min-h-10 rounded-xl border border-[#343B56] px-4 text-sm transition hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40">← Previous</button>
        <span className="min-w-20 text-center text-sm text-[#C7CDE0]">Page {page}{pagination.totalPages > 0 ? ` of ${pagination.totalPages}` : ""}</span>
        <button type="button" disabled={loading || !pagination.hasNext} onClick={() => onPageChange(page + 1)} className="min-h-10 rounded-xl border border-[#343B56] px-4 text-sm transition hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40">Next →</button>
      </div>
    </div>
  );
}

function TransactionHistory({ transactions }) {
  return (
    <>
      <div className="divide-y divide-[#343B56] md:hidden">
        {transactions.map((transaction, index) => {
          const prefix = getAmountPrefix(transaction);
          return (
            <article key={transaction.transactionId ?? index} className="space-y-3 px-4 py-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <p className="break-words text-sm font-semibold">{getTransactionLabel(transaction)}</p>
                  <p className="mt-1 text-xs text-[#A7B1C8]">{formatDate(transaction.createdAt || transaction.updatedAt)}</p>
                </div>
                <p className="max-w-[45%] break-words text-right font-semibold">{prefix}{formatAmount(transaction.amount)}{transaction.currency ? ` ${transaction.currency}` : ""}</p>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-xs text-[#A7B1C8]">{transaction.transactionType || transaction.type || "Transaction"}</span>
                <span className={`rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(isWithdrawalTransaction(transaction) ? transaction.withdrawalStatus : transaction.status)}`}>{getDisplayedTransactionStatus(transaction)}</span>
              </div>
              {isWithdrawalTransaction(transaction) && (
                <div className="space-y-1 rounded-xl border border-[#343B56] bg-[#171827]/60 px-3 py-2.5 text-xs leading-5 text-[#A7B1C8]">
                  <p>{getWithdrawalStatusMessage(transaction.withdrawalStatus)}</p>
                  {transaction.withdrawalRejectionReason && <p className="break-words text-red-200">Rejection reason: {transaction.withdrawalRejectionReason}</p>}
                  {transaction.withdrawalReviewNote && <p className="break-words">Review note: {transaction.withdrawalReviewNote}</p>}
                  {transaction.withdrawalId && <p className="break-all text-[#7F8AA6]">Request ID: {transaction.withdrawalId}</p>}
                </div>
              )}
            </article>
          );
        })}
      </div>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[620px] text-left text-sm">
          <thead className="bg-[#282E48] text-[#A7B1C8]"><tr>
            <th scope="col" className="px-5 py-4 font-medium">Transaction</th>
            <th scope="col" className="px-5 py-4 font-medium">Currency</th>
            <th scope="col" className="px-5 py-4 font-medium">Amount</th>
            <th scope="col" className="px-5 py-4 font-medium">Status</th>
            <th scope="col" className="px-5 py-4 font-medium">Date</th>
          </tr></thead>
          <tbody className="divide-y divide-[#343B56]">
            {transactions.map((transaction, index) => (
              <tr key={transaction.transactionId ?? index} className="transition hover:bg-white/[0.025]">
                <td className="max-w-[280px] px-5 py-4"><p className="break-words font-medium">{getTransactionLabel(transaction)}</p>{transaction.transactionId != null && <p className="mt-1 break-all text-xs text-[#7F8AA6]">Transaction ID: {transaction.transactionId}</p>}{isWithdrawalTransaction(transaction) && transaction.withdrawalId && <p className="mt-1 break-all text-xs text-[#7F8AA6]">Request ID: {transaction.withdrawalId}</p>}{isWithdrawalTransaction(transaction) && transaction.withdrawalRejectionReason && <p className="mt-2 break-words text-xs text-red-200">Rejection reason: {transaction.withdrawalRejectionReason}</p>}{isWithdrawalTransaction(transaction) && transaction.withdrawalReviewNote && <p className="mt-1 break-words text-xs text-[#A7B1C8]">Review note: {transaction.withdrawalReviewNote}</p>}</td>
                <td className="whitespace-nowrap px-5 py-4">{transaction.currency || "—"}</td>
                <td className="whitespace-nowrap px-5 py-4 font-semibold">{getAmountPrefix(transaction)}{formatAmount(transaction.amount)}</td>
                <td className="px-5 py-4"><span className={`rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(isWithdrawalTransaction(transaction) ? transaction.withdrawalStatus : transaction.status)}`}>{getDisplayedTransactionStatus(transaction)}</span>{isWithdrawalTransaction(transaction) && <p className="mt-2 min-w-40 text-xs leading-5 text-[#A7B1C8]">{getWithdrawalStatusMessage(transaction.withdrawalStatus)}</p>}</td>
                <td className="whitespace-nowrap px-5 py-4 text-[#A7B1C8]">{formatDate(transaction.createdAt || transaction.updatedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

export default function Wallet() {
  const navigate = useNavigate();
  const mountedRef = useRef(false);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [transactionPage, setTransactionPage] = useState(1);
  const [transactionFilter, setTransactionFilter] = useState("ALL");
  const [withdrawalStatusFilter, setWithdrawalStatusFilter] = useState("ALL");
  const [transactionPagination, setTransactionPagination] = useState({ totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false });
  const [walletLoading, setWalletLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [walletError, setWalletError] = useState("");
  const [transactionError, setTransactionError] = useState("");
  const [withdrawalActionError, setWithdrawalActionError] = useState("");
  const [withdrawalActionMessage, setWithdrawalActionMessage] = useState("");
  const [cancellingWithdrawalId, setCancellingWithdrawalId] = useState(null);

  const loadWallet = useCallback(async () => {
    setWalletLoading(true);
    setWalletError("");
    try {
      const data = normalizeWallet(await getWallet());
      if (mountedRef.current) setWallet(data);
    } catch (error) {
      if (mountedRef.current) setWalletError(error.message && !error.response ? error.message : getErrorMessage(error));
    } finally {
      if (mountedRef.current) setWalletLoading(false);
    }
  }, []);

  const loadTransactions = useCallback(async () => {
    setTransactionsLoading(true);
    setTransactionError("");
    try {
      const response = transactionFilter === "WITHDRAWALS"
        ? await getWithdrawalHistory(transactionPage, PAGE_SIZE, withdrawalStatusFilter)
        : await getWalletTransactions(transactionPage, PAGE_SIZE, transactionFilter);
      const data = unwrapData(response);
      if (data?.success === false) throw new Error(data.message || "Unable to load history.");
      const result = normalizePage(response);
      let items = result.items;

      // A wallet-ledger debit can be COMPLETED while its withdrawal request is
      // still PENDING/PROCESSING or has later been REJECTED/CANCELLED. Resolve
      // each withdrawal ledger row against the authoritative withdrawal API.
      if (transactionFilter === "ALL") {
        items = await Promise.all(items.map(async (transaction) => {
          if (!isWithdrawalTransaction(transaction)) return transaction;
          const withdrawalId = transaction.referenceId;
          if (!withdrawalId) {
            return { ...transaction, withdrawalStatus: "STATUS_UNAVAILABLE" };
          }

          try {
            const responseData = unwrapWithdrawal(await getWithdrawalById(withdrawalId));
            if (!responseData?.status) {
              return { ...transaction, withdrawalStatus: "STATUS_UNAVAILABLE" };
            }
            return {
              ...transaction,
              withdrawalId: responseData.withdrawalId || withdrawalId,
              withdrawalStatus: responseData.status,
              withdrawalRejectionReason: responseData.rejectionReason || "",
              withdrawalReviewNote: responseData.reviewNote || "",
            };
          } catch {
            // Never mislabel the ledger's COMPLETED status as the payout status.
            return { ...transaction, withdrawalStatus: "STATUS_UNAVAILABLE" };
          }
        }));
      }

      if (mountedRef.current) {
        setTransactions(items);
        setTransactionPagination(result);
      }
    } catch (error) {
      if (mountedRef.current) setTransactionError(error.message && !error.response ? error.message : getErrorMessage(error));
    } finally {
      if (mountedRef.current) setTransactionsLoading(false);
    }
  }, [transactionPage, transactionFilter, withdrawalStatusFilter]);

  useEffect(() => {
    mountedRef.current = true;
    loadWallet();
    return () => { mountedRef.current = false; };
  }, [loadWallet]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const refreshAll = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([loadWallet(), loadTransactions()]);
    if (mountedRef.current) setRefreshing(false);
  }, [loadWallet, loadTransactions]);

  async function handleCancelWithdrawal(withdrawalId) {
    if (!withdrawalId || cancellingWithdrawalId) return;

    const confirmed = window.confirm(
      "Cancel this pending withdrawal request? If VEs were already deducted, the backend will reverse the deduction when it cancels the request."
    );
    if (!confirmed) return;

    setCancellingWithdrawalId(withdrawalId);
    setWithdrawalActionError("");
    setWithdrawalActionMessage("");

    try {
      const response = await cancelWithdrawal(withdrawalId);
      const result = unwrapData(response);
      if (result?.success === false) {
        throw new Error(result.message || "The withdrawal could not be cancelled.");
      }

      setWithdrawalActionMessage(
        result?.message || "Withdrawal cancelled. Your wallet and history are being refreshed."
      );
      await Promise.all([loadWallet(), loadTransactions()]);
    } catch (error) {
      setWithdrawalActionError(
        error.message && !error.response ? error.message : getErrorMessage(error)
      );
    } finally {
      if (mountedRef.current) setCancellingWithdrawalId(null);
    }
  }

  function handleLogout() {
    navigate("/login");
  }

  let displayName = "My Wallet";
  try {
    const user = JSON.parse(localStorage.getItem("veloop_user") || "null");
    displayName = user?.name || user?.email || displayName;
  } catch {
    // Keep the fallback label if stored profile data is invalid.
  }

  return (
    <main className="min-h-screen overflow-x-clip bg-[#171827] px-3 py-4 text-[#F5F6FF] sm:px-6 sm:py-6">
      <div className="mx-auto w-full max-w-[1440px] space-y-6 sm:space-y-7">
        <WalletHeader onLogout={handleLogout} onRefresh={refreshAll} refreshing={refreshing} />

        <section className="grid grid-cols-1 items-stretch gap-4 lg:grid-cols-[minmax(0,1.5fr)_minmax(280px,0.8fr)]" aria-labelledby="wallet-overview-title">
          <div className="relative flex min-w-0 flex-col justify-between overflow-hidden rounded-[26px] border border-[#5141AD]/60 bg-gradient-to-br from-[#242044] via-[#1C2039] to-[#171D32] p-5 sm:p-7 lg:p-8">
            <div>
              <span className="rounded-lg border border-[#8B7BFF]/25 bg-[#7567FF]/10 px-2.5 py-1 text-xs font-semibold text-[#C5BCFF]">MY REWARDS</span>
              <p className="mt-7 text-sm text-[#A7B1C8]">Welcome back,</p>
              <h2 id="wallet-overview-title" className="mt-1 break-words text-xl font-bold sm:text-2xl">{displayName}</h2>
              <p className="mt-7 text-sm font-medium text-[#B7AAFF]">Available VEs balance</p>
              <div className="mt-2 flex flex-wrap items-baseline gap-3">
                <p className="break-words text-4xl font-bold sm:text-5xl" aria-live="polite">{walletLoading || walletError ? "—" : formatAmount(wallet?.ves ?? wallet?.vesBalance)}</p>
                <span className="text-sm font-semibold text-[#B7AAFF]">VEs</span>
              </div>
            </div>
            <button type="button" onClick={() => navigate("/payout")} className="mt-8 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#6855D9] px-5 py-3 text-sm font-semibold transition hover:bg-[#7968E8] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#B7AAFF] focus-visible:ring-offset-2 focus-visible:ring-offset-[#242044] sm:w-auto sm:self-start">
              Redeem rewards <span aria-hidden="true">→</span>
            </button>
          </div>

          <aside className="rounded-[26px] border border-[#343B56] bg-[#202438] p-5 sm:p-6" aria-labelledby="balance-overview-title">
            <div className="flex items-center justify-between gap-3">
              <h2 id="balance-overview-title" className="font-semibold">Other balances</h2>
              <span className="rounded-lg bg-white/[0.04] px-2.5 py-1 text-xs text-[#A7B1C8]">Live data</span>
            </div>
            <p className="mt-2 text-sm text-[#A7B1C8]">Balances provided by your wallet.</p>
            <div className="mt-5 grid grid-cols-1 gap-4 min-[400px]:grid-cols-2 lg:grid-cols-1">
              {secondaryCurrencies.map((currency) => (
                <CurrencyCard key={currency.key} currency={currency} value={wallet?.[currency.key]} loading={walletLoading} error={walletError} />
              ))}
            </div>
            {walletError && <div className="mt-5"><p role="alert" className="text-sm text-red-200">{walletError}</p><button type="button" onClick={loadWallet} className="mt-2 min-h-10 text-sm font-semibold text-[#B7AAFF] underline underline-offset-4">Retry loading balances</button></div>}
          </aside>
        </section>

        <section className="overflow-hidden rounded-[24px] border border-[#343B56] bg-[#1D2134]" aria-labelledby="transaction-history-title">
          <div className="flex flex-col gap-4 border-b border-[#343B56] p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
            <div>
              <h2 id="transaction-history-title" className="text-xl font-bold">Transaction history</h2>
              <p className="mt-1 text-sm text-[#A7B1C8]">Review your recent wallet activity.</p>
            </div>
            <button type="button" onClick={loadTransactions} disabled={transactionsLoading} className="min-h-10 rounded-xl border border-[#343B56] px-4 py-2 text-sm transition hover:bg-white/5 disabled:opacity-50">{transactionsLoading ? "Loading…" : "Refresh history"}</button>
          </div>
          <div className="space-y-4 p-4 sm:p-5">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <FilterButtons value={transactionFilter} onChange={(value) => { setTransactionFilter(value); setTransactionPage(1); setWithdrawalStatusFilter("ALL"); }} />
              {transactionFilter === "WITHDRAWALS" && <WithdrawalStatusFilter value={withdrawalStatusFilter} onChange={(value) => { setWithdrawalStatusFilter(value); setTransactionPage(1); }} />}
            </div>
            {transactionFilter === "WITHDRAWALS" && <p className="text-xs leading-5 text-[#8F9BB5]">Request statuses are loaded from your withdrawal history. Only pending requests can be cancelled.</p>}
            {withdrawalActionError && <p role="alert" className="rounded-xl border border-red-400/25 bg-red-400/5 px-4 py-3 text-sm text-red-200">{withdrawalActionError}</p>}
            {withdrawalActionMessage && <p role="status" className="rounded-xl border border-emerald-400/25 bg-emerald-400/5 px-4 py-3 text-sm text-emerald-200">{withdrawalActionMessage}</p>}
            {transactionsLoading ? <LoadingState label={transactionFilter === "WITHDRAWALS" ? "Loading withdrawal requests…" : "Loading wallet transactions…"} /> : transactionError ? <ErrorState message={transactionError} onRetry={loadTransactions} /> : transactions.length === 0 ? <EmptyState isWithdrawals={transactionFilter === "WITHDRAWALS"} /> : <div className="overflow-hidden rounded-2xl border border-[#343B56] bg-[#202438]">{transactionFilter === "WITHDRAWALS" ? <WithdrawalHistory withdrawals={transactions} onCancelWithdrawal={handleCancelWithdrawal} cancellingWithdrawalId={cancellingWithdrawalId} /> : <TransactionHistory transactions={transactions} />}<Pagination pagination={transactionPagination} page={transactionPage} onPageChange={setTransactionPage} loading={transactionsLoading} /></div>}
            {!transactionsLoading && !transactionError && transactions.length === 0 && transactionPagination.totalElements > 0 && <Pagination pagination={transactionPagination} page={transactionPage} onPageChange={setTransactionPage} loading={transactionsLoading} />}
          </div>
        </section>

        <footer className="pb-2 text-center text-xs text-[#7F8AA6]">VELoop Rewards Wallet</footer>
      </div>
    </main>
  );
}
