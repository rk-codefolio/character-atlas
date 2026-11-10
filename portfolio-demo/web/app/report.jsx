"use client";

import { useId, useRef, useState } from "react";

export const reasons = { SPAM: "宣伝・スパム", ABUSE: "迷惑行為・不適切な内容", OTHER: "その他" };

export default function ReportButton({ postId }) {
  const dialog = useRef(null);
  const title = useId();
  const [reason, setReason] = useState("");
  const [note, setNote] = useState("");
  const [status, setStatus] = useState("");
  async function submit(event) {
    event.preventDefault();
    if (!reason || status === "busy") return;
    setStatus("busy");
    try {
      const response = await fetch("/api/posts/" + postId + "/reports", {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ reason, note }),
      });
      if (!response.ok) throw new Error();
      dialog.current.close();
      setStatus("sent");
    } catch {
      setStatus("error");
    }
  }
  return <div className="form-actions">
    {status === "sent" ? <span role="status">通報を受け付けました。</span> :
      <button type="button" onClick={() => dialog.current.showModal()}>通報</button>}
    <dialog ref={dialog} aria-labelledby={title}>
      <form className="post-form" onSubmit={submit}>
        <h2 id={title}>投稿を通報する</h2>
        <label>理由<select required value={reason} onChange={event => setReason(event.target.value)}>
          <option value="">理由を選ぶ</option>
          {Object.entries(reasons).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>補足<textarea rows={3} maxLength={500} value={note} onChange={event => setNote(event.target.value)} /></label>
        {status === "error" && <p className="form-error" role="alert">通報を送信できませんでした。</p>}
        <div className="form-actions">
          <button className="primary" disabled={status === "busy"}>通報する</button>
          <button type="button" disabled={status === "busy"} onClick={() => dialog.current.close()}>キャンセル</button>
        </div>
      </form>
    </dialog>
  </div>;
}
