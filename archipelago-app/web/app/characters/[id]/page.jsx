import { notFound } from "next/navigation";
import Atlas from "../../atlas";
import { readCatalog } from "../../catalog";

export default async function CharacterDetail({ params }) {
  const { id } = await params;
  if (!/^[a-z0-9][a-z0-9-]{0,39}$/.test(id) || !await readCatalog("characters/" + id)) notFound();
  return <Atlas initialCharacter={id} />;
}
