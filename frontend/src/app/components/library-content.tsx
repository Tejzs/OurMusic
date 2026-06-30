"use client";

import type { Album, Artist, LibraryStats, MostPlayedSong, Playlist, Song, View } from "../music-types";
import { AlbumSongsView } from "./album-songs-view";
import { AlbumsView } from "./albums-view";
import { AdminUsersView } from "./admin-users-view";
import { ArtistsTable } from "./artists-table";
import { ArtistSongsView } from "./artist-songs-view";
import { HomeView } from "./home-view";
import { PlaylistsView } from "./playlists-view";
import { SongsView } from "./songs-view";

type LibraryContentProps = {
  view: View;
  songs: Song[];
  currentSong: Song | null;
  stats: LibraryStats | null;
  albums: Album[];
  artists: Artist[];
  playlists: Playlist[];
  likedSongs: Song[];
  isLikedSongsLoading: boolean;
  recentlyPlayedSongs: Song[];
  mostPlayedSongs: MostPlayedSong[];
  likedSongIds: Set<number>;
  hasMoreArtists: boolean;
  isArtistsLoading: boolean;
  songPage: number;
  totalSongPages?: number;
  hasPreviousSongPage: boolean;
  hasNextSongPage: boolean;
  isSongsLoading: boolean;
  albumPage: number;
  totalAlbumPages?: number;
  hasPreviousAlbumPage: boolean;
  hasNextAlbumPage: boolean;
  isAlbumsLoading: boolean;
  recentlyPlayedPage: number;
  hasPreviousRecentlyPlayedPage: boolean;
  hasNextRecentlyPlayedPage: boolean;
  isRecentlyPlayedLoading: boolean;
  mostPlayedPage: number;
  hasPreviousMostPlayedPage: boolean;
  hasNextMostPlayedPage: boolean;
  isMostPlayedLoading: boolean;
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
  onPlaylistsChanged: () => void;
  refreshPlaylistSongsToken: number;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  onAddToPlaylist: (song: Song, playlistId: number) => void;
  onToggleLike: (song: Song) => void;
  onPreviousSongPage: () => void;
  onNextSongPage: () => void;
  onPreviousAlbumPage: () => void;
  onNextAlbumPage: () => void;
  onPreviousRecentlyPlayedPage: () => void;
  onNextRecentlyPlayedPage: () => void;
  onPreviousMostPlayedPage: () => void;
  onNextMostPlayedPage: () => void;
  onBackFromAlbumSongs: () => void;
  onBackToArtists: () => void;
  onPlayAlbum: () => void;
  onQueueAlbum: () => void;
  onPlaySelectedAlbum: (album: Album) => void;
  onQueueSelectedAlbum: (album: Album) => void;
  onViewSongs: () => void;
  onViewAlbums: () => void;
  onViewPlaylists: () => void;
  onViewArtists: () => void;
  onViewRecentlyPlayed: () => void;
  onViewMostPlayed: () => void;
  onAlbumViewportMeasure: (size: { width: number; height: number }) => void;
};

