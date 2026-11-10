import Link from "next/link";
import { CharacterCard } from "./cards";
import { first, readCatalog, stats } from "./catalog";

export default async function CharacterList({ searchParams }) {
  const params = await searchParams;
  const name = (first(params.name) ?? "").slice(0, 80);
  const stat = Object.hasOwn(stats, first(params.stat)) ? first(params.stat) : "";
  const direction = first(params.direction) === "DESC" ? "DESC" : "ASC";
  const query = new URLSearchParams({ name, direction });
  if (stat) query.set("stat", stat);
  const characters = await readCatalog("characters", query);

  return <>
    <section className="intro">
      <p className="kicker">群島航海局 · 渡り手図鑑</p>
      <h1>渡り手一覧</h1>
      <p>風を読み、潮を越え、夜に灯をともす。群島を渡る仲間たち。</p>
      <Link className="action" href="/search">技で探す →</Link>
    </section>
    <form className="controls" action="/">
      <label>名前<input type="search" name="name" defaultValue={name} maxLength={80} /></label>
      <label>並び替え<select name="stat" defaultValue={stat}>
        <option value="">図鑑順</option>
        {Object.entries(stats).map(([id, label]) => <option key={id} value={id}>{label}</option>)}
      </select></label>
      <label>順序<select name="direction" defaultValue={direction}>
        <option value="ASC">昇順</option><option value="DESC">降順</option>
      </select></label>
      <button type="submit">表示する</button>
    </form>
    <p className="count">{characters.length}件</p>
    <div className="grid">{characters.map(character => <CharacterCard key={character.id} character={character}>
      {stat && <p>{stats[stat]} {character.stats[stat] ?? "—"}</p>}
    </CharacterCard>)}</div>
    {!characters.length && <p>該当する渡り手がいません。</p>}
  </>;
}
