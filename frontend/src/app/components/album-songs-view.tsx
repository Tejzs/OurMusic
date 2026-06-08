"use client";

import { ChevronLeft, Play } from "lucide-react";
import type { Album, Song } from "../music-types";
import { PlayQueueActions } from "./play-queue-actions";

type AlbumSongsViewProps = {
  selectedAlbum: Album;
  albumSongs: Song[];
  onBack: () => void;
  onPlayAlbum: () => void;
  onQueueAlbum: () => void;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
};

export function AlbumSongsView({
  selectedAlbum,
  albumSongs,
  onBack,
  onPlayAlbum,
  onQueueAlbum,
  onPlaySong,
  onAddToQueue,
}: AlbumSongsViewProps) {
  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <button
        type="button"
        onClick={onBack}
        className="flex items-center gap-2 self-start rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm hover:bg-zinc-800"
      >
        <ChevronLeft className="h-4 w-4" />
        Back
      </button>

      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/80 p-4 shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset]">
        <div className="mb-4 border-b border-zinc-800 pb-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h2 className="text-3xl font-semibold">{selectedAlbum.title}</h2>
              <p className="mt-2 text-zinc-400">{selectedAlbum.artist}</p>
            </div>

            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                className="flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-2 text-xs font-medium text-white backdrop-blur-md transition hover:border-white/20 hover:bg-white/15"
                onClick={onPlayAlbum}
                disabled={albumSongs.length === 0}
              >
                <Play className="h-3 w-3 fill-current" />
                Play album
              </button>

              <button
                type="button"
                className="flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-2 text-xs font-medium text-zinc-100 backdrop-blur-md transition hover:border-white/20 hover:bg-white/10"
                onClick={onQueueAlbum}
                disabled={albumSongs.length === 0}
              >
                <span className="text-sm font-semibold leading-none text-white">+</span>
                Add album to queue
              </button>
            </div>
          </div>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden" data-motion-list>
          <table className="w-full table-fixed border-collapse">
            <thead className="sticky top-0 z-10 bg-zinc-950/90 text-left text-xs uppercase tracking-[0.18em] text-zinc-500 backdrop-blur">
              <tr>
                <th className="w-[6%] px-5 py-3 font-medium">#</th>
                <th className="w-[44%] px-5 py-3 font-medium">Title</th>
                <th className="w-[22%] px-5 py-3 font-medium">Artist</th>
                <th className="w-[12%] px-5 py-3 font-medium">Time</th>
                <th className="w-[16%] px-5 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {albumSongs.map((song, index) => (
                <tr
                  key={song.id}
                  data-motion-item
                  className="border-t border-zinc-800 transition hover:bg-zinc-800/70"
                >
                  <td className="px-5 py-4 text-sm text-zinc-500 align-middle">
                    {index + 1}
                  </td>
                  <td className="px-5 py-4 align-middle">
                    <strong className="block truncate text-sm font-medium text-white">
                      {song.title}
                    </strong>
                  </td>
                  <td className="px-5 py-4 align-middle text-sm text-zinc-400">
                    <span className="block truncate">{song.artist}</span>
                  </td>
                  <td className="px-5 py-4 align-middle text-sm text-zinc-400">
                    {formatTime(song.duration)}
                  </td>
                  <td className="px-5 py-4 align-middle">
                    <PlayQueueActions song={song} onPlaySong={onPlaySong} onAddToQueue={onAddToQueue} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function formatTime(seconds: number) {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return "0:00";
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.floor(seconds % 60)
    .toString()
    .padStart(2, "0");

  return `${minutes}:${remainingSeconds}`;
}
