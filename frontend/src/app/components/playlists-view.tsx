"use client";

import Image from "next/image";
import { Download, Ellipsis, GripVertical, Heart, ListMusic, Play, Plus, Trash2, Upload, X } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";

import type { Playlist, Song } from "../music-types";
import { apiUrl, songArtworkUrl } from "../lib/auth";

type PlaylistsViewProps = {
  onUnauthorized?: () => void;
  onPlaylistsChanged?: () => void;
  refreshSongsToken?: number;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  likedSongIds: Set<number>;
  onToggleLike: (song: Song) => void;
};

type PlaylistsResponse = Playlist[];
type SongsResponse = Song[];

async function fetchPlaylists() {
  const response = await fetch(apiUrl("/api/playlists"), {
    credentials: "include",
  });

  const data = (await response.json().catch(() => [])) as PlaylistsResponse;
  return { response, data };
}

async function createPlaylist(name: string) {
  const response = await fetch(apiUrl("/api/playlists"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({ name }),
  });

  const data = (await response.json().catch(() => ({}))) as {
    message?: string;
    playlistId?: number;
  };

  return { response, data };
}

async function fetchPlaylistSongs(playlistId: number) {
  const response = await fetch(apiUrl(`/api/playlists/${playlistId}/songs`), {
    credentials: "include",
  });

  const data = (await response.json().catch(() => [])) as SongsResponse;
  return { response, data };
}

async function deletePlaylistSong(playlistId: number, songId: number) {
  const response = await fetch(apiUrl(`/api/playlists/${playlistId}/songs/${songId}`), {
    method: "DELETE",
    credentials: "include",
  });

  return response;
}

async function deletePlaylist(playlistId: number) {
  const response = await fetch(apiUrl(`/api/playlists/${playlistId}`), {
    method: "DELETE",
    credentials: "include",
  });

  return response;
}

async function uploadPlaylistCover(playlistId: number, file: File) {
  const formData = new FormData();
  formData.append("cover", file);

  const response = await fetch(apiUrl(`/api/playlists/${playlistId}/cover`), {
    method: "PATCH",
    credentials: "include",
    body: formData,
  });

  return response;
}

async function updatePlaylistSongPosition(playlistId: number, songId: number, position: number) {
  const response = await fetch(
    apiUrl(`/api/playlists/${playlistId}/songs/${songId}/position/${position}`),
    {
      method: "PATCH",
      credentials: "include",
    },
  );

  return response;
}

