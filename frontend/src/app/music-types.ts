export type View = "songs" | "albums" | "artists" | "adminUsers" | "albumSongs" | "artistSongs";

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

export type Song = {
  id: number;
  title: string;
  artist: string;
  album: string;
  duration: number;
};
