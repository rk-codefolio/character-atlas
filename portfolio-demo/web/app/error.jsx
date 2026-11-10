"use client";

export default function ErrorPage({ reset }) {
  return <section className="intro">
    <h1>図鑑を読み込めませんでした</h1>
    <p>しばらくしてからもう一度お試しください。</p>
    <button type="button" onClick={reset}>再試行</button>
  </section>;
}
