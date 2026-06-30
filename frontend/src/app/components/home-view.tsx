"use client";

import Image from "next/image";
import { Disc3, Download, ListMusic, Play, Plus, UsersRound } from "lucide-react";
import type { ReactNode } from "react";
import type { Album, LibraryStats, MostPlayedSong, Playlist, Song } from "../music-types";
import { apiUrl, songArtworkUrl } from "../lib/auth";

type HomeViewProps = {
  currentSong: Song | null;
  songs: Song[];
  albums: Album[];
  playlists: Playlist[];
  stats: LibraryStats | null;
  recentlyPlayedSongs: Song[];
  mostPlayedSongs: MostPlayedSong[];
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  onViewSongs: () => void;
  onViewAlbums: () => void;
  onViewPlaylists: () => void;
  onViewArtists: () => void;
  onViewRecentlyPlayed: () => void;
  onViewMostPlayed: () => void;
};

export function HomeView({
  currentSong,
  songs,
  albums,
  playlists,
  stats,
  recentlyPlayedSongs,
  mostPlayedSongs,
  onPlaySong,
  onAddToQueue,
  onViewSongs,
  onViewAlbums,
  onViewPlaylists,
  onViewArtists,
  onViewRecentlyPlayed,
  onViewMostPlayed,
}: HomeViewProps) {
  const starterSongs = recentlyPlayedSongs.length > 0 ? recentlyPlayedSongs : songs;
  const totalSongs = stats?.totalSongs ?? songs.length;
  const totalAlbums = stats?.totalAlbums ?? albums.length;
  const totalArtists = stats?.totalArtists ?? 0;
  const totalPlaylists = stats?.totalPlaylists ?? playlists.length;

  return (
    <div className="h-full min-h-0 overflow-y-auto overflow-x-hidden pr-1">
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.2fr)_minmax(320px,0.8fr)]">
        <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950/45 ourmusic-animate-fade-up">
          <div className="border-b border-zinc-800 px-5 py-4">
            <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">Home</p>
            <h2 className="mt-2 text-2xl font-semibold">Start listening</h2>
          </div>

          <div className="grid gap-3 p-4 sm:grid-cols-2">
            <button
              type="button"
              onClick={onViewSongs}
              className="rounded-2xl border border-white/10 bg-white/5 p-4 text-left transition hover:border-white/20 hover:bg-white/10"
            >
              <MusicMetric label="Songs" value={totalSongs} />
            </button>
            <button
              type="button"
              onClick={onViewAlbums}
              className="rounded-2xl border border-white/10 bg-white/5 p-4 text-left transition hover:border-white/20 hover:bg-white/10"
            >
              <MusicMetric label="Albums" value={totalAlbums} icon={<Disc3 className="h-4 w-4" />} />
            </button>
            <button
              type="button"
              onClick={onViewPlaylists}
              className="rounded-2xl border border-white/10 bg-white/5 p-4 text-left transition hover:border-white/20 hover:bg-white/10"
            >
              <MusicMetric label="Playlists" value={totalPlaylists} icon={<ListMusic className="h-4 w-4" />} />
            </button>
            <button
              type="button"
              onClick={onViewArtists}
              className="rounded-2xl border border-white/10 bg-white/5 p-4 text-left transition hover:border-white/20 hover:bg-white/10"
            >
              <MusicMetric label="Artists" value={totalArtists} icon={<UsersRound className="h-4 w-4" />} />
            </button>
          </div>
        </section>

        <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950/45 ourmusic-animate-fade-up">
          <div className="border-b border-zinc-800 px-5 py-4">
            <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">Now Playing</p>
            <h2 className="mt-2 text-2xl font-semibold">Current track</h2>
          </div>

          {currentSong ? (
            <SongPanel song={currentSong} />
          ) : (
            <div className="p-5 text-sm text-zinc-500">Nothing is playing.</div>
          )}
        </section>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <SongListSection
          title="Recently Played"
          songs={recentlyPlayedSongs.slice(0, 5)}
          emptySongs={starterSongs.slice(0, 5)}
          onViewAll={onViewRecentlyPlayed}
          onPlaySong={onPlaySong}
          onAddToQueue={onAddToQueue}
        />

        <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950/45 ourmusic-animate-fade-up">
          <div className="flex items-center justify-between border-b border-zinc-800 px-5 py-4">
            <div>
              <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">Top Tracks</p>
              <h2 className="mt-2 text-xl font-semibold">Most Played</h2>
            </div>
            <button type="button" onClick={onViewMostPlayed} className="text-sm text-zinc-300 hover:text-white">
              View
            </button>
          </div>

          <div className="divide-y divide-zinc-800">
            {mostPlayedSongs.slice(0, 6).map((entry, index) => (
              <TrackRow
                key={`${entry.song.id}-${index}`}
                song={entry.song}
                badge={`${entry.playCount} plays`}
                onPlaySong={onPlaySong}
                onAddToQueue={onAddToQueue}
              />
            ))}
            {mostPlayedSongs.length === 0 ? (
              <div className="px-5 py-5 text-sm text-zinc-500">No most played songs yet.</div>
            ) : null}
          </div>
        </section>
      </div>
    </div>
  );
}

