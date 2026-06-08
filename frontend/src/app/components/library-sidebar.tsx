"use client";

import Image from "next/image";
import { Shuffle, X, Trash2 } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { Song, View } from "../music-types";

type LibraryItem = {
  id: "songs" | "albums" | "artists" | "adminUsers";
  label: string;
  icon: LucideIcon;
};

type LibrarySidebarProps = {
  view: View;
  libraryItems: LibraryItem[];
  queue: Song[];
  authUser?: string | null;
  onViewSongs: () => void;
  onViewAlbums: () => void;
  onViewArtists: () => void;
  onViewAdminUsers: () => void;
  onRemoveFromQueue: (index: number) => void;
  onClearQueue: () => void;
  onShuffleQueue: () => void;
  onSignOut?: () => void;
  onClose?: () => void;
  isDrawer?: boolean;
  className?: string;
};

export function LibrarySidebar({
  view,
  libraryItems,
  queue,
  authUser,
  onViewSongs,
  onViewAlbums,
  onViewArtists,
  onViewAdminUsers,
  onRemoveFromQueue,
  onClearQueue,
  onShuffleQueue,
  onSignOut,
  onClose,
  isDrawer = false,
  className = "",
}: LibrarySidebarProps) {
  const containerClassName = isDrawer
    ? "flex h-full w-full flex-col gap-6 overflow-y-auto bg-zinc-900/95 p-4 backdrop-blur"
    : "flex shrink-0 flex-col gap-6 rounded-3xl border border-zinc-800 bg-zinc-900/80 p-4 backdrop-blur lg:h-full lg:w-[300px] lg:overflow-y-auto";

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
                if (item.id === "songs") {
                  onViewSongs();
                } else if (item.id === "albums") {
                  onViewAlbums();
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

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-zinc-500">
            Up Next
          </h3>
          <div className="flex items-center gap-2">
            <span className="text-xs text-zinc-500">{queue.length}</span>
            <button
              type="button"
              onClick={onShuffleQueue}
              disabled={queue.length < 2}
              className="flex items-center gap-1 rounded-full border border-zinc-800 bg-zinc-950/60 px-2.5 py-1 text-[11px] text-zinc-300 transition hover:border-zinc-700 hover:bg-zinc-900 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <Shuffle className="h-3 w-3" />
              Shuffle
            </button>
            <button
              type="button"
              onClick={onClearQueue}
              disabled={queue.length === 0}
              className="flex items-center gap-1 rounded-full border border-zinc-800 bg-zinc-950/60 px-2.5 py-1 text-[11px] text-zinc-300 transition hover:border-zinc-700 hover:bg-zinc-900 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <Trash2 className="h-3 w-3" />
              Clear
            </button>
          </div>
        </div>

        {queue.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-zinc-800 bg-zinc-950/50 px-4 py-5 text-sm text-zinc-500">
            Queue is empty.
          </p>
        ) : (
          <div className="space-y-2">
            {queue.map((song, index) => (
              <div
                key={`${song.id}-${index}`}
                className="relative overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3"
              >
                <Image
                  src={`http://192.168.1.76:8808/api/songs/${song.id}/artwork`}
                  alt={song.title}
                  className="absolute inset-0 h-full w-full object-cover object-center scale-110 blur-xs"
                  width={500}
                  height={500}
                  loading="lazy"
                  unoptimized
                />
                <div className="absolute inset-0 bg-zinc-950/70" />

                <div className="relative z-10 flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{song.title}</p>
                    <p className="truncate text-xs text-zinc-300">{song.artist}</p>
                  </div>

                  <button
                    type="button"
                    onClick={() => onRemoveFromQueue(index)}
                    className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white backdrop-blur-md transition hover:border-white/20 hover:bg-white/15"
                    aria-label={`Remove ${song.title} from queue`}
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {authUser && onSignOut ? (
        <section className="mt-auto rounded-3xl border border-white/10 bg-white/5 p-4 backdrop-blur-md">
          <p className="text-xs uppercase tracking-[0.18em] text-zinc-500">Account</p>
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
