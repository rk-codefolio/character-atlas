"use client";

import { useEffect, useState } from "react";
import MultiChoices from "./choices";
import ReportButton from "./report";

const labels = { COMMENT: "コメント", BUILD: "ビルド", CONTENT: "攻略投稿" };

const selections = (details, field, legacy) => details[field] ?? (details[legacy] ? [details[legacy]] : []);
const newMatrix = () => ({ name: "", points: "" });

function newDraft() {
  return { title: "", body: "", details: { shell: "", matrices: [newMatrix(), newMatrix()], mainStats: ["", "", ""] } };
}

function BuildFields({ details, options, change }) {
  const { shell, matrices, mainStats } = details;
  const total = matrices.reduce((sum, row) => sum + Number(row.points || 0), 0);
  function updateMatrix(index, update) {
    change({ matrices: matrices.map((row, position) => position === index ? { ...row, ...update } : row) });
  }
  return <>
    <label>航海具 <span className="required">必須</span>
      <select required value={shell} onChange={event => change({ shell: event.target.value })}>
        <option value="">航海具を選ぶ</option>
        {options.shells.map(name => <option key={name}>{name}</option>)}
      </select>
    </label>
    <fieldset>
      <legend>航路紋 <span className="required">2種類以上</span></legend>
      <p className="caption">ポイントの入力は任意です。</p>
      {matrices.map((row, index) => {
        const thresholds = options.matrices.find(matrix => matrix.name === row.name)?.thresholds ?? [];
        const reached = thresholds.filter(points => points <= Number(row.points)).at(-1);
        return <div key={index} className="matrix-row">
          <label>航路紋 {index + 1}
            <select required value={row.name} onChange={event => updateMatrix(index, { name: event.target.value })}>
              <option value="">名称を選ぶ</option>
              {options.matrices.map(matrix => <option key={matrix.name} disabled={matrices.some((other, position) =>
                position !== index && other.name === matrix.name)}>{matrix.name}</option>)}
            </select>
          </label>
          <label>ポイント {index + 1}
            <input type="number" min="0" step="1" value={row.points ?? ""}
              onChange={event => updateMatrix(index, { points: event.target.value })} />
          </label>
          <span className="stage" aria-live="polite">{row.name && row.points != null && row.points !== "" && (reached ? reached + "ポイント段階" : "未発動")}</span>
          {matrices.length > 2 && <button type="button" aria-label={`航路紋 ${index + 1} を外す`}
            onClick={() => change({ matrices: matrices.filter((_, position) => position !== index) })}>外す</button>}
        </div>;
      })}
      <div className="form-actions">
        <button type="button" disabled={matrices.length >= 9} onClick={() =>
          change({ matrices: [...matrices, newMatrix()] })}>＋ 航路紋を追加</button>
        {matrices.some(row => row.points != null && row.points !== "") &&
          <span aria-live="polite" className="caption">入力したポイントの合計 {total}</span>}
      </div>
    </fieldset>
    <fieldset>
      <legend>装具スロットのメインステータス <span className="required">必須</span></legend>
      <div className="core-fields">
        {["④", "⑤", "⑥"].map((label, index) => <label key={label}>コア {label}
          <select required value={mainStats[index]} onChange={event =>
            change({ mainStats: mainStats.map((stat, position) => position === index ? event.target.value : stat) })}>
            <option value="">選択</option>
            {options.mainStats.map(stat => <option key={stat}>{stat}</option>)}
          </select>
        </label>)}
      </div>
    </fieldset>
  </>;
}

