"use client";

import {
  Disc3,
  Heart,
  History,
  House,
  ListMusic,
  Music2,
  TrendingUp,
  UsersRound,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { LibraryContent } from "./components/library-content";
import { LibraryHeader } from "./components/library-header";
import { LibrarySidebar } from "./components/library-sidebar";
import { PlayerBar } from "./components/player-bar";
import { QueueSidebar } from "./components/queue-sidebar";
import type { Album, Artist, LibraryStats, MostPlayedSong, Playlist, Song, SongLyrics, View } from "./music-types";
import { apiUrl, clearFrontendSessionCookie, fetchAuthSession, logoutUser } from "./lib/auth";

type RepeatMode = "off" | "one" | "queue";

type StoredPlayerState = {
  version: 1;
  currentSong: Song | null;
  queue: Song[];
  playbackPosition: number;
  shuffleEnabled: boolean;
  repeatMode: RepeatMode;
};

type PlaySongOptions = {
  addCurrentToHistory?: boolean;
  clearForwardStack?: boolean;
};

const PLAYER_STATE_STORAGE_KEY = "ourmusic_player_state";

async function getArtistAlbums(artistId: number): Promise<Album[]> {
  const resp = await fetch(apiUrl(`/api/artists/${artistId}/albums`));
  return resp.json();
}

type GetArtistsParams = {
  limit: number;
  offset: number;
  signal?: AbortSignal;
};

async function getArtistsPaged({ limit, offset, signal }: GetArtistsParams): Promise<Artist[]> {
  const url = new URL(apiUrl("/api/artists"), window.location.origin);
  url.searchParams.set("limit", String(limit));
  url.searchParams.set("offset", String(offset));

  const resp = await fetch(url, { signal });
  return resp.json();
}

async function getArtistSongs(artistId: number): Promise<Song[]> {
  const resp = await fetch(apiUrl(`/api/artists/${artistId}/songs`));
  return resp.json();
}

async function getAlbumSongs(albumId: number): Promise<Song[]> {
  const resp = await fetch(apiUrl(`/api/albums/${albumId}/songs`));
  return resp.json();
}

async function getPlaylists(): Promise<Playlist[]> {
  const resp = await fetch(apiUrl("/api/playlists"), {
    credentials: "include",
  });
  return resp.json();
}

async function addSongToPlaylist(playlistId: number, songId: number) {
  const resp = await fetch(apiUrl(`/api/playlists/${playlistId}/songs`), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({ songId }),
  });

  const data = await resp.json().catch(() => ({}));
  return { response: resp, data };
}

async function getLikedSongs(): Promise<Song[]> {
  const resp = await fetch(apiUrl("/api/me/liked-songs"), {
    credentials: "include",
  });

  if (!resp.ok) {
    throw new Error("Unable to load liked songs.");
  }

  const data = await resp.json();
  return Array.isArray(data) ? data : [];
}

async function likeSong(songId: number) {
  const response = await fetch(apiUrl(`/api/me/liked-songs/${songId}`), {
    method: "POST",
    credentials: "include",
  });
  return response;
}

async function unlikeSong(songId: number) {
  const response = await fetch(apiUrl(`/api/me/liked-songs/${songId}`), {
    method: "DELETE",
    credentials: "include",
  });
  return response;
}

async function getRecentlyPlayed(offset: number, limit: number): Promise<Song[]> {
  const url = new URL(apiUrl("/api/me/recently-played"), window.location.origin);
  url.searchParams.set("offset", String(offset));
  url.searchParams.set("limit", String(limit));

  const resp = await fetch(url, {
    credentials: "include",
  });

  if (!resp.ok) {
    throw new Error("Unable to load recently played songs.");
  }

  const data = await resp.json();
  return Array.isArray(data) ? data : [];
}

async function recordRecentlyPlayed(songId: number) {
  const response = await fetch(apiUrl(`/api/me/recently-played/${songId}`), {
    method: "POST",
    credentials: "include",
  });
  return response;
}

async function getSongLyrics(songId: number, signal?: AbortSignal): Promise<SongLyrics> {
  const response = await fetch(apiUrl(`/api/songs/${songId}/lyrics`), {
    credentials: "include",
    signal,
  });

  if (!response.ok) {
    throw new Error("Unable to load lyrics.");
  }

  const data = await response.json();
  return typeof data === "string" ? JSON.parse(data) : data;
}

type MostPlayedSongResponse = {
  song: Partial<Song> & Pick<Song, "id" | "title" | "artist">;
  playCount: number;
};

type LibraryStatsResponse = Partial<Record<
  | "totalSongs"
  | "totalAlbums"
  | "totalArtists"
  | "totalPlaylists"
  | "songs"
  | "songs:"
  | "albums"
  | "albums:"
  | "artists"
  | "artists:"
  | "playlists"
  | "playlists:"
  | "songCount"
  | "albumCount"
  | "artistCount"
  | "playlistCount"
  | "total_songs"
  | "total_albums"
  | "total_artists"
  | "total_playlists",
  unknown
>>;

function readStatNumber(data: LibraryStatsResponse, keys: Array<keyof LibraryStatsResponse>) {
  for (const key of keys) {
    const value = data[key];

    if (typeof value === "number" && Number.isFinite(value)) {
      return value;
    }

    if (typeof value === "string") {
      const parsedValue = Number(value);
      if (Number.isFinite(parsedValue)) {
        return parsedValue;
      }
    }
  }

  return 0;
}

function normalizeSong(song: Partial<Song> & Pick<Song, "id" | "title" | "artist">): Song {
  return {
    id: song.id,
    title: song.title,
    artist: song.artist,
    album: song.album ?? "",
    duration: song.duration ?? 0,
  };
}

async function getMostPlayedSongs(offset: number, limit: number): Promise<MostPlayedSong[]> {
  const url = new URL(apiUrl("/api/me/most-played-songs"), window.location.origin);
  url.searchParams.set("offset", String(offset));
  url.searchParams.set("limit", String(limit));

  const resp = await fetch(url, {
    credentials: "include",
  });

  if (!resp.ok) {
    throw new Error("Unable to load most played songs.");
  }

  const data = await resp.json();
  const entries = Array.isArray(data) ? (data as MostPlayedSongResponse[]) : [];

  return entries.map((entry) => ({
    song: normalizeSong(entry.song),
    playCount: entry.playCount,
  }));
}

async function getLibraryStats(): Promise<LibraryStats> {
  const resp = await fetch(apiUrl("/api/stats/"), {
    credentials: "include",
  });
  const data = (await resp.json()) as LibraryStatsResponse;

  return {
    totalSongs: readStatNumber(data, ["totalSongs", "songs", "songs:", "songCount", "total_songs"]),
    totalAlbums: readStatNumber(data, ["totalAlbums", "albums", "albums:", "albumCount", "total_albums"]),
    totalArtists: readStatNumber(data, ["totalArtists", "artists", "artists:", "artistCount", "total_artists"]),
    totalPlaylists: readStatNumber(data, ["totalPlaylists", "playlists", "playlists:", "playlistCount", "total_playlists"]),
  };
}

