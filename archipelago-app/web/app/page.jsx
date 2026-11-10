import Atlas from "./atlas";
import { first } from "./catalog";

export default async function Page({ searchParams }) {
  const query = await searchParams;
  const screen = query.admin != null ? "admin" : query.contents != null ? "contents" : "list";
  const direction = first(query.direction);
  return <Atlas initialScreen={screen} listFilters={{
    name: (first(query.name) ?? "").slice(0, 80),
    stat: first(query.stat) ?? "",
    direction: direction === "ASC" || direction === "DESC" ? direction : null,
  }} />;
}
