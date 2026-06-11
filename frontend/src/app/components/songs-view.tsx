"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import type { Playlist, Song } from "../music-types";
import { SongsGrid } from "./songs-grid";

type SongsViewProps = {
  songs: Song[];
  title?: string;
  emptyMessage?: string;
  page: number;
  totalPages?: number;
  scrollKey: string;
  hasPreviousPage: boolean;
  hasNextPage: boolean;
  isLoading: boolean;
  onPreviousPage: () => void;
  onNextPage: () => void;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  playlists: Playlist[];
  onAddToPlaylist: (song: Song, playlistId: number) => void;
  likedSongIds: Set<number>;
  onToggleLike: (song: Song) => void;
  songBadges?: Map<number, string>;
};

export function SongsView({
  songs,
  title = "Songs",
  emptyMessage = "No songs found on this page.",
  page,
  totalPages,
  scrollKey,
  hasPreviousPage,
  hasNextPage,
  isLoading,
  onPreviousPage,
  onNextPage,
  onPlaySong,
  onAddToQueue,
  playlists,
  onAddToPlaylist,
  likedSongIds,
  onToggleLike,
  songBadges,
}: SongsViewProps) {
  return (
    <div className="flex h-full min-h-[420px] flex-col overflow-hidden">
      <div className="shrink-0 mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">{title}</h2>
          <p className="text-sm text-zinc-500">
            Page {page}
            {totalPages ? ` of ${totalPages}` : ""}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onPreviousPage}
            disabled={!hasPreviousPage || isLoading}
            className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm text-zinc-100 transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronLeft className="h-4 w-4" />
            Prev
          </button>

          <button
            type="button"
            onClick={onNextPage}
            disabled={!hasNextPage || isLoading}
            className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm text-zinc-100 transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Next
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>

      <div
        data-scroll-key={scrollKey}
        className="flex min-h-0 flex-1 flex-col overflow-y-auto overflow-x-hidden px-0 py-3"
      >
        <div className="flex min-h-[60vh] flex-1 flex-col">
          {songs.length === 0 && isLoading ? (
            <div className="flex min-h-[320px] flex-1 items-center justify-center rounded-[24px] border border-zinc-800 bg-zinc-950/50 px-6 text-sm text-zinc-400">
              Loading songs...
            </div>
          ) : songs.length === 0 ? (
            <div className="flex min-h-[320px] flex-1 items-center justify-center rounded-[24px] border border-dashed border-zinc-800 bg-zinc-950/50 px-6 text-sm text-zinc-500">
              {emptyMessage}
            </div>
          ) : (
            <SongsGrid
              songs={songs}
              playlists={playlists}
              onPlaySong={onPlaySong}
              onAddToQueue={onAddToQueue}
              onAddToPlaylist={onAddToPlaylist}
              likedSongIds={likedSongIds}
              onToggleLike={onToggleLike}
              songBadges={songBadges}
            />
          )}
        </div>
      </div>
    </div>
  );
}
