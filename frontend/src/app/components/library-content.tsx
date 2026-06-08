"use client";

import type { Album, Artist, Song, View } from "../music-types";
import { AlbumSongsView } from "./album-songs-view";
import { AlbumsView } from "./albums-view";
import { AdminUsersView } from "./admin-users-view";
import { ArtistsTable } from "./artists-table";
import { ArtistSongsView } from "./artist-songs-view";
import { SongsView } from "./songs-view";

type LibraryContentProps = {
  view: View;
  songs: Song[];
  albums: Album[];
  artists: Artist[];
  hasMoreArtists: boolean;
  isArtistsLoading: boolean;
  songPage: number;
  hasPreviousSongPage: boolean;
  hasNextSongPage: boolean;
  isSongsLoading: boolean;
  albumPage: number;
  hasPreviousAlbumPage: boolean;
  hasNextAlbumPage: boolean;
  isAlbumsLoading: boolean;
  albumSongs: Song[];
  artistSongs: Song[];
  artistAlbums: Album[];
  selectedAlbum: Album | null;
  selectedArtist: Artist | null;
  onOpenAlbum: (album: Album) => void;
  onOpenArtistAlbum: (album: Album) => void;
  onOpenArtist: (artist: Artist) => void;
  onLoadMoreArtists: () => void;
  onUnauthorized: () => void;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  onPreviousSongPage: () => void;
  onNextSongPage: () => void;
  onPreviousAlbumPage: () => void;
  onNextAlbumPage: () => void;
  onBackFromAlbumSongs: () => void;
  onBackToArtists: () => void;
  onPlayAlbum: () => void;
  onQueueAlbum: () => void;
  onPlaySelectedAlbum: (album: Album) => void;
  onQueueSelectedAlbum: (album: Album) => void;
  onAlbumViewportMeasure: (size: { width: number; height: number }) => void;
  onSongViewportMeasure: (size: { width: number; height: number }) => void;
};

export function LibraryContent({
  view,
  songs,
  albums,
  artists,
  hasMoreArtists,
  isArtistsLoading,
  songPage,
  hasPreviousSongPage,
  hasNextSongPage,
  isSongsLoading,
  albumPage,
  hasPreviousAlbumPage,
  hasNextAlbumPage,
  isAlbumsLoading,
  albumSongs,
  artistSongs,
  artistAlbums,
  selectedAlbum,
  selectedArtist,
  onOpenAlbum,
  onOpenArtistAlbum,
  onOpenArtist,
  onLoadMoreArtists,
  onUnauthorized,
  onPlaySong,
  onAddToQueue,
  onPreviousSongPage,
  onNextSongPage,
  onPreviousAlbumPage,
  onNextAlbumPage,
  onBackFromAlbumSongs,
  onBackToArtists,
  onPlayAlbum,
  onQueueAlbum,
  onPlaySelectedAlbum,
  onQueueSelectedAlbum,
  onAlbumViewportMeasure,
  onSongViewportMeasure,
}: LibraryContentProps) {
  if (view === "songs") {
    return (
      <SongsView
        songs={songs}
        page={songPage}
        scrollKey={`songs-${songPage}-${songs.length}`}
        hasPreviousPage={hasPreviousSongPage}
        hasNextPage={hasNextSongPage}
        isLoading={isSongsLoading}
        onPreviousPage={onPreviousSongPage}
        onNextPage={onNextSongPage}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
        onViewportMeasure={onSongViewportMeasure}
      />
    );
  }

  if (view === "albums") {
    return (
      <AlbumsView
        albums={albums}
        page={albumPage}
        scrollKey={`albums-${albumPage}-${albums.length}`}
        hasPreviousPage={hasPreviousAlbumPage}
        hasNextPage={hasNextAlbumPage}
        isLoading={isAlbumsLoading}
        onPreviousPage={onPreviousAlbumPage}
        onNextPage={onNextAlbumPage}
        onOpenAlbum={onOpenAlbum}
        onPlayAlbum={onPlaySelectedAlbum}
        onQueueAlbum={onQueueSelectedAlbum}
        onViewportMeasure={onAlbumViewportMeasure}
      />
    );
  }

  if (view === "albumSongs" && selectedAlbum !== null) {
    return (
      <AlbumSongsView
        selectedAlbum={selectedAlbum}
        albumSongs={albumSongs}
        onBack={onBackFromAlbumSongs}
        onPlayAlbum={onPlayAlbum}
        onQueueAlbum={onQueueAlbum}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
      />
    );
  }

  if (view === "artists") {
    return (
      <ArtistsTable
        artists={artists}
        hasMore={hasMoreArtists}
        isLoading={isArtistsLoading}
        onOpenArtist={onOpenArtist}
        onLoadMore={onLoadMoreArtists}
      />
    );
  }

  if (view === "adminUsers") {
    return <AdminUsersView onUnauthorized={onUnauthorized} />;
  }

  if (view === "artistSongs" && selectedArtist !== null) {
    return (
      <ArtistSongsView
        selectedArtist={selectedArtist}
        artistAlbums={artistAlbums}
        artistSongs={artistSongs}
        onBackToArtists={onBackToArtists}
        onOpenArtistAlbum={onOpenArtistAlbum}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
      />
    );
  }

  return null;
}
