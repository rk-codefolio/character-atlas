import Atlas from "../atlas";
import { first } from "../catalog";

const values = value => Array.isArray(value) ? value : value ? [value] : [];

export default async function Search({ searchParams }) {
  const query = await searchParams;
  return <Atlas initialScreen="search" searchFilters={{
    mode: first(query.mode) === "OR" ? "OR" : "AND",
    terms: values(query.term),
    groups: values(query.group),
    role: first(query.role) ?? "",
    content: values(query.content),
  }} />;
}
