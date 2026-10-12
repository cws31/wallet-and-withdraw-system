import { useCallback, useEffect, useRef, useState } from "react";
import { getPayoutConfiguration } from "../api/payoutApi";
import { createWithdrawal } from "../api/withdrawalApi";

const panelClass = "rounded-2xl border border-[#343B56] bg-[#202438]";
const fieldClass = "min-h-12 w-full rounded-xl border border-[#343B56] bg-[#171827] px-4 py-3 text-sm text-[#F5F6FF] placeholder:text-[#7F8AA6] outline-none transition focus:border-[#8B7BFF] focus:ring-2 focus:ring-[#5144C8]/40 disabled:cursor-not-allowed disabled:opacity-60";
const primaryButtonClass = "inline-flex min-h-11 items-center justify-center rounded-xl bg-[#6855D9] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#7968E8] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A99EFF] focus-visible:ring-offset-2 focus-visible:ring-offset-[#202438] disabled:cursor-not-allowed disabled:opacity-50";
const secondaryButtonClass = "inline-flex min-h-11 items-center justify-center rounded-xl border border-[#343B56] px-4 py-3 text-sm font-medium text-[#C7CDE0] transition hover:border-[#575078] hover:bg-white/[0.04] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8B7BFF] disabled:cursor-not-allowed disabled:opacity-50";

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "—";
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "—";
  return new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 }).format(amount);
}

function getErrorMessage(error) {
  if (!error?.response) {
    return "Unable to connect to the server. Check your connection and try again.";
  }
  const { status, data } = error.response;
  if (status === 401) return "Your session has expired. Please log in again.";
  if (status === 403) return "You are not allowed to perform this action.";
  if (status === 409) return data?.message || "This request conflicts with an existing request. Review your request and try again.";
  if (status === 429) return "Too many requests. Please wait a moment and try again.";
  if (status >= 500) return "The service is temporarily unavailable. Please try again later.";
  if (Array.isArray(data?.errors) && data.errors.length) {
    const messages = data.errors
      .map((item) => (typeof item === "string" ? item : item?.defaultMessage || item?.message || ""))
      .filter(Boolean);
    if (messages.length) return messages.join(" ");
  }
  return data?.message || data?.error || error.message || "Unable to process your request.";
}

function getMethodCode(method) {
  return String(method?.code ?? method?.methodCode ?? method?.methodId ?? "").toLowerCase();
}

function getMethodName(method) {
  return method?.name || method?.displayName || method?.methodName || method?.code || method?.methodCode || "Payout method";
}

function getDetailsField(method) {
  const value = `${method?.code || method?.methodCode || ""} ${getMethodName(method)}`.toLowerCase();
  if (value.includes("upi")) return { label: "UPI ID", placeholder: "name@bank", type: "text", autoComplete: "off" };
  if (value.includes("paypal")) return { label: "PayPal email", placeholder: "you@example.com", type: "email", autoComplete: "email" };
  if (value.includes("amazon") || value.includes("google")) {
    return { label: "Recipient details", placeholder: "Enter the required recipient details", type: "text", autoComplete: "off" };
  }
  return { label: "Payout details", placeholder: "Enter your payout details", type: "text", autoComplete: "off" };
}

function normalizeMethods(response) {
  const data = response?.data ?? response;
  const list = Array.isArray(data) ? data : data?.methods || data?.payoutMethods || data?.content || [];
  if (!Array.isArray(list)) return [];

  // Render only methods returned by the backend. Inactive configured methods remain
  // visible as Coming soon; the frontend never invents payout methods or options.
  return list
    .filter((method) => method?.id != null)
    .map((method) => ({
      ...method,
      options: Array.isArray(method.options) ? method.options : [],
    }));
}

function isMethodAvailable(method) {
  return Boolean(method) &&
    method.active !== false &&
    method.isActive !== false &&
    method.enabled !== false &&
    method.available !== false &&
    method.options.length > 0;
}

function optionLabel(option) {
  return option?.payoutAmount == null ? "Payout option" : `₹${formatAmount(option.payoutAmount)}`;
}

