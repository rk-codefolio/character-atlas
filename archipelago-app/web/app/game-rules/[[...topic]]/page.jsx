import Link from "next/link";
import { notFound } from "next/navigation";
import { guides } from "../guides";

const topics = guides;

const groups = [
  ["行動順・ターン", ["action-order", "speed-formula", "opening-gauge", "gauge-ties", "gauge-changes",
    "priority-turns", "turn-timing"]],
  ["スキル・効果", ["skill-cooldown", "effect-stacks", "effect-frames", "cleanse-dispel-immunity",
    "periodic-effects", "effect-hit-resist"]],
  ["属性・ダメージ", ["affinity", "shields-mitigation"]],
  ["装備・育成", ["core-main-stats", "matrix-points", "shell-effects", "imprint-levels", "growth-bonuses"]],
];

function Guide({ entry, topic }) {
  return <>
    {entry.sections.map(([title, text]) => <section key={title}><h2>{title}</h2><p>{text}</p></section>)}
    {topic === "effect-hit-resist" && <table className="rules-table">
      <caption>基本付与率70%のときの例</caption>
      <thead><tr><th>効果的中</th><th>抵抗20%</th><th>抵抗50%</th><th>抵抗80%</th></tr></thead>
      <tbody><tr><th>20%</th><td>70%</td><td>55%</td><td>40%</td></tr>
        <tr><th>40%</th><td>80%</td><td>65%</td><td>50%</td></tr>
        <tr><th>70%</th><td>95%</td><td>80%</td><td>65%</td></tr></tbody>
    </table>}
    {entry.related.length > 0 && <section><h2>関連する仕様</h2>
      <ul>{entry.related.map(id => <li key={id}><Link href={"/game-rules/" + id}>{topics[id].title}</Link></li>)}</ul>
    </section>}
  </>;
}

export default async function RulesPage({ params }) {
  const { topic = [] } = await params;
  const key = topic[0];
  const entry = Object.hasOwn(topics, key) ? topics[key] : undefined;
  if (topic.length > 1 || (key && !entry)) notFound();
  return <>
    <header className="archive-top"><strong>⬡ 風綴りの群島 / 渡り手図鑑</strong><span>群島航海局</span></header>
    <main className="archive-layout">
      <nav className="archive-nav" aria-label="渡り手図鑑">
        <p className="archive-kicker">INDEX</p>
        <Link href="/">渡り手一覧</Link>
        <Link href="/game-rules" aria-current="page">ゲーム仕様</Link>
      </nav>
      <div className="archive-body">
        <section className="panel rules">
          {entry ? <>
            <Link href="/game-rules" className="back action-link">← ゲーム仕様に戻る</Link>
            <h1>{entry.title}</h1>
            <Guide entry={entry} topic={key} />
          </> : <>
            <h1>ゲーム仕様</h1>
            {groups.map(([name, ids]) => <section key={name}>
              <h2>{name}</h2>
              <ul className="records">{ids.map(id => <li className="search-row" key={id}>
                <Link className="record-link" href={"/game-rules/" + id}>{topics[id].title}</Link>
                <p className="caption">{topics[id].summary}</p>
              </li>)}</ul>
            </section>)}
          </>}
        </section>
      </div>
    </main>
  </>;
}
