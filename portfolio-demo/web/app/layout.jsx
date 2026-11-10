import "./style.css";

export const metadata = {
  title: "風綴りの群島 | 渡り手図鑑",
  description: "架空の群島を旅する渡り手の技・効果・航海の記録。",
};

export default function Layout({ children }) {
  return <html lang="ja"><body>{children}</body></html>;
}
