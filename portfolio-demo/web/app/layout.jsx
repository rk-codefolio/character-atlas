import Link from "next/link";
import "./style.css";

export const metadata = {
  title: "風綴りの群島 | 渡り手図鑑",
  description: "架空の群島を旅する渡り手を探すキャラクター図鑑。",
};

export default function Layout({ children }) {
  return <html lang="ja">
    <body>
      <header className="masthead">
        <div className="frame masthead-inner">
          <Link className="brand" href="/">風綴りの群島</Link>
          <nav aria-label="図鑑メニュー"><Link href="/">渡り手一覧</Link><Link href="/search">技で探す</Link></nav>
        </div>
      </header>
      <main className="frame">{children}</main>
      <footer className="frame footer">風綴りの群島は、この図鑑のために作った架空のゲームです。</footer>
    </body>
  </html>;
}
