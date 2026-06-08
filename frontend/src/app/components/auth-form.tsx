"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { FormEvent } from "react";
import { useState } from "react";
import {
  clearFrontendSessionCookie,
  fetchAuthSession,
  loginUser,
  registerUser,
  setFrontendSessionCookie,
} from "../lib/auth";

type AuthMode = "login" | "signup";

type AuthFormProps = {
  mode: AuthMode;
  showSignupLink?: boolean;
};

export function AuthForm({ mode, showSignupLink = true }: AuthFormProps) {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function performLogin(nextUsername: string, nextPassword: string) {
    const { response, data } = await loginUser(nextUsername, nextPassword);

    if (!response.ok) {
      throw new Error(data.message === "invalid" ? "Invalid username or password." : "Login failed.");
    }
  }

  async function performSignup(nextUsername: string, nextPassword: string) {
    const { response, data } = await registerUser(nextUsername, nextPassword);

    if (!response.ok) {
      if (response.status === 409 || data.message === "exists") {
        throw new Error("Username already exists.");
      }

      throw new Error("Signup failed.");
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      if (mode === "login") {
        await performLogin(username.trim(), password);
      } else {
        await performSignup(username.trim(), password);
      }

      const session = await fetchAuthSession();
      if (!session) {
        clearFrontendSessionCookie();
        router.replace("/login");
        return;
      }

      setFrontendSessionCookie(session.username);
      router.replace("/");
      router.refresh();
    } catch (submitError) {
      clearFrontendSessionCookie();
      setError(submitError instanceof Error ? submitError.message : "Something went wrong.");
    } finally {
      setIsSubmitting(false);
    }
  }

  const title = mode === "login" ? "Welcome back" : "Create your account";
  const subtitle =
    mode === "login"
      ? "Sign in to keep your music library, queue, and playback in one place."
      : "Create a local account to unlock the library interface.";
  const cta = mode === "login" ? "Login" : "Sign up";
  const alternateHref = mode === "login" ? "/signup" : "/login";
  const alternateText = mode === "login" ? "Create an account" : "I already have an account";

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.09),transparent_24%),radial-gradient(circle_at_bottom_right,rgba(16,185,129,0.12),transparent_22%),linear-gradient(180deg,#09090b_0%,#050505_100%)] text-white">
      <div className="mx-auto grid min-h-screen max-w-[1400px] gap-6 p-4 md:grid-cols-[1.1fr_0.9fr] md:p-6">
        <section className="relative overflow-hidden rounded-[32px] border border-white/10 bg-zinc-950/70 p-6 shadow-[0_32px_80px_rgba(0,0,0,0.45)] backdrop-blur-xl md:p-10">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.08),transparent_24%),radial-gradient(circle_at_bottom_left,rgba(16,185,129,0.10),transparent_22%)]" />
          <div className="relative z-10 flex h-full flex-col justify-between gap-8">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-zinc-500">OurMusic</p>
              <h1 className="mt-4 max-w-xl text-4xl font-semibold tracking-tight md:text-6xl">
                {title}
              </h1>
              <p className="mt-4 max-w-lg text-sm leading-6 text-zinc-300 md:text-base">
                {subtitle}
              </p>
            </div>

            <div className="grid gap-3 text-sm text-zinc-300 sm:grid-cols-2">
              <div className="rounded-3xl border border-white/10 bg-white/5 p-4 backdrop-blur-md">
                Fast local playback
              </div>
              <div className="rounded-3xl border border-white/10 bg-white/5 p-4 backdrop-blur-md">
                Queue, repeat, and search in one place
              </div>
              <div className="rounded-3xl border border-white/10 bg-white/5 p-4 backdrop-blur-md">
                Works on desktop and mobile
              </div>
              <div className="rounded-3xl border border-white/10 bg-white/5 p-4 backdrop-blur-md">
                Signed-in library state
              </div>
            </div>
          </div>
        </section>

        <section className="flex items-center justify-center">
          <div className="w-full max-w-md rounded-[32px] border border-white/10 bg-zinc-900/80 p-6 shadow-[0_32px_80px_rgba(0,0,0,0.5)] backdrop-blur-xl md:p-8">
            <div>
              <p className="text-xs uppercase tracking-[0.24em] text-zinc-500">
                {mode === "login" ? "Login" : "Signup"}
              </p>
              <h2 className="mt-2 text-3xl font-semibold">{cta}</h2>
              <p className="mt-2 text-sm leading-6 text-zinc-400">{subtitle}</p>
            </div>

            <form className="mt-8 space-y-4" onSubmit={handleSubmit}>
              <label className="block space-y-2">
                <span className="text-sm font-medium text-zinc-200">Username</span>
                <input
                  type="text"
                  autoComplete="username"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  className="w-full rounded-2xl border border-white/10 bg-zinc-950/80 px-4 py-3 text-white outline-none transition placeholder:text-zinc-600 focus:border-white/20 focus:bg-zinc-950"
                  required
                />
              </label>

              <label className="block space-y-2">
                <span className="text-sm font-medium text-zinc-200">Password</span>
                <input
                  type="password"
                  autoComplete={mode === "login" ? "current-password" : "new-password"}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className="w-full rounded-2xl border border-white/10 bg-zinc-950/80 px-4 py-3 text-white outline-none transition placeholder:text-zinc-600 focus:border-white/20 focus:bg-zinc-950"
                  required
                />
              </label>

              {error ? (
                <p className="rounded-2xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-200">
                  {error}
                </p>
              ) : null}

              <button
                type="submit"
                disabled={isSubmitting}
                className="flex w-full items-center justify-center rounded-2xl bg-white px-4 py-3 font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting ? "Please wait..." : cta}
              </button>
            </form>

            {showSignupLink ? (
              <div className="mt-6 border-t border-white/10 pt-4 text-sm text-zinc-400">
                {mode === "login" ? "Need an account?" : "Already registered?"}{" "}
                <Link href={alternateHref} className="font-medium text-white transition hover:text-zinc-200">
                  {alternateText}
                </Link>
              </div>
            ) : null}
          </div>
        </section>
      </div>
    </main>
  );
}