export function PlaylistsView({
  onUnauthorized,
  onPlaylistsChanged,
  refreshSongsToken,
  onPlaySong,
  onAddToQueue,
  likedSongIds,
  onToggleLike,
}: PlaylistsViewProps) {
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [selectedPlaylist, setSelectedPlaylist] = useState<Playlist | null>(null);
  const [playlistSongs, setPlaylistSongs] = useState<Song[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSongsLoading, setIsSongsLoading] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [name, setName] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deletingSongId, setDeletingSongId] = useState<number | null>(null);
  const [deletingPlaylistId, setDeletingPlaylistId] = useState<number | null>(null);
  const [openPlaylistMenuId, setOpenPlaylistMenuId] = useState<number | null>(null);
  const [draggedSongId, setDraggedSongId] = useState<number | null>(null);
  const [dragOverSongId, setDragOverSongId] = useState<number | null>(null);
  const [isReordering, setIsReordering] = useState(false);
  const [playlistCoverVersions, setPlaylistCoverVersions] = useState<Record<number, number>>({});
  const [isUploadingCover, setIsUploadingCover] = useState(false);
  const [, setError] = useState<string | null>(null);
  const [, setMessage] = useState<string | null>(null);
  const playlistMenuRef = useRef<HTMLDivElement | null>(null);
  const playlistCoverInputRef = useRef<HTMLInputElement | null>(null);
  const playlistSongsRef = useRef<HTMLDivElement | null>(null);

  const loadPlaylists = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const { response, data } = await fetchPlaylists();

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to load playlists.");
      }

      setPlaylists(data);
      if (data.length === 0) {
        setPlaylistSongs([]);
      }
      setSelectedPlaylist((current) => {
        if (current === null) {
          return data[0] ?? null;
        }

        return data.find((playlist) => playlist.id === current.id) ?? data[0] ?? null;
      });
    } catch (fetchError) {
      setError(fetchError instanceof Error ? fetchError.message : "Something went wrong.");
    } finally {
      setIsLoading(false);
    }
  }, [onUnauthorized]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadPlaylists();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [loadPlaylists]);

  useEffect(() => {
    if (openPlaylistMenuId === null) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      if (playlistMenuRef.current?.contains(event.target as Node)) {
        return;
      }

      setOpenPlaylistMenuId(null);
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpenPlaylistMenuId(null);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [openPlaylistMenuId]);

  const loadPlaylistSongs = useCallback(
    async (playlistId: number) => {
      setIsSongsLoading(true);
      setError(null);

      try {
        const { response, data } = await fetchPlaylistSongs(playlistId);

        if (response.status === 401 || response.status === 403) {
          onUnauthorized?.();
          return;
        }

        if (!response.ok) {
          throw new Error("Failed to load playlist songs.");
        }

        setPlaylistSongs(data);
      } catch (fetchError) {
        setError(fetchError instanceof Error ? fetchError.message : "Something went wrong.");
      } finally {
        setIsSongsLoading(false);
      }
    },
    [onUnauthorized],
  );

  useEffect(() => {
    if (selectedPlaylist === null) {
      return;
    }

    const timer = window.setTimeout(() => {
      void loadPlaylistSongs(selectedPlaylist.id);
    }, 0);

    return () => window.clearTimeout(timer);
  }, [loadPlaylistSongs, refreshSongsToken, selectedPlaylist]);

  async function handleCreatePlaylist(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedName = name.trim();

    if (trimmedName === "") {
      setError("Playlist name is required.");
      return;
    }

    setError(null);
    setMessage(null);
    setIsSubmitting(true);

    try {
      const { response } = await createPlaylist(trimmedName);

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to create playlist.");
      }

      setMessage(`Created playlist ${trimmedName}.`);
      setName("");
      setIsFormOpen(false);
      await loadPlaylists();
      onPlaylistsChanged?.();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Something went wrong.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleCoverInputClick() {
    playlistCoverInputRef.current?.click();
  }

  async function handlePlaylistCoverChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.currentTarget.files?.[0];
    event.currentTarget.value = "";

    if (selectedPlaylist === null || file === undefined) {
      return;
    }

    setIsUploadingCover(true);
    setError(null);
    setMessage(null);

    try {
      const response = await uploadPlaylistCover(selectedPlaylist.id, file);

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to upload playlist cover.");
      }

      setPlaylistCoverVersions((current) => ({
        ...current,
        [selectedPlaylist.id]: (current[selectedPlaylist.id] ?? 0) + 1,
      }));
      setMessage("Playlist cover updated.");
      await loadPlaylists();
      onPlaylistsChanged?.();
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : "Something went wrong.");
    } finally {
      setIsUploadingCover(false);
    }
  }

  function handleSelectPlaylist(playlist: Playlist) {
    if (selectedPlaylist?.id === playlist.id) {
      setOpenPlaylistMenuId(null);
      return;
    }

    setSelectedPlaylist(playlist);
    setPlaylistSongs([]);
    setOpenPlaylistMenuId(null);
  }

  function handlePlayPlaylist() {
    if (playlistSongs.length === 0) {
      return;
    }

    onPlaySong(playlistSongs[0]);
    playlistSongs.slice(1).forEach((song) => onAddToQueue(song));
  }

  function handleQueuePlaylist() {
    playlistSongs.forEach((song) => onAddToQueue(song));
  }

  async function handleDropSong(targetSong: Song) {
    if (selectedPlaylist === null || draggedSongId === null || draggedSongId === targetSong.id || isReordering) {
      setDraggedSongId(null);
      setDragOverSongId(null);
      return;
    }

    const draggedIndex = playlistSongs.findIndex((song) => song.id === draggedSongId);
    const targetIndex = playlistSongs.findIndex((song) => song.id === targetSong.id);

    if (draggedIndex < 0 || targetIndex < 0) {
      setDraggedSongId(null);
      setDragOverSongId(null);
      return;
    }

    const draggedSong = playlistSongs[draggedIndex];
    const previousSongs = playlistSongs;
    const nextSongs = [...playlistSongs];
    [nextSongs[draggedIndex], nextSongs[targetIndex]] = [nextSongs[targetIndex], nextSongs[draggedIndex]];

    setPlaylistSongs(nextSongs);
    setIsReordering(true);
    setError(null);
    setMessage(null);
    setDraggedSongId(null);
    setDragOverSongId(null);

    try {
      const [draggedResponse, targetResponse] = await Promise.all([
        updatePlaylistSongPosition(selectedPlaylist.id, draggedSong.id, targetIndex + 1),
        updatePlaylistSongPosition(selectedPlaylist.id, targetSong.id, draggedIndex + 1),
      ]);

      if (
        draggedResponse.status === 401 ||
        draggedResponse.status === 403 ||
        targetResponse.status === 401 ||
        targetResponse.status === 403
      ) {
        onUnauthorized?.();
        return;
      }

      if (!draggedResponse.ok || !targetResponse.ok) {
        throw new Error("Failed to reorder playlist songs.");
      }
    } catch (dropError) {
      setPlaylistSongs(previousSongs);
      setError(dropError instanceof Error ? dropError.message : "Something went wrong.");
    } finally {
      setIsReordering(false);
    }
  }

  async function handleDeleteSong(song: Song) {
    if (selectedPlaylist === null) {
      return;
    }

    setDeletingSongId(song.id);
    setError(null);
    setMessage(null);

    try {
      const response = await deletePlaylistSong(selectedPlaylist.id, song.id);

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to remove song from playlist.");
      }

      setPlaylistSongs((currentSongs) => currentSongs.filter((playlistSong) => playlistSong.id !== song.id));
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Something went wrong.");
    } finally {
      setDeletingSongId(null);
    }
  }

  async function handleDeletePlaylist(playlist: Playlist) {
    setDeletingPlaylistId(playlist.id);
    setError(null);
    setMessage(null);

    try {
      const response = await deletePlaylist(playlist.id);

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to delete playlist.");
      }

      setPlaylists((currentPlaylists) => {
        const nextPlaylists = currentPlaylists.filter((currentPlaylist) => currentPlaylist.id !== playlist.id);

        if (selectedPlaylist?.id === playlist.id) {
          setSelectedPlaylist(nextPlaylists[0] ?? null);
          setPlaylistSongs([]);
        }

        return nextPlaylists;
      });
      setOpenPlaylistMenuId(null);
      onPlaylistsChanged?.();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Something went wrong.");
    } finally {
      setDeletingPlaylistId(null);
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/80 p-4 shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset]">
      <div className="shrink-0">
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">Library</p>
            <h2 className="mt-2 text-2xl font-semibold">Playlists</h2>
          </div>

          {isFormOpen ? (
            <form
              className="flex w-full max-w-md items-center gap-2 rounded-2xl border border-white/10 bg-zinc-950/80 p-1.5 md:w-auto"
              onSubmit={handleCreatePlaylist}
            >
              <input
                type="text"
                autoComplete="off"
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="min-w-0 flex-1 bg-transparent px-3 py-1.5 text-sm text-white outline-none placeholder:text-zinc-600 md:w-52"
                placeholder="Playlist name"
                required
                autoFocus
              />
              <button
                type="submit"
                disabled={isSubmitting || name.trim() === ""}
                className="shrink-0 rounded-xl bg-white px-3 py-1.5 text-sm font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting ? "Creating..." : "Create"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setIsFormOpen(false);
                  setName("");
                }}
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl border border-white/10 text-zinc-300 transition hover:bg-white/10 hover:text-white"
                aria-label="Close create playlist form"
              >
                <X className="h-4 w-4" />
              </button>
            </form>
          ) : (
            <button
              type="button"
              onClick={() => setIsFormOpen(true)}
              className="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white px-4 py-2 text-sm font-medium text-zinc-950 transition hover:bg-zinc-200"
            >
              <Plus className="h-4 w-4" />
              Create playlist
            </button>
          )}
        </div>

        <div className="mt-4 flex flex-wrap gap-2 text-xs text-zinc-400">
          <span className="rounded-full border border-zinc-800 bg-zinc-950/60 px-3 py-1">
            {playlists.length} playlists
          </span>
        </div>
      </div>

      <input
        ref={playlistCoverInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handlePlaylistCoverChange}
      />

      <div className="mt-6 grid h-full min-h-0 flex-1 gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
        <section className="h-full min-h-0 overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950/40">
          <div className="h-full overflow-auto">
            <table className="min-w-full table-fixed divide-y divide-zinc-800">
              <thead className="sticky top-0 z-10 bg-zinc-950/90 backdrop-blur">
                <tr className="text-left text-xs uppercase tracking-[0.18em] text-zinc-500">
                  <th className="px-4 py-3 font-semibold">Playlist</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800">
                {isLoading ? (
                  <tr>
                    <td className="px-4 py-5 text-sm text-zinc-400">
                      Loading playlists...
                    </td>
                  </tr>
                ) : playlists.length === 0 ? (
                  <tr>
                    <td className="px-4 py-5 text-sm text-zinc-400">
                      No playlists found.
                    </td>
                  </tr>
                ) : (
                  playlists.map((playlist, index) => {
                    const isActive = selectedPlaylist?.id === playlist.id;

                    return (
                      <tr
                        key={playlist.id}
                        className="group ourmusic-animate-fade-up"
                        style={{ animationDelay: `${index * 18}ms` }}
                      >
                        <td className="p-0">
                          <div
                            role="button"
                            tabIndex={0}
                            onClick={() => handleSelectPlaylist(playlist)}
                            onKeyDown={(event) => {
                              if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                handleSelectPlaylist(playlist);
                              }
                            }}
                            className={`flex w-full items-center gap-3 px-4 py-4 text-left transition ${
                              isActive
                                ? "bg-white text-zinc-950"
                                : "cursor-pointer text-white hover:bg-white/5"
                            }`}
                          >
                            <PlaylistCoverImage
                              key={`${playlist.id}-${playlist.hasCover ? "cover" : "default"}-${playlistCoverVersions[playlist.id] ?? 0}`}
                              playlistId={playlist.id}
                              hasCover={playlist.hasCover}
                              refreshToken={playlistCoverVersions[playlist.id] ?? 0}
                              alt={playlist.name}
                              size="list"
                            />
                            <span className="min-w-0 flex-1 truncate text-left text-sm font-medium">
                              {playlist.name}
                            </span>

                            <div
                              ref={openPlaylistMenuId === playlist.id ? playlistMenuRef : null}
                              className="relative shrink-0"
                            >
                              <button
                                type="button"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  setOpenPlaylistMenuId((current) => (current === playlist.id ? null : playlist.id));
                                }}
                                aria-label={`Open actions for ${playlist.name}`}
                                className={`h-8 w-8 items-center justify-center rounded-full border transition ${
                                  openPlaylistMenuId === playlist.id
                                    ? "flex border-white/20 bg-white/10"
                                    : "hidden border-white/10 bg-white/5 opacity-0 hover:bg-white/10 group-hover:flex group-hover:opacity-100 focus:flex focus:opacity-100"
                                } ${isActive ? "text-zinc-950 hover:bg-zinc-200" : "text-zinc-300 hover:text-white"}`}
                              >
                                <Ellipsis className="h-4 w-4" />
                              </button>

                              {openPlaylistMenuId === playlist.id ? (
                                <div className="absolute right-0 top-10 z-20 w-36 rounded-2xl border border-white/10 bg-zinc-950/95 p-1.5 shadow-[0_16px_50px_rgba(0,0,0,0.45)]">
                                  <button
                                    type="button"
                                    onClick={(event) => {
                                      event.stopPropagation();
                                      void handleDeletePlaylist(playlist);
                                    }}
                                    disabled={deletingPlaylistId === playlist.id}
                                    className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-xs font-medium text-red-200 transition hover:bg-red-500/10 disabled:cursor-not-allowed disabled:opacity-50"
                                  >
                                {deletingPlaylistId === playlist.id ? (
                                  <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-red-300/40 border-t-red-100" />
                                ) : (
                                  <Trash2 className="h-4 w-4" />
                                )}
                                    Delete
                                  </button>
                                </div>
                              ) : null}
                            </div>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="flex min-h-0 flex-col overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950/40">
          <div className="border-b border-zinc-800 px-5 py-4">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div className="flex min-w-0 items-start gap-4">
                <PlaylistCoverImage
                  key={`${selectedPlaylist?.id ?? "none"}-${selectedPlaylist?.hasCover ? "cover" : "default"}-${selectedPlaylist ? playlistCoverVersions[selectedPlaylist.id] ?? 0 : 0}`}
                  playlistId={selectedPlaylist?.id ?? null}
                  hasCover={selectedPlaylist?.hasCover ?? false}
                  refreshToken={selectedPlaylist ? playlistCoverVersions[selectedPlaylist.id] ?? 0 : 0}
                  alt={selectedPlaylist ? selectedPlaylist.name : "Playlist cover"}
                  size="hero"
                  priority
                />

                <div className="min-w-0">
                  <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">Playlist</p>
                  <h3 className="mt-2 truncate text-2xl font-semibold">
                    {selectedPlaylist ? selectedPlaylist.name : "Select a playlist"}
                  </h3>
                  <p className="mt-2 text-sm text-zinc-400">
                    {selectedPlaylist ? `${playlistSongs.length} songs` : "Choose a playlist to view its songs."}
                  </p>
                  {selectedPlaylist ? (
                    <div className="mt-4 flex flex-wrap gap-2">
                      <button
                        type="button"
                        className="flex items-center gap-2 rounded-full border border-white/10 bg-white px-3 py-2 text-xs font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-40"
                        onClick={handleCoverInputClick}
                        disabled={isUploadingCover}
                      >
                        <Upload className="h-3.5 w-3.5" />
                        {isUploadingCover ? "Uploading..." : "Upload cover"}
                      </button>
                    </div>
                  ) : null}
                </div>
              </div>

              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  className="flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-2 text-xs font-medium text-white backdrop-blur-md transition hover:border-white/20 hover:bg-white/15 disabled:cursor-not-allowed disabled:opacity-40"
                  onClick={handlePlayPlaylist}
                  disabled={playlistSongs.length === 0}
                >
                  <Play className="h-3 w-3 fill-current" />
                  Play all
                </button>

                <button
                  type="button"
                  className="flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-2 text-xs font-medium text-zinc-100 backdrop-blur-md transition hover:border-white/20 hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                  onClick={handleQueuePlaylist}
                  disabled={playlistSongs.length === 0}
                >
                  <span className="text-sm font-semibold leading-none text-white">+</span>
                  Add all to queue
                </button>
              </div>
            </div>
          </div>

          <div
            ref={playlistSongsRef}
            className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden"
          >
            <table className="w-full table-fixed border-collapse">
              <thead className="sticky top-0 z-10 bg-zinc-950/90 text-left text-xs uppercase tracking-[0.18em] text-zinc-500 backdrop-blur">
                <tr>
                  <th className="w-[96px] px-4 py-3 font-medium">#</th>
                  <th className="px-4 py-3 font-medium">Title</th>
                  <th className="w-[24%] px-4 py-3 font-medium">Artist</th>
                  <th className="w-[84px] px-4 py-3 font-medium">Time</th>
                  <th className="w-[236px] px-4 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {selectedPlaylist === null ? (
                  <tr>
                    <td className="px-5 py-5 text-sm text-zinc-400" colSpan={5}>
                      Select a playlist to view songs.
                    </td>
                  </tr>
                ) : isSongsLoading ? (
                  <tr>
                    <td className="px-5 py-5 text-sm text-zinc-400" colSpan={5}>
                      Loading songs...
                    </td>
                  </tr>
                ) : playlistSongs.length === 0 ? (
                  <tr>
                    <td className="px-5 py-5 text-sm text-zinc-400" colSpan={5}>
                      No songs in this playlist.
                    </td>
                  </tr>
                ) : (
                  playlistSongs.map((song, index) => {
                    const isDragging = draggedSongId === song.id;
                    const isDragTarget = dragOverSongId === song.id && draggedSongId !== song.id;

                    return (
                      <tr
                        key={song.id}
                        onDragOver={(event) => {
                          if (draggedSongId === null || draggedSongId === song.id || isReordering) {
                            return;
                          }

                          event.preventDefault();
                          event.dataTransfer.dropEffect = "move";
                          setDragOverSongId(song.id);
                        }}
                        onDragLeave={() => {
                          setDragOverSongId((currentSongId) => (currentSongId === song.id ? null : currentSongId));
                        }}
                        onDrop={(event) => {
                          event.preventDefault();
                          void handleDropSong(song);
                        }}
                        className={`group border-t border-zinc-800 transition ${
                          isDragging
                            ? "bg-zinc-800/50 opacity-60"
                            : isDragTarget
                              ? "bg-white/10"
                              : "hover:bg-zinc-800/70"
                        } ourmusic-animate-fade-up`}
                        style={{ animationDelay: `${index * 16}ms` }}
                      >
                        <td className="px-4 py-3 text-sm text-zinc-500 align-middle">
                          <div className="flex h-10 items-center gap-2">
                            <span
                              role="button"
                              tabIndex={0}
                              draggable={!isReordering}
                              onDragStart={(event) => {
                                event.dataTransfer.effectAllowed = "move";
                                event.dataTransfer.setData("text/plain", String(song.id));
                                setDraggedSongId(song.id);
                              }}
                              onDragEnd={() => {
                                setDraggedSongId(null);
                                setDragOverSongId(null);
                              }}
                              className="inline-flex h-8 w-6 cursor-grab items-center justify-center rounded-full text-zinc-600 transition hover:bg-white/10 hover:text-zinc-200 active:cursor-grabbing"
                              aria-label={`Reorder ${song.title}`}
                            >
                              <GripVertical className="h-4 w-4" />
                            </span>

                            <span className="inline-flex h-8 w-8 items-center justify-center tabular-nums">
                              {index + 1}
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3 align-middle">
                          <div className="flex min-w-0 items-center gap-3">
                            <Image
                              src={songArtworkUrl(song.id)}
                              alt={song.title}
                              className="h-10 w-10 shrink-0 rounded-xl object-cover ring-1 ring-white/10"
                              width={40}
                              height={40}
                              loading="lazy"
                              unoptimized
                            />
                            <div className="min-w-0">
                              <strong className="block truncate text-sm font-medium text-white">{song.title}</strong>
                              <p className="mt-0.5 truncate text-xs text-zinc-500">{song.album}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 align-middle text-sm text-zinc-400">
                          <span className="block truncate">{song.artist}</span>
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 align-middle text-sm tabular-nums text-zinc-400">
                          {formatTime(song.duration)}
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 align-middle">
                          <div className="flex flex-nowrap justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => onPlaySong(song)}
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white transition hover:border-white/20 hover:bg-white/15"
                              aria-label={`Play ${song.title}`}
                            >
                              <Play className="h-3.5 w-3.5 fill-current" />
                            </button>
                            <button
                              type="button"
                              onClick={() => onAddToQueue(song)}
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 text-white transition hover:border-white/20 hover:bg-white/10"
                              aria-label={`Add ${song.title} to queue`}
                            >
                              <Plus className="h-4 w-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => onToggleLike(song)}
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 text-zinc-100 transition hover:border-white/20 hover:bg-white/10"
                              aria-label={likedSongIds.has(song.id) ? `Unlike ${song.title}` : `Like ${song.title}`}
                            >
                              <Heart
                                className={`h-4 w-4 ${
                                  likedSongIds.has(song.id) ? "fill-red-400 text-red-400" : ""
                                }`}
                              />
                            </button>
                            <a
                              href={apiUrl(`/api/songs/${song.id}/download`)}
                              download
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 text-zinc-100 transition hover:border-white/20 hover:bg-white/10"
                              aria-label={`Download ${song.title}`}
                            >
                              <Download className="h-4 w-4" />
                            </a>
                            <button
                              type="button"
                              onClick={() => handleDeleteSong(song)}
                              disabled={deletingSongId === song.id}
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-red-500/20 bg-red-500/10 text-red-200 transition hover:border-red-400/30 hover:bg-red-500/15 disabled:cursor-not-allowed disabled:opacity-40"
                              aria-label={`Remove ${song.title} from playlist`}
                            >
                              {deletingSongId === song.id ? (
                                <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-red-300/40 border-t-red-100" />
                              ) : (
                                <Trash2 className="h-4 w-4" />
                              )}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}

function formatTime(seconds: number) {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return "0:00";
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.floor(seconds % 60)
    .toString()
    .padStart(2, "0");

  return `${minutes}:${remainingSeconds}`;
}

function PlaylistCoverImage({
  playlistId,
  hasCover,
  refreshToken,
  alt,
  size,
  priority = false,
}: {
  playlistId: number | null;
  hasCover: boolean;
  refreshToken: number;
  alt: string;
  size: "list" | "hero";
  priority?: boolean;
}) {
  const source =
    hasCover && playlistId !== null
      ? apiUrl(`/api/playlists/${playlistId}/cover?v=${refreshToken}`)
      : null;
  const [hasErrored, setHasErrored] = useState(false);

  const dimensions =
    size === "hero"
      ? { className: "h-24 w-24 rounded-3xl", width: 120, height: 120 }
      : { className: "h-10 w-10 rounded-2xl", width: 40, height: 40 };

  return (
    <span
      className={`relative shrink-0 overflow-hidden border border-white/10 bg-zinc-900 ${dimensions.className}`}
    >
      {source !== null && !hasErrored ? (
        <Image
          src={source}
          alt={alt}
          className="h-full w-full object-cover"
          width={dimensions.width}
          height={dimensions.height}
          priority={priority}
          unoptimized
          onError={() => {
            if (!hasErrored) {
              setHasErrored(true);
            }
          }}
        />
      ) : (
        <span className="flex h-full w-full items-center justify-center bg-zinc-800 text-zinc-200">
          <ListMusic className="h-4 w-4" />
        </span>
      )}
    </span>
  );
}
