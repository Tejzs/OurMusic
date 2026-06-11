"use client";

import Image from "next/image";
import type { Album } from "../music-types";
import { PlayQueueActions } from "./play-queue-actions";
import { songArtworkUrl } from "../lib/auth";

type AlbumsGridProps = {
  albums: Album[];
  onOpenAlbum: (album: Album) => void;
  onPlayAlbum: (album: Album) => void;
  onQueueAlbum: (album: Album) => void;
};

export function AlbumsGrid({ albums, onOpenAlbum, onPlayAlbum, onQueueAlbum }: AlbumsGridProps) {
  return (
    <div
      className="grid h-full min-h-0 auto-rows-[minmax(0,auto)] grid-cols-2 content-start gap-3 sm:gap-4 lg:grid-cols-[repeat(auto-fill,minmax(170px,170px))] lg:justify-start lg:gap-x-4 lg:gap-y-5"
    >
      {albums.map((album, index) => (
        <article
          key={album.id}
          role="button"
          tabIndex={0}
          onClick={() => onOpenAlbum(album)}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              onOpenAlbum(album);
            }
          }}
          className="group relative aspect-[3/4] overflow-hidden rounded-[24px] border border-zinc-800 bg-zinc-950/80 text-left transition hover:-translate-y-0.5 hover:border-zinc-600 sm:aspect-square sm:rounded-[28px] ourmusic-animate-fade-up"
          style={{ animationDelay: `${index * 22}ms` }}
        >
          <Image
            src={songArtworkUrl(album.artworkSongId)}
            alt={album.title}
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
                <strong className="block truncate text-sm font-semibold text-white sm:text-base">{album.title}</strong>
                <p className="truncate text-xs text-zinc-300 sm:text-sm">{album.artist}</p>
                <p className="truncate text-xs text-zinc-400 sm:text-sm">{album.songCount} songs</p>
              </div>

              <PlayQueueActions
                album={album}
                onPlayAlbum={onPlayAlbum}
                onQueueAlbum={onQueueAlbum}
                variant="card"
                playLabel="Play"
                queueLabel="Add"
                queueGlyph="+"
              />
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
