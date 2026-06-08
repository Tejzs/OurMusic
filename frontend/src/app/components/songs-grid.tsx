"use client";

import Image from "next/image";
import type { Song } from "../music-types";
import { PlayQueueActions } from "./play-queue-actions";

type SongsGridProps = {
  songs: Song[];
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
};

export function SongsGrid({ songs, onPlaySong, onAddToQueue }: SongsGridProps) {
  return (
    <div className="grid h-full min-h-0 auto-rows-[minmax(0,auto)] grid-cols-2 content-start gap-3 sm:gap-4 lg:grid-cols-[repeat(auto-fill,170px)] lg:justify-between lg:gap-x-4 lg:gap-y-5">
      {songs.map((song) => (
        <article
          key={song.id}
          data-motion-item
          className="group relative aspect-[3/4] overflow-hidden rounded-[24px] border border-zinc-800 bg-zinc-950/80 transition hover:-translate-y-0.5 hover:border-zinc-600 sm:aspect-square sm:rounded-[28px]"
        >
          <Image
            src={`http://192.168.1.76:8808/api/songs/${song.id}/artwork`}
            alt={song.title}
            className="absolute inset-0 h-full w-full object-cover transition duration-300 group-hover:scale-105"
            width={500}
            height={500}
            loading="lazy"
            unoptimized
          />
          <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/45 to-transparent" />

          <div className="absolute inset-x-0 bottom-0 p-3 sm:p-4">
            <div className="flex min-w-0 flex-col gap-3">
              <div className="min-w-0">
                <strong className="block truncate text-sm font-semibold text-white sm:text-base">{song.title}</strong>
                <p className="truncate text-xs text-zinc-300 sm:text-sm">{song.artist}</p>
              </div>

              <PlayQueueActions song={song} onPlaySong={onPlaySong} onAddToQueue={onAddToQueue} variant="card" />
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
