"use client";

import { useEffect, useState } from "react";
import Posts from "./posts";
import { reasons } from "./report";

const kinds = { COMMENT: "コメント", BUILD: "ビルド", CONTENT: "攻略投稿" };
const actions = { EDIT_POST: "投稿を編集", DELETE_POST: "投稿を削除", SEARCH_CONTENT: "検索候補を変更", RESOLVE_REPORT: "通報に対応" };
const loginMessages = {
  expired: "ログインの有効期限が切れました。もう一度お試しください。",
  cancelled: "Discord認証をキャンセルしました。",
  denied: "このDiscordアカウントには管理権限がありません。",
  error: "Discord認証に失敗しました。もう一度お試しください。",
};

export default function Admin({ options, onLogout }) {
  const [session, setSession] = useState(null);
  const [tab, setTab] = useState("posts");
  const [report, setReport] = useState(null);
  const [offset, setOffset] = useState(0);
  const [rows, setRows] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState("");
  const [loginNotice, setLoginNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  const allowed = session?.owner || session?.moderator;
  useEffect(() => {
    const query = new URLSearchParams(window.location.search);
    setLoginNotice(loginMessages[query.get("auth")] ?? "");
    const target = query.get("report");
    if (target && /^[0-9a-f-]{36}$/.test(target)) {
      setReport(target);
      setTab("reports");
      sessionStorage.setItem("atlas-report", target);
    } else if (query.has("admin") && !query.has("auth")) {
      const pending = sessionStorage.getItem("atlas-report");
      if (pending) {
        sessionStorage.removeItem("atlas-report");
        setReport(pending);
        setTab("reports");
      }
    }
    if (query.has("auth")) window.history.replaceState(null, "", "/?admin=1");
    const controller = new AbortController();
    fetch("/api/admin/session", { signal: controller.signal }).then(response => {
      if (!response.ok) throw new Error();
      return response.json();
    }).then(setSession).catch(() => {
      if (!controller.signal.aborted) setError("管理画面に接続できません。");
    });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!allowed || selected) return;
    const controller = new AbortController();
    const params = new URLSearchParams({ offset });
    if (tab === "reports" && report) params.set("report", report);
    const path = tab === "contents" ? "search-contents" : "admin/" + tab + "?" + params;
    fetch("/api/" + path, { signal: controller.signal }).then(response => {
      if (!response.ok) throw new Error();
      return response.json();
    }).then(setRows).catch(() => {
      if (!controller.signal.aborted) setError("管理データを読み込めません。");
    });
    return () => controller.abort();
  }, [allowed, tab, offset, selected, revision, report]);

  function navigate(next) {
    if (next === tab) return;
    setTab(next);
    setReport(null);
    setRows([]);
    setOffset(0);
    setSelected(null);
    setError("");
  }

  async function update(path, method, data) {
    setBusy(true);
    setError("");
    try {
      const response = await fetch("/api/admin/" + path, {
        method, headers: data ? { "Content-Type": "application/json" } : undefined,
        body: data ? JSON.stringify(data) : undefined,
      });
      if (!response.ok) throw new Error();
      if (path === "logout") {
        setSession({ configured: true, owner: false, moderator: false });
        onLogout();
        setSelected(null);
        setRows([]);
      } else {
        setRevision(value => value + 1);
      }
    } catch {
      setError("変更できませんでした。権限と接続を確認してください。");
    } finally {
      setBusy(false);
    }
  }

  const visible = new Set(rows.map(row => row.id));
  const pagination = tab !== "contents" && !selected && (offset > 0 || rows.length === 50);
  return <>
    <div className="section-heading"><h1>管理画面</h1>
      {allowed && <div className="form-actions"><span>{session.owner ? "オーナー" : "モデレーター"}</span>
        <button onClick={() => update("logout", "POST")} disabled={busy}>ログアウト</button></div>}
    </div>
    {loginNotice && <p className="message">{loginNotice}</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {!session && !error && <p className="message">読み込み中…</p>}
    {session && !allowed && <>
      <p className="message">投稿管理には、許可されたDiscordアカウントでのログインが必要です。</p>
      {session.configured ? <a className="action-link primary" href="/api/admin/discord/start">Discordでログイン</a> :
        <p className="caption">Discord認証の設定待ちです。</p>}
    </>}
    {allowed && <>
      <div className="tabs admin-tabs">
        <button aria-pressed={tab === "posts"} onClick={() => navigate("posts")}>投稿管理</button>
        <button aria-pressed={tab === "reports"} onClick={() => navigate("reports")}>通報</button>
        {session.owner && <>
          <button aria-pressed={tab === "contents"} onClick={() => navigate("contents")}>検索のコンテンツ候補</button>
          <button aria-pressed={tab === "logs"} onClick={() => navigate("logs")}>操作ログ</button>
        </>}
      </div>
      {tab === "posts" && (selected ? <>
        <button className="back" onClick={() => setSelected(null)}>← 投稿管理に戻る</button>
        <Posts key={selected.id} kind={selected.kind} subject={selected.subject} options={options}
          subjectName={selected.subjectName} moderate postId={selected.id} />
      </> : <ul className="records">{rows.map(row => <li className="search-row" key={row.id}>
        <button className="record-link" onClick={() => setSelected({
          ...row, subjectName: row.kind === "CONTENT" ? options?.contents.find(c => c.id === row.subject)?.name ?? row.subject : row.subjectName,
        })}>{row.title || row.excerpt || "投稿を開く"}</button>
        <p className="caption">{kinds[row.kind]} · {row.kind === "CONTENT"
          ? options?.contents.find(c => c.id === row.subject)?.name ?? row.subject : row.subjectName}</p>
      </li>)}</ul>)}
      {tab === "contents" && <>
        <p className="caption">詳細検索の「コンテンツ関連投稿」に表示する項目を選びます。</p>
        <ul className="records">{options?.contents.map(item => <li key={item.id}>
          <span>{item.name}</span>
          <button type="button" disabled={busy} aria-pressed={visible.has(item.id)}
            onClick={() => update("search-contents/" + item.id, "PUT", { visible: !visible.has(item.id) })}>
            {visible.has(item.id) ? "表示中" : "非表示"}
          </button>
        </li>)}</ul>
      </>}
      {tab === "reports" && <>
        {report && <button onClick={() => { setReport(null); window.history.replaceState(null, "", "/?admin=1"); }}>すべての通報を表示</button>}
        {!rows.length && <p className="caption">表示する通報がありません。</p>}
        <ul className="records">{rows.map(row => {
          const post = JSON.parse(row.snapshot);
          return <li key={row.id} className="search-row">
            <h3>{post.title || kinds[post.kind] || "投稿"}</h3>
            <p className="caption">{reasons[row.reason]} · {new Date(row.createdAt).toLocaleString("ja-JP")} · {row.resolvedAt ? "対応済み" : "未対応"}</p>
            {row.note && <p className="post-body">{row.note}</p>}
            <details>
              <summary>通報時の投稿を見る</summary>
              <p className="post-body">{post.body}</p>
              {post.details && Object.keys(post.details).length > 0 && <>
                <h4>通報時の投稿詳細</h4>
                <pre>{JSON.stringify(post.details, null, 2)}</pre>
              </>}
            </details>
            <div className="form-actions">
              {!row.resolvedAt && <button disabled={busy}
                onClick={() => update("reports/" + row.id, "PUT")}>対応済みにする</button>}
            </div>
          </li>;
        })}</ul>
      </>}
      {tab === "logs" && <ul className="records">{rows.map(row => <li className="search-row" key={row.id}>
        <details><summary>{actions[row.action]} · {new Date(row.at).toLocaleString("ja-JP")}</summary>
          <p>実行者 {row.actor}　対象 {row.target}</p>
          <h3>変更前</h3><pre>{JSON.stringify(JSON.parse(row.before), null, 2)}</pre>
          {row.after != null && <><h3>変更後</h3><pre>{JSON.stringify(JSON.parse(row.after), null, 2)}</pre></>}
        </details>
      </li>)}</ul>}
      {pagination && <div className="form-actions">
        <button disabled={offset === 0} onClick={() => setOffset(offset - 50)}>前の50件</button>
        <button disabled={rows.length < 50} onClick={() => setOffset(offset + 50)}>次の50件</button>
      </div>}
    </>}
  </>;
}
