"use client";

import { Eye, EyeOff, Pencil, Plus, Trash2, UserRound } from "lucide-react";
import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import type { AdminUser } from "../music-types";
import { apiUrl, registerUser } from "../lib/auth";

type AdminUsersViewProps = {
  onUnauthorized?: () => void;
};

type UserRow = AdminUser & {
  password?: string;
};

async function fetchAdminUsers() {
  const response = await fetch(apiUrl("/api/admin/users"), {
    credentials: "include",
  });

  const data = (await response.json().catch(() => [])) as UserRow[];
  return { response, data };
}

export function AdminUsersView({ onUnauthorized }: AdminUsersViewProps) {
  const viewRef = useRef<HTMLDivElement | null>(null);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deletingUserId, setDeletingUserId] = useState<number | null>(null);
  const [editingUserId, setEditingUserId] = useState<number | null>(null);
  const [editingField, setEditingField] = useState<"username" | "password" | null>(null);
  const [draftValue, setDraftValue] = useState("");
  const [savingUserId, setSavingUserId] = useState<number | null>(null);
  const [visiblePasswordUserId, setVisiblePasswordUserId] = useState<number | null>(null);
  const [editError, setEditError] = useState<{
    userId: number;
    field: "username" | "password";
    message: string;
  } | null>(null);
  const [editErrorNonce, setEditErrorNonce] = useState(0);
  const [, setError] = useState<string | null>(null);
  const [, setMessage] = useState<string | null>(null);

  const adminCount = useMemo(() => users.filter((user) => user.isAdmin).length, [users]);

  const loadUsers = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const { response, data } = await fetchAdminUsers();

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to load users.");
      }

      setUsers(
        data.map((user) => ({
          id: user.id,
          username: user.username,
          isAdmin: Boolean(user.isAdmin),
        })),
      );
    } catch (fetchError) {
      setError(fetchError instanceof Error ? fetchError.message : "Something went wrong.");
    } finally {
      setIsLoading(false);
    }
  }, [onUnauthorized]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadUsers();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [loadUsers]);

  async function handleCreateUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setIsSubmitting(true);

    try {
      const { response, data } = await registerUser(username.trim(), password);

      if (!response.ok) {
        if (response.status === 409 || data.message === "exists") {
          throw new Error("Username already exists.");
        }

        throw new Error("Failed to create user.");
      }

      setMessage(`Created user ${username.trim()}.`);
      setUsername("");
      setPassword("");
      setIsFormOpen(false);
      await loadUsers();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Something went wrong.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDeleteUser(userId: number) {
    setError(null);
    setMessage(null);
    setDeletingUserId(userId);

    try {
      const response = await fetch(apiUrl(`/api/admin/users/${userId}`), {
        method: "DELETE",
        credentials: "include",
      });

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to delete user.");
      }

      setMessage("User deleted.");
      await loadUsers();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Something went wrong.");
    } finally {
      setDeletingUserId(null);
    }
  }

  function startEdit(user: AdminUser, field: "username" | "password") {
    if (user.isAdmin) {
      return;
    }

    setError(null);
    setMessage(null);
    setEditError(null);
    setEditErrorNonce((current) => current + 1);
    setEditingUserId(user.id);
    setEditingField(field);
    setDraftValue(field === "username" ? user.username : "");
  }

  function cancelEdit() {
    setEditingUserId(null);
    setEditingField(null);
    setDraftValue("");
    setEditError(null);
    setVisiblePasswordUserId(null);
  }

  function renderInlineEditor(user: AdminUser, field: "username" | "password", value: string) {
    const isEditing = editingUserId === user.id && editingField === field;
    const isEditFailed =
      editError?.userId === user.id && editError.field === field;
    const animationKey = isEditFailed ? `${editErrorNonce}-${user.id}-${field}` : "";
    const isPasswordField = field === "password";

    if (!isEditing) {
      return (
        <div className="inline-flex min-w-0 items-center gap-1.5 text-white">
          <span className="truncate">{value}</span>
          <button
            type="button"
            onClick={() => startEdit(user, field)}
            disabled={user.isAdmin}
            className="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full border border-white/5 bg-white/5 text-zinc-500 transition hover:border-white/10 hover:bg-white/10 hover:text-zinc-300 disabled:cursor-not-allowed disabled:opacity-50"
            aria-label={`Edit ${field} for ${user.username}`}
          >
            <Pencil className="h-3 w-3" />
          </button>
        </div>
      );
    }

    if (isPasswordField) {
      return (
        <div className="flex min-w-0 flex-col gap-2">
          <div className="flex min-w-0 items-center gap-1.5">
            <div className="relative min-w-0 flex-[0.85]">
              <input
                key={animationKey}
                type={visiblePasswordUserId === user.id ? "text" : "password"}
                value={draftValue}
                onChange={(event) => {
                  setDraftValue(event.target.value);
                  setEditError(null);
                }}
                className={`w-full rounded-2xl border bg-zinc-950/80 py-1.5 pl-3 pr-10 text-sm text-white outline-none transition focus:bg-zinc-950 ${
                  isEditFailed
                    ? "border-red-500/40 focus:border-red-400/60 animate-[admin-edit-shake_0.35s_ease-in-out_1]"
                    : "border-white/10 focus:border-white/20"
                }`}
                placeholder="Enter new password"
                autoFocus
              />
              <button
                type="button"
                onClick={() => setVisiblePasswordUserId((current) => (current === user.id ? null : user.id))}
                className="absolute inset-y-0 right-1 flex w-8 items-center justify-center rounded-full text-zinc-400 transition hover:bg-white/5 hover:text-zinc-200"
                aria-label={visiblePasswordUserId === user.id ? "Hide password" : "Show password"}
              >
                {visiblePasswordUserId === user.id ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
            <button
              type="button"
              onClick={() => saveEdit(user.id)}
              disabled={savingUserId === user.id}
              className="shrink-0 rounded-2xl bg-white px-2.5 py-2 text-xs font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {savingUserId === user.id ? "Saving..." : "Save"}
            </button>
            <button
              type="button"
              onClick={cancelEdit}
              className="shrink-0 rounded-2xl border border-zinc-700 bg-zinc-900 px-2.5 py-2 text-xs text-zinc-100 transition hover:bg-zinc-800"
            >
              Cancel
            </button>
          </div>
          {isEditFailed ? <p className="text-xs text-red-300">{editError.message}</p> : null}
        </div>
      );
    }

    return (
      <div className="flex min-w-0 flex-col gap-2">
        <div className="flex min-w-0 items-center gap-2">
          <input
            key={animationKey}
            type="text"
            value={draftValue}
            onChange={(event) => {
              setDraftValue(event.target.value);
              setEditError(null);
            }}
            className={`min-w-0 flex-1 rounded-2xl border bg-zinc-950/80 px-3 py-1.5 text-sm text-white outline-none transition focus:bg-zinc-950 ${
              isEditFailed
                ? "border-red-500/40 focus:border-red-400/60 animate-[admin-edit-shake_0.35s_ease-in-out_1]"
                : "border-white/10 focus:border-white/20"
            }`}
            placeholder="Enter new username"
            autoFocus
          />
          <button
            type="button"
            onClick={() => saveEdit(user.id)}
            disabled={savingUserId === user.id}
            className="rounded-2xl bg-white px-3 py-2 text-xs font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {savingUserId === user.id ? "Saving..." : "Save"}
          </button>
          <button
            type="button"
            onClick={cancelEdit}
            className="rounded-2xl border border-zinc-700 bg-zinc-900 px-3 py-2 text-xs text-zinc-100 transition hover:bg-zinc-800"
          >
            Cancel
          </button>
        </div>
        {isEditFailed ? <p className="text-xs text-red-300">{editError.message}</p> : null}
      </div>
    );
  }

  async function saveEdit(userId: number) {
    if (!editingField) {
      return;
    }

    const nextValue = draftValue.trim();
    if (nextValue === "") {
      setError("Value cannot be empty.");
      return;
    }

    setError(null);
    setMessage(null);
    setSavingUserId(userId);

    try {
      const response = await fetch(apiUrl(`/api/admin/users/${userId}/${editingField}`), {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify(editingField === "username" ? { username: nextValue } : { password: nextValue }),
      });

      if (response.status === 401 || response.status === 403) {
        onUnauthorized?.();
        return;
      }

      if (!response.ok) {
        const fieldLabel = editingField === "username" ? "username" : "password";
        const messageText = response.status === 500
          ? `Could not update ${fieldLabel}.`
          : `Failed to update ${fieldLabel}.`;
        setEditError({
          userId,
          field: editingField,
          message: messageText,
        });
        setEditErrorNonce((current) => current + 1);
        return;
      }

      setMessage(`${editingField === "username" ? "Username" : "Password"} updated.`);
      cancelEdit();
      await loadUsers();
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "Something went wrong.");
    } finally {
      setSavingUserId(null);
    }
  }

  return (
    <div ref={viewRef} className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/80 p-4 shadow-[0_0_0_1px_rgba(255,255,255,0.02)_inset]">
      <div className="shrink-0 ourmusic-animate-fade-up">
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.22em] text-zinc-500">Admin</p>
            <h2 className="mt-2 text-2xl font-semibold">Users</h2>
            <p className="mt-2 text-sm text-zinc-400">
              Manage library accounts and create new users from here.
            </p>
          </div>

          <button
            type="button"
            onClick={() => setIsFormOpen((current) => !current)}
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white px-4 py-2 text-sm font-medium text-zinc-950 transition hover:bg-zinc-200"
          >
            <Plus className="h-4 w-4" />
            {isFormOpen ? "Close form" : "Add new user"}
          </button>
        </div>

        <div className="mt-4 flex flex-wrap gap-2 text-xs text-zinc-400">
          <span className="rounded-full border border-zinc-800 bg-zinc-950/60 px-3 py-1">
            {users.length} users
          </span>
          <span className="rounded-full border border-zinc-800 bg-zinc-950/60 px-3 py-1">
            {adminCount} admins
          </span>
        </div>
      </div>

      {isFormOpen ? (
        <form className="mt-6 grid gap-4 rounded-3xl border border-zinc-800 bg-zinc-950/60 p-4 md:grid-cols-[1fr_1fr_auto] md:items-end ourmusic-animate-pop-in" onSubmit={handleCreateUser}>
          <label className="block space-y-2">
            <span className="text-sm font-medium text-zinc-200">Username</span>
            <input
              type="text"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-zinc-950/80 px-4 py-3 text-white outline-none transition focus:border-white/20 focus:bg-zinc-950"
              required
            />
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-zinc-200">Password</span>
            <input
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-zinc-950/80 px-4 py-3 text-white outline-none transition focus:border-white/20 focus:bg-zinc-950"
              required
            />
          </label>

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center justify-center rounded-2xl bg-white px-5 py-3 font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "Creating..." : "Create"}
          </button>
        </form>
      ) : null}

      <div className="mt-6 min-h-0 flex-1 overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950/40">
        <div className="h-full overflow-auto">
          <table className="min-w-full table-fixed divide-y divide-zinc-800">
            <thead className="sticky top-0 z-10 bg-zinc-950/90 backdrop-blur">
              <tr className="text-left text-xs uppercase tracking-[0.18em] text-zinc-500">
                <th className="w-[38%] px-4 py-3 font-semibold">User</th>
                <th className="w-[18%] px-4 py-3 font-semibold">Role</th>
                <th className="w-[32%] px-4 py-3 font-semibold">Password</th>
                <th className="w-[12%] px-4 py-3 font-semibold text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {isLoading ? (
                <tr>
                  <td className="px-4 py-5 text-sm text-zinc-400" colSpan={4}>
                    Loading users...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td className="px-4 py-5 text-sm text-zinc-400" colSpan={4}>
                    No users found.
                  </td>
                </tr>
              ) : (
                users.map((user, index) => (
                  <Fragment key={user.id}>
                    <tr
                      className="transition hover:bg-white/5 ourmusic-animate-fade-up"
                      style={{ animationDelay: `${index * 16}ms` }}
                    >
                      <td className="px-4 py-4 align-top">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-zinc-800 text-zinc-200">
                            <UserRound className="h-4 w-4" />
                          </div>
                          <div className="min-w-0 flex-1 space-y-1">
                            {renderInlineEditor(user, "username", user.username)}
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-4 align-top">
                        <span
                          className={`inline-flex min-w-24 items-center justify-center rounded-full px-2.5 py-1 text-xs font-medium uppercase tracking-[0.18em] ${
                            user.isAdmin
                              ? "border border-emerald-500/20 bg-emerald-500/10 text-emerald-200"
                              : "border border-zinc-700 bg-zinc-900 text-zinc-300"
                          }`}
                        >
                          {user.isAdmin ? "Admin" : "User"}
                        </span>
                      </td>
                      <td className="px-4 py-4 align-top">
                        {renderInlineEditor(user, "password", "••••••••")}
                      </td>
                      <td className="px-4 py-4 align-top text-right">
                        <button
                          type="button"
                          onClick={() => handleDeleteUser(user.id)}
                          disabled={deletingUserId === user.id || user.isAdmin}
                          className="inline-flex items-center gap-1 rounded-2xl border border-red-500/20 bg-red-500/10 px-3 py-2 text-xs font-medium text-red-200 transition hover:border-red-400/30 hover:bg-red-500/15 disabled:cursor-not-allowed disabled:opacity-50"
                          aria-label={`Delete ${user.username}`}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                          {deletingUserId === user.id ? "Deleting..." : "Delete"}
                        </button>
                      </td>
                    </tr>
                  </Fragment>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
