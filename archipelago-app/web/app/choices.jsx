"use client";

import { useId, useRef, useState } from "react";

const normalize = text => text.normalize("NFKC").trim().toLowerCase();

export default function MultiChoices({ label, items, selected, apply }) {
  const title = useId();
  const dialog = useRef(null);
  const [draft, setDraft] = useState([]);
  const [query, setQuery] = useState("");
  const names = new Map(items.map(item => [item.id, item.name]));
  const groups = new Map();
  for (const item of items) {
    if (![item.name, ...(item.aliases ?? [])].some(name => normalize(name).includes(normalize(query)))) continue;
    const group = item.kind ?? "";
    if (!groups.has(group)) groups.set(group, []);
    groups.get(group).push(item);
  }
  function open() {
    setDraft(selected);
    setQuery("");
    dialog.current.showModal();
  }
  return <div className="multi-choices">
    <span>{label}</span>
    <button type="button" onClick={open} disabled={!items.length}>＋ {label}を選ぶ</button>
    {!!selected.length && <ul className="selection" aria-label={label + "の選択済み項目"}>
      {selected.map(id => <li key={id}><button type="button" aria-label={(names.get(id) ?? id) + "を外す"}
        onClick={() => apply(selected.filter(value => value !== id))}>{names.get(id) ?? id} ×</button></li>)}
    </ul>}
    <dialog ref={dialog} aria-labelledby={title}>
      <div className="section-heading"><h2 id={title}>{label}を選ぶ</h2>
        <button type="button" aria-label="選択を閉じる" onClick={() => dialog.current.close()}>×</button></div>
      <label className="choice-search">名前で探す<input type="search" value={query} onChange={event => setQuery(event.target.value)} /></label>
      <div className="choice-results">
        {[...groups.entries()].map(([group, rows]) => <section key={group}>
          {group && <h3>{group}</h3>}
          <div className="picker-results">{rows.map(item => <button type="button" key={item.id}
            aria-pressed={draft.includes(item.id)} onClick={() => setDraft(values => values.includes(item.id)
              ? values.filter(id => id !== item.id) : [...values, item.id])}>{item.name}</button>)}</div>
        </section>)}
        {!groups.size && <p className="message">該当する項目がありません。</p>}
      </div>
      <div className="form-actions">
        <button type="button" className="primary" onClick={() => { apply(draft); dialog.current.close(); }}>適用する（{draft.length}）</button>
        <button type="button" onClick={() => dialog.current.close()}>キャンセル</button>
      </div>
    </dialog>
  </div>;
}
