"use client";

import Image from "next/image";
import {
  Pause,
  Play,
  Repeat1,
  Repeat2,
  RepeatOff,
  SkipBack,
  SkipForward,
  Volume1,
  Volume2,
  VolumeX,
} from "lucide-react";
import type { RefObject } from "react";
import type { Song } from "../music-types";

type RepeatMode = "off" | "one" | "queue";

type PlayerBarProps = {
  currentSong: Song | null;
  isPlaying: boolean;
  isMuted: boolean;
  volume: number;
  progress: number;
  duration: number;
  progressPercent: number;
  volumePercent: number;
  playNonce: number;
  hasQueuedSongs: boolean;
  repeatMode: RepeatMode;
  playerBarRef: RefObject<HTMLDivElement | null>;
  volumeControlRef: RefObject<HTMLDivElement | null>;
  audioRef: RefObject<HTMLAudioElement | null>;
  onPreviousTrack: () => void;
  onTogglePlay: () => void;
  onCycleRepeatMode: () => void;
  onNextTrack: () => void;
  onSeek: (value: string) => void;
  onToggleMute: () => void;
  onVolumeChange: (value: string) => void;
  onVolumeHoverChange: (isHovered: boolean) => void;
  onEnded: () => void;
  onLoadedMetadata: (duration: number) => void;
  onTimeUpdate: (currentTime: number) => void;
  onPlay: () => void;
  onPause: () => void;
  onSeeked: (currentTime: number) => void;
  onVolumeUpdate: (volume: number, muted: boolean) => void;
};

