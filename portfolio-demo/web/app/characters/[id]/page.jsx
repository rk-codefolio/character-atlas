import Link from "next/link";
import { notFound } from "next/navigation";
import { Attribute } from "../../cards";
import { readCatalog, stats } from "../../catalog";

export default async function CharacterDetail({ params }) {
  const { id } = await params;
  if (!/^[a-z0-9-]{1,40}$/.test(id)) notFound();
  const [character, terms] = await Promise.all([readCatalog("characters/" + id), readCatalog("terms")]);
  if (!character) notFound();
  const labels = new Map(terms.map(term => [term.id, term.label]));

  return <>
    <p className="back"><Link href="/">← 渡り手一覧</Link></p>
    <section className="intro">
      <p className="kicker">群島航海局 · 個人記録</p>
      <h1>{character.name}</h1><p><Attribute attribute={character.attribute} /> {character.rarity}</p>
    </section>
    <h2>ステータス</h2>
    <dl className="metrics">{Object.entries(stats).map(([id, label]) => <div key={id}>
      <dt>{label}</dt><dd>{character.stats[id] ?? "—"}</dd>
    </div>)}</dl>
    <h2>スキル</h2>
    {character.skills.map(skill => <article className="card skill" key={skill.id}>
      <h3>{skill.name} <small>Lv.{skill.level}</small></h3><p>{skill.description}</p>
      {skill.matches.length > 0 && <ul className="hits">{skill.matches.map(match => <li key={match.termId}>
        {labels.get(match.termId)}{match.evidence === "RELATED_CANDIDATE" && "（関連候補）"}
      </li>)}</ul>}
    </article>)}
  </>;
}
