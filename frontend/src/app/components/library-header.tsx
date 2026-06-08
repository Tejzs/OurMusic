"use client";

import { ScanSearch, Search } from "lucide-react";

type LibraryHeaderProps = {
  pattern: string;
  onPatternChange: (value: string) => void;
  onFullScan: () => void;
};

export function LibraryHeader({
  pattern,
  onPatternChange,
  onFullScan,
}: LibraryHeaderProps) {
  return (
    <div className="mb-6 shrink-0 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p className="text-sm uppercase tracking-[0.24em] text-zinc-500">Music Library</p>
        <h1 className="mt-2 text-3xl font-semibold sm:text-4xl">Browse and play</h1>
      </div>

      <div className="flex w-full flex-col gap-3 lg:max-w-2xl lg:flex-row">
        <div className="flex min-w-0 flex-1 items-center gap-3 rounded-2xl border border-zinc-800/90 bg-zinc-900/85 px-4 py-3 shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset] transition focus-within:border-zinc-600 focus-within:bg-zinc-900">
          <Search className="h-4 w-4 shrink-0 text-zinc-500" />
          <input
            className="min-w-0 flex-1 bg-transparent outline-none placeholder:text-zinc-500"
            type="search"
            placeholder="Search songs, artists, albums..."
            value={pattern}
            onChange={(e) => onPatternChange(e.target.value)}
          />
        </div>

        <button
          type="button"
          className="flex items-center gap-2 rounded-2xl border border-zinc-800/90 bg-zinc-900/85 px-5 py-3 font-medium text-white shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset] transition hover:border-zinc-700 hover:bg-zinc-800"
          onClick={onFullScan}
        >
          <ScanSearch className="h-4 w-4" />
          Scan
        </button>
      </div>
    </div>
  );
}
