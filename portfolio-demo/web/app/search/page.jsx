import Link from "next/link";
import { CharacterCard } from "../cards";
import { first, readCatalog } from "../catalog";

export default async function Search({ searchParams }) {
  const params = await searchParams;
  const mode = first(params.mode) === "OR" ? "OR" : "AND";
  const terms = await readCatalog("terms");
  const labels = new Map(terms.map(term => [term.id, term.label]));
  const submitted = Array.isArray(params.term) ? params.term : [params.term];
  const selected = new Set(submitted.filter(id => labels.has(id)));
  const query = new URLSearchParams({ mode });
  for (const id of selected) query.append("term", id);
  const results = await readCatalog("search", query);

  return <>
    <section className="intro">
      <p className="kicker">群島航海局 · 渡り手図鑑</p><h1>技で探す</h1>
      <p>指定した効果を持つ渡り手を探します。効果やタグが明示された結果を先に表示します。</p>
    </section>
    <form className="controls" action="/search">
      <fieldset><legend>効果・タグ</legend><div className="choices">
        {terms.map(term => <label key={term.id} className="choice">
          <input type="checkbox" name="term" value={term.id} defaultChecked={selected.has(term.id)} />{term.label}
        </label>)}
      </div></fieldset>
      <label>条件の組み合わせ<select name="mode" defaultValue={mode}>
        <option value="AND">すべて (AND)</option><option value="OR">いずれか (OR)</option>
      </select></label>
      <button type="submit">検索する</button>
      <Link href="/search">クリア</Link>
    </form>
    <p className="count">{results.length}件</p>
    <div className="grid">{results.map(row => <CharacterCard key={row.character.id} character={row.character}>
      {row.candidateOnly && <p className="candidate">関連候補</p>}
      {row.hits.length > 0 && <ul className="hits">{row.hits.map(hit => <li key={hit.skillId + "/" + hit.termId}>
        {hit.skillName} · {labels.get(hit.termId)}{hit.evidence === "RELATED_CANDIDATE" && "（候補）"}
      </li>)}</ul>}
    </CharacterCard>)}</div>
    {!results.length && <p>該当する渡り手がいません。</p>}
  </>;
}
