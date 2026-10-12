
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../api/authApi";

// Enable only during manager review.
// Set VITE_ENABLE_DEMO_ACCOUNTS=true in the frontend .env file.
const DEMO_ACCOUNTS_ENABLED =
  import.meta.env.VITE_ENABLE_DEMO_ACCOUNTS === "true";

const DEMO_USERS = Array.from({ length: 10 }, (_, index) => {
  const number = index + 1;

  return {
    label: `Demo User ${number}`,
    email:
      number === 1
        ? "demo@veloop.test"
        : `demo.user${String(number).padStart(2, "0")}@veloop.test`,
    password: "Demo@12345",
    role: "USER",
  };
});

const DEMO_ADMIN = {
  label: "Demo Admin",
  email: "admin@veloop.test",
  password: "Admin@12345",
  role: "ADMIN",
};

export default function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showDemoAccounts, setShowDemoAccounts] = useState(false);

  function useDemoAccount(account) {
    setEmail(account.email);
    setPassword(account.password);
    setError("");
    setShowDemoAccounts(false);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const normalizedEmail = email.trim();

    if (!normalizedEmail || !password) {
      setError("Please enter your email and password.");
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setError("Please enter a valid email address.");
      return;
    }

    setLoading(true);

    try {
      const response = await loginUser({
        email: normalizedEmail,
        password,
      });

      const data = response?.data;

      if (response?.success !== true || !data?.token) {
        setError(response?.message || "Unable to sign in.");
        return;
      }

      const role = String(data.role || "")
        .replace(/^ROLE_/i, "")
        .toUpperCase();

      localStorage.setItem("veloop_token", data.token);

      localStorage.setItem(
        "veloop_user",
        JSON.stringify({
          userId: data.userId,
          email: data.email,
          name: data.name,
          role: data.role,
        })
      );

      if (role === "ADMIN") {
        navigate("/admin/withdrawals", { replace: true });
      } else {
        navigate("/wallet", { replace: true });
      }
    } catch (err) {
      const status = err.response?.status;
      const message = err.response?.data?.message;

      if (!err.response) {
        setError("Unable to connect to the server. Please try again.");
      } else if (status === 401 || status === 403) {
        setError(message || "Invalid email or password.");
      } else if (status === 429) {
        setError("Too many attempts. Please try again later.");
      } else if (status >= 500) {
        setError("Server unavailable. Please try again later.");
      } else {
        setError(message || "Login failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  function DemoAccountRow({ account }) {
    return (
      <div className="flex flex-col gap-3 rounded-xl border border-white/10 bg-[#252738] p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <p className="font-semibold text-white">{account.label}</p>
          <p className="mt-1 break-all text-sm text-gray-300">
            {account.email}
          </p>
          <p className="mt-1 text-xs text-gray-400">
            Role: {account.role}
          </p>
        </div>

        <button
          type="button"
          onClick={() => useDemoAccount(account)}
          disabled={loading}
          className="shrink-0 rounded-lg bg-[#3980F5] px-4 py-2 text-sm font-semibold text-white hover:bg-[#2D70E0] disabled:opacity-50"
        >
          Use credentials
        </button>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-[#171827] px-4 py-4 text-white sm:px-6">
      <header className="mx-auto flex min-h-[78px] max-w-[1700px] items-center gap-4 rounded-[20px] border border-white/[0.08] bg-[#141625] px-5 sm:px-8">
        <button
          type="button"
          onClick={() => navigate(-1)}
          aria-label="Go back"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white text-[#171827] hover:bg-gray-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            className="h-6 w-6"
            aria-hidden="true"
          >
            <path
              d="M15 18L9 12L15 6"
              stroke="currentColor"
              strokeWidth="2.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>

        <h1 className="text-2xl font-bold tracking-wider sm:text-[30px]">
          Login
        </h1>
      </header>

      <section className="mx-auto flex w-full max-w-[730px] flex-col items-center pb-10 pt-8 sm:pt-10">
        <h2 className="mb-7 text-center text-2xl font-medium sm:text-[30px]">
          Login To Your Account
        </h2>

        <div className="w-full rounded-[18px] border border-white/25 bg-[#3A3B49] px-6 py-8 sm:px-10 sm:py-10">
          <form onSubmit={handleSubmit} noValidate>
            <div className="mb-7 flex items-center gap-3">
              <div className="h-px flex-1 border-t border-dashed border-white/80" />
              <span className="rounded-lg bg-white px-3 py-1 text-lg font-semibold text-black">
                OR
              </span>
              <div className="h-px flex-1 border-t border-dashed border-white/80" />
            </div>

            <div className="space-y-6">
              <div>
                <label
                  htmlFor="email"
                  className="mb-2 block text-base sm:text-[17px]"
                >
                  Email:
                </label>

                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="username"
                  inputMode="email"
                  placeholder="Email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  disabled={loading}
                  required
                  className="h-[46px] w-full rounded-xl bg-white px-5 text-base text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-400 disabled:opacity-70 sm:h-[47px]"
                />
              </div>

              <div>
                <label
                  htmlFor="password"
                  className="mb-2 block text-base sm:text-[17px]"
                >
                  Password:
                </label>

                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="Password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  disabled={loading}
                  required
                  className="h-[46px] w-full rounded-xl bg-white px-5 text-base text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-400 disabled:opacity-70 sm:h-[47px]"
                />
              </div>
            </div>

            {error && (
              <p
                role="alert"
                className="mt-5 rounded-lg border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-100"
              >
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="mx-auto mt-8 flex min-h-[60px] w-full max-w-[475px] items-center justify-center rounded-[20px] bg-[#3980F5] px-5 py-3 text-lg font-semibold text-white hover:bg-[#2D70E0] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300 focus-visible:ring-offset-2 focus-visible:ring-offset-[#3A3B49] disabled:cursor-not-allowed disabled:opacity-70 sm:mt-10"
            >
              {loading ? "Logging in..." : "Login"}
            </button>
          </form>

          {DEMO_ACCOUNTS_ENABLED && (
            <div className="mt-8 border-t border-white/15 pt-6">
              <div className="text-center">
                <p className="text-lg font-semibold">
                  Manager Verification
                </p>
                <p className="mt-2 text-sm leading-6 text-gray-300">
                  Use a seeded demo account to verify the wallet,
                  transactions, payout options, withdrawals and admin
                  workflows.
                </p>

                <button
                  type="button"
                  onClick={() =>
                    setShowDemoAccounts((previous) => !previous)
                  }
                  aria-expanded={showDemoAccounts}
                  className="mt-4 rounded-xl border border-blue-400/50 px-5 py-3 text-sm font-semibold text-blue-200 hover:bg-blue-500/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
                >
                  {showDemoAccounts
                    ? "Hide demo accounts"
                    : "View demo accounts"}
                </button>
              </div>

              {showDemoAccounts && (
                <div className="mt-5 space-y-6">
                  <div>
                    <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-300">
                      User accounts (10)
                    </h3>

                    <div className="space-y-3">
                      {DEMO_USERS.map((account) => (
                        <DemoAccountRow
                          key={account.email}
                          account={account}
                        />
                      ))}
                    </div>
                  </div>

                  <div>
                    <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-300">
                      Administrator
                    </h3>

                    <DemoAccountRow account={DEMO_ADMIN} />

                    <p className="mt-3 text-xs leading-5 text-amber-200">
                      Demo credentials are public to anyone who can access
                      this page. Enable this only for a controlled review,
                      and disable it afterward. Never use these credentials
                      in production.
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
// build by sonu kumar