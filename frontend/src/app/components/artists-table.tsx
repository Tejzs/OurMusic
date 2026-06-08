"use client";

import type { Artist } from "../music-types";

type ArtistsTableProps = {
  artists: Artist[];
  hasMore: boolean;
  isLoading: boolean;
  onOpenArtist: (artist: Artist) => void;
  onLoadMore: () => void;
};

export function ArtistsTable({
  artists,
  hasMore,
  isLoading,
  onOpenArtist,
  onLoadMore,
}: ArtistsTableProps) {
  const filteredArtists = artists;

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/80" data-motion-list>
      <div className="border-b border-zinc-800 px-5 py-4">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="text-lg font-semibold">Artists</h2>
            <p className="text-sm text-zinc-500">Select an artist to open albums and songs.</p>
          </div>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden">
        <table className="w-full table-fixed border-collapse">
          <thead className="sticky top-0 z-10 bg-zinc-950/90 text-left text-xs uppercase tracking-[0.18em] text-zinc-500 backdrop-blur">
            <tr>
              <th className="w-[55%] px-5 py-3 font-medium">Artist</th>
              <th className="w-[15%] px-5 py-3 font-medium">Albums</th>
              <th className="w-[15%] px-5 py-3 font-medium">Songs</th>
              <th className="w-[15%] px-5 py-3 font-medium">Open</th>
            </tr>
          </thead>
          <tbody>
            {filteredArtists.map((artist) => (
              <tr
                key={artist.id}
                onClick={() => onOpenArtist(artist)}
                data-motion-item
                className="cursor-pointer border-t border-zinc-800 transition hover:bg-zinc-800/70"
              >
                <td className="px-5 py-4">
                  <div className="flex items-center gap-4">
                    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-zinc-800 text-sm font-semibold text-zinc-300">
                      {artist.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <strong className="block truncate text-sm font-medium">{artist.name}</strong>
                      <p className="truncate text-xs text-zinc-500">Artist library</p>
                    </div>
                  </div>
                </td>
                <td className="px-5 py-4 text-sm text-zinc-300">{artist.albumCount}</td>
                <td className="px-5 py-4 text-sm text-zinc-300">{artist.songCount}</td>
                <td className="px-5 py-4 text-sm text-zinc-500">View</td>
              </tr>
            ))}
            {filteredArtists.length === 0 && (
              <tr className="border-t border-zinc-800">
                <td className="px-5 py-6 text-sm text-zinc-500" colSpan={4}>
                  No artists available.
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {hasMore && (
          <div className="border-t border-zinc-800 px-5 py-4">
            <button
              type="button"
              onClick={onLoadMore}
              disabled={isLoading}
              className="w-full rounded-2xl border border-zinc-800 bg-zinc-950/70 px-4 py-3 text-sm font-medium text-white transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isLoading ? "Loading..." : "Load more"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
