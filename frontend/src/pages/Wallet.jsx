import { useCallback, useEffect, useRef, useState } from "react";

import { useNavigate } from "react-router-dom";

import PayoutFlow from "../components/PayoutFlow";

import { getWallet, getWalletTransactions } from "../api/walletApi";

import { getPayoutConfiguration } from "../api/payoutApi";

import { getWithdrawalHistory } from "../api/withdrawalApi";

const PAGE_SIZE = 10;

const secondaryCurrencies = [

  { key: "sves", label: "SVES", color: "#69A7FF", icon: "◆" },

  { key: "gems", label: "Gems", color: "#D58AFF", icon: "✦" },

  { key: "tokens", label: "Tokens", color: "#F1B95B", icon: "●" },

  { key: "spins", label: "Spins", color: "#35D6A0", icon: "↻" },

];

const transactionFilters = [

  { value: "ALL", label: "All activity" },

  { value: "REWARDS", label: "Rewards" },

  { value: "WITHDRAWALS", label: "Withdrawals" },

];

const withdrawalFilters = [

  { value: "ALL", label: "All" },

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

  return new Intl.NumberFormat("en-IN", {

    maximumFractionDigits: 2,

  }).format(amount);

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

  if (!error.response) {

    return "Unable to connect to the server. Check your connection and try again.";

  }

  if (status === 401) return "Your session has expired. Please log in again.";

  if (status === 403) return "You do not have permission to view this information.";

  if (status === 429) return "Too many requests. Please wait and try again.";

  if (status >= 500) return "The server is temporarily unavailable. Please try again.";

  return (

    error.response?.data?.message ||

    error.message ||

    "Unable to load the requested information."

  );

}

function unwrapData(response) {

  return response?.data ?? response;

}

function normalizeList(response) {

  const data = unwrapData(response);

  if (Array.isArray(data)) return data;

  if (Array.isArray(data?.content)) return data.content;

  if (Array.isArray(data?.transactions)) return data.transactions;

  if (Array.isArray(data?.items)) return data.items;

  return [];

}

function normalizePage(response) {

  const data = unwrapData(response);

  return {

    items: normalizeList(response),

    page: Number(data?.page ?? data?.number + 1 ?? 1),

    limit: Number(data?.limit ?? data?.size ?? PAGE_SIZE),

    totalElements: Number(data?.totalElements ?? 0),

    totalPages: Number(data?.totalPages ?? 0),

    hasNext: Boolean(data?.hasNext),

    hasPrevious: Boolean(data?.hasPrevious),

  };

}

function normalizePayoutMethods(response) {

  const data = unwrapData(response);

  if (Array.isArray(data)) return data;

  if (Array.isArray(data?.methods)) return data.methods;

  if (Array.isArray(data?.payoutMethods)) return data.payoutMethods;

  if (Array.isArray(data?.content)) return data.content;

  if (Array.isArray(response?.methods)) return response.methods;

  if (Array.isArray(response?.payoutMethods)) return response.payoutMethods;

  return null;

}

function getTransactionLabel(transaction) {

  return (

    transaction.description ||

    transaction.source ||

    transaction.transactionType ||

    "Wallet transaction"

  );

}

function getAmountPrefix(transaction) {

  const type = String(transaction.transactionType || "").toUpperCase();

  if (

    [

      "CREDIT",

      "REWARD",

      "BONUS",

      "REFERRAL",

      "DAILY_REWARD",

      "AD_REWARD",

      "GAME_REWARD",

      "ADMIN_CREDIT",

      "EXCHANGE_CREDIT",

    ].includes(type)

  ) {

    return "+";

  }

  if (

    [

      "DEBIT",

      "WITHDRAWAL",

      "EXCHANGE_DEBIT",

      "ADMIN_DEBIT",

    ].includes(type)

  ) {

    return "−";

  }

  // CORRECTION can represent a credit reversal or another adjustment.

  // Don't infer its sign from its name; the ledger amount is authoritative.

  return "";

}

function getStatusStyle(status) {

  const value = String(status || "").toUpperCase();

  if (["SUCCESS", "COMPLETED", "APPROVED", "SUCCESSFUL"].includes(value)) {

    return "border-emerald-400/25 bg-emerald-400/10 text-emerald-300";

  }

  if (["PENDING", "PROCESSING", "IN_PROGRESS"].includes(value)) {

    return "border-amber-400/25 bg-amber-400/10 text-amber-200";

  }

  if (["FAILED", "REJECTED", "CANCELLED", "CANCELED"].includes(value)) {

    return "border-red-400/25 bg-red-400/10 text-red-300";

  }

  return "border-white/10 bg-white/5 text-[#C1C8DA]";

}

