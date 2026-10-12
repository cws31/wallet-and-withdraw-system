import { useMemo } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import PayoutFlow from "../components/PayoutFlow";

const panelClass = "rounded-[22px] border border-[#343B56] bg-[#1D2134]";

export default function Payout() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const initialMethodCode = useMemo(
    () => (searchParams.get("method") || "").trim().toLowerCase(),
    [searchParams]
  );

  const returnToWallet = () => navigate("/wallet");

  return (
    <main className="min-h-screen overflow-x-clip bg-[#171827] px-3 py-4 text-[#F5F6FF] sm:px-6 sm:py-7">
      <div className="mx-auto w-full max-w-4xl space-y-5 sm:space-y-6">
        <header className={`${panelClass} flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-6`}>
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#292442] text-xl text-[#B7AAFF]" aria-hidden="true">
              ◈
            </div>
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[#B7AAFF]">VELoop Rewards</p>
              <h1 className="mt-1 text-xl font-bold tracking-tight sm:text-2xl">Redeem rewards</h1>
              <p className="mt-1 max-w-xl text-sm leading-5 text-[#A7B1C8]">
                Choose a payout, enter the required details, and review your request before submitting.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={returnToWallet}
            className="inline-flex min-h-11 w-full shrink-0 items-center justify-center gap-2 rounded-xl border border-[#343B56] px-4 py-2.5 text-sm font-semibold text-[#D5D9E8] transition hover:border-[#575078] hover:bg-white/[0.04] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8B7BFF] sm:w-auto"
          >
            <span aria-hidden="true">←</span> Back to wallet
          </button>
        </header>

        <section className={`${panelClass} p-3 sm:p-5`} aria-label="Payout request">
          <PayoutFlow
            initialMethodCode={initialMethodCode}
            onCancel={returnToWallet}
            returnLabel="Back to wallet"
          />
        </section>
        <p className="px-1 text-center text-xs leading-5 text-[#7F8AA6]">
          Available methods, redemption values, and eligibility are provided and validated by VELoop.
        </p>
      </div>
    </main>
  );
}