type GetAlbumsParams = {
  limit: number;
  offset: number;
  signal?: AbortSignal;
};

async function getAlbums({ limit, offset, signal }: GetAlbumsParams): Promise<Album[]> {
  const url = new URL(apiUrl("/api/albums"), window.location.origin);
  url.searchParams.set("limit", String(limit));
  url.searchParams.set("offset", String(offset));

  const resp = await fetch(url, { signal });
  return resp.json();
}

type GetSongsParams = {
  pattern?: string;
  limit: number;
  offset: number;
  signal?: AbortSignal;
};

async function getSongs({ pattern, limit, offset, signal }: GetSongsParams) {
  const url = new URL(apiUrl("/api/songs"), window.location.origin);

  if (pattern && pattern.trim() !== "") {
    url.searchParams.set("search", pattern.trim());
  }

  url.searchParams.set("limit", String(limit));
  url.searchParams.set("offset", String(offset));

  const resp = await fetch(url, { signal });
  return resp.json();
}

async function fullScan() {
  const resp = await fetch(apiUrl("/api/library/scan/full"), {
    method: "POST",
  });
  return resp.json();
}

const ARTIST_PAGE_SIZE = 50;
const SONG_PAGE_SIZE = 50;
const GRID_CARD_MIN_SIZE = 170;
const GRID_CARD_GAP = 14;

function isRepeatMode(value: unknown): value is RepeatMode {
  return value === "off" || value === "one" || value === "queue";
}

function isSong(value: unknown): value is Song {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const song = value as Partial<Song>;
  return (
    typeof song.id === "number" &&
    typeof song.title === "string" &&
    typeof song.artist === "string" &&
    typeof song.album === "string" &&
    typeof song.duration === "number"
  );
}

function readStoredPlayerState(): StoredPlayerState | null {
  try {
    const rawState = window.localStorage.getItem(PLAYER_STATE_STORAGE_KEY);
    if (!rawState) {
      return null;
    }

    const parsed = JSON.parse(rawState) as Partial<StoredPlayerState>;
    if (parsed.version !== 1) {
      return null;
    }

    const currentSong = parsed.currentSong === null || isSong(parsed.currentSong)
      ? parsed.currentSong
      : null;
    const queue = Array.isArray(parsed.queue) ? parsed.queue.filter(isSong) : [];
    const playbackPosition =
      typeof parsed.playbackPosition === "number" && Number.isFinite(parsed.playbackPosition)
        ? Math.max(0, parsed.playbackPosition)
        : 0;

    return {
      version: 1,
      currentSong,
      queue,
      playbackPosition,
      shuffleEnabled: Boolean(parsed.shuffleEnabled),
      repeatMode: isRepeatMode(parsed.repeatMode) ? parsed.repeatMode : "off",
    };
  } catch {
    return null;
  }
}

function calculateAutoPageSize(width: number, height: number, minCardSize: number, gap: number) {
  if (width <= 0 || height <= 0) {
    return 4;
  }

  let bestCount = 4;
  let bestCardSize = 0;
  const maxColumns = Math.max(1, Math.floor((width + gap) / (minCardSize + gap)));
  const maxRows = Math.max(1, Math.floor((height + gap) / (minCardSize + gap)));

  for (let columns = 1; columns <= maxColumns; columns += 1) {
    const widthLimitedSize = Math.floor((width - gap * (columns - 1)) / columns);

    if (widthLimitedSize < minCardSize) {
      continue;
    }

    for (let rows = 1; rows <= maxRows; rows += 1) {
      const heightLimitedSize = Math.floor((height - gap * (rows - 1)) / rows);
      const cardSize = Math.min(widthLimitedSize, heightLimitedSize);

      if (cardSize < minCardSize) {
        continue;
      }

      const count = rows * columns;

      if (count > bestCount || (count === bestCount && cardSize > bestCardSize)) {
        bestCount = count;
        bestCardSize = cardSize;
      }
    }
  }

  return Math.min(bestCount, 50);
}