function getMethodName(method) {

  return (

    method.name ||

    method.displayName ||

    method.methodName ||

    method.code ||

    method.methodCode ||

    "Payout method"

  );

}

function getMethodCode(method) {

  return method.code || method.methodCode || method.methodId || "";

}

function SectionHeading({ title, description, children }) {

  return (

    <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">

      <div>

        <h2 className="text-xl font-bold sm:text-2xl">{title}</h2>

        <p className="mt-1 text-sm leading-6 text-[#A7B1C8]">

          {description}

        </p>

      </div>

      {children}

    </div>

  );

}

function WalletHeader({ onLogout, onRefresh, refreshing }) {

  return (

    <header className="flex flex-col gap-4 rounded-[22px] border border-white/[0.07] bg-[#141625] p-4 sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:py-5">

      <div className="flex min-w-0 items-center gap-3">

        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#292442] text-xl text-[#B7AAFF]">

          ◈

        </div>

        <div>

          <h1 className="text-xl font-bold tracking-tight sm:text-2xl">

            VELoop <span className="text-[#B7AAFF]">Wallet</span>

          </h1>

          <p className="mt-0.5 text-xs text-[#8F9BB5] sm:text-sm">

            Your rewards at a glance

          </p>

        </div>

      </div>

      <div className="grid grid-cols-2 gap-2 sm:flex">

        <button

          type="button"

          onClick={onRefresh}

          disabled={refreshing}

          className="min-h-11 rounded-xl border border-[#343B56] px-4 py-2 text-sm font-medium hover:bg-white/5 disabled:opacity-60"

        >

          {refreshing ? "Refreshing…" : "↻ Refresh"}

        </button>

        <button

          type="button"

          onClick={onLogout}

          className="min-h-11 rounded-xl bg-[#6855D9] px-4 py-2 text-sm font-semibold hover:bg-[#7968E8]"

        >

          Logout

        </button>

      </div>

    </header>

  );

}

function LoadingState({ label }) {

  return (

    <div

      role="status"

      aria-live="polite"

      className="rounded-2xl border border-[#343B56] bg-[#202438] px-5 py-10 text-center text-sm text-[#A7B1C8]"

    >

      <span className="mx-auto mb-3 block h-6 w-6 animate-spin rounded-full border-2 border-[#53617D] border-t-[#9275FF]" />

      {label}

    </div>

  );

}

function ErrorState({ message, onRetry }) {

  return (

    <div className="rounded-2xl border border-red-400/25 bg-[#202438] p-5">

      <p role="alert" className="break-words text-sm text-red-200">

        {message}

      </p>

      <button

        type="button"

        onClick={onRetry}

        className="mt-4 min-h-10 rounded-xl bg-[#5144C8] px-4 py-2 text-sm font-semibold"

      >

        Try again

      </button>

    </div>

  );

}

function CurrencyCard({ currency, value }) {

  return (

    <article className="min-w-0 rounded-[20px] border border-[#343B56] bg-[#202438] p-4 transition hover:border-[#575078] sm:p-5">

      <div className="flex items-center gap-3">

        <span

          className="flex h-9 w-9 items-center justify-center rounded-xl text-lg"

          style={{

            color: currency.color,

            backgroundColor: `${currency.color}18`,

          }}

        >

          {currency.icon}

        </span>

        <h3 className="text-sm font-semibold text-[#C7CDE0]">

          {currency.label}

        </h3>

      </div>

      <p className="mt-4 break-words text-2xl font-bold sm:text-3xl">

        {formatAmount(value)}

      </p>

      <p className="mt-1 text-xs text-[#8F9BB5]">Available balance</p>

    </article>

  );

}

