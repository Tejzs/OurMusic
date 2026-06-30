"use client";

import Image from "next/image";
import { Shuffle, Trash2, X } from "lucide-react";
import { useLayoutEffect, useMemo, useRef } from "react";
import type { Song, SongLyrics } from "../music-types";
import { songArtworkUrl } from "../lib/auth";

type SyncedLyricLine = {
  time: number;
  text: string;
};

type QueueSidebarProps = {
  currentSong: Song | null;
  queue: Song[];
  shuffleEnabled: boolean;
  playbackTime: number;
  isLyricsOpen: boolean;
  lyrics: SongLyrics | null;
  isLyricsLoading: boolean;
  lyricsError: string | null;
  onRemoveFromQueue: (index: number) => void;
  onClearQueue: () => void;
  onShuffleQueue: () => void;
  onSeekToTime: (time: number) => void;
  className?: string;
};

export function QueueSidebar({
  currentSong,
  queue,
  shuffleEnabled,
  playbackTime,
  isLyricsOpen,
  lyrics,
  isLyricsLoading,
  lyricsError,
  onRemoveFromQueue,
  onClearQueue,
  onShuffleQueue,
  onSeekToTime,
  className = "",
}: QueueSidebarProps) {
  const lyricsScrollRef = useRef<HTMLDivElement | null>(null);
  const syncedLyrics = useMemo(() => {
    const lrc = lyrics?.syncedLyrics ?? lyrics?.lyricsfile ?? null;
    return lrc ? parseSyncedLyrics(lrc) : [];
  }, [lyrics]);
  const activeLyricIndex = useMemo(
    () => getActiveLyricIndex(syncedLyrics, playbackTime),
    [playbackTime, syncedLyrics],
  );
  const queueListHeight = isLyricsOpen
    ? `${Math.max(1, Math.min(queue.length, 5)) * 72}px`
    : undefined;

  useLayoutEffect(() => {
    const scrollContainer = lyricsScrollRef.current;

    if (!isLyricsOpen || activeLyricIndex < 0 || !scrollContainer) {
      return;
    }

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const activeLine = scrollContainer.querySelector<HTMLElement>(`[data-lyric-index="${activeLyricIndex}"]`);

    if (!activeLine) {
      return;
    }

    const targetTop = activeLine.offsetTop - scrollContainer.clientHeight / 2 + activeLine.offsetHeight / 2;

    scrollContainer.scrollTo({
      top: Math.max(0, targetTop),
      behavior: prefersReducedMotion ? "auto" : "smooth",
    });
  }, [activeLyricIndex, isLyricsOpen]);

  return (
    <aside
      className={`flex shrink-0 flex-col gap-4 rounded-3xl border border-zinc-800 bg-zinc-900/80 p-4 backdrop-blur lg:h-full lg:w-[300px] lg:overflow-y-auto ${className}`.trim()}
    >
      <section className={isLyricsOpen ? "shrink-0" : "min-h-0"}>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Queue</p>
            <h2 className="mt-2 text-2xl font-semibold">Up Next</h2>
          </div>
          <span className="rounded-full border border-zinc-800 bg-zinc-950/60 px-3 py-1 text-xs text-zinc-400">
            {queue.length}
          </span>
        </div>

        <div className="mb-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-zinc-500">
            Now Playing
          </p>
          {currentSong ? (
            <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-zinc-950/70 p-3">
              <Image
                src={songArtworkUrl(currentSong.id)}
                alt={currentSong.title}
                className="absolute inset-0 h-full w-full scale-110 object-cover object-center blur-xs"
                width={500}
                height={500}
                loading="lazy"
                unoptimized
              />
              <div className="absolute inset-0 bg-zinc-950/75" />
              <div className="relative z-10 flex min-w-0 items-center gap-3">
                <Image
                  src={songArtworkUrl(currentSong.id)}
                  alt={currentSong.title}
                  className="h-11 w-11 shrink-0 rounded-xl object-cover ring-1 ring-white/10"
                  width={44}
                  height={44}
                  loading="lazy"
                  unoptimized
                />
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-white">{currentSong.title}</p>
                  <p className="truncate text-xs text-zinc-300">{currentSong.artist}</p>
                </div>
              </div>
            </div>
          ) : (
            <p className="rounded-2xl border border-dashed border-zinc-800 bg-zinc-950/50 px-4 py-4 text-sm text-zinc-500">
              Nothing is playing.
            </p>
          )}
        </div>

        <div className="mb-4 flex items-center gap-2">
          <button
            type="button"
            onClick={onShuffleQueue}
            disabled={queue.length < 2}
            aria-pressed={shuffleEnabled}
            className={`flex items-center gap-1 rounded-full border px-3 py-1.5 text-xs transition disabled:cursor-not-allowed disabled:opacity-40 ${
              shuffleEnabled
                ? "border-white/20 bg-white text-zinc-950 hover:bg-zinc-200"
                : "border-zinc-800 bg-zinc-950/60 text-zinc-300 hover:border-zinc-700 hover:bg-zinc-900"
            }`}
          >
            <Shuffle className="h-3.5 w-3.5" />
            Shuffle
          </button>
          <button
            type="button"
            onClick={onClearQueue}
            disabled={queue.length === 0}
            className="flex items-center gap-1 rounded-full border border-zinc-800 bg-zinc-950/60 px-3 py-1.5 text-xs text-zinc-300 transition hover:border-zinc-700 hover:bg-zinc-900 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Clear
          </button>
        </div>

        {queue.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-zinc-800 bg-zinc-950/50 px-4 py-5 text-sm text-zinc-500">
            Queue is empty.
          </p>
        ) : (
          <div
            style={queueListHeight ? { height: queueListHeight } : undefined}
            className={isLyricsOpen ? "space-y-2 overflow-y-auto pr-1" : "space-y-2"}
          >
            {queue.map((song, index) => (
              <div
                key={`${song.id}-${index}`}
                className="relative overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3 ourmusic-animate-fade-up"
                style={{ animationDelay: `${index * 16}ms` }}
              >
                <Image
                  src={songArtworkUrl(song.id)}
                  alt={song.title}
                  className="absolute inset-0 h-full w-full scale-110 object-cover object-center blur-xs"
                  width={500}
                  height={500}
                  loading="lazy"
                  unoptimized
                />
                <div className="absolute inset-0 bg-zinc-950/70" />

                <div className="relative z-10 flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{song.title}</p>
                    <p className="truncate text-xs text-zinc-300">{song.artist}</p>
                  </div>

                  <button
                    type="button"
                    onClick={(event) => {
                      event.stopPropagation();
                      onRemoveFromQueue(index);
                    }}
                    className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white backdrop-blur-md transition hover:border-white/20 hover:bg-white/15"
                    aria-label={`Remove ${song.title} from queue`}
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {isLyricsOpen ? (
        <section className="relative flex min-h-[260px] flex-1 overflow-hidden rounded-3xl border border-white/10 bg-zinc-950 shadow-[0_24px_70px_rgba(0,0,0,0.42)] ourmusic-animate-pop-in">
          {currentSong ? (
            <Image
              src={songArtworkUrl(currentSong.id)}
              alt=""
              className="absolute inset-0 h-full w-full scale-125 object-cover opacity-75 blur-2xl saturate-150"
              width={640}
              height={640}
              loading="lazy"
              unoptimized
            />
          ) : null}
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_0%,rgba(255,255,255,0.16),transparent_34%),linear-gradient(180deg,rgba(24,24,27,0.34),rgba(9,9,11,0.84)_46%,rgba(9,9,11,0.84))]" />

          <div className="relative z-20 flex min-h-0 w-full flex-col">
            <div className="h-[112px] shrink-0 px-5 pt-5">
              <h2 className="text-3xl font-semibold leading-8 text-white">Lyrics</h2>
              {currentSong ? (
                <div className="mt-3 min-w-0">
                  <p className="truncate text-sm font-medium leading-5 text-white/82">{currentSong.title}</p>
                  <p className="truncate text-sm leading-5 text-white/58">{currentSong.artist}</p>
                </div>
              ) : (
                <p className="mt-3 truncate text-sm leading-5 text-white/58">No song selected</p>
              )}
            </div>

            <div className="min-h-0 flex-1 px-5 pb-4">
              {currentSong === null ? (
                <div className="flex h-full min-h-0 items-center">
                  <p className="text-xl font-semibold leading-8 text-white/55">
                    Start a song to view lyrics.
                  </p>
                </div>
              ) : isLyricsLoading ? (
                <div className="flex h-full min-h-0 items-center">
                  <p className="text-xl font-semibold leading-8 text-white/65">
                    Loading lyrics...
                  </p>
                </div>
              ) : lyricsError ? (
                <div className="flex h-full min-h-0 items-center">
                  <p className="text-xl font-semibold leading-8 text-red-100/85">
                    {lyricsError}
                  </p>
                </div>
              ) : lyrics?.instrumental ? (
                <div className="flex h-full min-h-0 items-center">
                  <p className="text-xl font-semibold leading-8 text-white/65">
                    This track is marked instrumental.
                  </p>
                </div>
              ) : syncedLyrics.length > 0 ? (
                <div ref={lyricsScrollRef} className="no-scrollbar h-full min-h-0 overflow-y-auto pr-1">
                  <div className="space-y-5 pb-8 pt-2">
                    {syncedLyrics.map((line, index) => {
                      const isActive = index === activeLyricIndex;

                      return (
                        <button
                          type="button"
                          key={`${line.time}-${index}`}
                          data-lyric-index={index}
                          onClick={() => onSeekToTime(line.time)}
                          className={`block w-full text-left text-2xl font-bold leading-8 transition duration-300 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40 ${
                            isActive
                              ? "translate-x-0 text-white opacity-100 drop-shadow-[0_8px_22px_rgba(0,0,0,0.45)]"
                              : index < activeLyricIndex
                                ? "text-white/30 opacity-80"
                                : "text-white/52 opacity-95"
                          }`}
                          aria-label={`Seek to ${line.text}`}
                        >
                          {line.text}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ) : lyrics?.plainLyrics?.trim() ? (
                <div className="no-scrollbar h-full min-h-0 overflow-y-auto pr-1">
                  <p className="whitespace-pre-wrap text-xl font-semibold leading-8 text-white/82">
                    {lyrics.plainLyrics.trim()}
                  </p>
                </div>
              ) : (
                <div className="flex h-full min-h-0 items-center">
                  <p className="text-xl font-semibold leading-8 text-white/55">
                    No lyrics found for this song.
                  </p>
                </div>
              )}
            </div>
          </div>
        </section>
      ) : null}
    </aside>
  );
}

function parseSyncedLyrics(lrc: string): SyncedLyricLine[] {
  return lrc
    .split(/\r?\n/)
    .flatMap((line) => {
      const matches = [...line.matchAll(/\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]/g)];
      if (matches.length === 0) {
        return [];
      }

      const text = line.replace(/\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]/g, "").trim();
      if (!text) {
        return [];
      }

      return matches.map((match) => {
        const minutes = Number(match[1]);
        const seconds = Number(match[2]);
        const fraction = match[3] ?? "0";
        const milliseconds = Number(fraction.padEnd(3, "0").slice(0, 3));

        return {
          time: minutes * 60 + seconds + milliseconds / 1000,
          text,
        };
      });
    })
    .sort((a, b) => a.time - b.time);
}

function getActiveLyricIndex(lines: SyncedLyricLine[], playbackTime: number) {
  if (lines.length === 0 || !Number.isFinite(playbackTime)) {
    return -1;
  }

  let activeIndex = -1;
  for (let index = 0; index < lines.length; index += 1) {
    if (lines[index].time > playbackTime + 0.15) {
      break;
    }

    activeIndex = index;
  }

  return activeIndex;
}
