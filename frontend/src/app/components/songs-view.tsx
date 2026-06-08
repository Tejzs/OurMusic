"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useLayoutEffect, useRef } from "react";
import type { Song } from "../music-types";
import { SongsGrid } from "./songs-grid";

type SongsViewProps = {
  songs: Song[];
  page: number;
  scrollKey: string;
  hasPreviousPage: boolean;
  hasNextPage: boolean;
  isLoading: boolean;
  onPreviousPage: () => void;
  onNextPage: () => void;
  onPlaySong: (song: Song) => void;
  onAddToQueue: (song: Song) => void;
  onViewportMeasure: (size: { width: number; height: number }) => void;
};

export function SongsView({
  songs,
  page,
  scrollKey,
  hasPreviousPage,
  hasNextPage,
  isLoading,
  onPreviousPage,
  onNextPage,
  onPlaySong,
  onAddToQueue,
  onViewportMeasure,
}: SongsViewProps) {
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const gridAreaRef = useRef<HTMLDivElement | null>(null);

  useLayoutEffect(() => {
    const el = gridAreaRef.current;
    if (!el) {
      return;
    }

    const updateSize = () => {
      const rect = el.getBoundingClientRect();
      onViewportMeasure({
        width: rect.width,
        height: rect.height,
      });
    };

    updateSize();

    const observer = new ResizeObserver(updateSize);
    observer.observe(el);

    return () => {
      observer.disconnect();
    };
  }, [onViewportMeasure, scrollKey]);

  return (
    <div className="flex min-h-0 flex-col overflow-hidden lg:h-full">
      <div className="shrink-0 mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Songs</h2>
          <p className="text-sm text-zinc-500">Page {page}</p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onPreviousPage}
            disabled={!hasPreviousPage || isLoading}
            className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm text-zinc-100 transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronLeft className="h-4 w-4" />
            Prev
          </button>

          <button
            type="button"
            onClick={onNextPage}
            disabled={!hasNextPage || isLoading}
            className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm text-zinc-100 transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Next
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>

      <div
        ref={viewportRef}
        data-scroll-key={scrollKey}
        className="flex min-h-0 flex-1 flex-col overflow-hidden px-0 py-3 lg:overflow-hidden"
      >
        <div ref={gridAreaRef} className="flex min-h-[60vh] flex-1 flex-col">
          {songs.length === 0 && !isLoading ? (
            <div className="flex min-h-[320px] flex-1 items-center justify-center rounded-[24px] border border-dashed border-zinc-800 bg-zinc-950/50 px-6 text-sm text-zinc-500">
              No songs found on this page.
            </div>
          ) : (
            <SongsGrid
              songs={songs}
              onPlaySong={onPlaySong}
              onAddToQueue={onAddToQueue}
            />
          )}
        </div>
      </div>
    </div>
  );
}