function optionCost(option) {
  if (option?.currencyAmount == null) return null;
  const amount = Number(option.currencyAmount);
  if (!Number.isFinite(amount)) return null;

  // Display the configured cost and its configured currency exactly as returned.
  // Do not convert or infer a currency in the frontend.
  const currency = String(option.currency || option.costCurrency || "").trim();
  return currency ? `${formatAmount(amount)} ${currency}` : formatAmount(amount);
}

function optionCostLabel(option) {
  const currency = String(option?.currency || option?.costCurrency || "").trim();
  return currency ? `${currency} required` : "Reward cost";
}

function makeIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  if (globalThis.crypto?.getRandomValues) {
    const bytes = new Uint8Array(16);
    globalThis.crypto.getRandomValues(bytes);
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
  }
  throw new Error("Secure request IDs are not available in this browser context. Open the app over HTTPS or use an up-to-date browser.");
}

function StepLabel({ number, title, description, complete }) {
  return (
    <div className="flex items-start gap-3">
      <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-sm font-bold ${complete ? "border-[#7567FF] bg-[#5144C8]/25 text-[#D1C8FF]" : "border-[#343B56] bg-[#171827] text-[#A7B1C8]"}`}>
        {complete ? "✓" : number}
      </span>
      <div className="min-w-0 pt-0.5">
        <h3 className="text-sm font-semibold text-[#F5F6FF]">{title}</h3>
        {description && <p className="mt-1 text-xs leading-5 text-[#8F9BB5]">{description}</p>}
      </div>
    </div>
  );
}

function ChoiceButton({ selected, onClick, title, description, trailing, disabled = false, status }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-pressed={selected}
      className={`group flex min-h-[92px] min-w-0 w-full items-start justify-between gap-3 rounded-xl border p-4 text-left outline-none transition focus-visible:ring-2 focus-visible:ring-[#8B7BFF] disabled:cursor-not-allowed disabled:opacity-75 ${selected ? "border-[#7567FF] bg-[#5144C8]/20 ring-1 ring-[#7567FF]/70" : disabled ? "border-[#343B56] bg-[#171827]/45" : "border-[#343B56] bg-[#171827]/70 hover:border-[#575078] hover:bg-white/[0.025]"}`}
    >
      <span className="min-w-0 flex-1">
        <span className="block break-words text-sm font-semibold text-[#F5F6FF]">{title}</span>
        {description && <span className="mt-1 block break-words text-xs leading-5 text-[#A7B1C8]">{description}</span>}
        {trailing && <span className="mt-2 block text-xs font-medium text-[#B7AAFF]">{trailing}</span>}
        {status && <span className={`mt-2 inline-flex rounded-full border px-2 py-1 text-[11px] font-semibold ${disabled ? "border-amber-300/20 bg-amber-300/[0.08] text-amber-200" : "border-emerald-300/20 bg-emerald-300/[0.08] text-emerald-200"}`}>{status}</span>}
      </span>
      {!disabled && <span aria-hidden="true" className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border text-xs ${selected ? "border-[#8B7BFF] bg-[#6855D9] text-white" : "border-[#53617D] text-transparent"}`}>✓</span>}
    </button>
  );
}

function InlineError({ children }) {
  if (!children) return null;
  return <p role="alert" className="rounded-xl border border-red-400/25 bg-red-400/[0.07] px-4 py-3 text-sm leading-5 text-red-200">{children}</p>;
}

export default function PayoutFlow({
  initialMethodCode = "",
  onSubmitted,
  onCancel,
  showMethodSelection = true,
  returnLabel = "Back",
}) {
  const [methods, setMethods] = useState([]);
  const [selectedMethodId, setSelectedMethodId] = useState("");
  const [selectedOptionId, setSelectedOptionId] = useState("");
  const [payoutDetails, setPayoutDetails] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [pageError, setPageError] = useState("");
  const [formError, setFormError] = useState("");
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [withdrawalResult, setWithdrawalResult] = useState(null);
  const idempotencyKeyRef = useRef(null);

  const loadConfiguration = useCallback(async () => {
    setLoading(true);
    setPageError("");
    try {
      const response = await getPayoutConfiguration();
      if (response?.success === false) throw new Error(response.message || "Unable to load payout configuration.");
      const availableMethods = normalizeMethods(response);
      setMethods(availableMethods);
      setSelectedMethodId((currentId) => {
        const current = availableMethods.find((method) => String(method.id) === String(currentId) && isMethodAvailable(method));
        if (current) return String(current.id);
        if (initialMethodCode) {
          const match = availableMethods.find((method) => getMethodCode(method) === initialMethodCode.toLowerCase() && isMethodAvailable(method));
          if (match) return String(match.id);
        }
        const selectableMethods = availableMethods.filter(isMethodAvailable);
        if (!showMethodSelection && selectableMethods.length === 1) return String(selectableMethods[0].id);
        return "";
      });
    } catch (error) {
      setPageError(error.message && !error.response ? error.message : getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [initialMethodCode, showMethodSelection]);

  useEffect(() => { loadConfiguration(); }, [loadConfiguration]);

  const selectedMethod = methods.find((method) => String(method.id) === String(selectedMethodId) && isMethodAvailable(method));
  const options = selectedMethod?.options || [];
  const selectedOption = options.find((option) => String(option.id) === String(selectedOptionId));
  const detailsField = getDetailsField(selectedMethod);
  const methodStep = showMethodSelection ? 1 : null;
  const optionStep = showMethodSelection ? 2 : 1;
  const detailsStep = showMethodSelection ? 3 : 2;

  function resetRequestState() {
    idempotencyKeyRef.current = null;
    setFormError("");
    setWithdrawalResult(null);
    setShowConfirmation(false);
  }

  function handleSelectMethod(method) {
    if (!isMethodAvailable(method)) return;
    setSelectedMethodId(String(method.id));
    setSelectedOptionId("");
    setPayoutDetails("");
    resetRequestState();
  }

  function handleSelectOption(option) {
    setSelectedOptionId(String(option.id));
    resetRequestState();
  }

  function handleContinue(event) {
    event.preventDefault();
    setFormError("");
    if (!selectedMethod) return setFormError("Choose a payout method to continue.");
    if (!selectedOption) return setFormError("Choose a payout amount to continue.");
    const details = payoutDetails.trim();
    if (!details) return setFormError(`Enter your ${detailsField.label.toLowerCase()} to continue.`);
    if (details.length > 320) return setFormError("Payout details must not exceed 320 characters.");
    if (detailsField.type === "email" && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(details)) {
      return setFormError("Enter a valid PayPal email address.");
    }
    setShowConfirmation(true);
  }

  async function handleConfirm() {
    if (submitting) return;
    if (!selectedMethod || !selectedOption) {
      setFormError("Choose a payout method and amount before submitting.");
      setShowConfirmation(false);
      return;
    }
    setSubmitting(true);
    setFormError("");
    try {
      if (!idempotencyKeyRef.current) idempotencyKeyRef.current = makeIdempotencyKey();
      const response = await createWithdrawal({
        payoutMethodId: selectedMethod.id,
        payoutOptionId: selectedOption.id,
        payoutDetails: payoutDetails.trim(),
      }, idempotencyKeyRef.current);
      const result = response?.data ?? response;
      if (response?.success === false || result?.withdrawalId == null) {
        throw new Error(response?.message || "The server did not confirm the withdrawal.");
      }
      setWithdrawalResult(result);
      setShowConfirmation(false);
      idempotencyKeyRef.current = null;
      if (onSubmitted) {
        try { await onSubmitted(result); } catch { /* The accepted request remains successful if refresh fails. */ }
      }
    } catch (error) {
      setFormError(error.response ? getErrorMessage(error) : error.message || getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <div role="status" aria-live="polite" className={`${panelClass} px-5 py-12 text-center`}>
      <span className="mx-auto mb-4 block h-7 w-7 animate-spin rounded-full border-2 border-[#53617D] border-t-[#9275FF]" />
      <p className="text-sm font-medium text-[#E3E5F2]">Loading payout options…</p>
    </div>;
  }

  if (pageError) {
    return <div className={`${panelClass} p-5 sm:p-6`}>
      <h2 className="font-semibold text-[#F5F6FF]">Payout options unavailable</h2>
      <p role="alert" className="mt-2 text-sm text-red-200">{pageError}</p>
      <div className="mt-5 flex flex-col gap-3 min-[400px]:flex-row">
        <button type="button" onClick={loadConfiguration} className={primaryButtonClass}>Try again</button>
        {onCancel && <button type="button" onClick={onCancel} className={secondaryButtonClass}>{returnLabel}</button>}
      </div>
    </div>;
  }

  if (methods.length === 0) {
    return <div className={`${panelClass} p-6 text-center sm:p-8`}>
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#292442] text-xl text-[#B7AAFF]" aria-hidden="true">◈</div>
      <h2 className="mt-4 font-semibold text-[#F5F6FF]">No payout options available</h2>

      <div className="mt-5 flex flex-col justify-center gap-3 min-[400px]:flex-row">
        <button type="button" onClick={loadConfiguration} className={primaryButtonClass}>Refresh options</button>
        {onCancel && <button type="button" onClick={onCancel} className={secondaryButtonClass}>{returnLabel}</button>}
      </div>
    </div>;
  }

  return (
    <div className="space-y-6 text-[#F5F6FF]">
      <div>
        <h2 className="text-lg font-bold sm:text-xl">Withdraw rewards</h2>
      </div>

      {withdrawalResult ? (
        <div className="rounded-2xl border border-emerald-400/25 bg-emerald-400/[0.06] p-4 sm:p-6">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-400/10 text-lg text-emerald-300" aria-hidden="true">✓</span>
            <div className="min-w-0">
              <h3 className="font-semibold text-emerald-300">Request submitted</h3>

            </div>
          </div>
          <dl className="mt-5 grid grid-cols-1 gap-3 rounded-xl border border-white/[0.06] bg-[#171827]/50 p-4 sm:grid-cols-2">
            <div className="min-w-0"><dt className="text-xs text-[#8F9BB5]">Withdrawal ID</dt><dd className="mt-1 break-all text-sm font-semibold">{withdrawalResult.withdrawalId}</dd></div>
            <div><dt className="text-xs text-[#8F9BB5]">Current status</dt><dd className="mt-1 text-sm font-semibold">{withdrawalResult.status || "PENDING"}</dd></div>
            <div><dt className="text-xs text-[#8F9BB5]">Payout method</dt><dd className="mt-1 break-words text-sm font-semibold">{getMethodName(selectedMethod)}</dd></div>
            <div><dt className="text-xs text-[#8F9BB5]">Payout amount</dt><dd className="mt-1 text-sm font-semibold">{withdrawalResult.payoutAmount != null ? `₹${formatAmount(withdrawalResult.payoutAmount)}` : optionLabel(selectedOption)}</dd></div>
          </dl>
          <div className="mt-5 flex flex-col gap-3 sm:flex-row">
            <button type="button" onClick={() => { setWithdrawalResult(null); setSelectedOptionId(""); setPayoutDetails(""); resetRequestState(); }} className={primaryButtonClass}>Make another request</button>
            {onCancel && <button type="button" onClick={onCancel} className={secondaryButtonClass}>{returnLabel}</button>}
          </div>
        </div>
      ) : (
        <form onSubmit={handleContinue} className="space-y-6">
          {showMethodSelection && <section className="space-y-3" aria-labelledby="payout-method-heading">
            <StepLabel number={methodStep} title="Payout method" complete={Boolean(selectedMethod)} />
            <h3 id="payout-method-heading" className="sr-only">Choose a payout method</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {methods.map((method) => {
                const available = isMethodAvailable(method);
                return <ChoiceButton
                  key={method.id}
                  selected={String(method.id) === String(selectedMethodId)}
                  onClick={() => handleSelectMethod(method)}
                  title={getMethodName(method)}
                  description={method.description || (available ? "Receive your reward using this method." : "This payout method is not available yet.")}
                  status={available ? "Available" : "Coming soon"}
                  method={method}
                  disabled={!available}
                />;
              })}
            </div>
          </section>}

          {selectedMethod && <section className="space-y-3" aria-labelledby="payout-amount-heading">
            <StepLabel number={optionStep} title="Payout amount" complete={Boolean(selectedOption)} />
            <h3 id="payout-amount-heading" className="sr-only">Choose a payout amount</h3>
            <div className="grid grid-cols-1 gap-3 min-[400px]:grid-cols-2 sm:grid-cols-3">
              {options.map((option) => <ChoiceButton key={option.id} selected={String(option.id) === String(selectedOptionId)} onClick={() => handleSelectOption(option)} title={optionLabel(option)} description={optionCost(option) ? `Cost: ${optionCost(option)}` : "Cost unavailable"} />)}
            </div>
          </section>}

          {selectedMethod && <section className="space-y-3" aria-labelledby="payout-details-heading">
            <StepLabel number={detailsStep} title="Payout details" complete={Boolean(payoutDetails.trim())} />
            <div className="rounded-xl border border-[#343B56] bg-[#171827]/50 p-4 sm:p-5">
              <label htmlFor="payout-details" id="payout-details-heading" className="mb-2 block text-sm font-semibold text-[#E3E5F2]">{detailsField.label}</label>
              <input id="payout-details" name="payoutDetails" type={detailsField.type} value={payoutDetails} onChange={(event) => { setPayoutDetails(event.target.value); resetRequestState(); }} placeholder={detailsField.placeholder} maxLength={320} autoComplete={detailsField.autoComplete} required disabled={submitting} aria-describedby="payout-details-help" className={fieldClass} />

            </div>
          </section>}

          <InlineError>{formError}</InlineError>
          <div className="flex flex-col-reverse gap-3 border-t border-[#343B56] pt-5 min-[420px]:flex-row min-[420px]:items-center min-[420px]:justify-between">
            {onCancel ? <button type="button" onClick={onCancel} disabled={submitting} className={secondaryButtonClass}>{returnLabel}</button> : <span className="text-xs leading-5 text-[#8F9BB5]">Nothing is submitted until you confirm.</span>}
            <button type="submit" disabled={submitting || !selectedMethod || !selectedOption || !payoutDetails.trim()} className={`${primaryButtonClass} w-full min-[420px]:w-auto`}>
              Review request <span className="ml-2" aria-hidden="true">→</span>
            </button>
          </div>
        </form>
      )}

      {showConfirmation && selectedMethod && selectedOption && !withdrawalResult && (
        <div className="fixed inset-0 z-50 flex items-end justify-center overflow-y-auto bg-black/75 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !submitting) setShowConfirmation(false); }}>
          <section className="my-auto max-h-[92dvh] w-full overflow-y-auto rounded-t-2xl border border-[#343B56] bg-[#202438] p-4 text-[#F5F6FF] shadow-2xl sm:max-w-lg sm:rounded-2xl sm:p-6" role="dialog" aria-modal="true" aria-labelledby="withdrawal-confirmation-title" aria-describedby="withdrawal-confirmation-description">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#292442] text-lg text-[#B7AAFF]" aria-hidden="true">✓</span>
              <div className="min-w-0">
                <h2 id="withdrawal-confirmation-title" className="text-lg font-bold">Review your request</h2>

              </div>
            </div>
            <dl className="mt-5 divide-y divide-[#343B56] rounded-xl border border-[#343B56] bg-[#171827]/60 px-4">
              <div className="flex flex-col gap-1 py-3 min-[380px]:flex-row min-[380px]:justify-between min-[380px]:gap-4"><dt className="text-xs text-[#8F9BB5]">Payout method</dt><dd className="break-words text-sm font-semibold min-[380px]:text-right">{getMethodName(selectedMethod)}</dd></div>
              <div className="flex flex-col gap-1 py-3 min-[380px]:flex-row min-[380px]:justify-between min-[380px]:gap-4"><dt className="text-xs text-[#8F9BB5]">You will receive</dt><dd className="text-sm font-semibold min-[380px]:text-right">{optionLabel(selectedOption)}</dd></div>
              <div className="flex flex-col gap-1 py-3 min-[380px]:flex-row min-[380px]:justify-between min-[380px]:gap-4"><dt className="text-xs text-[#8F9BB5]">{optionCostLabel(selectedOption)}</dt><dd className="text-sm font-semibold min-[380px]:text-right">{optionCost(selectedOption) || "Not provided by VELoop"}</dd></div>
              <div className="flex flex-col gap-1 py-3"><dt className="text-xs text-[#8F9BB5]">{detailsField.label}</dt><dd className="break-all text-sm font-semibold">{payoutDetails.trim()}</dd></div>
            </dl>

            <InlineError>{formError}</InlineError>
            <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
              <button type="button" disabled={submitting} onClick={() => setShowConfirmation(false)} className={secondaryButtonClass}>Back</button>
              <button type="button" disabled={submitting} onClick={handleConfirm} className={primaryButtonClass}>{submitting ? "Submitting…" : "Confirm"}</button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
