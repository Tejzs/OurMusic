"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthForm } from "../components/auth-form";
import { fetchAuthSession } from "../lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    void fetchAuthSession(controller.signal)
      .then((session) => {
        if (session) {
          router.replace("/");
          return;
        }

        setIsReady(true);
      })
      .catch(() => {
        setIsReady(true);
      });

    return () => controller.abort("login-page-unmounted");
  }, [router]);

  if (!isReady) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-zinc-950 text-white">
        <div className="rounded-[28px] border border-white/10 bg-white/5 px-6 py-5 text-sm text-zinc-300 backdrop-blur-xl">
          Checking access...
        </div>
      </main>
    );
  }

  return <AuthForm mode="login" showSignupLink={false} />;
}
