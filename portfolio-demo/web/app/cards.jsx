import Link from "next/link";

const emblems = { "風": "↝", "潮": "≈", "灯": "✦" };

export function Attribute({ attribute }) {
  return <span className="attribute" data-element={attribute}>
    <span aria-hidden="true">{emblems[attribute]}</span> {attribute}属性
  </span>;
}

export function CharacterCard({ character, children }) {
  return <article className="card">
    <div className="card-meta"><Attribute attribute={character.attribute} /><span>{character.rarity}</span></div>
    <h2><Link href={"/characters/" + encodeURIComponent(character.id)}>{character.name}</Link></h2>
    {children}
  </article>;
}