function PayoutMethods({ methods, loading, error, onRetry, onSelect }) {

  if (loading) return <LoadingState label="Loading payout methods..." />;

  if (error) return <ErrorState message={error} onRetry={onRetry} />;

  if (methods.length === 0) {

    return (

      <div className="rounded-2xl border border-[#343B56] bg-[#202438] px-5 py-10 text-center">

        <h3 className="font-semibold">No payout methods to display</h3>

        <p className="mt-2 text-sm text-[#A7B1C8]">

          No supported payout methods were found in the configuration response.

        </p>

        <button

          type="button"

          onClick={onRetry}

          className="mt-4 rounded-xl border border-[#5144C8] px-4 py-2 text-sm"

        >

          Reload methods

        </button>

      </div>

    );

  }

  return (

    <div className="grid grid-cols-1 gap-3 min-[480px]:grid-cols-2 xl:grid-cols-3">

      {methods.map((method, index) => {

        const name = getMethodName(method);

        const code = getMethodCode(method);

        const active = method.active !== false;

        const id = method.id ?? method.methodId ?? code ?? index;

        return (

          <article

            key={id}

            className="flex min-w-0 flex-col rounded-[20px] border border-[#343B56] bg-[#202438] p-4 sm:p-5"

          >

            <div className="flex items-start justify-between gap-3">

              <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#292442] text-lg font-bold text-[#B7AAFF]">

                {name.slice(0, 1).toUpperCase()}

              </span>

              <span

                className={`rounded-full border px-2.5 py-1 text-xs ${

                  active

                    ? "border-emerald-400/20 bg-emerald-400/10 text-emerald-300"

                    : "border-white/10 bg-white/5 text-[#A7B1C8]"

                }`}

              >

                {active ? "Available" : "Unavailable"}

              </span>

            </div>

            <h3 className="mt-4 break-words font-semibold">{name}</h3>

            <p className="mt-1 min-h-10 break-words text-sm text-[#A7B1C8]">

              {method.description || method.type || "Payout option"}

            </p>

            <button

              type="button"

              disabled={!active}

              onClick={() => onSelect(method)}

              className="mt-4 min-h-11 w-full rounded-xl bg-[#5144C8] px-4 py-3 text-sm font-semibold disabled:cursor-not-allowed disabled:bg-[#34384C]"

            >

              {active ? "View redemption options" : "Unavailable"}

            </button>

          </article>

        );

      })}

    </div>

  );

}

function FilterButtons({ label, value, options, onChange }) {

  return (

    <div>

      <p className="mb-2 text-xs font-medium text-[#8F9BB5]">{label}</p>

      <div className="flex flex-wrap gap-2" role="group" aria-label={label}>

        {options.map((option) => (

          <button

            key={option.value}

            type="button"

            aria-pressed={value === option.value}

            onClick={() => onChange(option.value)}

            className={`min-h-10 rounded-xl border px-3 py-2 text-sm font-medium transition ${

              value === option.value

                ? "border-[#7567FF] bg-[#5144C8]/25 text-[#D1C8FF]"

                : "border-[#343B56] text-[#A7B1C8] hover:bg-white/5 hover:text-white"

            }`}

          >

            {option.label}

          </button>

        ))}

      </div>

    </div>

  );

}

function Pagination({ pagination, page, onPageChange, loading }) {

  const totalPages = pagination.totalPages;

  const firstItem =

    pagination.totalElements === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;

  const lastItem = Math.min(page * PAGE_SIZE, pagination.totalElements);

  return (

    <div className="flex flex-col gap-3 border-t border-[#343B56] px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">

      <p className="text-sm text-[#A7B1C8]" aria-live="polite">

        Showing {firstItem}–{lastItem} of {pagination.totalElements}

      </p>

      <div className="flex items-center gap-2">

        <button

          type="button"

          disabled={loading || !pagination.hasPrevious}

          onClick={() => onPageChange(page - 1)}

          className="min-h-10 rounded-xl border border-[#343B56] px-4 text-sm disabled:cursor-not-allowed disabled:opacity-40"

        >

          ← Previous

        </button>

        <span className="min-w-20 text-center text-sm text-[#C7CDE0]">

          Page {page}{totalPages > 0 ? ` of ${totalPages}` : ""}

        </span>

        <button

          type="button"

          disabled={loading || !pagination.hasNext}

          onClick={() => onPageChange(page + 1)}

          className="min-h-10 rounded-xl border border-[#343B56] px-4 text-sm disabled:cursor-not-allowed disabled:opacity-40"

        >

          Next →

        </button>

      </div>

    </div>

  );

}

