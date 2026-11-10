"use client";

import { useEffect, useRef, useState } from "react";
import MultiChoices from "./choices";
import Posts from "./posts";
import Portrait from "./portrait";
import Admin from "./admin";
import Link from "next/link";
import { EffectDetails, ProgressPopover } from "./progress";

const STATS = {
  SPEED: "速度", HP: "HP", ATTACK: "攻撃力", DEFENCE: "防御力",
  CRITICAL_RATE: "会心率", CRITICAL_DAMAGE: "会心ダメージ", EFFECT_HIT: "効果命中", EFFECT_RESIST: "効果抵抗",
};

const formatStat = (key, value) => {
  if (value == null) return "未確認";
  return ["CRITICAL_RATE", "CRITICAL_DAMAGE", "EFFECT_HIT", "EFFECT_RESIST"].includes(key)
    ? new Intl.NumberFormat("ja", { style: "percent", maximumFractionDigits: 1 }).format(value)
    : new Intl.NumberFormat("ja", { maximumFractionDigits: 1 }).format(value);
};

async function get(route, params, signal) {
  const suffix = params ? "?" + new URLSearchParams(params) : "";
  const response = await fetch("/api/" + route + suffix, { signal });
  if (response.status === 404) throw new Error("該当データが見つかりません。");
  if (!response.ok) throw new Error("データを読み込めませんでした。APIの起動状態を確認してください。");
  return response.json();
}

function useCatalog(route, params) {
  const [state, setState] = useState(null);
  const query = JSON.stringify(params ?? null);
  const key = route + ":" + query;
  useEffect(() => {
    if (!route) return;
    const controller = new AbortController();
    get(route, JSON.parse(query), controller.signal)
      .then(value => setState({ key, value, error: null }))
      .catch(error => {
        if (!controller.signal.aborted) setState({ key, value: null, error: error.message });
      });
    return () => controller.abort();
  }, [route, query, key]);
  return state?.key === key ? state : { value: null, error: null };
}

function Status({ state, emptyText = "該当する渡り手がいません。" }) {
  if (state.error) return <p className="message" role="alert">{state.error}</p>;
  if (!state.value) return <p className="message" role="status">読み込み中…</p>;
  if (Array.isArray(state.value) && !state.value.length) return <p className="message">{emptyText}</p>;
  return null;
}

function Awakening({ awakened, setAwakened }) {
  return <label>育成状態<select value={awakened ? "after" : "before"} onChange={event => setAwakened(event.target.value === "after")}>
    <option value="after">最大覚醒後</option><option value="before">覚醒前</option>
  </select></label>;
}

function CharacterBadges({ character, options }) {
  if (!options) return null;
  const attribute = options.attributes?.[character.attribute];
  return <span className="character-badges">
    <span className="attribute-badge" data-attribute={attribute ? character.attribute : undefined}>
      {attribute ?? "属性未確認"}
    </span>
    {options.advancedIds?.includes(character.id) && <span className="advanced-badge">星航級</span>}
  </span>;
}

