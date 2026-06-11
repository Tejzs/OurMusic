"use client";

import Image from "next/image";
import { ChevronLeft } from "lucide-react";
import type { Album, Artist, Playlist, Song } from "../music-types";
import { AddToPlaylistPrompt } from "./add-to-playlist-prompt";
import { LikeSongButton } from "./like-song-button";
import { PlayQueueActions } from "./play-queue-actions";
import { songArtworkUrl } from "../lib/auth";

type ArtistSongsViewProps = {
  selectedArtist: Artist;
  artistAlbums: Album[];
  artistSongs: Song[];
  onBackToArtists: () => void;
  onOpenArtistAlbum: (album: Album) => void;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  playlists: Playlist[];
  onAddToPlaylist: (song: Song, playlistId: number) => void;
  likedSongIds: Set<number>;
  onToggleLike: (song: Song) => void;
};

export function ArtistSongsView({
  selectedArtist,
  artistAlbums,
  artistSongs,
  onBackToArtists,
  onOpenArtistAlbum,
  onPlaySong,
  onAddToQueue,
  playlists,
  onAddToPlaylist,
  likedSongIds,
  onToggleLike,
}: ArtistSongsViewProps) {
  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <button
        type="button"
        onClick={onBackToArtists}
        className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm hover:bg-zinc-800"
      >
        <ChevronLeft className="h-4 w-4" />
        Back to Artists
      </button>

      <div>
        <p className="text-sm uppercase tracking-[0.18em] text-zinc-500">Artist detail</p>
        <div className="mt-2 flex flex-col gap-2 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="text-3xl font-semibold">{selectedArtist.name}</h2>
            <p className="text-zinc-400">
              {selectedArtist.albumCount} albums · {selectedArtist.songCount} songs
            </p>
          </div>
        </div>
      </div>

      <div className="grid min-h-0 flex-1 gap-0 lg:grid-cols-[0.95fr_1.05fr]">
        <section className="border-b border-zinc-800 lg:flex lg:min-h-0 lg:flex-col lg:border-b-0 lg:border-r">
          <div className="border-b border-zinc-800 px-5 py-4">
            <h3 className="text-lg font-semibold">Albums</h3>
          </div>

          <div
            className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden divide-y divide-zinc-800"
          >
                {artistAlbums.map((album, index) => (
                  <button
                    type="button"
                    key={album.id}
                    onClick={() => onOpenArtistAlbum(album)}
                    className="relative flex h-[88px] w-full items-center justify-between gap-4 overflow-hidden px-5 py-4 text-left transition hover:brightness-110 ourmusic-animate-fade-up"
                    style={{ animationDelay: `${index * 16}ms` }}
                  >
                <Image
                  src={songArtworkUrl(album.artworkSongId)}
                  alt={album.title}
                  className="absolute inset-0 h-full w-full object-cover object-center scale-110 blur-xs"
                  width={500}
                  height={500}
                  loading="lazy"
                  unoptimized
                />
                <div className="absolute inset-0 bg-zinc-950/65" />

                <div className="relative z-10 flex min-w-0 items-center gap-4">
                  <Image
                    src={songArtworkUrl(album.artworkSongId)}
                    alt={album.title}
                    className="h-12 w-12 shrink-0 rounded-md object-cover ring-1 ring-white/10"
                    width={48}
                    height={48}
                    loading="lazy"
                    unoptimized
                  />
                  <div className="min-w-0">
                    <strong className="block truncate text-sm font-medium text-white">{album.title}</strong>
                    <p className="truncate text-xs text-zinc-300">{album.artist}</p>
                  </div>
                </div>

                <span className="relative z-10 shrink-0 text-sm text-zinc-200">{album.songCount} songs</span>
              </button>
            ))}
          </div>
        </section>

        <section className="lg:flex lg:min-h-0 lg:flex-col">
          <div className="border-b border-zinc-800 px-5 py-4">
            <h3 className="text-lg font-semibold">Songs</h3>
          </div>

          <div
            className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden"
          >
            <table className="w-full table-fixed border-collapse">
              <thead className="bg-zinc-950/60 text-left text-xs uppercase tracking-[0.18em] text-zinc-500">
                <tr>
                  <th className="w-[38%] px-5 py-3 font-medium align-middle">Title</th>
                  <th className="w-[22%] px-5 py-3 font-medium align-middle">Album</th>
                  <th className="w-[40%] px-5 py-3 font-medium align-middle">Actions</th>
                </tr>
              </thead>
              <tbody>
                {artistSongs.map((song, index) => (
                  <tr
                    key={`${song.id}-${index}`}
                    className="h-[88px] border-t border-zinc-800 ourmusic-animate-fade-up"
                    style={{ animationDelay: `${index * 16}ms` }}
                  >
                    <td className="px-5 py-4 align-middle">
                      <div className="flex h-full items-center">
                        <strong className="block truncate text-sm font-medium">{song.title}</strong>
                      </div>
                    </td>
                    <td className="px-5 py-4 text-sm text-zinc-400 align-middle">
                      <div className="flex h-full items-center">
                        <span className="block truncate">{song.album}</span>
                      </div>
                    </td>
                    <td className="px-5 py-4 align-middle">
                      <PlayQueueActions song={song} onPlaySong={onPlaySong} onAddToQueue={onAddToQueue}>
                        <AddToPlaylistPrompt
                          song={song}
                          playlists={playlists}
                          onAddToPlaylist={onAddToPlaylist}
                        />
                        <LikeSongButton
                          song={song}
                          isLiked={likedSongIds.has(song.id)}
                          onToggleLike={onToggleLike}
                        />
                      </PlayQueueActions>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}
