"use client";

import { Heart } from "lucide-react";
import type { Song } from "../music-types";

type LikeSongButtonProps = {
  song: Song;
  isLiked: boolean;
  onToggleLike: (song: Song) => void;
  variant?: "card" | "table";
};

export function LikeSongButton({
  song,
  isLiked,
  onToggleLike,
  variant = "table",
}: LikeSongButtonProps) {
  return (
    <button
      type="button"
      aria-label={isLiked ? `Unlike ${song.title}` : `Like ${song.title}`}
      className={
        variant === "card"
          ? "flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white shadow-lg shadow-black/30 backdrop-blur-xl transition hover:border-white/20 hover:bg-white/15"
          : "flex min-w-[44px] shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 px-3 py-2 text-xs text-zinc-100 backdrop-blur-md transition hover:border-white/20 hover:bg-white/10"
      }
      onClick={(event) => {
        event.stopPropagation();
        onToggleLike(song);
      }}
    >
      <Heart className={`h-4 w-4 ${isLiked ? "fill-red-400 text-red-400" : ""}`} />
    </button>
  );
}