export function LibraryContent({
  view,
  songs,
  currentSong,
  stats,
  albums,
  artists,
  playlists,
  likedSongs,
  isLikedSongsLoading,
  recentlyPlayedSongs,
  mostPlayedSongs,
  likedSongIds,
  hasMoreArtists,
  isArtistsLoading,
  songPage,
  totalSongPages,
  hasPreviousSongPage,
  hasNextSongPage,
  isSongsLoading,
  albumPage,
  totalAlbumPages,
  hasPreviousAlbumPage,
  hasNextAlbumPage,
  isAlbumsLoading,
  recentlyPlayedPage,
  hasPreviousRecentlyPlayedPage,
  hasNextRecentlyPlayedPage,
  isRecentlyPlayedLoading,
  mostPlayedPage,
  hasPreviousMostPlayedPage,
  hasNextMostPlayedPage,
  isMostPlayedLoading,
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
  onPlaylistsChanged,
  refreshPlaylistSongsToken,
  onPlaySong,
  onAddToQueue,
  onAddToPlaylist,
  onToggleLike,
  onPreviousSongPage,
  onNextSongPage,
  onPreviousAlbumPage,
  onNextAlbumPage,
  onPreviousRecentlyPlayedPage,
  onNextRecentlyPlayedPage,
  onPreviousMostPlayedPage,
  onNextMostPlayedPage,
  onBackFromAlbumSongs,
  onBackToArtists,
  onPlayAlbum,
  onQueueAlbum,
  onPlaySelectedAlbum,
  onQueueSelectedAlbum,
  onViewSongs,
  onViewAlbums,
  onViewPlaylists,
  onViewArtists,
  onViewRecentlyPlayed,
  onViewMostPlayed,
  onAlbumViewportMeasure,
}: LibraryContentProps) {
  if (view === "home") {
    return (
      <HomeView
        currentSong={currentSong}
        songs={songs}
        albums={albums}
        playlists={playlists}
        stats={stats}
        recentlyPlayedSongs={recentlyPlayedSongs}
        mostPlayedSongs={mostPlayedSongs}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
        onViewSongs={onViewSongs}
        onViewAlbums={onViewAlbums}
        onViewPlaylists={onViewPlaylists}
        onViewArtists={onViewArtists}
        onViewRecentlyPlayed={onViewRecentlyPlayed}
        onViewMostPlayed={onViewMostPlayed}
      />
    );
  }

  if (view === "songs") {
    return (
      <SongsView
        songs={songs}
        page={songPage}
        totalPages={totalSongPages}
        scrollKey={`songs-${songPage}-${songs.length}`}
        hasPreviousPage={hasPreviousSongPage}
        hasNextPage={hasNextSongPage}
        isLoading={isSongsLoading}
        onPreviousPage={onPreviousSongPage}
        onNextPage={onNextSongPage}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
        playlists={playlists}
        onAddToPlaylist={onAddToPlaylist}
        likedSongIds={likedSongIds}
        onToggleLike={onToggleLike}
      />
    );
  }

  if (view === "likedSongs") {
    return (
      <SongsView
        songs={likedSongs}
        title="Liked Songs"
        emptyMessage="No liked songs yet."
        page={1}
        scrollKey={`liked-songs-${likedSongs.length}`}
        hasPreviousPage={false}
        hasNextPage={false}
        isLoading={isLikedSongsLoading}
        onPreviousPage={() => undefined}
        onNextPage={() => undefined}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
        playlists={playlists}
        onAddToPlaylist={onAddToPlaylist}
        likedSongIds={likedSongIds}
        onToggleLike={onToggleLike}
      />
    );
  }

  if (view === "recentlyPlayed") {
    return (
      <SongsView
        songs={recentlyPlayedSongs}
        title="Recently Played"
        emptyMessage="No recently played songs yet."
        page={recentlyPlayedPage}
        scrollKey={`recently-played-${recentlyPlayedPage}-${recentlyPlayedSongs.length}`}
        hasPreviousPage={hasPreviousRecentlyPlayedPage}
        hasNextPage={hasNextRecentlyPlayedPage}
        isLoading={isRecentlyPlayedLoading}
        onPreviousPage={onPreviousRecentlyPlayedPage}
        onNextPage={onNextRecentlyPlayedPage}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
        playlists={playlists}
        onAddToPlaylist={onAddToPlaylist}
        likedSongIds={likedSongIds}
        onToggleLike={onToggleLike}
      />
    );
  }

  if (view === "mostPlayed") {
    return (
      <SongsView
        songs={mostPlayedSongs.map((entry) => entry.song)}
        title="Most Played"
        emptyMessage="No most played songs yet."
        page={mostPlayedPage}
        scrollKey={`most-played-${mostPlayedPage}-${mostPlayedSongs.length}`}
        hasPreviousPage={hasPreviousMostPlayedPage}
        hasNextPage={hasNextMostPlayedPage}
        isLoading={isMostPlayedLoading}
        onPreviousPage={onPreviousMostPlayedPage}
        onNextPage={onNextMostPlayedPage}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
        playlists={playlists}
        onAddToPlaylist={onAddToPlaylist}
        likedSongIds={likedSongIds}
        onToggleLike={onToggleLike}
        songBadges={new Map(
          mostPlayedSongs.map((entry) => [
            entry.song.id,
            `${entry.playCount} ${entry.playCount === 1 ? "play" : "plays"}`,
          ]),
        )}
      />
    );
  }

  if (view === "albums") {
    return (
      <AlbumsView
        albums={albums}
        page={albumPage}
        totalPages={totalAlbumPages}
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
        playlists={playlists}
        onAddToPlaylist={onAddToPlaylist}
        likedSongIds={likedSongIds}
        onToggleLike={onToggleLike}
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

  if (view === "playlists") {
    return (
      <PlaylistsView
        onUnauthorized={onUnauthorized}
        onPlaylistsChanged={onPlaylistsChanged}
        refreshSongsToken={refreshPlaylistSongsToken}
        onPlaySong={onPlaySong}
        onAddToQueue={onAddToQueue}
        likedSongIds={likedSongIds}
        onToggleLike={onToggleLike}
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
        playlists={playlists}
        onAddToPlaylist={onAddToPlaylist}
        likedSongIds={likedSongIds}
        onToggleLike={onToggleLike}
      />
    );
  }

  return null;
}