function EmptyState({ title, description }) {

  return (

    <div className="px-5 py-12 text-center">

      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#292442] text-xl text-[#B7AAFF]">

        ◈

      </div>

      <h3 className="mt-4 font-semibold">{title}</h3>

      <p className="mt-2 text-sm text-[#A7B1C8]">{description}</p>

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

            <article

              key={transaction.transactionId ?? index}

              className="space-y-3 px-4 py-4"

            >

              <div className="flex items-start justify-between gap-3">

                <div className="min-w-0 flex-1">

                  <p className="break-words text-sm font-semibold">

                    {getTransactionLabel(transaction)}

                  </p>

                  <p className="mt-1 text-xs text-[#A7B1C8]">

                    {formatDate(transaction.createdAt)}

                  </p>

                </div>

                <p className="max-w-[45%] break-words text-right font-semibold">

                  {prefix}

                  {formatAmount(transaction.amount)}

                  {transaction.currency ? ` ${transaction.currency}` : ""}

                </p>

              </div>

              <div className="flex flex-wrap items-center justify-between gap-2">

                <span className="text-xs text-[#A7B1C8]">

                  {transaction.transactionType || "Transaction"}

                </span>

                <span

                  className={`rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(transaction.status)}`}

                >

                  {transaction.status || "Status unavailable"}

                </span>

              </div>

              {transaction.transactionId != null && (

                <p className="break-all text-xs text-[#7F8AA6]">

                  ID: {transaction.transactionId}

                </p>

              )}

            </article>

          );

        })}

      </div>

      <div className="hidden overflow-x-auto md:block">

        <table className="w-full min-w-[680px] text-left text-sm">

          <thead className="bg-[#282E48] text-[#A7B1C8]">

            <tr>

              <th scope="col" className="px-5 py-4 font-medium">Transaction</th>

              <th scope="col" className="px-5 py-4 font-medium">Currency</th>

              <th scope="col" className="px-5 py-4 font-medium">Amount</th>

              <th scope="col" className="px-5 py-4 font-medium">Status</th>

              <th scope="col" className="px-5 py-4 font-medium">Date</th>

            </tr>

          </thead>

          <tbody className="divide-y divide-[#343B56]">

            {transactions.map((transaction, index) => (

              <tr

                key={transaction.transactionId ?? index}

                className="transition hover:bg-white/[0.025]"

              >

                <td className="max-w-[260px] px-5 py-4">

                  <p className="break-words font-medium">

                    {getTransactionLabel(transaction)}

                  </p>

                  {transaction.transactionId != null && (

                    <p className="mt-1 break-all text-xs text-[#7F8AA6]">

                      ID: {transaction.transactionId}

                    </p>

                  )}

                </td>

                <td className="whitespace-nowrap px-5 py-4">

                  {transaction.currency || "—"}

                </td>

                <td className="whitespace-nowrap px-5 py-4 font-semibold">

                  {getAmountPrefix(transaction)}

                  {formatAmount(transaction.amount)}

                </td>

                <td className="px-5 py-4">

                  <span

                    className={`rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(transaction.status)}`}

                  >

                    {transaction.status || "—"}

                  </span>

                </td>

                <td className="whitespace-nowrap px-5 py-4 text-[#A7B1C8]">

                  {formatDate(transaction.createdAt)}

                </td>

              </tr>

            ))}

          </tbody>

        </table>

      </div>

    </>

  );

}

