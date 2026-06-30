"use client";

import Image from "next/image";
import { ListMusic, Plus, Search, X } from "lucide-react";
import { createPortal } from "react-dom";
import { useEffect, useMemo, useState } from "react";
import type { Playlist, Song } from "../music-types";
import { apiUrl } from "../lib/auth";

type AddToPlaylistPromptProps = {
  song: Song;
  playlists: Playlist[];
  onAddToPlaylist: (song: Song, playlistId: number) => void;
  variant?: "card" | "table";
};

export function AddToPlaylistPrompt({
  song,
  playlists,
  onAddToPlaylist,
  variant = "table",
}: AddToPlaylistPromptProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [addedPlaylistIds, setAddedPlaylistIds] = useState<Set<number>>(() => new Set());

  const filteredPlaylists = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    if (normalizedQuery === "") {
      return playlists;
    }

    return playlists.filter((playlist) => playlist.name.toLowerCase().includes(normalizedQuery));
  }, [playlists, query]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open]);

  const openPrompt = () => {
    setOpen(true);
  };

  const handleToggle = () => {
    if (open) {
      closePrompt();
      return;
    }

    openPrompt();
  };

  const closePrompt = () => {
    setOpen(false);
  };

  function handleAdd(playlist: Playlist) {
    onAddToPlaylist(song, playlist.id);
    setAddedPlaylistIds((current) => {
      const next = new Set(current);
      next.add(playlist.id);
      return next;
    });
  }

  return (
    <>
      <button
        type="button"
        aria-label="Add to playlist"
        className={
          variant === "table"
            ? "flex min-w-[44px] shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 px-3 py-2 text-xs text-zinc-100 backdrop-blur-md transition hover:border-white/20 hover:bg-white/10"
            : "flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white shadow-lg shadow-black/30 backdrop-blur-xl transition hover:border-white/20 hover:bg-white/15"
        }
        onClick={(event) => {
          event.stopPropagation();
          handleToggle();
        }}
      >
        <Plus className={variant === "table" ? "h-4 w-4" : "h-4 w-4"} />
      </button>

      {typeof document !== "undefined" && open
        ? createPortal(
            <div
              className="fixed inset-0 z-[9999] flex items-end justify-center bg-black/60 p-3 backdrop-blur-sm sm:items-center sm:p-6 ourmusic-animate-fade-in"
            >
              <button
                type="button"
                className="absolute inset-0 cursor-default"
                aria-label="Close add to playlist"
                onClick={closePrompt}
              />

              <div
                className="relative z-10 max-h-[calc(100vh-1.5rem)] w-full max-w-[380px] overflow-hidden rounded-t-3xl border border-white/10 bg-zinc-950/95 shadow-[0_24px_80px_rgba(0,0,0,0.55)] backdrop-blur-2xl sm:max-h-[min(560px,calc(100vh-3rem))] sm:rounded-3xl ourmusic-animate-pop-in"
              >
                <div className="flex items-start justify-between gap-3 border-b border-white/10 bg-white/[0.03] px-4 py-3">
                  <div className="min-w-0">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-zinc-400">Add to playlist</p>
                    <p className="mt-1 truncate text-sm font-medium text-white">{song.title}</p>
                    <p className="truncate text-xs text-zinc-500">{song.artist}</p>
                  </div>
                  <button
                    type="button"
                    className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 text-zinc-300 transition hover:bg-white/10 hover:text-white"
                    onClick={closePrompt}
                    aria-label="Close add to playlist"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>

                <div className="space-y-3 p-3">
                  <div className="flex items-center gap-2 rounded-2xl border border-white/10 bg-white/[0.04] px-3 py-2">
                    <Search className="h-4 w-4 shrink-0 text-zinc-500" />
                    <input
                      type="search"
                      value={query}
                      onChange={(event) => setQuery(event.target.value)}
                      className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-zinc-600"
                      placeholder="Search playlists"
                    />
                  </div>

                  <div className="max-h-[min(44vh,260px)] space-y-1.5 overflow-y-auto pr-1 sm:max-h-64">
                    {filteredPlaylists.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-white/10 bg-white/[0.03] px-3 py-4 text-sm text-zinc-500">
                        No playlists found.
                      </div>
                    ) : (
                      filteredPlaylists.map((playlist) => {
                        const isAdded = addedPlaylistIds.has(playlist.id);

                        return (
                          <button
                            key={playlist.id}
                            type="button"
                            className={`flex w-full items-center gap-3 rounded-2xl border px-3 py-2.5 text-left text-sm transition ${
                              isAdded
                                ? "border-emerald-500/25 bg-emerald-500/10 text-emerald-100"
                                : "border-white/10 bg-white/[0.04] text-zinc-100 hover:border-white/20 hover:bg-white/10"
                            }`}
                            onClick={(event) => {
                              event.stopPropagation();
                              handleAdd(playlist);
                            }}
                          >
                            <PlaylistCoverThumb playlist={playlist} />
                            <span className="min-w-0 flex-1 truncate font-medium">{playlist.name}</span>
                            {isAdded ? <span className="shrink-0 text-xs text-emerald-200">Added</span> : null}
                          </button>
                        );
                      })
                    )}
                  </div>
                </div>
              </div>
            </div>,
            document.body,
          )
        : null}
    </>
  );
}

function PlaylistCoverThumb({ playlist }: { playlist: Playlist }) {
  const [hasErrored, setHasErrored] = useState(false);
  const source = playlist.hasCover ? apiUrl(`/api/playlists/${playlist.id}/cover`) : null;

  return (
    <span className="relative flex h-10 w-10 shrink-0 overflow-hidden rounded-2xl border border-white/10 bg-zinc-800">
      {source !== null && !hasErrored ? (
        <Image
          src={source}
          alt={playlist.name}
          className="h-full w-full object-cover"
          width={40}
          height={40}
          loading="lazy"
          unoptimized
          onError={() => {
            if (!hasErrored) {
              setHasErrored(true);
            }
          }}
        />
      ) : (
        <span className="flex h-full w-full items-center justify-center text-zinc-200">
          <ListMusic className="h-4 w-4" />
        </span>
      )}
    </span>
  );
}
