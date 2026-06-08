"use client";

import {
  Disc3,
  Music2,
  UsersRound,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { LibraryContent } from "./components/library-content";
import { LibraryHeader } from "./components/library-header";
import { LibrarySidebar } from "./components/library-sidebar";
import { PlayerBar } from "./components/player-bar";
import type { Album, Artist, Song, View } from "./music-types";
import { clearFrontendSessionCookie, fetchAuthSession, logoutUser } from "./lib/auth";

async function getArtistAlbums(artistId: number): Promise<Album[]> {
  const resp = await fetch(`http://192.168.1.76:8808/api/artists/${artistId}/albums`);
  return resp.json();
}

type GetArtistsParams = {
  limit: number;
  offset: number;
  signal?: AbortSignal;
};

async function getArtistsPaged({ limit, offset, signal }: GetArtistsParams): Promise<Artist[]> {
  const url = new URL("http://192.168.1.76:8808/api/artists");
  url.searchParams.set("limit", String(limit));
  url.searchParams.set("offset", String(offset));

  const resp = await fetch(url, { signal });
  return resp.json();
}

async function getArtistSongs(artistId: number): Promise<Song[]> {
  const resp = await fetch(`http://192.168.1.76:8808/api/artists/${artistId}/songs`);
  return resp.json();
}

async function getAlbumSongs(albumId: number): Promise<Song[]> {
  const resp = await fetch(`http://192.168.1.76:8808/api/albums/${albumId}/songs`);
  return resp.json();
}

type GetAlbumsParams = {
  limit: number;
  offset: number;
  signal?: AbortSignal;
};

async function getAlbums({ limit, offset, signal }: GetAlbumsParams): Promise<Album[]> {
  const url = new URL("http://192.168.1.76:8808/api/albums");
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
  const url = new URL("http://192.168.1.76:8808/api/songs");

  if (pattern && pattern.trim() !== "") {
    url.searchParams.set("search", pattern.trim());
  }

  url.searchParams.set("limit", String(limit));
  url.searchParams.set("offset", String(offset));

  const resp = await fetch(url, { signal });
  return resp.json();
}

async function fullScan() {
  const resp = await fetch("http://192.168.1.76:8808/api/library/scan/full", {
    method: "POST",
  });
  return resp.json();
}

const ARTIST_PAGE_SIZE = 50;
const GRID_CARD_MIN_SIZE = 170;
const GRID_CARD_GAP = 14;

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
  const previousVolumeRef = useRef(0.9);
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

  const [albumSongs, setAlbumSongs] = useState<Song[]>([]);
  const [selectedAlbum, setSelectedAlbum] = useState<Album | null>(null);
  const [albumBackView, setAlbumBackView] = useState<View>("albums");

  const [view, setView] = useState<View>("songs");
  const [albums, setAlbums] = useState<Album[]>([]);
  const [albumPage, setAlbumPage] = useState(1);
  const [hasNextAlbumPage, setHasNextAlbumPage] = useState(false);
  const [isAlbumsLoading, setIsAlbumsLoading] = useState(false);
  const [songs, setSongs] = useState<Song[]>([]);
  const [pattern, setPattern] = useState("");
  const [songPage, setSongPage] = useState(1);
  const [hasNextSongPage, setHasNextSongPage] = useState(false);
  const [isSongsLoading, setIsSongsLoading] = useState(false);
  const [songGridBox, setSongGridBox] = useState({ width: 0, height: 0 });
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
  const [repeatMode, setRepeatMode] = useState<"off" | "one" | "queue">("off");

  const albumLimit = useMemo(
    () => calculateAutoPageSize(albumGridBox.width, albumGridBox.height, GRID_CARD_MIN_SIZE, GRID_CARD_GAP),
    [albumGridBox.height, albumGridBox.width],
  );

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

  const songLimit = useMemo(
    () => calculateAutoPageSize(songGridBox.width, songGridBox.height, GRID_CARD_MIN_SIZE, GRID_CARD_GAP),
    [songGridBox.height, songGridBox.width],
  );

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

  const libraryItems = useMemo(
    () => [
      { id: "songs" as const, label: "Songs", icon: Music2 },
      { id: "albums" as const, label: "Albums", icon: Disc3 },
      { id: "artists" as const, label: "Artists", icon: UsersRound },
      ...(authIsAdmin ? [{ id: "adminUsers" as const, label: "Users", icon: UsersRound }] : []),
    ],
    [authIsAdmin],
  );

  useEffect(() => {
    if (!authReady) {
      return;
    }

    async function loadLibrary() {
      const [songsData, albumsData] = await Promise.all([
        getSongs({ limit: songLimit + 1, offset: 0 }),
        getAlbums({ limit: albumLimit + 1, offset: 0 }),
      ]);

      applySongsPage(songsData);
      applyAlbumsPage(albumsData);
    }

    loadLibrary();
  }, [albumLimit, applyAlbumsPage, applySongsPage, authReady, songLimit]);

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

    const query = pattern.trim();
    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      setIsSongsLoading(true);
      void getSongs({
        pattern: query === "" ? undefined : query,
        limit: songLimit + 1,
        offset: (songPage - 1) * songLimit,
        signal: controller.signal,
      })
        .then((songsData) => {
          if (!controller.signal.aborted) {
            applySongsPage(songsData);
          }
        })
        .catch(() => {
          if (!controller.signal.aborted) {
            setIsSongsLoading(false);
          }
        });
    }, 250);

    return () => {
      controller.abort("songs-page-unmounted");
      window.clearTimeout(timeout);
    };
  }, [applySongsPage, authReady, pattern, songLimit, songPage]);

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

  async function handleOpenAlbum(album: Album) {
    setAlbumBackView("albums");
    setSelectedAlbum(album);
    setAlbumSongs([]);
    setView("albumSongs");

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }

    const data = await getAlbumSongs(album.id);
    setAlbumSongs(data);
  }

  async function handleOpenArtistAlbum(album: Album) {
    setAlbumBackView("artistSongs");
    setSelectedAlbum(album);
    setAlbumSongs([]);
    setView("albumSongs");

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }

    const data = await getAlbumSongs(album.id);
    setAlbumSongs(data);
  }

  async function handleViewAlbums() {
    setView("albums");
    setSelectedArtist(null);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
  }

  function handleViewSongs() {
    setView("songs");
    setSelectedArtist(null);
    setSongPage(1);

    if (contentRef.current) {
      contentRef.current.scrollTop = 0;
    }
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

    const [songsData, albumsData] = await Promise.all([
      getArtistSongs(artist.id),
      getArtistAlbums(artist.id),
    ]);

    setArtistSongs(songsData);
    setArtistAlbums(albumsData);
  }

  async function handleFullScan() {
    await fullScan();
    setSongPage(1);
    setAlbumPage(1);
    setArtistPage(1);
    const [songsData, albumsData, artistsData] = await Promise.all([
      getSongs({ limit: songLimit + 1, offset: 0 }),
      getAlbums({ limit: albumLimit + 1, offset: 0 }),
      getArtistsPaged({ limit: ARTIST_PAGE_SIZE + 1, offset: 0 }),
    ]);

    applySongsPage(songsData);
    applyAlbumsPage(albumsData);
    setHasNextArtistPage(artistsData.length > ARTIST_PAGE_SIZE);
    setArtists(artistsData.slice(0, ARTIST_PAGE_SIZE));
  }

  function playSong(song: Song) {
    setCurrentSong(song);
    setPlayNonce((n) => n + 1);
    setIsPlaying(true);
    setProgress(0);
    setDuration(0);
  }

  function addToQueue(song: Song) {
    setQueue((currentQueue) => [...currentQueue, song]);
  }

  function removeFromQueue(index: number) {
    setQueue((currentQueue) => currentQueue.filter((_, queueIndex) => queueIndex !== index));
  }

  function clearQueue() {
    setQueue([]);
    setRepeatMode("off");
  }

  function handleCycleRepeatMode() {
    setRepeatMode((current) => (current === "off" ? "one" : current === "one" ? "queue" : "off"));
  }

  function shuffleQueue() {
    setQueue((currentQueue) => {
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
    setQueue(rest);
    playSong(firstTrack);
  }

  async function queueAlbumFromCard(album: Album) {
    const tracks = await getAlbumSongs(album.id);

    if (tracks.length === 0) {
      return;
    }

    setQueue((currentQueue) => [...currentQueue, ...tracks]);
  }

  function playAlbum() {
    if (albumSongs.length === 0) {
      return;
    }

    const [firstSong, ...remainingSongs] = albumSongs;
    setQueue(remainingSongs);
    playSong(firstSong);
  }

  function queueAlbum() {
    if (albumSongs.length === 0) {
      return;
    }

    setQueue((currentQueue) => [...currentQueue, ...albumSongs]);
  }

  function playNext() {
    setQueue((currentQueue) => {
      if (currentQueue.length === 0) {
        setCurrentSong(null);
        setIsPlaying(false);
        setProgress(0);
        return [];
      }

      const nextSong = currentQueue[0];
      const remainingQueue = currentQueue.slice(1);

      playSong(nextSong);
      if (repeatMode === "queue") {
        return [...remainingQueue, nextSong];
      }

      return remainingQueue;
    });
  }

  function handleTogglePlay() {
    if (currentSong === null) {
      setQueue((currentQueue) => {
        const [nextSong, ...remainingQueue] = currentQueue;

        if (!nextSong) {
          return currentQueue;
        }

        playSong(nextSong);
        return remainingQueue;
      });
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
        setCurrentSong(null);
        setQueue([]);
        setIsPlaying(false);
        setProgress(0);
        setDuration(0);
        setAuthUser(null);
        setAuthIsAdmin(false);
        setAuthReady(false);
        router.replace("/login");
      });
  }, [router]);

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
      <div className="mx-auto flex min-h-screen max-w-[1600px] flex-col gap-4 p-4 lg:h-full lg:min-h-0 lg:flex-row lg:gap-6 lg:p-6 lg:pb-6">
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
            queue={queue}
            authUser={authUser}
            onViewSongs={handleViewSongs}
            onViewAlbums={handleViewAlbums}
            onViewArtists={handleViewArtists}
            onViewAdminUsers={handleViewAdminUsers}
            onRemoveFromQueue={removeFromQueue}
            onClearQueue={clearQueue}
            onShuffleQueue={shuffleQueue}
            onSignOut={handleSignOut}
            className="hidden lg:flex"
          />

        {isSidebarOpen ? (
          <div className="fixed inset-0 z-50 lg:hidden">
            <button
              type="button"
              aria-label="Close library menu"
              className="absolute inset-0 bg-black/60 backdrop-blur-[1px]"
              onClick={() => setIsSidebarOpen(false)}
            />

            <LibrarySidebar
              view={view}
              libraryItems={libraryItems}
              queue={queue}
              authUser={authUser}
              onViewSongs={handleViewSongs}
              onViewAlbums={handleViewAlbums}
              onViewArtists={handleViewArtists}
              onViewAdminUsers={handleViewAdminUsers}
              onRemoveFromQueue={removeFromQueue}
              onClearQueue={clearQueue}
              onShuffleQueue={shuffleQueue}
              onSignOut={handleSignOut}
              onClose={() => setIsSidebarOpen(false)}
              isDrawer
              className="absolute inset-y-0 left-0 z-10 w-[min(88vw,360px)] rounded-r-3xl rounded-l-none border-r border-zinc-800 bg-zinc-900/95 shadow-2xl"
            />
          </div>
        ) : null}

        <section className="min-w-0 flex min-h-0 flex-1 flex-col overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/80 p-4 shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset] max-lg:w-full max-lg:rounded-3xl">
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

          <div ref={contentRef} className="min-h-0 flex-1 overflow-hidden overflow-x-hidden">
            <LibraryContent
              view={activeView}
              songs={songs}
              albums={albums}
              artists={artists}
              hasMoreArtists={hasNextArtistPage}
              isArtistsLoading={isArtistsLoading}
              songPage={songPage}
              hasPreviousSongPage={hasPreviousSongPage}
              hasNextSongPage={hasNextSongPage}
              isSongsLoading={isSongsLoading}
              albumPage={albumPage}
              hasPreviousAlbumPage={albumPage > 1}
              hasNextAlbumPage={hasNextAlbumPage}
              isAlbumsLoading={isAlbumsLoading}
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
              onPlaySong={playSong}
              onAddToQueue={addToQueue}
              onPreviousSongPage={handlePreviousSongPage}
              onNextSongPage={handleNextSongPage}
              onPreviousAlbumPage={handlePreviousAlbumPage}
              onNextAlbumPage={handleNextAlbumPage}
              onBackFromAlbumSongs={() => setView(albumBackView)}
              onBackToArtists={handleViewArtists}
              onPlayAlbum={playAlbum}
              onQueueAlbum={queueAlbum}
              onPlaySelectedAlbum={playAlbumFromCard}
              onQueueSelectedAlbum={queueAlbumFromCard}
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
              onSongViewportMeasure={(size) => {
                if (size.width <= 0 || size.height <= 0) {
                  return;
                }

                setSongGridBox((current) => {
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
            playerBarRef={playerBarRef}
            volumeControlRef={volumeControlRef}
            audioRef={audioRef}
            onPreviousTrack={handlePreviousTrack}
            onTogglePlay={handleTogglePlay}
            onCycleRepeatMode={handleCycleRepeatMode}
            onNextTrack={handleNextTrack}
            onSeek={handleSeek}
            onToggleMute={handleToggleMute}
            onVolumeChange={handleVolumeChange}
            onVolumeHoverChange={setIsVolumeHovered}
            onEnded={handleEnded}
            onLoadedMetadata={setDuration}
            onTimeUpdate={setProgress}
            onPlay={() => setIsPlaying(true)}
            onPause={() => setIsPlaying(false)}
            onSeeked={setProgress}
            onVolumeUpdate={(nextVolume, nextMuted) => {
              setVolume(nextVolume);
              setIsMuted(nextMuted);
            }}
          />
        </section>
      </div>
    </main>
  );
}