function MusicMetric({ label, value, icon }: { label: string; value: number; icon?: ReactNode }) {
  return (
    <div>
      <div className="flex items-center gap-2 text-xs uppercase tracking-[0.18em] text-zinc-500">
        {icon}
        {label}
      </div>
      <p className="mt-3 text-3xl font-semibold text-white">{value}</p>
    </div>
  );
}

function SongPanel({ song }: { song: Song }) {
  return (
    <div className="flex min-w-0 items-center gap-4 p-5">
      <Image
        src={songArtworkUrl(song.id)}
        alt={song.title}
        className="h-20 w-20 shrink-0 rounded-2xl object-cover ring-1 ring-white/10"
        width={80}
        height={80}
        loading="lazy"
        unoptimized
      />
      <div className="min-w-0 flex-1">
        <strong className="block truncate text-lg font-semibold text-white">{song.title}</strong>
        <p className="truncate text-sm text-zinc-400">{song.artist}</p>
        <p className="mt-2 truncate text-xs text-zinc-500">{song.album}</p>
      </div>
    </div>
  );
}

function SongListSection({
  title,
  songs,
  emptySongs,
  onViewAll,
  onPlaySong,
  onAddToQueue,
}: {
  title: string;
  songs: Song[];
  emptySongs: Song[];
  onViewAll: () => void;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
}) {
  const displaySongs = songs.length > 0 ? songs : emptySongs;

  return (
    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950/45 ourmusic-animate-fade-up">
      <div className="flex items-center justify-between border-b border-zinc-800 px-5 py-4">
        <div>
          <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">Listen Again</p>
          <h2 className="mt-2 text-xl font-semibold">{title}</h2>
        </div>
        <button type="button" onClick={onViewAll} className="text-sm text-zinc-300 hover:text-white">
          View
        </button>
      </div>

      <div className="divide-y divide-zinc-800">
        {displaySongs.map((song, index) => (
          <TrackRow
            key={`${song.id}-${index}`}
            song={song}
            badge={undefined}
            onPlaySong={onPlaySong}
            onAddToQueue={onAddToQueue}
            animationDelayMs={index * 18}
          />
        ))}
        {displaySongs.length === 0 ? (
          <div className="px-5 py-5 text-sm text-zinc-500">No songs available yet.</div>
        ) : null}
      </div>
    </section>
  );
}

function TrackRow({
  song,
  badge,
  onPlaySong,
  onAddToQueue,
  animationDelayMs = 0,
}: {
  song: Song;
  badge?: string;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  animationDelayMs?: number;
}) {
  return (
    <div
      className="flex min-w-0 items-center gap-3 px-5 py-3 transition hover:bg-white/[0.03] ourmusic-animate-fade-up"
      style={{ animationDelay: `${animationDelayMs}ms` }}
    >
      <Image
        src={songArtworkUrl(song.id)}
        alt={song.title}
        className="h-11 w-11 shrink-0 rounded-xl object-cover ring-1 ring-white/10"
        width={44}
        height={44}
        loading="lazy"
        unoptimized
      />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-white">{song.title}</p>
        <p className="truncate text-xs text-zinc-400">{song.artist}</p>
      </div>
      {badge ? (
        <span className="hidden shrink-0 items-center gap-1 rounded-full border border-white/10 bg-white/5 px-2 py-1 text-[11px] text-zinc-300 sm:inline-flex">
          {badge}
        </span>
      ) : null}
      <button
        type="button"
        onClick={() => onPlaySong(song)}
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white transition hover:bg-white/15"
        aria-label={`Play ${song.title}`}
      >
        <Play className="h-3.5 w-3.5 fill-current" />
      </button>
      <button
        type="button"
        onClick={() => onAddToQueue(song)}
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 text-white transition hover:bg-white/10"
        aria-label={`Add ${song.title} to queue`}
      >
        <Plus className="h-3.5 w-3.5" />
      </button>
      <a
        href={apiUrl(`/api/songs/${song.id}/download`)}
        download
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 text-white transition hover:bg-white/10"
        aria-label={`Download ${song.title}`}
      >
        <Download className="h-3.5 w-3.5" />
      </a>
    </div>
  );
}
