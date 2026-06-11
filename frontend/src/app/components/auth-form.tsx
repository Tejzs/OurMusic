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

  const title = mode === "login" ? "Login" : "Sign up";
  const cta = mode === "login" ? "Login" : "Sign up";
  const alternateHref = mode === "login" ? "/signup" : "/login";
  const alternateText = mode === "login" ? "Create an account" : "I already have an account";

  return (
    <main className="flex min-h-screen items-center justify-center bg-zinc-950 px-4 text-white">
      <section className="w-full max-w-sm rounded-2xl border border-white/10 bg-zinc-900/80 p-6 shadow-[0_24px_70px_rgba(0,0,0,0.42)]">
        <div>
          <h1 className="text-2xl font-semibold">{title}</h1>
        </div>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <label className="block space-y-2">
            <span className="text-sm font-medium text-zinc-200">Username</span>
            <input
              type="text"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="w-full rounded-xl border border-white/10 bg-zinc-950 px-3 py-2.5 text-white outline-none transition placeholder:text-zinc-600 focus:border-white/25"
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
              className="w-full rounded-xl border border-white/10 bg-zinc-950 px-3 py-2.5 text-white outline-none transition placeholder:text-zinc-600 focus:border-white/25"
              required
            />
          </label>

          {error ? <p className="text-sm text-red-300">{error}</p> : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex w-full items-center justify-center rounded-xl bg-white px-4 py-2.5 font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "Please wait..." : cta}
          </button>
        </form>

        {showSignupLink ? (
          <div className="mt-5 border-t border-white/10 pt-4 text-sm text-zinc-400">
            {mode === "login" ? "Need an account?" : "Already registered?"}{" "}
            <Link href={alternateHref} className="font-medium text-white transition hover:text-zinc-200">
              {alternateText}
            </Link>
          </div>
        ) : null}
      </section>
    </main>
  );
}
