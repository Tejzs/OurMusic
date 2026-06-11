import type { Playlist, Song } from "../music-types";
import { SongCard } from "./song-card";

type SongsGridProps = {
  songs: Song[];
  playlists: Playlist[];
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  onAddToPlaylist: (song: Song, playlistId: number) => void;
  likedSongIds: Set<number>;
  onToggleLike: (song: Song) => void;
  songBadges?: Map<number, string>;
  showDownload?: boolean;
};

export function SongsGrid({
  songs,
  playlists,
  onPlaySong,
  onAddToQueue,
  onAddToPlaylist,
  likedSongIds,
  onToggleLike,
  songBadges,
  showDownload = true,
}: SongsGridProps) {
  return (
    <div
      className="grid w-full auto-rows-auto grid-cols-2 content-start gap-3 sm:gap-4 lg:grid-cols-[repeat(auto-fill,minmax(170px,170px))] lg:justify-start lg:gap-x-4 lg:gap-y-5"
    >
      {songs.map((song, index) => (
        <SongCard
          key={`${song.id}-${index}`}
          song={song}
          playlists={playlists}
          likedSongIds={likedSongIds}
          onPlaySong={onPlaySong}
          onAddToQueue={onAddToQueue}
          onAddToPlaylist={onAddToPlaylist}
          onToggleLike={onToggleLike}
          songBadges={songBadges}
          showDownload={showDownload}
          animationDelayMs={index * 20}
        />
      ))}
    </div>
  );
}
