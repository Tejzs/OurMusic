"use client";

import type { LucideIcon } from "lucide-react";
import { useEffect, useState } from "react";
import type { View } from "../music-types";
import { apiUrl } from "../lib/auth";

type LibraryItem = {
  id: "home" | "songs" | "albums" | "playlists" | "likedSongs" | "recentlyPlayed" | "mostPlayed" | "artists" | "adminUsers";
  label: string;
  icon: LucideIcon;
};

type LibrarySidebarProps = {
  view: View;
  libraryItems: LibraryItem[];
  authUser?: string | null;
  onViewHome: () => void;
  onViewSongs: () => void;
  onViewLikedSongs: () => void;
  onViewRecentlyPlayed: () => void;
  onViewMostPlayed: () => void;
  onViewAlbums: () => void;
  onViewPlaylists: () => void;
  onViewArtists: () => void;
  onViewAdminUsers: () => void;
  onSignOut?: () => void;
  onClose?: () => void;
  isDrawer?: boolean;
  className?: string;
};

export function LibrarySidebar({
  view,
  libraryItems,
  authUser,
  onViewHome,
  onViewSongs,
  onViewLikedSongs,
  onViewRecentlyPlayed,
  onViewMostPlayed,
  onViewAlbums,
  onViewPlaylists,
  onViewArtists,
  onViewAdminUsers,
  onSignOut,
  onClose,
  isDrawer = false,
  className = "",
}: LibrarySidebarProps) {
  const [serverStatus, setServerStatus] = useState<"active" | "down">("down");
  const containerClassName = isDrawer
    ? "flex h-full w-full flex-col gap-6 overflow-y-auto bg-zinc-900/95 p-4 backdrop-blur"
    : "flex shrink-0 flex-col gap-6 rounded-3xl border border-zinc-800 bg-zinc-900/80 p-4 backdrop-blur lg:h-full lg:w-[280px] lg:overflow-y-auto";

  useEffect(() => {
    let isMounted = true;
    let controller: AbortController | null = null;

    const checkServerStatus = async () => {
      controller?.abort();
      controller = new AbortController();

      try {
        await fetch(apiUrl("/api/auth/me"), {
          credentials: "include",
          signal: controller.signal,
        });

        if (isMounted) {
          setServerStatus("active");
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }

        if (isMounted) {
          setServerStatus("down");
        }
      }
    };

    const initialTimer = window.setTimeout(() => {
      void checkServerStatus();
    }, 0);
    const interval = window.setInterval(() => {
      void checkServerStatus();
    }, 15000);

    return () => {
      isMounted = false;
      window.clearTimeout(initialTimer);
      window.clearInterval(interval);
      controller?.abort();
    };
  }, []);

  return (
    <aside className={`${containerClassName} ${className}`.trim()}>
      {isDrawer && onClose ? (
        <div className="flex items-center justify-between lg:hidden">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Library</p>
            <h2 className="mt-2 text-2xl font-semibold">OurMusic</h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-zinc-800 bg-zinc-950/70 px-3 py-2 text-sm text-zinc-300 transition hover:bg-zinc-800 hover:text-white"
          >
            Close
          </button>
        </div>
      ) : (
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Library</p>
          <h2 className="mt-2 text-2xl font-semibold">OurMusic</h2>
        </div>
      )}

      <nav className="space-y-2">
        {libraryItems.map((item) => {
          const isActive = view === item.id;
          const Icon = item.icon;

          return (
            <button
              key={item.id}
              type="button"
              onClick={() => {
                if (item.id === "home") {
                  onViewHome();
                } else if (item.id === "songs") {
                  onViewSongs();
                } else if (item.id === "likedSongs") {
                  onViewLikedSongs();
                } else if (item.id === "recentlyPlayed") {
                  onViewRecentlyPlayed();
                } else if (item.id === "mostPlayed") {
                  onViewMostPlayed();
                } else if (item.id === "albums") {
                  onViewAlbums();
                } else if (item.id === "playlists") {
                  onViewPlaylists();
                } else if (item.id === "adminUsers") {
                  onViewAdminUsers();
                } else {
                  onViewArtists();
                }

                onClose?.();
              }}
              className={`flex w-full items-center justify-between rounded-2xl border px-4 py-3 text-left transition ${
                isActive
                  ? "border-white/20 bg-white text-zinc-950"
                  : "border-zinc-800 bg-zinc-950/60 text-zinc-100 hover:border-zinc-700 hover:bg-zinc-800/60"
              }`}
            >
              <span className="flex items-center gap-3 font-medium">
                <Icon className="h-4 w-4" />
                {item.label}
              </span>
            </button>
          );
        })}
      </nav>

      {authUser && onSignOut ? (
        <section className="mt-auto rounded-3xl border border-white/10 bg-white/5 p-4 backdrop-blur-md">
          <div className="flex items-center justify-between gap-3">
            <p className="text-xs uppercase tracking-[0.18em] text-zinc-500">Account</p>
            <span className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-zinc-950/60 px-2.5 py-1 text-[11px] font-medium text-zinc-300">
              <span
                className={`h-2 w-2 rounded-full ${
                  serverStatus === "active" ? "bg-emerald-400" : "bg-yellow-400"
                }`}
              />
              {serverStatus === "active" ? "Active" : "Down"}
            </span>
          </div>
          <p className="mt-2 truncate text-sm font-medium text-white">{authUser}</p>
          <button
            type="button"
            onClick={onSignOut}
            className="mt-4 w-full rounded-2xl border border-white/10 bg-zinc-950/70 px-4 py-2 text-sm text-zinc-100 transition hover:border-white/20 hover:bg-zinc-900"
          >
            Log out
          </button>
        </section>
      ) : null}
    </aside>
  );
}
