export type View =
  | "home"
  | "songs"
  | "albums"
  | "playlists"
  | "likedSongs"
  | "recentlyPlayed"
  | "mostPlayed"
  | "artists"
  | "adminUsers"
  | "albumSongs"
  | "artistSongs";

export type AdminUser = {
  id: number;
  username: string;
  isAdmin: boolean;
};

export type Artist = {
  id: number;
  name: string;
  albumCount: number;
  songCount: number;
};

export type Album = {
  id: number;
  title: string;
  artistId: number;
  artist: string;
  songCount: number;
  artworkSongId: number;
};

export type Playlist = {
  id: number;
  name: string;
  hasCover: boolean;
};

export type Song = {
  id: number;
  title: string;
  artist: string;
  album: string;
  genre?: string | null;
  duration: number;
  bitRate?: number | null;
  samplingRate?: number | null;
  channelCount?: number | null;
  bitDepth?: number | null;
  year?: number | null;
  track?: number | null;
  discNumber?: number | null;
};

export type SongLyrics = {
  id: number;
  name: string;
  trackName: string;
  artistName: string;
  albumName: string;
  duration: number;
  instrumental: boolean;
  plainLyrics: string | null;
  syncedLyrics?: string | null;
  lyricsfile: string | null;
};

export type MostPlayedSong = {
  song: Song;
  playCount: number;
};

export type LibraryStats = {
  totalSongs: number;
  totalAlbums: number;
  totalArtists: number;
  totalPlaylists: number;
};
