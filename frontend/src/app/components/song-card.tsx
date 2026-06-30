"use client";

import Image from "next/image";
import { Download } from "lucide-react";

import type { Playlist, Song } from "../music-types";
import { AddToPlaylistPrompt } from "./add-to-playlist-prompt";
import { LikeSongButton } from "./like-song-button";
import { PlayQueueActions } from "./play-queue-actions";
import { apiUrl, songArtworkUrl } from "../lib/auth";

type SongCardProps = {
  song: Song;
  playlists: Playlist[];
  likedSongIds: Set<number>;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  onAddToPlaylist: (song: Song, playlistId: number) => void;
  onToggleLike: (song: Song) => void;
  songBadges?: Map<number, string>;
  showDownload?: boolean;
  animationDelayMs?: number;
};

export function SongCard({
  song,
  playlists,
  likedSongIds,
  onPlaySong,
  onAddToQueue,
  onAddToPlaylist,
  onToggleLike,
  songBadges,
  showDownload = true,
  animationDelayMs = 0,
}: SongCardProps) {
  return (
    <article
      className="group relative overflow-visible transition-transform duration-200 hover:-translate-y-0.5 focus-within:-translate-y-0.5 ourmusic-animate-fade-up"
      style={{ animationDelay: `${animationDelayMs}ms` }}
    >
      <div className="relative aspect-[3/4] overflow-hidden rounded-[24px] border border-zinc-800 bg-zinc-950/80 transition-colors hover:border-zinc-600 sm:aspect-square sm:rounded-[28px]">
        <Image
          src={songArtworkUrl(song.id)}
          alt={song.title}
          className="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105 group-focus-within:scale-105"
          width={500}
          height={500}
          loading="lazy"
          unoptimized
        />
        <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/45 to-transparent" />

        <div className="absolute inset-x-0 bottom-0 z-20 p-3 sm:p-4">
          <div className="flex min-w-0 flex-col gap-3">
            <div className="min-w-0">
              <strong className="block truncate text-sm font-semibold text-white sm:text-base">
                {song.title}
              </strong>
              <p className="truncate text-xs text-zinc-300 sm:text-sm">
                {song.artist}
              </p>
              {songBadges?.has(song.id) ? (
                <span className="mt-1 inline-flex w-fit rounded-full border border-white/10 bg-white/10 px-2 py-0.5 text-[10px] font-medium text-zinc-100 backdrop-blur-md">
                  {songBadges.get(song.id)}
                </span>
              ) : null}
            </div>

            <PlayQueueActions
              song={song}
              onPlaySong={onPlaySong}
              onAddToQueue={onAddToQueue}
              variant="card"
              showDownload={false}
            />
          </div>
        </div>
      </div>

      <div className="absolute right-2 top-2 z-30 flex gap-2">
        <LikeSongButton
          song={song}
          isLiked={likedSongIds.has(song.id)}
          onToggleLike={onToggleLike}
          variant="card"
        />
        <AddToPlaylistPrompt
          song={song}
          playlists={playlists}
          onAddToPlaylist={onAddToPlaylist}
          variant="card"
        />
        {showDownload ? (
          <a
            href={apiUrl(`/api/songs/${song.id}/download`)}
            download
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-white/10 bg-zinc-950/55 text-zinc-100 shadow-lg backdrop-blur-md transition hover:border-white/20 hover:bg-zinc-900/75"
            aria-label={`Download ${song.title}`}
          >
            <Download className="h-4 w-4" />
          </a>
        ) : null}
      </div>
    </article>
  );
}