function CharacterList({ options, initial }) {
  const initialStat = Object.hasOwn(STATS, initial.stat) ? initial.stat : "";
  const [filters, setFilters] = useState({
    name: initial.name, stat: initialStat,
    direction: initialStat ? initial.direction ?? "DESC" : "DESC",
    archiveDirection: initialStat ? "ASC" : initial.direction ?? "ASC",
  });
  const { name, stat, direction, archiveDirection } = filters;
  const state = useCatalog("characters", [...(name ? [["name", name]] : []),
    ...(stat ? [["stat", stat], ["direction", direction]] : [["archiveDirection", archiveDirection]])]);
  function update(change) {
    const next = { ...filters, ...change };
    setFilters(next);
    const query = new URLSearchParams();
    if (next.name) query.set("name", next.name);
    if (next.stat) {
      query.set("stat", next.stat);
      query.set("direction", next.direction);
    } else if (next.archiveDirection !== "ASC") {
      query.set("direction", next.archiveDirection);
    }
    window.history.replaceState(null, "", query.size ? "/?" + query : "/");
  }
  return <>
    <h1>渡り手一覧</h1>
    <p className="caption">航海ランク6・Lv.50・装備なし</p>
    <div className="controls">
      <label>渡り手名<input type="search" maxLength={80} value={name} onChange={event => update({ name: event.target.value })} /></label>
      <label>並び替え<select value={stat} onChange={event => update({ stat: event.target.value })}>
        <option value="">図鑑順</option>{Object.entries(STATS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
      </select></label>
      <label>順序<select value={stat ? direction : archiveDirection}
        onChange={event => update({ [stat ? "direction" : "archiveDirection"]: event.target.value })}>
        <option value="DESC">降順</option><option value="ASC">昇順</option>
      </select></label>
    </div>
    <Status state={state} />
    <ul className="character-grid">{state.value?.map(character => <li key={character.id}>
      <Link className="character-card" href={"/characters/" + encodeURIComponent(character.id)} prefetch={false}>
        <Portrait character={character} />
        <span className="card-copy"><strong>{character.name}</strong>
          <CharacterBadges character={character} options={options} />
          {stat && <span className="caption">{STATS[stat]} {formatStat(stat, character.stats[stat])}</span>}
        </span>
      </Link>
    </li>)}</ul>
  </>;
}

const ROLE_ORDER = ["攻撃", "防御・保護", "サポート", "妨害"];
const tagNames = { BUFF: "バフ", DEBUFF: "デバフ", CD: "CD", EFFECT: "その他の効果" };
const effectKinds = { Buff: "BUFF", Debuff: "DEBUFF", Effect: "EFFECT" };
const tagAliases = { "CD増加": ["CD延長"] };

function ConditionPicker({ options, selected, apply }) {
  const dialog = useRef(null);
  const [draft, setDraft] = useState([]);
  const [filter, setFilter] = useState("");
  const visible = options.filter(option =>
    [option.label, ...(tagAliases[option.label] ?? [])].some(name =>
      name.normalize("NFKC").toLowerCase().includes(filter.normalize("NFKC").trim().toLowerCase())));
  function open() {
    setDraft(selected);
    setFilter("");
    dialog.current.showModal();
  }
  return <>
    <button className="primary" onClick={open} disabled={!options.length}>＋ 条件を追加</button>
    <dialog ref={dialog} aria-labelledby="condition-title">
      <div className="section-heading"><h2 id="condition-title">検索条件を選ぶ</h2>
        <button aria-label="条件追加を閉じる" onClick={() => dialog.current.close()}>×</button></div>
      <div className="controls">
        <label>条件名で検索<input type="search" value={filter} onChange={event => setFilter(event.target.value)} /></label>
      </div>
      <div className="condition-groups">
        {Object.entries(tagNames).map(([kind, label]) => {
          const group = visible.filter(option => option.kind === kind);
          if (!group.length) return null;
          return <section key={kind} aria-label={label}>
            <h3>{label}</h3>
            <div className="picker-results">{group.map(option => <button key={option.label} type="button"
              className={"term " + kind.toLowerCase()} aria-pressed={draft.includes(option.label)}
              onClick={() => setDraft(current => current.includes(option.label)
                ? current.filter(value => value !== option.label) : [...current, option.label])}>
              {option.label}
            </button>)}</div>
          </section>;
        })}
        {!visible.length && <p className="message">該当する条件がありません。</p>}
      </div>
      <div className="form-actions">
        <button className="primary" onClick={() => { apply(draft); dialog.current.close(); }}>適用する（{draft.length}）</button>
        <button onClick={() => dialog.current.close()}>キャンセル</button>
      </div>
    </dialog>
  </>;
}

function searchParams({ mode, terms, groups, role, content }) {
  const query = new URLSearchParams();
  if (mode !== "AND") query.set("mode", mode);
  if (role) query.set("role", role);
  for (const id of terms) query.append("term", id);
  for (const label of groups) query.append("group", label);
  for (const id of content) query.append("content", id);
  return query;
}

function DetailedSearch({ options, initial }) {
  const terms = useCatalog("terms", [["shared", "true"]]);
  const allTerms = useCatalog("terms");
  const contentOptions = useCatalog("search-contents");
  const [filters, setFilters] = useState(initial);
  const { mode, role, content } = filters;
  const labels = new Map((allTerms.value ?? []).map(row => [row.id, row.label]));
  const selected = [...new Set([...filters.groups, ...filters.terms.map(id => labels.get(id)).filter(Boolean)])];
  const result = useCatalog(allTerms.value ? "search" : null, [...searchParams({
    ...filters, terms: filters.terms.filter(id => labels.has(id)),
  })]);
  function update(change) {
    const next = { ...filters, ...change };
    setFilters(next);
    const query = searchParams(next);
    window.history.replaceState(null, "", query.size ? "/search?" + query : "/search");
  }
  function setSelected(groups) {
    const directTerms = filters.terms.filter(id => groups.includes(labels.get(id)) && !filters.groups.includes(labels.get(id)));
    const directLabels = new Set(directTerms.map(id => labels.get(id)));
    update({ terms: directTerms, groups: groups.filter(label => !directLabels.has(label)) });
  }
  const available = new Map();
  for (const row of terms.value ?? []) {
    if (!available.has(row.label) || row.kind === "EFFECT") {
      const kind = row.kind === "TAG" ? "CD" :
        effectKinds[options?.effectKinds[row.label]] ?? "EFFECT";
      available.set(row.label, { label: row.label, kind });
    }
  }
  const searchOptions = [...available.values()].sort((a, b) => a.label.localeCompare(b.label, "ja"));
  return <>
    <h1>渡り手詳細検索</h1>
    <p className="caption">役割や、複数の渡り手が持つ効果・CD操作で絞り込めます。</p>
    <div className="controls search-controls">
      <ConditionPicker options={searchOptions} selected={selected} apply={setSelected} />
      <label>役割<select value={role} onChange={event => update({ role: event.target.value })}>
        <option value="">すべての役割</option>
        {ROLE_ORDER.filter(label => options?.roles[label]).map(label => <option key={label}>{label}</option>)}
      </select></label>
      <MultiChoices label="コンテンツ関連投稿" items={contentOptions.value ?? []} selected={content} apply={content => update({ content })} />
      <label>条件の組み合わせ<select value={mode} onChange={event => update({ mode: event.target.value })}>
        <option value="AND">すべて (AND)</option><option value="OR">いずれか (OR)</option>
      </select></label>
      <button type="button" onClick={() => update({ mode: "AND", role: "", content: [], terms: [], groups: [] })}>クリア</button>
    </div>
    {(terms.error || allTerms.error) && <Status state={terms.error ? terms : allTerms} />}
    <ul className="selection" aria-label="選択中の条件">{selected.map(label => <li key={label}>
      <button className={"term " + (available.get(label)?.kind.toLowerCase() ?? "")}
        onClick={() => setSelected(selected.filter(value => value !== label))} aria-label={`${label}を外す`}>{label} ×</button>
    </li>)}</ul>
    <Status state={result} />
    {result.value && <p className="caption" role="status">{result.value.length}件</p>}
    <ul className="records">{result.value?.map(row => {
      const hits = new Map();
      for (const hit of row.hits) {
        const label = labels.get(hit.termId);
        if (!label) continue;
        const key = hit.skillId + "/" + label;
        if (!hits.has(key) || hits.get(key).evidence === "RELATED_CANDIDATE") hits.set(key, { ...hit, label });
      }
      return <li key={row.character.id} className="search-row">
        <div className="search-identity"><Portrait character={row.character} />
          <div className="search-name"><Link className="record-link" href={"/characters/" + encodeURIComponent(row.character.id)}
            prefetch={false}>{row.character.name}</Link>
            <CharacterBadges character={row.character} options={options} />
            {row.candidateOnly && <span className="candidate">関連候補</span>}
          </div>
        </div>
        {hits.size > 0 && <ul className="hits">{[...hits.entries()].map(([key, hit]) => <li key={key}>
          {hit.skillName} · {hit.label}{hit.evidence === "RELATED_CANDIDATE" && "（候補）"}
        </li>)}</ul>}
      </li>;
    })}</ul>
  </>;
}

function Contents({ options }) {
  const [selected, setSelected] = useState(null);
  const [query, setQuery] = useState("");
  const search = query.normalize("NFKC").trim().toLowerCase();
  const matches = (options?.contents ?? []).filter(content => [content.name, ...content.aliases]
    .some(name => name.normalize("NFKC").toLowerCase().includes(search)));
  return <>
    <h1>コンテンツ一覧</h1>
    {selected ? <>
      <button className="back" onClick={() => setSelected(null)}>← コンテンツ一覧に戻る</button>
      <h2>{selected.name}</h2>
      <Posts key={selected.id} kind="CONTENT" subject={selected.id} subjectName={selected.name} options={options} />
    </> : <>
      <div className="controls"><label>コンテンツ名で探す<input type="search" value={query} onChange={event => setQuery(event.target.value)} /></label></div>
      {options && !matches.length && <p className="message">該当するコンテンツがありません。</p>}
      {["常設コンテンツ", "期間限定イベント"].map(kind => <section className="content-group" key={kind}>
      <h2>{kind}</h2>
      <ul className="records">{matches.filter(content => content.kind === kind).map(content => <li key={content.id}>
        <button className="record-link" onClick={() => setSelected(content)}>{content.name}</button>
      </li>)}</ul>
      </section>)}
    </>}
  </>;
}

function Detail({ id, options }) {
  const route = "characters/" + encodeURIComponent(id);
  const state = useCatalog(route);
  const progress = useCatalog(route + "/progress");
  const [awakened, setAwakened] = useState(true);
  const [tab, setTab] = useState("comments");
  const effects = useCatalog(tab === "skills" ? route + "/effects" : null);
  const character = state.value;
  const stats = awakened ? character?.maxAwakenedStats : character?.stats;
  return <>
    <Link href="/" className="back action-link">← 図鑑に戻る</Link>
    <Status state={state} />
    {character && <>
      <div className="profile-heading">
        <div><p className="archive-kicker">VOYAGER FILE</p><h1>{character.name}</h1>
          <CharacterBadges character={character} options={options} />
        </div>
        <Portrait character={character} detail />
      </div>
      {progress.value?.awakening && <div className="controls"><Awakening awakened={awakened} setAwakened={setAwakened} /></div>}
      <dl className="stats">{Object.entries(STATS).map(([key, label]) => <div key={key}>
        <dt>{label}</dt><dd>{formatStat(key, stats?.[key])}</dd>
      </div>)}</dl>
      <div className="tabs" aria-label="詳細の表示内容">
        <button aria-pressed={tab === "comments"} onClick={() => setTab("comments")}>コメント</button>
        <button aria-pressed={tab === "builds"} onClick={() => setTab("builds")}>ビルド</button>
        <button aria-pressed={tab === "skills"} onClick={() => setTab("skills")}>スキル</button>
      </div>
      {tab !== "skills" ? <Posts key={tab} kind={tab === "comments" ? "COMMENT" : "BUILD"}
        subject={id} subjectName={character.name} options={options} /> :
        <>{effects.error && <Status state={effects} />}
        <ul className="records">{character.skills.map(skill => {
          const info = progress.value?.skillInfo[skill.id];
          return <li key={skill.id} className="search-row">
          <div className="skill-heading">
            <h2>{skill.name} <small>Lv.{skill.level}</small></h2>
            {info?.passive && <span className="attribute-badge">パッシブ</span>}
            {info && <span className="caption">CD {info.cooldown == null ? "未確認" :
              info.cooldown === 0 ? "なし" : info.cooldown + "ターン"}</span>}
            {progress.value?.skills[skill.id] && <ProgressPopover label="レベル別の効果"
              rows={progress.value.skills[skill.id]} />}
          </div>
          <p>{skill.description ?? "このレベルの説明は未収録です。"}</p>
          <EffectDetails tips={effects.value?.[skill.id]} />
        </li>})}</ul></>}
    </>}
  </>;
}

export default function Atlas({ initialScreen: screen = "list", initialCharacter: character = null, listFilters, searchFilters }) {
  const [staff, setStaff] = useState(false);
  const options = useCatalog("review-options");
  const session = useCatalog("admin/session");
  useEffect(() => {
    setStaff(Boolean(session.value?.owner || session.value?.moderator));
  }, [session.value]);

  return <>
    <header className="archive-top"><strong><span aria-hidden="true">⬡</span> 風綴りの群島 <span aria-hidden="true">/</span> 渡り手図鑑</strong>
      <span>群島航海局</span></header>
    <main className="archive-layout">
      <nav className="archive-nav" aria-label="渡り手図鑑">
        <p className="archive-kicker">INDEX</p>
        {[["list", "/", "渡り手一覧"], ["search", "/search", "詳細検索"], ["contents", "/?contents=1", "コンテンツ一覧"]].map(([id, href, label]) =>
          <a key={id} href={href} aria-current={!character && screen === id ? "page" : undefined}>{label}</a>)}
        <Link href="/game-rules">ゲーム仕様</Link>
        {staff && <Link href="/?admin=1" aria-current={screen === "admin" ? "page" : undefined}>管理画面</Link>}
      </nav>
      <div className="archive-body">
        {!character && screen === "list" && <section className="archive-hero" aria-label="渡り手図鑑">
          <div className="archive-core" aria-hidden="true"><span /><span /><span /><b /></div>
          <p className="archive-kicker">HARBOR STATION / VOYAGER ARCHIVE</p>
          <p className="archive-title">渡り手図鑑</p>
          <p>渡り手のスキル、効果、ビルドを調べる。</p>
        </section>}
        <section className="panel">
          {!character && screen === "list" && <CharacterList initial={listFilters} options={options.value} />}
          {screen === "search" && <DetailedSearch initial={searchFilters} options={options.value} />}
          {screen === "contents" && <Contents options={options.value} />}
          {screen === "admin" && <Admin options={options.value} onLogout={() => setStaff(false)} />}
          {character && <Detail key={character} id={character} options={options.value} />}
        </section>
      </div>
    </main>
  </>;
}