export default function Posts({ kind, subject, subjectName, options, moderate = false, postId }) {
  const [posts, setPosts] = useState(null);
  const [draft, setDraft] = useState(null);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [characters, setCharacters] = useState([]);
  const composingContent = kind === "CONTENT" && !!draft;

  useEffect(() => {
    if (!composingContent) return;
    const controller = new AbortController();
    fetch("/api/characters", { signal: controller.signal })
      .then(response => response.ok ? response.json() : [])
      .then(setCharacters).catch(() => {});
    return () => controller.abort();
  }, [composingContent]);

  useEffect(() => {
    const controller = new AbortController();
    const query = new URLSearchParams({ kind, subject });
    fetch("/api/posts?" + query, { signal: controller.signal })
      .then(response => {
        if (!response.ok) throw new Error();
        return response.json();
      })
      .then(rows => setPosts(rows.map(row => ({ ...row, details: JSON.parse(row.details) }))))
      .catch(() => { if (!controller.signal.aborted) setError("投稿を読み込めませんでした。"); });
    return () => controller.abort();
  }, [kind, subject, revision]);

  function open(post) {
    setEditing(post?.id ?? null);
    if (post && kind === "BUILD" && post.details.matrices.length < 2) {
      setDraft({ ...post, details: { ...post.details, matrices: [...post.details.matrices, newMatrix()] } });
    } else {
      setDraft(post ?? newDraft());
    }
    setError("");
  }

  function changeDetails(update) {
    setDraft(value => ({ ...value, details: { ...value.details, ...update } }));
  }

  async function save(event) {
    event.preventDefault();
    if (busy) return;
    const { title, body, details } = draft;
    const contentTags = selections(details, "contentTags", "contentTag");
    const payload = kind === "BUILD" ? {
      shell: details.shell, mainStats: details.mainStats, targetStats: details.targetStats ?? "", contentTags,
      matrices: details.matrices.map(row => ({ name: row.name,
        points: row.points == null || row.points === "" ? null : Number(row.points) })),
    } : kind === "CONTENT" ? {
      stage: details.stage ?? "", relatedCharacters: selections(details, "relatedCharacters", "relatedCharacter"),
    } : { contentTags };
    setBusy(true);
    setError("");
    try {
      const response = await fetch("/api/posts" + (editing ? "/" + editing : ""), {
        method: editing ? "PUT" : "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ kind, subject, title, body, details: payload }),
      });
      if (!response.ok) throw new Error();
      setDraft(null);
      setRevision(value => value + 1);
    } catch {
      setError("保存できませんでした。入力内容と接続状態を確認してください。");
    } finally {
      setBusy(false);
    }
  }

  async function remove(id) {
    setBusy(true);
    setError("");
    try {
      const response = await fetch("/api/posts/" + id, { method: "DELETE" });
      if (!response.ok) throw new Error();
      setDeleting(null);
      setRevision(value => value + 1);
    } catch {
      setError("削除できませんでした。");
    } finally {
      setBusy(false);
    }
  }

  const visiblePosts = postId ? posts?.filter(post => post.id === postId) : posts;
  return <section className="posts">
    <div className="section-heading">
      <h2>{labels[kind]}</h2>
      {!draft && !moderate && <button className="primary" onClick={() => open()} disabled={!options}>＋ {labels[kind]}を書く</button>}
    </div>
    {draft && <form className="post-form" onSubmit={save}>
      <h3>{editing ? "投稿を編集" : labels[kind] + "を書く"}</h3>
      <p className="caption">投稿先　{subjectName}</p>
      {kind !== "COMMENT" && <label>タイトル <span className="required">必須</span>
        <input required maxLength={120} value={draft.title}
          onChange={event => setDraft({ ...draft, title: event.target.value })} />
      </label>}
      {kind === "BUILD" && <BuildFields details={draft.details} options={options} change={changeDetails} />}
      {kind === "BUILD" && <label>目標ステータス
        <input value={draft.details.targetStats ?? ""} maxLength={240} placeholder="速度 180、効果命中 60% など"
          onChange={event => changeDetails({ targetStats: event.target.value })} />
      </label>}
      {kind !== "CONTENT" && <MultiChoices label="コンテンツタグ" items={options.contents}
        selected={selections(draft.details, "contentTags", "contentTag")} apply={contentTags => changeDetails({ contentTags })} />}
      {kind === "CONTENT" && <>
        <label>ステージ・難易度
          <input value={draft.details.stage ?? ""} maxLength={120} onChange={event => changeDetails({ stage: event.target.value })} />
        </label>
        <MultiChoices label="関連渡り手" items={characters}
          selected={selections(draft.details, "relatedCharacters", "relatedCharacter")}
          apply={relatedCharacters => changeDetails({ relatedCharacters })} />
        <div className="guide">
          <p>攻略の要点・編成やビルド・立ち回り・注意点を、必要なところから記入できます。</p>
          <button type="button" disabled={!!draft.body.trim()} onClick={() =>
            setDraft({ ...draft, body: options.template.map(heading => "【" + heading + "】").join("\n\n") })}>フォーマットを入力する</button>
        </div>
      </>}
      <label>{kind === "BUILD" ? "用途・解説" : "本文"} {kind !== "BUILD" && <span className="required">必須</span>}
        <textarea required={kind !== "BUILD"} rows={kind === "CONTENT" ? 9 : 4} maxLength={8000} value={draft.body}
          placeholder={kind === "BUILD" ? "得意なコンテンツ、採用理由、使い方など" :
            kind === "CONTENT" ? "使った渡り手のビルド（航海具・航路紋・目標ステータスなど）や立ち回りを書いてください。" : undefined}
          onChange={event => setDraft({ ...draft, body: event.target.value })} />
      </label>
      <div className="form-actions">
        <button className="primary" type="submit" disabled={busy}>{editing ? "変更を保存" : "投稿する"}</button>
        <button type="button" disabled={busy} onClick={() => setDraft(null)}>キャンセル</button>
      </div>
    </form>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {!posts && !error && <p className="message">読み込み中…</p>}
    {visiblePosts?.length === 0 && <p className="message">まだ投稿はありません。</p>}
    <ul className="records post-list">
      {visiblePosts?.map(post => <li className="search-row post-card" key={post.id}>
        {post.title && <h3>{post.title}</h3>}
        {kind === "BUILD" && <p className="caption">{post.details.shell} · {post.details.matrices.map(row => row.name + (row.points == null || row.points === "" ? "" : " " + row.points + "pt")).join("／")}
          <br />装具スロット④・⑤・⑥　{post.details.mainStats.join("／")}</p>}
        {kind === "BUILD" && post.details.targetStats && <p className="caption">目標ステータス　{post.details.targetStats}</p>}
        {kind === "CONTENT" && post.details.stage && <p className="caption">{post.details.stage}</p>}
        <div className="post-tags">
          {post.relatedCharacterNames?.map((name, index) => <span key={index}>{name}</span>)}
          {selections(post.details, "contentTags", "contentTag").map(id => {
            const content = options?.contents.find(content => content.id === id);
            return content && <span key={id}>{content.name}</span>;
          })}
        </div>
        <p className="post-body">{post.body}</p>
        {!moderate && <ReportButton postId={post.id} />}
        {moderate && (deleting === post.id ? <div className="form-actions">
          <span>この投稿を削除しますか</span>
          <button disabled={busy} onClick={() => remove(post.id)}>削除する</button>
          <button disabled={busy} onClick={() => setDeleting(null)}>キャンセル</button>
        </div> : <div className="form-actions">
          <button onClick={() => open(post)} disabled={busy || !options}>編集</button>
          <button onClick={() => setDeleting(post.id)} disabled={busy}>削除</button>
        </div>)}
      </li>)}
    </ul>
  </section>;
}
