
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../api/authApi";

export default function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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
        </div>
      </section>
    </main>
  );
}