export default function Home() {
  const router = useRouter();
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const contentRef = useRef<HTMLDivElement | null>(null);
  const playerBarRef = useRef<HTMLDivElement | null>(null);
  const volumeControlRef = useRef<HTMLDivElement | null>(null);
  const mobileDrawerOverlayRef = useRef<HTMLDivElement | null>(null);
  const mobileDrawerPanelRef = useRef<HTMLDivElement | null>(null);
  const previousVolumeRef = useRef(0.9);
  const hasRestoredPlayerStateRef = useRef(false);
  const restoredPlaybackPositionRef = useRef(0);
  const progressAnimationFrameRef = useRef<number | null>(null);
  const playbackRetryRef = useRef(0);
  const currentSongRef = useRef<Song | null>(null);
  const queueRef = useRef<Song[]>([]);
  const playHistoryRef = useRef<Song[]>([]);
  const playForwardRef = useRef<Song[]>([]);
  const lyricsCacheRef = useRef<Map<number, SongLyrics>>(new Map());
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [authReady, setAuthReady] = useState(false);
  const [authUser, setAuthUser] = useState<string | null>(null);
  const [authIsAdmin, setAuthIsAdmin] = useState(false);

  const [artists, setArtists] = useState<Artist[]>([]);
  const [artistPage, setArtistPage] = useState(1);
  const [hasNextArtistPage, setHasNextArtistPage] = useState(false);
  const [isArtistsLoading, setIsArtistsLoading] = useState(false);
  const [artistSongs, setArtistSongs] = useState<Song[]>([]);
  const [artistAlbums, setArtistAlbums] = useState<Album[]>([]);
  const [selectedArtist, setSelectedArtist] = useState<Artist | null>(null);
  const [libraryStats, setLibraryStats] = useState<LibraryStats | null>(null);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [likedSongs, setLikedSongs] = useState<Song[]>([]);
  const [likedSongIds, setLikedSongIds] = useState<Set<number>>(() => new Set());
  const [isLikedSongsLoading, setIsLikedSongsLoading] = useState(false);
  const [recentlyPlayedSongs, setRecentlyPlayedSongs] = useState<Song[]>([]);
  const [recentlyPlayedPage, setRecentlyPlayedPage] = useState(1);
  const [hasNextRecentlyPlayedPage, setHasNextRecentlyPlayedPage] = useState(false);
  const [isRecentlyPlayedLoading, setIsRecentlyPlayedLoading] = useState(false);
  const [mostPlayedSongs, setMostPlayedSongs] = useState<MostPlayedSong[]>([]);
  const [mostPlayedPage, setMostPlayedPage] = useState(1);
  const [hasNextMostPlayedPage, setHasNextMostPlayedPage] = useState(false);
  const [isMostPlayedLoading, setIsMostPlayedLoading] = useState(false);

  const [albumSongs, setAlbumSongs] = useState<Song[]>([]);
  const [selectedAlbum, setSelectedAlbum] = useState<Album | null>(null);
  const [albumBackView, setAlbumBackView] = useState<View>("albums");

  const [view, setView] = useState<View>("home");
  const [albums, setAlbums] = useState<Album[]>([]);
  const [albumPage, setAlbumPage] = useState(1);
  const [hasNextAlbumPage, setHasNextAlbumPage] = useState(false);
  const [isAlbumsLoading, setIsAlbumsLoading] = useState(false);
  const [songs, setSongs] = useState<Song[]>([]);
  const [pattern, setPattern] = useState("");
  const [songPage, setSongPage] = useState(1);
  const [hasNextSongPage, setHasNextSongPage] = useState(false);
  const [isSongsLoading, setIsSongsLoading] = useState(false);
  const [albumGridBox, setAlbumGridBox] = useState({ width: 0, height: 0 });
  const [currentSong, setCurrentSong] = useState<Song | null>(null);
  const [queue, setQueue] = useState<Song[]>([]);
  const [playNonce, setPlayNonce] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.9);
  const [isMuted, setIsMuted] = useState(false);
  const [isVolumeHovered, setIsVolumeHovered] = useState(false);
  const [shuffleEnabled, setShuffleEnabled] = useState(false);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>("off");
  const [isLyricsOpen, setIsLyricsOpen] = useState(false);
  const [lyrics, setLyrics] = useState<SongLyrics | null>(null);
  const [lyricsSongId, setLyricsSongId] = useState<number | null>(null);
  const [isLyricsLoading, setIsLyricsLoading] = useState(false);
  const [lyricsError, setLyricsError] = useState<string | null>(null);

  const albumLimit = useMemo(
    () => calculateAutoPageSize(albumGridBox.width, albumGridBox.height, GRID_CARD_MIN_SIZE, GRID_CARD_GAP),
    [albumGridBox.height, albumGridBox.width],
  );

  function setCurrentSongState(song: Song | null) {
    currentSongRef.current = song;
    setCurrentSong(song);
  }

  function setQueueState(nextQueue: Song[] | ((currentQueue: Song[]) => Song[])) {
    const resolvedQueue = typeof nextQueue === "function" ? nextQueue(queueRef.current) : nextQueue;
    queueRef.current = resolvedQueue;
    setQueue(resolvedQueue);
  }

  useEffect(() => {
    const controller = new AbortController();

    void fetchAuthSession(controller.signal).then((session) => {
      if (!session) {
        clearFrontendSessionCookie();
        router.replace("/login");
        return;
      }

      setAuthUser(session.username);
      setAuthIsAdmin(session.isAdmin);
      setAuthReady(true);
    });

    return () => controller.abort("home-auth-check-unmounted");
  }, [router]);

  const songLimit = SONG_PAGE_SIZE;
  const totalSongPages = useMemo(() => {
    if (!libraryStats || songLimit <= 0) {
      return undefined;
    }

    return Math.max(1, Math.ceil(libraryStats.totalSongs / songLimit));
  }, [libraryStats, songLimit]);
  const totalAlbumPages = useMemo(() => {
    if (!libraryStats || albumLimit <= 0) {
      return undefined;
    }

    return Math.max(1, Math.ceil(libraryStats.totalAlbums / albumLimit));
  }, [albumLimit, libraryStats]);

  const resetSongsFeed = useCallback((nextSongs: Song[]) => {
    setSongs(nextSongs);
    setIsSongsLoading(false);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }, []);

  const applySongsPage = useCallback((nextSongs: Song[]) => {
    setHasNextSongPage(nextSongs.length > songLimit);
    resetSongsFeed(nextSongs.slice(0, songLimit));
  }, [resetSongsFeed, songLimit]);

  const resetAlbumsFeed = useCallback((nextAlbums: Album[]) => {
    setAlbums(nextAlbums);
    setIsAlbumsLoading(false);
  }, []);

  const applyAlbumsPage = useCallback((nextAlbums: Album[]) => {
    setHasNextAlbumPage(nextAlbums.length > albumLimit);
    resetAlbumsFeed(nextAlbums.slice(0, albumLimit));
  }, [albumLimit, resetAlbumsFeed]);

  const refreshLibraryStats = useCallback(async () => {
    if (!authReady) {
      return;
    }

    try {
      const statsData = await getLibraryStats();
      setLibraryStats(statsData);
    } catch {
      setLibraryStats(null);
    }
  }, [authReady]);

  const refreshPlaylists = useCallback(async () => {
    if (!authReady) {
      return;
    }

    try {
      const playlistData = await getPlaylists();
      setPlaylists(playlistData);
      void refreshLibraryStats();
    } catch {
      setPlaylists([]);
    }
  }, [authReady, refreshLibraryStats]);

  const refreshLikedSongs = useCallback(async () => {
    if (!authReady) {
      return;
    }

    setIsLikedSongsLoading(true);

    try {
      const likedSongsData = await getLikedSongs();
      setLikedSongs(likedSongsData);
      setLikedSongIds(new Set(likedSongsData.map((song) => song.id)));
    } catch {
      setLikedSongs([]);
      setLikedSongIds(new Set());
    } finally {
      setIsLikedSongsLoading(false);
    }
  }, [authReady]);

  const refreshSongsPage = useCallback(async (page = songPage, signal?: AbortSignal) => {
    if (!authReady) {
      return;
    }

    const query = pattern.trim();
    setIsSongsLoading(true);

    try {
      const songsData = await getSongs({
        pattern: query === "" ? undefined : query,
        limit: songLimit + 1,
        offset: (page - 1) * songLimit,
        signal,
      });
      if (!signal?.aborted) {
        applySongsPage(songsData);
      }
    } catch {
      if (!signal?.aborted) {
        setIsSongsLoading(false);
      }
    }
  }, [applySongsPage, authReady, pattern, songLimit, songPage]);

  const refreshRecentlyPlayed = useCallback(async (page = recentlyPlayedPage) => {
    if (!authReady) {
      return;
    }

    setIsRecentlyPlayedLoading(true);

    try {
      const recentlyPlayedData = await getRecentlyPlayed((page - 1) * songLimit, songLimit + 1);
      setRecentlyPlayedSongs(recentlyPlayedData.slice(0, songLimit));
      setHasNextRecentlyPlayedPage(recentlyPlayedData.length > songLimit);
    } catch {
      setRecentlyPlayedSongs([]);
      setHasNextRecentlyPlayedPage(false);
    } finally {
      setIsRecentlyPlayedLoading(false);
    }
  }, [authReady, recentlyPlayedPage, songLimit]);

  const refreshMostPlayedSongs = useCallback(async (page = mostPlayedPage) => {
    if (!authReady) {
      return;
    }

    setIsMostPlayedLoading(true);

    try {
      const mostPlayedData = await getMostPlayedSongs((page - 1) * songLimit, songLimit + 1);
      setMostPlayedSongs(mostPlayedData.slice(0, songLimit));
      setHasNextMostPlayedPage(mostPlayedData.length > songLimit);
    } catch {
      setMostPlayedSongs([]);
      setHasNextMostPlayedPage(false);
    } finally {
      setIsMostPlayedLoading(false);
    }
  }, [authReady, mostPlayedPage, songLimit]);

  const libraryItems = useMemo(
    () => [
      { id: "home" as const, label: "Home", icon: House },
      { id: "songs" as const, label: "Songs", icon: Music2 },
      { id: "likedSongs" as const, label: "Liked Songs", icon: Heart },
      { id: "recentlyPlayed" as const, label: "Recently Played", icon: History },
      { id: "mostPlayed" as const, label: "Most Played", icon: TrendingUp },
      { id: "albums" as const, label: "Albums", icon: Disc3 },
      { id: "playlists" as const, label: "Playlists", icon: ListMusic },
      { id: "artists" as const, label: "Artists", icon: UsersRound },
      ...(authIsAdmin ? [{ id: "adminUsers" as const, label: "Users", icon: UsersRound }] : []),
    ],
    [authIsAdmin],
  );

  useEffect(() => {
    if (!authReady || hasRestoredPlayerStateRef.current) {
      return;
    }

    hasRestoredPlayerStateRef.current = true;
    const storedState = readStoredPlayerState();
    if (!storedState) {
      return;
    }

    const timeout = window.setTimeout(() => {
      setCurrentSongState(storedState.currentSong);
      setQueueState(storedState.queue);
      setProgress(storedState.playbackPosition);
      setDuration(storedState.currentSong?.duration ?? 0);
      setShuffleEnabled(storedState.shuffleEnabled);
      setRepeatMode(storedState.repeatMode);
      setIsPlaying(false);
      restoredPlaybackPositionRef.current = storedState.playbackPosition;
    }, 0);

    return () => window.clearTimeout(timeout);
  }, [authReady]);

  useEffect(() => {
    if (!authReady || !hasRestoredPlayerStateRef.current) {
      return;
    }

    const storedState: StoredPlayerState = {
      version: 1,
      currentSong,
      queue,
      playbackPosition: currentSong === null ? 0 : progress,
      shuffleEnabled,
      repeatMode,
    };

    window.localStorage.setItem(PLAYER_STATE_STORAGE_KEY, JSON.stringify(storedState));
  }, [authReady, currentSong, progress, queue, repeatMode, shuffleEnabled]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    async function loadInitialAlbums() {
      try {
        const albumsData = await getAlbums({ limit: albumLimit + 1, offset: 0 });
        applyAlbumsPage(albumsData);
      } catch {
        setAlbums([]);
        setHasNextAlbumPage(false);
        setIsAlbumsLoading(false);
      }
    }

    void loadInitialAlbums();
  }, [albumLimit, applyAlbumsPage, authReady]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const timeout = window.setTimeout(() => {
      void refreshLibraryStats();
    }, 0);

    return () => window.clearTimeout(timeout);
  }, [authReady, refreshLibraryStats]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const timeout = window.setTimeout(() => {
      void refreshPlaylists();
    }, 0);

    return () => window.clearTimeout(timeout);
  }, [authReady, refreshPlaylists]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const timeout = window.setTimeout(() => {
      void refreshLikedSongs();
    }, 0);

    return () => window.clearTimeout(timeout);
  }, [authReady, refreshLikedSongs]);

  useEffect(() => {
    if (!isLyricsOpen) {
      return;
    }

    if (currentSong === null) {
      return;
    }

    const cachedLyrics = lyricsCacheRef.current.get(currentSong.id);
    if (cachedLyrics) {
      const timeout = window.setTimeout(() => {
        setLyrics(cachedLyrics);
        setLyricsSongId(currentSong.id);
        setLyricsError(null);
        setIsLyricsLoading(false);
      }, 0);

      return () => window.clearTimeout(timeout);
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      setLyrics(null);
      setLyricsSongId(currentSong.id);
      setLyricsError(null);
      setIsLyricsLoading(true);
    }, 0);

    void getSongLyrics(currentSong.id, controller.signal)
      .then((nextLyrics) => {
        if (controller.signal.aborted) {
          return;
        }

        lyricsCacheRef.current.set(currentSong.id, nextLyrics);
        setLyrics(nextLyrics);
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setLyricsError("Lyrics are unavailable for this song.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLyricsLoading(false);
        }
      });

    return () => {
      window.clearTimeout(timeout);
      controller.abort("lyrics-request-changed");
    };
  }, [currentSong, isLyricsOpen]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const timeout = window.setTimeout(() => {
      void refreshRecentlyPlayed(recentlyPlayedPage);
    }, 0);

    return () => window.clearTimeout(timeout);
  }, [authReady, recentlyPlayedPage, refreshRecentlyPlayed]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const timeout = window.setTimeout(() => {
      void refreshMostPlayedSongs(mostPlayedPage);
    }, 0);

    return () => window.clearTimeout(timeout);
  }, [authReady, mostPlayedPage, refreshMostPlayedSongs]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      setIsArtistsLoading(true);
      void getArtistsPaged({
        limit: ARTIST_PAGE_SIZE + 1,
        offset: (artistPage - 1) * ARTIST_PAGE_SIZE,
        signal: controller.signal,
      })
        .then((artistsData) => {
          if (!controller.signal.aborted) {
            setHasNextArtistPage(artistsData.length > ARTIST_PAGE_SIZE);
            const nextBatch = artistsData.slice(0, ARTIST_PAGE_SIZE);

            setArtists((currentArtists) => {
              if (artistPage === 1) {
                return nextBatch;
              }

              const seenIds = new Set(currentArtists.map((artist) => artist.id));
              const merged = [
                ...currentArtists,
                ...nextBatch.filter((artist) => !seenIds.has(artist.id)),
              ];

              return merged;
            });
            setIsArtistsLoading(false);
          }
        })
        .catch(() => {
          if (!controller.signal.aborted) {
            setIsArtistsLoading(false);
          }
        });
    }, 250);

    return () => {
      controller.abort("artists-page-unmounted");
      window.clearTimeout(timeout);
    };
  }, [authReady, artistPage]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      void refreshSongsPage(songPage, controller.signal);
    }, 250);

    return () => {
      controller.abort("songs-page-unmounted");
      window.clearTimeout(timeout);
    };
  }, [authReady, refreshSongsPage, songPage]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    if (view !== "albums") {
      return;
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      setIsAlbumsLoading(true);
      void getAlbums({
        limit: albumLimit + 1,
        offset: (albumPage - 1) * albumLimit,
        signal: controller.signal,
      })
        .then((albumsData) => {
          if (!controller.signal.aborted) {
            applyAlbumsPage(albumsData);
          }
        })
        .catch(() => {
          if (!controller.signal.aborted) {
            setIsAlbumsLoading(false);
          }
        });
    }, 250);

    return () => {
      controller.abort("albums-page-unmounted");
      window.clearTimeout(timeout);
    };
  }, [albumLimit, albumPage, applyAlbumsPage, authReady, view]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    audio.volume = volume;
  }, [authReady, volume]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    audio.muted = isMuted;
  }, [authReady, isMuted]);

  useEffect(() => {
    if (!authReady) {
      return;
    }

    const audio = audioRef.current;
    if (!audio || currentSong === null) {
      return;
    }

    audio.load();

    if (playNonce === 0) {
      return;
    }

    const play = async () => {
      try {
        await audio.play();
        setIsPlaying(true);
      } catch {
        setIsPlaying(false);
      }
    };

    play();
  }, [authReady, currentSong, playNonce]);

  useEffect(() => {
    if (!authReady || !isPlaying || currentSong === null) {
      if (progressAnimationFrameRef.current !== null) {
        window.cancelAnimationFrame(progressAnimationFrameRef.current);
        progressAnimationFrameRef.current = null;
      }
      return;
    }

    const updateProgress = () => {
      const audio = audioRef.current;

      if (!audio || audio.paused || audio.ended) {
        progressAnimationFrameRef.current = null;
        return;
      }

      setProgress(audio.currentTime);
      progressAnimationFrameRef.current = window.requestAnimationFrame(updateProgress);
    };

    progressAnimationFrameRef.current = window.requestAnimationFrame(updateProgress);

    return () => {
      if (progressAnimationFrameRef.current !== null) {
        window.cancelAnimationFrame(progressAnimationFrameRef.current);
        progressAnimationFrameRef.current = null;
      }
    };
  }, [authReady, currentSong, isPlaying]);

  async function handleOpenAlbum(album: Album) {
    setAlbumBackView("albums");
    setSelectedAlbum(album);
    setAlbumSongs([]);
    setView("albumSongs");

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }

    try {
      const data = await getAlbumSongs(album.id);
      setAlbumSongs(data);
    } catch {
      setAlbumSongs([]);
    }
  }

  async function handleOpenArtistAlbum(album: Album) {
    setAlbumBackView("artistSongs");
    setSelectedAlbum(album);
    setAlbumSongs([]);
    setView("albumSongs");

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }

    try {
      const data = await getAlbumSongs(album.id);
      setAlbumSongs(data);
    } catch {
      setAlbumSongs([]);
    }
  }

  async function handleViewAlbums() {
    setView("albums");
    setSelectedArtist(null);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewHome() {
    setView("home");
    setSelectedAlbum(null);
    setSelectedArtist(null);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewSongs() {
    setView("songs");
    setSelectedArtist(null);
    setSongPage(1);
    if (songPage === 1) {
      void refreshSongsPage(1);
    }

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewPlaylists() {
    setView("playlists");
    setSelectedAlbum(null);
    setSelectedArtist(null);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewLikedSongs() {
    setView("likedSongs");
    setSelectedAlbum(null);
    setSelectedArtist(null);
    void refreshLikedSongs();

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewRecentlyPlayed() {
    setView("recentlyPlayed");
    setSelectedAlbum(null);
    setSelectedArtist(null);
    setRecentlyPlayedPage(1);
    if (recentlyPlayedPage === 1) {
      void refreshRecentlyPlayed(1);
    }

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewMostPlayed() {
    setView("mostPlayed");
    setSelectedAlbum(null);
    setSelectedArtist(null);
    setMostPlayedPage(1);
    if (mostPlayedPage === 1) {
      void refreshMostPlayedSongs(1);
    }

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handlePreviousRecentlyPlayedPage() {
    setRecentlyPlayedPage((currentPage) => Math.max(1, currentPage - 1));
  }

  function handleNextRecentlyPlayedPage() {
    if (!hasNextRecentlyPlayedPage || isRecentlyPlayedLoading) {
      return;
    }

    setRecentlyPlayedPage((currentPage) => currentPage + 1);
  }

  function handlePreviousMostPlayedPage() {
    setMostPlayedPage((currentPage) => Math.max(1, currentPage - 1));
  }

  function handleNextMostPlayedPage() {
    if (!hasNextMostPlayedPage || isMostPlayedLoading) {
      return;
    }

    setMostPlayedPage((currentPage) => currentPage + 1);
  }

  function handlePreviousAlbumPage() {
    setAlbumPage((currentPage) => Math.max(1, currentPage - 1));
  }

  function handleNextAlbumPage() {
    if (!hasNextAlbumPage || isAlbumsLoading) {
      return;
    }

    setAlbumPage((currentPage) => currentPage + 1);
  }

  function handlePreviousSongPage() {
    setSongPage((currentPage) => Math.max(1, currentPage - 1));
  }

  function handleNextSongPage() {
    if (!hasNextSongPage || isSongsLoading) {
      return;
    }

    setSongPage((currentPage) => currentPage + 1);
  }

  async function handleViewArtists() {
    setView("artists");
    setSelectedAlbum(null);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewAdminUsers() {
    setView("adminUsers");
    setSelectedAlbum(null);
    setSelectedArtist(null);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleLoadMoreArtists() {
    if (!hasNextArtistPage || isArtistsLoading) {
      return;
    }

    setArtistPage((currentPage) => currentPage + 1);
  }

  async function handleOpenArtist(artist: Artist) {
    setSelectedArtist(artist);
    setArtistSongs([]);
    setArtistAlbums([]);
    setView("artistSongs");

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }

    const [songsResult, albumsResult] = await Promise.allSettled([
      getArtistSongs(artist.id),
      getArtistAlbums(artist.id),
    ]);

    setArtistSongs(songsResult.status === "fulfilled" ? songsResult.value : []);
    setArtistAlbums(albumsResult.status === "fulfilled" ? albumsResult.value : []);
  }

  async function handleFullScan() {
    try {
      await fullScan();
    } catch {
      return;
    }

    setSongPage(1);
    setAlbumPage(1);
    setArtistPage(1);
    const [songsResult, albumsResult, artistsResult] = await Promise.allSettled([
      getSongs({ limit: songLimit + 1, offset: 0 }),
      getAlbums({ limit: albumLimit + 1, offset: 0 }),
      getArtistsPaged({ limit: ARTIST_PAGE_SIZE + 1, offset: 0 }),
    ]);

    if (songsResult.status === "fulfilled") {
      applySongsPage(songsResult.value);
    }

    if (albumsResult.status === "fulfilled") {
      applyAlbumsPage(albumsResult.value);
    }

    if (artistsResult.status === "fulfilled") {
      setHasNextArtistPage(artistsResult.value.length > ARTIST_PAGE_SIZE);
      setArtists(artistsResult.value.slice(0, ARTIST_PAGE_SIZE));
    }

    void refreshLibraryStats();
  }

  function playSong(song: Song, { addCurrentToHistory = true, clearForwardStack = true }: PlaySongOptions = {}) {
    playbackRetryRef.current = 0;
    if (clearForwardStack) {
      playForwardRef.current = [];
    }

    const previousCurrentSong = currentSongRef.current;
    if (addCurrentToHistory && previousCurrentSong !== null && previousCurrentSong.id !== song.id) {
      const nextHistory = [...playHistoryRef.current, previousCurrentSong];
      playHistoryRef.current = nextHistory;
    }

    setCurrentSongState(song);
    setPlayNonce((n) => n + 1);
    setIsPlaying(true);
    setProgress(0);
    setDuration(0);
    void recordRecentlyPlayed(song.id)
      .then((response) => {
        if (response.status === 401 || response.status === 403) {
          handleSignOut();
          return;
        }

        if (recentlyPlayedPage === 1) {
          setRecentlyPlayedSongs((currentSongs) => {
            const nextSongs = [
              song,
              ...currentSongs,
            ];

            return nextSongs.slice(0, songLimit);
          });
        }
      })
      .catch(() => null);
  }

  function addToQueue(song: Song) {
    setQueueState((currentQueue) => [...currentQueue, song]);
  }

  function removeFromQueue(index: number) {
    setQueueState((currentQueue) => currentQueue.filter((_, queueIndex) => queueIndex !== index));
  }

  function clearQueue() {
    setQueueState([]);
    setRepeatMode("off");
    setShuffleEnabled(false);
  }

  function handleCycleRepeatMode() {
    setRepeatMode((current) => (current === "off" ? "one" : current === "one" ? "queue" : "off"));
  }

  function shuffleQueue() {
    setShuffleEnabled(true);
    setQueueState((currentQueue) => {
      if (currentQueue.length < 2) {
        return currentQueue;
      }

      const shuffled = [...currentQueue];

      for (let i = shuffled.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
      }

      return shuffled;
    });
  }

  async function playAlbumFromCard(album: Album) {
    const tracks = await getAlbumSongs(album.id);

    if (tracks.length === 0) {
      return;
    }

    const [firstTrack, ...rest] = tracks;
    setQueueState(rest);
    setShuffleEnabled(false);
    playSong(firstTrack);
  }

  async function queueAlbumFromCard(album: Album) {
    const tracks = await getAlbumSongs(album.id);

    if (tracks.length === 0) {
      return;
    }

    setQueueState((currentQueue) => [...currentQueue, ...tracks]);
  }

  function playAlbum() {
    if (albumSongs.length === 0) {
      return;
    }

    const [firstSong, ...remainingSongs] = albumSongs;
    setQueueState(remainingSongs);
    setShuffleEnabled(false);
    playSong(firstSong);
  }

  function queueAlbum() {
    if (albumSongs.length === 0) {
      return;
    }

    setQueueState((currentQueue) => [...currentQueue, ...albumSongs]);
  }

  function playNext() {
    const forwardSong = playForwardRef.current[0];

    if (forwardSong) {
      const nextForward = playForwardRef.current.slice(1);
      playForwardRef.current = nextForward;
      setQueueState((currentQueue) => [
        ...nextForward,
        ...currentQueue.filter(
          (queuedSong) =>
            queuedSong.id !== forwardSong.id &&
            !nextForward.some((forwardQueueSong) => forwardQueueSong.id === queuedSong.id),
        ),
      ]);
      playSong(forwardSong, { clearForwardStack: false });
      return;
    }

    const currentQueue = queueRef.current;
    if (currentQueue.length === 0) {
      setCurrentSongState(null);
      playHistoryRef.current = [];
      playForwardRef.current = [];
      setIsPlaying(false);
      setProgress(0);
      restoredPlaybackPositionRef.current = 0;
      return;
    }

    const currentSongId = currentSongRef.current?.id;
    const currentSongIndex =
      currentSongId === undefined
        ? -1
        : currentQueue.findIndex((queuedSong) => queuedSong.id === currentSongId);
    let nextSongIndex = currentSongIndex < 0 ? 0 : currentSongIndex + 1;

    if (nextSongIndex >= currentQueue.length) {
      if (repeatMode === "queue" && currentQueue.length > 0) {
        nextSongIndex = 0;
      } else {
        setCurrentSongState(null);
        setQueueState([]);
        setIsPlaying(false);
        setProgress(0);
        restoredPlaybackPositionRef.current = 0;
        return;
      }
    }

    if (nextSongIndex < 0) {
      setCurrentSongState(null);
      setQueueState([]);
      setIsPlaying(false);
      setProgress(0);
      restoredPlaybackPositionRef.current = 0;
      return;
    }

    const nextSong = currentQueue[nextSongIndex];
    const remainingQueue = currentQueue.slice(nextSongIndex + 1);

    setQueueState(repeatMode === "queue" ? [...remainingQueue, nextSong] : remainingQueue);
    playSong(nextSong);
  }

  function handleTogglePlay() {
    if (currentSongRef.current === null) {
      const [nextSong, ...remainingQueue] = queueRef.current;

      if (nextSong) {
        setQueueState(remainingQueue);
        playSong(nextSong);
      }
      return;
    }

    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    if (audio.paused) {
      void audio.play();
      setIsPlaying(true);
    } else {
      audio.pause();
      setIsPlaying(false);
    }
  }

  function handlePreviousTrack() {
    const previousSong = playHistoryRef.current.at(-1);

    if (previousSong) {
      const activeSong = currentSongRef.current;
      if (activeSong !== null) {
        const nextForward = [
          activeSong,
          ...playForwardRef.current.filter((forwardSong) => forwardSong.id !== activeSong.id),
        ];
        playForwardRef.current = nextForward;
        setQueueState((currentQueue) => [
          ...nextForward,
          ...currentQueue.filter(
            (queuedSong) =>
              queuedSong.id !== previousSong.id &&
              !nextForward.some((forwardQueueSong) => forwardQueueSong.id === queuedSong.id),
          ),
        ]);
      }

      const nextHistory = playHistoryRef.current.slice(0, -1);
      playHistoryRef.current = nextHistory;
      playSong(previousSong, { addCurrentToHistory: false, clearForwardStack: false });
      return;
    }

    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    audio.currentTime = 0;
    setProgress(0);
  }

  function handleNextTrack() {
    playNext();
  }

  function handleEnded() {
    if (repeatMode === "one" && currentSong !== null) {
      playSong(currentSong);
      return;
    }

    playNext();
  }

  function handleAudioFailure() {
    if (!isPlaying || currentSongRef.current === null) {
      return;
    }

    if (playbackRetryRef.current >= 2) {
      return;
    }

    playbackRetryRef.current += 1;
    restoredPlaybackPositionRef.current = progress;
    setPlayNonce((n) => n + 1);
  }

  function handleSeek(value: string) {
    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    const nextTime = Number(value);
    audio.currentTime = nextTime;
    setProgress(nextTime);
  }

  function handleVolumeChange(value: string) {
    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    const nextVolume = Number(value);
    previousVolumeRef.current = nextVolume;
    audio.volume = nextVolume;
    setVolume(nextVolume);
    setIsMuted(nextVolume === 0);
  }

  function handleToggleMute() {
    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    const nextMuted = !isMuted;
    audio.muted = nextMuted;
    setIsMuted(nextMuted);

    if (nextMuted) {
      if (volume > 0) {
        previousVolumeRef.current = volume;
      }

      audio.volume = 0;
      setVolume(0);
    } else {
      const restoredVolume = previousVolumeRef.current > 0 ? previousVolumeRef.current : 0.9;
      audio.volume = restoredVolume;
      setVolume(restoredVolume);
    }
  }

  const handleSignOut = useCallback(() => {
    clearFrontendSessionCookie();
    void logoutUser()
      .catch(() => null)
      .finally(() => {
        setCurrentSongState(null);
        setQueueState([]);
        playHistoryRef.current = [];
        playForwardRef.current = [];
        setIsPlaying(false);
        setProgress(0);
        setDuration(0);
        setShuffleEnabled(false);
        setAuthUser(null);
        setAuthIsAdmin(false);
        setAuthReady(false);
        router.replace("/login");
      });
  }, [router]);

  const handleAddSongToPlaylist = useCallback(async (song: Song, playlistId: number) => {
    const { response } = await addSongToPlaylist(playlistId, song.id);

    if (response.status === 401 || response.status === 403) {
      handleSignOut();
    }
  }, [handleSignOut]);

  const handleToggleLike = useCallback(async (song: Song) => {
    const isLiked = likedSongIds.has(song.id);

    setLikedSongIds((currentIds) => {
      const nextIds = new Set(currentIds);
      if (isLiked) {
        nextIds.delete(song.id);
      } else {
        nextIds.add(song.id);
      }
      return nextIds;
    });

    setLikedSongs((currentSongs) => {
      if (isLiked) {
        return currentSongs.filter((likedSong) => likedSong.id !== song.id);
      }

      if (currentSongs.some((likedSong) => likedSong.id === song.id)) {
        return currentSongs;
      }

      return [song, ...currentSongs];
    });

    const response = isLiked ? await unlikeSong(song.id) : await likeSong(song.id);

    if (response.status === 401 || response.status === 403) {
      handleSignOut();
      return;
    }

    if (!response.ok) {
      void refreshLikedSongs();
    }
  }, [handleSignOut, likedSongIds, refreshLikedSongs]);

  const handleVolumeWheel = useCallback((deltaY: number) => {
    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    const step = 0.05;
    const direction = deltaY > 0 ? -1 : 1;
    const nextVolume = Math.min(1, Math.max(0, volume + direction * step));

    if (nextVolume > 0 && isMuted) {
      audio.muted = false;
      setIsMuted(false);
    }

    previousVolumeRef.current = nextVolume > 0 ? nextVolume : previousVolumeRef.current;
    audio.volume = nextVolume;
    setVolume(nextVolume);

    if (nextVolume === 0) {
      audio.muted = true;
      setIsMuted(true);
    }
  }, [isMuted, volume]);

  useEffect(() => {
    const el = volumeControlRef.current;
    if (!el || !isVolumeHovered) {
      return;
    }

    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      event.stopPropagation();
      handleVolumeWheel(event.deltaY);
    };

    el.addEventListener("wheel", onWheel, { passive: false });

    return () => {
      el.removeEventListener("wheel", onWheel);
    };
  }, [handleVolumeWheel, isVolumeHovered]);

  function closeMobileSidebar() {
    setIsSidebarOpen(false);
  }

  const progressPercent =
    duration > 0 ? Math.min(100, Math.max(0, (progress / duration) * 100)) : 0;
  const volumePercent = Math.min(100, Math.max(0, volume * 100));
  const hasPreviousSongPage = songPage > 1;
  const activeView = view === "adminUsers" && !authIsAdmin ? "songs" : view;

  if (!authReady) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-zinc-950 text-white">
        <div className="rounded-[28px] border border-white/10 bg-white/5 px-6 py-5 text-sm text-zinc-300 backdrop-blur-xl">
          Checking your session...
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-zinc-950 text-white lg:h-screen lg:overflow-hidden">
      <div className="mx-auto flex min-h-screen w-full max-w-none flex-col gap-4 p-3 sm:p-4 lg:h-full lg:min-h-0 lg:flex-row lg:gap-4 lg:p-4">
        <div className="flex items-center justify-between rounded-3xl border border-zinc-800 bg-zinc-900/80 px-4 py-3 shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset] lg:hidden">
          <div>
            <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">OurMusic</p>
            <h2 className="mt-1 text-lg font-semibold">Browse and play</h2>
          </div>

          <button
            type="button"
            onClick={() => setIsSidebarOpen(true)}
            className="rounded-2xl border border-zinc-800 bg-zinc-950/70 px-4 py-2 text-sm font-medium text-zinc-100 transition hover:border-zinc-700 hover:bg-zinc-800/80"
          >
            Menu
          </button>
        </div>

        <LibrarySidebar
          view={view}
          libraryItems={libraryItems}
          authUser={authUser}
          onViewHome={handleViewHome}
          onViewSongs={handleViewSongs}
          onViewLikedSongs={handleViewLikedSongs}
          onViewRecentlyPlayed={handleViewRecentlyPlayed}
          onViewMostPlayed={handleViewMostPlayed}
          onViewAlbums={handleViewAlbums}
          onViewPlaylists={handleViewPlaylists}
          onViewArtists={handleViewArtists}
          onViewAdminUsers={handleViewAdminUsers}
          onSignOut={handleSignOut}
          className="hidden lg:flex"
        />

        {isSidebarOpen ? (
          <div ref={mobileDrawerOverlayRef} className="fixed inset-0 z-50 lg:hidden ourmusic-animate-fade-in">
            <button
              type="button"
              aria-label="Close library menu"
              className="absolute inset-0 bg-black/60 backdrop-blur-[1px]"
              onClick={closeMobileSidebar}
            />

            <div
              ref={mobileDrawerPanelRef}
              className="absolute inset-y-0 left-0 z-10 w-[min(88vw,360px)] shadow-2xl ourmusic-animate-slide-in-left"
            >
              <LibrarySidebar
                view={view}
                libraryItems={libraryItems}
                authUser={authUser}
                onViewHome={handleViewHome}
                onViewSongs={handleViewSongs}
                onViewLikedSongs={handleViewLikedSongs}
                onViewRecentlyPlayed={handleViewRecentlyPlayed}
                onViewMostPlayed={handleViewMostPlayed}
                onViewAlbums={handleViewAlbums}
                onViewPlaylists={handleViewPlaylists}
                onViewArtists={handleViewArtists}
                onViewAdminUsers={handleViewAdminUsers}
                onSignOut={handleSignOut}
                onClose={closeMobileSidebar}
                isDrawer
                className="rounded-r-3xl rounded-l-none border-r border-zinc-800 bg-zinc-900/95"
              />
            </div>
          </div>
        ) : null}

        <section className="min-w-0 flex min-h-0 flex-1 flex-col overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/80 p-3 shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset] max-lg:w-full max-lg:rounded-3xl sm:p-4">
          <div className="shrink-0">
            <LibraryHeader
              pattern={pattern}
              onPatternChange={(value) => {
                setSongPage(1);
                setPattern(value);
              }}
              onFullScan={handleFullScan}
            />
          </div>

          <div ref={contentRef} className="min-h-[420px] flex-1 overflow-y-auto overflow-x-hidden lg:min-h-0 lg:overflow-hidden">
            <LibraryContent
              view={activeView}
              songs={songs}
              currentSong={currentSong}
              stats={libraryStats}
              likedSongs={likedSongs}
              isLikedSongsLoading={isLikedSongsLoading}
              recentlyPlayedSongs={recentlyPlayedSongs}
              mostPlayedSongs={mostPlayedSongs}
              likedSongIds={likedSongIds}
              albums={albums}
              artists={artists}
              playlists={playlists}
              hasMoreArtists={hasNextArtistPage}
              isArtistsLoading={isArtistsLoading}
              songPage={songPage}
              totalSongPages={totalSongPages}
              hasPreviousSongPage={hasPreviousSongPage}
              hasNextSongPage={hasNextSongPage}
              isSongsLoading={isSongsLoading}
              albumPage={albumPage}
              totalAlbumPages={totalAlbumPages}
              hasPreviousAlbumPage={albumPage > 1}
              hasNextAlbumPage={hasNextAlbumPage}
              isAlbumsLoading={isAlbumsLoading}
              recentlyPlayedPage={recentlyPlayedPage}
              hasPreviousRecentlyPlayedPage={recentlyPlayedPage > 1}
              hasNextRecentlyPlayedPage={hasNextRecentlyPlayedPage}
              isRecentlyPlayedLoading={isRecentlyPlayedLoading}
              mostPlayedPage={mostPlayedPage}
              hasPreviousMostPlayedPage={mostPlayedPage > 1}
              hasNextMostPlayedPage={hasNextMostPlayedPage}
              isMostPlayedLoading={isMostPlayedLoading}
              albumSongs={albumSongs}
              artistSongs={artistSongs}
              artistAlbums={artistAlbums}
              selectedAlbum={selectedAlbum}
              selectedArtist={selectedArtist}
              onOpenAlbum={handleOpenAlbum}
              onOpenArtistAlbum={handleOpenArtistAlbum}
              onOpenArtist={handleOpenArtist}
              onLoadMoreArtists={handleLoadMoreArtists}
              onUnauthorized={handleSignOut}
              onPlaylistsChanged={refreshPlaylists}
              onPlaySong={playSong}
              onAddToQueue={addToQueue}
              onAddToPlaylist={handleAddSongToPlaylist}
              onToggleLike={handleToggleLike}
              onPreviousSongPage={handlePreviousSongPage}
              onNextSongPage={handleNextSongPage}
              onPreviousAlbumPage={handlePreviousAlbumPage}
              onNextAlbumPage={handleNextAlbumPage}
              onPreviousRecentlyPlayedPage={handlePreviousRecentlyPlayedPage}
              onNextRecentlyPlayedPage={handleNextRecentlyPlayedPage}
              onPreviousMostPlayedPage={handlePreviousMostPlayedPage}
              onNextMostPlayedPage={handleNextMostPlayedPage}
              onBackFromAlbumSongs={() => setView(albumBackView)}
              onBackToArtists={handleViewArtists}
              onPlayAlbum={playAlbum}
              onQueueAlbum={queueAlbum}
              onPlaySelectedAlbum={playAlbumFromCard}
              onQueueSelectedAlbum={queueAlbumFromCard}
              onViewSongs={handleViewSongs}
              onViewAlbums={handleViewAlbums}
              onViewPlaylists={handleViewPlaylists}
              onViewArtists={handleViewArtists}
              onViewRecentlyPlayed={handleViewRecentlyPlayed}
              onViewMostPlayed={handleViewMostPlayed}
              onAlbumViewportMeasure={(size) => {
                if (size.width <= 0 || size.height <= 0) {
                  return;
                }

                setAlbumGridBox((current) => {
                  if (current.width === size.width && current.height === size.height) {
                    return current;
                  }

                  return size;
                });
              }}
            />
          </div>

          <PlayerBar
            currentSong={currentSong}
            isPlaying={isPlaying}
            isMuted={isMuted}
            volume={volume}
            progress={progress}
            duration={duration}
            progressPercent={progressPercent}
            volumePercent={volumePercent}
            playNonce={playNonce}
            hasQueuedSongs={queue.length > 0}
            repeatMode={repeatMode}
            isCurrentSongLiked={currentSong !== null && likedSongIds.has(currentSong.id)}
            isLyricsOpen={isLyricsOpen}
            playerBarRef={playerBarRef}
            volumeControlRef={volumeControlRef}
            audioRef={audioRef}
            onPreviousTrack={handlePreviousTrack}
            onTogglePlay={handleTogglePlay}
            onCycleRepeatMode={handleCycleRepeatMode}
            onToggleLikeCurrentSong={() => {
              if (currentSong !== null) {
                void handleToggleLike(currentSong);
              }
            }}
            onToggleLyrics={() => setIsLyricsOpen((current) => !current)}
            onNextTrack={handleNextTrack}
            onSeek={handleSeek}
            onToggleMute={handleToggleMute}
            onVolumeChange={handleVolumeChange}
            onVolumeHoverChange={setIsVolumeHovered}
            onEnded={handleEnded}
            onLoadedMetadata={setDuration}
            onRestorePlaybackPosition={(nextDuration) => {
              const restoredPosition = restoredPlaybackPositionRef.current;
              if (restoredPosition <= 0) {
                return;
              }

              const audio = audioRef.current;
              if (!audio) {
                return;
              }

              const nextPosition =
                nextDuration > 0 ? Math.min(restoredPosition, Math.max(0, nextDuration - 0.25)) : restoredPosition;
              audio.currentTime = nextPosition;
              setProgress(nextPosition);
              restoredPlaybackPositionRef.current = 0;
              playbackRetryRef.current = 0;
            }}
            onTimeUpdate={setProgress}
            onPlay={() => {
              playbackRetryRef.current = 0;
              setIsPlaying(true);
            }}
            onPause={() => setIsPlaying(false)}
            onSeeked={setProgress}
            onVolumeUpdate={(nextVolume, nextMuted) => {
              setVolume(nextVolume);
              setIsMuted(nextMuted);
            }}
            onError={handleAudioFailure}
            onStalled={handleAudioFailure}
          />
        </section>

        <QueueSidebar
          currentSong={currentSong}
          queue={queue}
          shuffleEnabled={shuffleEnabled}
          playbackTime={progress}
          isLyricsOpen={isLyricsOpen}
          lyrics={lyricsSongId === currentSong?.id ? lyrics : null}
          isLyricsLoading={isLyricsLoading}
          lyricsError={lyricsError}
          onRemoveFromQueue={removeFromQueue}
          onClearQueue={clearQueue}
          onShuffleQueue={shuffleQueue}
          onSeekToTime={(time) => handleSeek(String(time))}
          className="hidden lg:flex"
        />
      </div>
    </main>
  );
}
