import Atlas from "../atlas";
import { first } from "../catalog";

export default async function Search({ searchParams }) {
  const query = await searchParams;
  const terms = Array.isArray(query.term) ? query.term : query.term ? [query.term] : [];
  return <Atlas initialScreen="search" initialMode={first(query.mode) === "OR" ? "OR" : "AND"} initialTerms={terms} />;
}