export function PlayerBar({
  currentSong,
  isPlaying,
  isMuted,
  volume,
  progress,
  duration,
  progressPercent,
  volumePercent,
  playNonce,
  hasQueuedSongs,
  repeatMode,
  playerBarRef,
  volumeControlRef,
  audioRef,
  onPreviousTrack,
  onTogglePlay,
  onCycleRepeatMode,
  onNextTrack,
  onSeek,
  onToggleMute,
  onVolumeChange,
  onVolumeHoverChange,
  onEnded,
  onLoadedMetadata,
  onTimeUpdate,
  onPlay,
  onPause,
  onSeeked,
  onVolumeUpdate,
}: PlayerBarProps) {
  return (
    <div ref={playerBarRef} className="relative z-50 mt-4 w-full">
      <div className="w-full rounded-[24px] border border-white/10 bg-zinc-950/75 px-3 py-3 shadow-[0_24px_60px_rgba(0,0,0,0.45)] backdrop-blur-xl sm:px-4 sm:py-4">
        <div className="grid gap-3 lg:grid-cols-[240px_minmax(0,1.6fr)_160px] lg:items-center">
          <div className="min-w-0 lg:justify-self-start">
            <div className="flex min-w-0 items-center gap-3 sm:gap-4">
              {currentSong ? (
                <>
                  <Image
                    src={`http://192.168.1.76:8808/api/songs/${currentSong.id}/artwork`}
                    alt={currentSong.title}
                    className="h-12 w-12 shrink-0 rounded-2xl object-cover ring-1 ring-white/10 sm:h-14 sm:w-14"
                    width={56}
                    height={56}
                    loading="lazy"
                    unoptimized
                  />

                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-white">{currentSong.title}</p>
                    <p className="truncate text-xs text-zinc-400">{currentSong.artist}</p>
                  </div>
                </>
              ) : (
                <div className="flex min-w-0 items-center gap-3 sm:gap-4">
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-dashed border-zinc-700 bg-zinc-900/60 text-zinc-500 sm:h-14 sm:w-14">
                    <Play className="h-4 w-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-white">Nothing is playing</p>
                    <p className="truncate text-xs text-zinc-400">Pick a song to start playback</p>
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="grid min-w-0 items-center gap-4 lg:justify-self-center">
            <div className="flex items-center justify-center gap-2.5 sm:gap-3">
              <button
                type="button"
                onClick={onPreviousTrack}
                disabled={currentSong === null}
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-zinc-300 transition hover:bg-white/10 hover:text-white disabled:cursor-not-allowed disabled:opacity-40 sm:h-10 sm:w-10"
                aria-label="Previous track"
              >
                <SkipBack className="h-4 w-4" />
              </button>

              <button
                type="button"
                onClick={onTogglePlay}
                disabled={currentSong === null && !hasQueuedSongs}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500 sm:h-11 sm:w-11"
                aria-label={
                  currentSong
                    ? isPlaying
                      ? "Pause"
                      : "Play"
                    : hasQueuedSongs
                      ? "Play queued song"
                      : "Nothing is playing"
                }
              >
                {currentSong ? (
                  isPlaying ? <Pause className="h-4 w-4 fill-current" /> : <Play className="h-4 w-4 fill-current" />
                ) : (
                  <Play className="h-4 w-4 fill-current" />
                )}
              </button>

              <button
                type="button"
                onClick={onNextTrack}
                disabled={currentSong === null}
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-zinc-300 transition hover:bg-white/10 hover:text-white disabled:cursor-not-allowed disabled:opacity-40 sm:h-10 sm:w-10"
                aria-label="Next track"
              >
                <SkipForward className="h-4 w-4" />
              </button>

              <button
                type="button"
                onClick={onCycleRepeatMode}
                disabled={currentSong === null && !hasQueuedSongs}
                className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full transition sm:h-9 sm:w-9 ${
                  repeatMode !== "off"
                    ? "bg-white text-zinc-950 hover:bg-zinc-200"
                    : "text-zinc-300 hover:bg-white/10 hover:text-white"
                } disabled:cursor-not-allowed disabled:opacity-40`}
                aria-label={
                  repeatMode === "off"
                    ? "Repeat off"
                    : repeatMode === "one"
                      ? "Repeat one"
                      : "Repeat queue"
                }
                aria-pressed={repeatMode !== "off"}
              >
                {repeatMode === "off" ? (
                  <RepeatOff className="h-3.5 w-3.5" />
                ) : repeatMode === "one" ? (
                  <Repeat1 className="h-3.5 w-3.5" />
                ) : (
                  <Repeat2 className="h-3.5 w-3.5" />
                )}
              </button>
            </div>

            <div className="grid grid-cols-[24px_minmax(0,1fr)_24px] items-center gap-2.5 sm:grid-cols-[32px_minmax(0,1fr)_32px] sm:gap-3">
              <span className="text-right text-[10px] tabular-nums text-zinc-400 sm:text-[11px]">{formatTime(progress)}</span>

              <input
                type="range"
                min={0}
                max={duration || 0}
                step="0.1"
                value={progress}
                onChange={(e) => onSeek(e.target.value)}
                style={{ ["--range-progress" as never]: `${progressPercent}%` }}
                disabled={currentSong === null}
                className="apple-range h-3 w-lg cursor-pointer"
              />

              <span className="text-left text-[10px] tabular-nums text-zinc-400 sm:text-[11px]">{formatTime(duration)}</span>
            </div>
          </div>

          <div
            ref={volumeControlRef}
            onMouseEnter={() => onVolumeHoverChange(true)}
            onMouseLeave={() => onVolumeHoverChange(false)}
            className="hidden items-center justify-end gap-3 lg:flex lg:justify-self-end"
          >
            <button
              type="button"
              onClick={onToggleMute}
              disabled={currentSong === null}
              className="flex h-10 w-10 items-center justify-center rounded-full text-zinc-300 transition hover:bg-white/10 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
              aria-label={isMuted ? "Unmute" : "Mute"}
            >
              {isMuted || volume === 0 ? (
                <VolumeX className="h-4 w-4" />
              ) : volume < 0.5 ? (
                <Volume1 className="h-4 w-4" />
              ) : (
                <Volume2 className="h-4 w-4" />
              )}
            </button>
            <input
              type="range"
              min={0}
              max={1}
              step="0.01"
              value={volume}
              onChange={(e) => onVolumeChange(e.target.value)}
              style={{ ["--range-progress" as never]: `${volumePercent}%` }}
              disabled={currentSong === null}
              className="apple-range w-28 cursor-pointer"
            />
          </div>

          {currentSong ? (
            <audio
              ref={audioRef}
              className="hidden"
              autoPlay
              onEnded={onEnded}
              onLoadedMetadata={(e) => onLoadedMetadata(e.currentTarget.duration || 0)}
              onTimeUpdate={(e) => onTimeUpdate(e.currentTarget.currentTime)}
              onPlay={onPlay}
              onPause={onPause}
              onSeeked={(e) => onSeeked(e.currentTarget.currentTime)}
              onVolumeChange={(e) => onVolumeUpdate(e.currentTarget.volume, e.currentTarget.muted)}
              src={`http://192.168.1.76:8808/api/songs/${currentSong.id}/stream?v=${playNonce}`}
            />
          ) : null}
        </div>
      </div>
    </div>
  );
}

function formatTime(seconds: number) {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return "0:00";
  }

  const wholeSeconds = Math.floor(seconds);
  const minutes = Math.floor(wholeSeconds / 60);
  const remainingSeconds = wholeSeconds % 60;
  return `${minutes}:${remainingSeconds.toString().padStart(2, "0")}`;
}
