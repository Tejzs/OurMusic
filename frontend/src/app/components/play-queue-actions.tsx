"use client";

import { Play } from "lucide-react";
import type { Album, Song } from "../music-types";

type PlayQueueActionsProps = {
  variant?: "card" | "table";
  song?: Song;
  album?: Album;
  onPlaySong?: (song: Song) => void;
  onAddToQueue?: (song: Song) => void;
  onPlayAlbum?: (album: Album) => void;
  onQueueAlbum?: (album: Album) => void;
  playLabel?: string;
  queueLabel?: string;
  queueGlyph?: string;
  isCard?: boolean;
};

export function PlayQueueActions({
  variant = "table",
  song,
  album,
  onPlaySong,
  onAddToQueue,
  onPlayAlbum,
  onQueueAlbum,
  playLabel,
  queueLabel,
  queueGlyph = "+",
}: PlayQueueActionsProps) {
  const isAlbumMode = album !== undefined;
  const effectivePlayLabel = playLabel ?? (isAlbumMode ? "Play album" : "Play");
  const effectiveQueueLabel = queueLabel ?? (isAlbumMode ? "Add" : "Queue");
  const handlePlay = () => {
    if (isAlbumMode) {
      onPlayAlbum?.(album as Album);
      return;
    }

    if (song) {
      onPlaySong?.(song);
    }
  };

  const handleQueue = () => {
    if (isAlbumMode) {
      onQueueAlbum?.(album as Album);
      return;
    }

    if (song) {
      onAddToQueue?.(song);
    }
  };

  if (variant === "card") {
    return (
      <div className="flex gap-2 opacity-100 transition-all duration-200 lg:max-h-0 lg:-translate-y-2 lg:overflow-hidden lg:opacity-0 lg:group-hover:max-h-16 lg:group-hover:translate-y-0 lg:group-hover:opacity-100">
        <button
          type="button"
          className="flex min-w-0 flex-1 items-center justify-center gap-1 rounded-full border border-white/10 bg-white/10 px-2 py-1.5 text-[9px] font-medium leading-none text-white backdrop-blur-md transition hover:border-white/20 hover:bg-white/15 sm:gap-1.5 sm:px-2.5 sm:py-2 sm:text-[10px]"
          onClick={(event) => {
            event.stopPropagation();
            handlePlay();
          }}
        >
          <Play className="h-3 w-3 fill-current" />
          {effectivePlayLabel}
        </button>

        <button
          type="button"
          className="flex min-w-0 flex-1 items-center justify-center gap-1 rounded-full border border-white/10 bg-white/5 px-2 py-1.5 text-[9px] font-medium leading-none text-zinc-100 backdrop-blur-md transition hover:border-white/20 hover:bg-white/10 sm:gap-1.5 sm:px-2.5 sm:py-2 sm:text-[10px]"
          onClick={(event) => {
            event.stopPropagation();
            handleQueue();
          }}
        >
          <span className="text-sm font-semibold leading-none text-white sm:text-base">{queueGlyph}</span>
          {effectiveQueueLabel}
        </button>
      </div>
    );
  }

  return (
    <div className="flex min-w-[176px] items-center gap-2">
      <button
        type="button"
        className="flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-2 text-xs font-medium text-white backdrop-blur-md transition hover:border-white/20 hover:bg-white/15"
        onClick={(event) => {
          event.stopPropagation();
          handlePlay();
        }}
      >
        <Play className="h-3 w-3 fill-current" />
        {effectivePlayLabel}
      </button>

      <button
        type="button"
        className="flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-2 text-xs text-zinc-100 backdrop-blur-md transition hover:border-white/20 hover:bg-white/10"
        onClick={(event) => {
          event.stopPropagation();
          handleQueue();
        }}
      >
        <span className="text-sm font-semibold leading-none text-white">{queueGlyph}</span>
        {effectiveQueueLabel}
      </button>
    </div>
  );
}
