import Link from "next/link";

export default function NotFound() {
  return <section className="intro">
    <h1>ページが見つかりません</h1>
    <Link href="/">← 渡り手一覧</Link>
  </section>;
}