function WithdrawalHistory({ withdrawals }) {

  return (

    <>

      <div className="divide-y divide-[#343B56] md:hidden">

        {withdrawals.map((withdrawal, index) => (

          <article

            key={withdrawal.withdrawalId ?? index}

            className="space-y-3 px-4 py-4"

          >

            <div className="flex items-start justify-between gap-3">

              <div className="min-w-0">

                <p className="break-all text-sm font-semibold">

                  {withdrawal.withdrawalId || "Withdrawal request"}

                </p>

                <p className="mt-1 text-xs text-[#A7B1C8]">

                  {formatDate(withdrawal.requestedAt || withdrawal.createdAt)}

                </p>

              </div>

              <span

                className={`shrink-0 rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(withdrawal.status)}`}

              >

                {withdrawal.status || "Unknown"}

              </span>

            </div>

            <p className="text-sm text-[#C7CDE0]">

              {withdrawal.payoutMethod || "Payout method"}

            </p>

            <p className="text-sm text-[#A7B1C8]">

              Payout amount: {formatAmount(withdrawal.payoutAmount)}{" "}

              {withdrawal.currency || ""}

            </p>

            {withdrawal.rejectionReason && (

              <p className="break-words text-xs text-red-300">

                Reason: {withdrawal.rejectionReason}

              </p>

            )}

          </article>

        ))}

      </div>

      <div className="hidden overflow-x-auto md:block">

        <table className="w-full min-w-[720px] text-left text-sm">

          <thead className="bg-[#282E48] text-[#A7B1C8]">

            <tr>

              <th scope="col" className="px-5 py-4 font-medium">Request ID</th>

              <th scope="col" className="px-5 py-4 font-medium">Method</th>

              <th scope="col" className="px-5 py-4 font-medium">Payout</th>

              <th scope="col" className="px-5 py-4 font-medium">Status</th>

              <th scope="col" className="px-5 py-4 font-medium">Requested</th>

            </tr>

          </thead>

          <tbody className="divide-y divide-[#343B56]">

            {withdrawals.map((withdrawal, index) => (

              <tr

                key={withdrawal.withdrawalId ?? index}

                className="transition hover:bg-white/[0.025]"

              >

                <td className="max-w-52 break-all px-5 py-4 font-medium">

                  {withdrawal.withdrawalId || "—"}

                  {withdrawal.rejectionReason && (

                    <p className="mt-1 break-words text-xs text-red-300">

                      {withdrawal.rejectionReason}

                    </p>

                  )}

                </td>

                <td className="px-5 py-4">

                  {withdrawal.payoutMethod || "—"}

                </td>

                <td className="whitespace-nowrap px-5 py-4 font-semibold">

                  {formatAmount(withdrawal.payoutAmount)}{" "}

                  {withdrawal.currency || ""}

                </td>

                <td className="px-5 py-4">

                  <span

                    className={`rounded-full border px-2.5 py-1 text-xs ${getStatusStyle(withdrawal.status)}`}

                  >

                    {withdrawal.status || "Unknown"}

                  </span>

                </td>

                <td className="whitespace-nowrap px-5 py-4 text-[#A7B1C8]">

                  {formatDate(withdrawal.requestedAt || withdrawal.createdAt)}

                </td>

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

  const refreshLockRef = useRef(false);

  const [activeTab, setActiveTab] = useState("payout");

  const [selectedPayoutMethod, setSelectedPayoutMethod] = useState(null);

  const [historyType, setHistoryType] = useState("transactions");

  const [wallet, setWallet] = useState(null);

  const [methods, setMethods] = useState([]);

  const [transactions, setTransactions] = useState([]);

  const [transactionPage, setTransactionPage] = useState(1);

  const [transactionFilter, setTransactionFilter] = useState("ALL");

  const [transactionPagination, setTransactionPagination] = useState({

    totalElements: 0,

    totalPages: 0,

    hasNext: false,

    hasPrevious: false,

  });

  const [withdrawals, setWithdrawals] = useState([]);

  const [withdrawalPage, setWithdrawalPage] = useState(1);

  const [withdrawalFilter, setWithdrawalFilter] = useState("ALL");

  const [withdrawalPagination, setWithdrawalPagination] = useState({

    totalElements: 0,

    totalPages: 0,

    hasNext: false,

    hasPrevious: false,

  });

  const [walletLoading, setWalletLoading] = useState(true);

  const [methodsLoading, setMethodsLoading] = useState(true);

  const [transactionsLoading, setTransactionsLoading] = useState(false);

  const [withdrawalsLoading, setWithdrawalsLoading] = useState(false);

  const [refreshing, setRefreshing] = useState(false);

  const [walletError, setWalletError] = useState("");

  const [methodsError, setMethodsError] = useState("");

  const [transactionError, setTransactionError] = useState("");

  const [withdrawalError, setWithdrawalError] = useState("");

  const loadWallet = useCallback(async () => {

    setWalletLoading(true);

    setWalletError("");

    try {

      const response = await getWallet();

      if (response?.success === false) {

        throw new Error(response.message || "Unable to load wallet.");

      }

      const data = unwrapData(response);

      if (!data || typeof data !== "object" || Array.isArray(data)) {

        throw new Error("The server returned an invalid wallet response.");

      }

      if (mountedRef.current) setWallet(data);

    } catch (error) {

      if (mountedRef.current) {

        setWalletError(

          error.message && !error.response

            ? error.message

            : getErrorMessage(error)

        );

      }

    } finally {

      if (mountedRef.current) setWalletLoading(false);

    }

  }, []);

  const loadMethods = useCallback(async () => {

    setMethodsLoading(true);

    setMethodsError("");

    try {

      const response = await getPayoutConfiguration();

      if (response?.success === false) {

        throw new Error(response.message || "Unable to load payout configuration.");

      }

      const configuredMethods = normalizePayoutMethods(response);

      if (configuredMethods === null) {

        throw new Error("The payout configuration response has an unsupported structure.");

      }

      if (mountedRef.current) setMethods(configuredMethods);

    } catch (error) {

      if (mountedRef.current) {

        setMethodsError(

          error.message && !error.response

            ? error.message

            : getErrorMessage(error)

        );

      }

    } finally {

      if (mountedRef.current) setMethodsLoading(false);

    }

  }, []);

  const loadTransactions = useCallback(async () => {

    setTransactionsLoading(true);

    setTransactionError("");

    try {

      const response = await getWalletTransactions(

        transactionPage,

        PAGE_SIZE,

        transactionFilter

      );

      if (response?.success === false) {

        throw new Error(response.message || "Unable to load transactions.");

      }

      const result = normalizePage(response);

      if (mountedRef.current) {

        setTransactions(result.items);

        setTransactionPagination(result);

      }

    } catch (error) {

      if (mountedRef.current) {

        setTransactionError(

          error.message && !error.response

            ? error.message

            : getErrorMessage(error)

        );

      }

    } finally {

      if (mountedRef.current) setTransactionsLoading(false);

    }

  }, [transactionPage, transactionFilter]);

  const loadWithdrawals = useCallback(async () => {

    setWithdrawalsLoading(true);

    setWithdrawalError("");

    try {

      const response = await getWithdrawalHistory(

        withdrawalPage,

        PAGE_SIZE,

        withdrawalFilter

      );

      if (response?.success === false) {

        throw new Error(response.message || "Unable to load withdrawal history.");

      }

      const result = normalizePage(response);

      if (mountedRef.current) {

        setWithdrawals(result.items);

        setWithdrawalPagination(result);

      }

    } catch (error) {

      if (mountedRef.current) {

        setWithdrawalError(

          error.message && !error.response

            ? error.message

            : getErrorMessage(error)

        );

      }

    } finally {

      if (mountedRef.current) setWithdrawalsLoading(false);

    }

  }, [withdrawalPage, withdrawalFilter]);

  const refreshAll = useCallback(async () => {

    if (refreshLockRef.current) return;

    refreshLockRef.current = true;

    setRefreshing(true);

    try {

      await Promise.all([loadWallet(), loadMethods(), loadTransactions(), loadWithdrawals()]);

    } finally {

      refreshLockRef.current = false;

      if (mountedRef.current) setRefreshing(false);

    }

  }, [loadWallet, loadMethods, loadTransactions, loadWithdrawals]);

  useEffect(() => {

    mountedRef.current = true;

    loadWallet();

    loadMethods();

    return () => {

      mountedRef.current = false;

    };

  }, [loadWallet, loadMethods]);

  useEffect(() => {

    loadTransactions();

  }, [loadTransactions]);

  useEffect(() => {

    loadWithdrawals();

  }, [loadWithdrawals]);

  function handleLogout() {

    // Keep authentication cleanup consistent with your existing login flow.

    navigate("/login");

  }

  function handleSelectMethod(method) {

    setSelectedPayoutMethod(method);

  }

  let displayName = "My Wallet";

  try {

    const user = JSON.parse(localStorage.getItem("veloop_user") || "null");

    displayName = user?.name || user?.email || displayName;

  } catch {

    // Use the fallback label if stored user data isn't valid JSON.

  }

  return (

    <main className="min-h-screen overflow-x-clip bg-[#171827] px-3 py-4 text-[#F5F6FF] sm:px-6 sm:py-6">

      <div className="mx-auto w-full max-w-[1440px] space-y-6 sm:space-y-7">

        <WalletHeader

          onLogout={handleLogout}

          onRefresh={refreshAll}

          refreshing={refreshing}

        />

        <section className="grid grid-cols-1 items-stretch gap-4 lg:grid-cols-[minmax(0,1.5fr)_minmax(280px,0.8fr)]">

          <div className="relative flex min-w-0 flex-col justify-between overflow-hidden rounded-[26px] border border-[#5141AD]/60 bg-gradient-to-br from-[#242044] via-[#1C2039] to-[#171D32] p-5 sm:p-7 lg:p-8">

            <div>

              <span className="rounded-lg border border-[#8B7BFF]/25 bg-[#7567FF]/10 px-2.5 py-1 text-xs font-semibold text-[#C5BCFF]">

                MY REWARDS

              </span>

              <p className="mt-7 text-sm text-[#A7B1C8]">Welcome back,</p>

              <h2 className="mt-1 break-words text-xl font-bold sm:text-2xl">

                {displayName}

              </h2>

              <p className="mt-7 text-sm font-medium text-[#B7AAFF]">

                Available VES balance

              </p>

              <div className="mt-2 flex flex-wrap items-baseline gap-3">

                <p className="break-words text-4xl font-bold sm:text-5xl">

                  {walletLoading || walletError ? "—" : formatAmount(wallet?.ves)}

                </p>

                <span className="text-sm font-semibold text-[#B7AAFF]">VES</span>

              </div>

            </div>

          </div>

          <aside className="rounded-[26px] border border-[#343B56] bg-[#202438] p-5 sm:p-6">

            <div className="flex items-center justify-between gap-3">

              <h2 className="font-semibold">Balance overview</h2>

              <span className="rounded-lg bg-white/[0.04] px-2.5 py-1 text-xs text-[#A7B1C8]">

                Live data

              </span>

            </div>

            <p className="mt-2 text-sm text-[#A7B1C8]">

              Your available balances, updated from your wallet.

            </p>

            <div className="mt-5 space-y-4">

              {secondaryCurrencies.map((currency) => (

                <div key={currency.key} className="flex items-center gap-3">

                  <span

                    className="h-2.5 w-2.5 rounded-full"

                    style={{ backgroundColor: currency.color }}

                  />

                  <span className="flex-1 text-sm text-[#A7B1C8]">

                    {currency.label}

                  </span>

                  <span className="text-right text-sm font-semibold">

                    {walletLoading || walletError

                      ? "—"

                      : formatAmount(wallet?.[currency.key])}

                  </span>

                </div>

              ))}

            </div>

            {walletError && (

              <div className="mt-5">

                <p role="alert" className="text-sm text-red-200">{walletError}</p>

                <button

                  type="button"

                  onClick={loadWallet}

                  className="mt-2 text-sm font-semibold text-[#B7AAFF]"

                >

                  Retry loading balances

                </button>

              </div>

            )}

          </aside>

        </section>

        <section className="overflow-hidden rounded-[24px] border border-[#343B56] bg-[#1D2134]">

          <div className="border-b border-[#343B56] p-3 sm:p-5">

            <div

              role="tablist"

              aria-label="Wallet sections"

              className="grid grid-cols-2 gap-1 rounded-2xl bg-[#141625] p-1 sm:inline-grid sm:w-auto"

            >

              <button

                type="button"

                role="tab"

                aria-selected={activeTab === "payout"}

                onClick={() => setActiveTab("payout")}

                className={`min-h-11 rounded-xl px-4 py-3 text-sm font-semibold ${

                  activeTab === "payout"

                    ? "bg-[#302951] text-[#D1C8FF]"

                    : "text-[#8F9BB5] hover:text-white"

                }`}

              >

                Payout Methods

              </button>

              <button

                type="button"

                role="tab"

                aria-selected={activeTab === "history"}

                onClick={() => setActiveTab("history")}

                className={`min-h-11 rounded-xl px-4 py-3 text-sm font-semibold ${

                  activeTab === "history"

                    ? "bg-[#302951] text-[#D1C8FF]"

                    : "text-[#8F9BB5] hover:text-white"

                }`}

              >

                History

              </button>

            </div>

          </div>

          {activeTab === "payout" ? (

            <div className="space-y-5 p-4 sm:p-6">

              <SectionHeading

                title="Choose how to redeem"

                description="Choose a payout method to see its available redemption options."

              />

              {selectedPayoutMethod ? (

                <div className="space-y-4">

                  <button

                    type="button"

                    onClick={() => setSelectedPayoutMethod(null)}

                    className="min-h-10 rounded-xl border border-[#343B56] px-4 py-2 text-sm font-medium text-[#C7CDE0] hover:bg-white/5"

                  >

                    ← Back to payout methods

                  </button>

                  <PayoutFlow

                    key={String(

                      selectedPayoutMethod.id ??

                      selectedPayoutMethod.methodId ??

                      getMethodCode(selectedPayoutMethod)

                    )}

                    initialMethodCode={String(

                      getMethodCode(selectedPayoutMethod)

                    ).toLowerCase()}

                    showMethodSelection={false}

                    onSubmitted={async () => {

                      await Promise.all([

                        loadWallet(),

                        loadTransactions(),

                        loadWithdrawals(),

                      ]);

                    }}

                    onCancel={() => setSelectedPayoutMethod(null)}

                    returnLabel="Back to payout methods"

                  />

                </div>

              ) : (

                <PayoutMethods

                  methods={methods}

                  loading={methodsLoading}

                  error={methodsError}

                  onRetry={loadMethods}

                  onSelect={handleSelectMethod}

                />

              )}

            </div>

          ) : (

            <div className="space-y-5 p-4 sm:p-6">

              <SectionHeading

                title="Wallet history"

                description="Review transactions and track your withdrawal requests."

              />

              <div className="flex flex-wrap gap-2" role="group" aria-label="History type">

                <button

                  type="button"

                  aria-pressed={historyType === "transactions"}

                  onClick={() => setHistoryType("transactions")}

                  className={`min-h-10 rounded-xl border px-4 py-2 text-sm ${

                    historyType === "transactions"

                      ? "border-[#7567FF] bg-[#5144C8]/25 text-[#D1C8FF]"

                      : "border-[#343B56] text-[#A7B1C8]"

                  }`}

                >

                  Wallet transactions

                </button>

                <button

                  type="button"

                  aria-pressed={historyType === "withdrawals"}

                  onClick={() => setHistoryType("withdrawals")}

                  className={`min-h-10 rounded-xl border px-4 py-2 text-sm ${

                    historyType === "withdrawals"

                      ? "border-[#7567FF] bg-[#5144C8]/25 text-[#D1C8FF]"

                      : "border-[#343B56] text-[#A7B1C8]"

                  }`}

                >

                  Withdrawal requests

                </button>

              </div>

              {historyType === "transactions" ? (

                <>

                  <FilterButtons

                    label="Filter wallet activity"

                    value={transactionFilter}

                    options={transactionFilters}

                    onChange={(value) => {

                      setTransactionFilter(value);

                      setTransactionPage(1);

                    }}

                  />

                  <div className="overflow-hidden rounded-2xl border border-[#343B56] bg-[#202438]">

                    <div className="border-b border-[#343B56] px-4 py-4 sm:px-5">

                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

                        <div>

                          <h3 className="font-semibold">Transaction history</h3>

                          

                        </div>

                        <button

                          type="button"

                          onClick={loadTransactions}

                          disabled={transactionsLoading}

                          className="min-h-10 rounded-xl border border-[#343B56] px-4 text-sm disabled:opacity-50"

                        >

                          {transactionsLoading ? "Loading…" : "Refresh"}

                        </button>

                      </div>

                    </div>

                    {transactionsLoading ? (

                      <LoadingState label="Loading wallet transactions..." />

                    ) : transactionError ? (

                      <div className="p-4">

                        <ErrorState message={transactionError} onRetry={loadTransactions} />

                      </div>

                    ) : transactions.length === 0 ? (

                      <EmptyState

                        title="No transactions found"

                        description="No wallet transactions match this filter yet."

                      />

                    ) : (

                      <TransactionHistory transactions={transactions} />

                    )}

                    {!transactionsLoading && !transactionError && (

                      <Pagination

                        pagination={transactionPagination}

                        page={transactionPage}

                        onPageChange={setTransactionPage}

                        loading={transactionsLoading}

                      />

                    )}

                  </div>

                </>

              ) : (

                <>

                  <FilterButtons

                    label="Filter withdrawal requests"

                    value={withdrawalFilter}

                    options={withdrawalFilters}

                    onChange={(value) => {

                      setWithdrawalFilter(value);

                      setWithdrawalPage(1);

                    }}

                  />

                  <div className="overflow-hidden rounded-2xl border border-[#343B56] bg-[#202438]">

                    <div className="border-b border-[#343B56] px-4 py-4 sm:px-5">

                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

                        <div>

                          <h3 className="font-semibold">Withdrawal requests</h3>

                          

                        </div>

                        <button

                          type="button"

                          onClick={loadWithdrawals}

                          disabled={withdrawalsLoading}

                          className="min-h-10 rounded-xl border border-[#343B56] px-4 text-sm disabled:opacity-50"

                        >

                          {withdrawalsLoading ? "Loading…" : "Refresh"}

                        </button>

                      </div>

                    </div>

                    {withdrawalsLoading ? (

                      <LoadingState label="Loading withdrawal requests..." />

                    ) : withdrawalError ? (

                      <div className="p-4">

                        <ErrorState message={withdrawalError} onRetry={loadWithdrawals} />

                      </div>

                    ) : withdrawals.length === 0 ? (

                      <EmptyState

                        title="No withdrawal requests found"

                        description="There are no withdrawal requests for this filter on the current page."

                      />

                    ) : (

                      <WithdrawalHistory withdrawals={withdrawals} />

                    )}

                    {!withdrawalsLoading && !withdrawalError && (

                      <Pagination

                        pagination={withdrawalPagination}

                        page={withdrawalPage}

                        onPageChange={setWithdrawalPage}

                        loading={withdrawalsLoading}

                      />

                    )}

                  </div>

                </>

              )}

            </div>

          )}

        </section>

        <footer className="pb-2 text-center text-xs text-[#7F8AA6]">

          VELoop Rewards Wallet

        </footer>

      </div>

    </main>

  );

}
