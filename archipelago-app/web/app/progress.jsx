"use client";

import { useId, useState } from "react";

export function ProgressPopover({ label, rows }) {
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const id = useId();
  const open = !dismissed && (hovered || focused || pinned);
  const groups = [];
  for (const { level, description } of rows) {
    const previous = groups.at(-1);
    if (previous?.description === description && previous.last + 1 === level) {
      previous.last = level;
    } else {
      groups.push({ level, last: level, description });
    }
  }
  return <span className="progress-popover"
    onPointerEnter={event => {
      if (event.pointerType !== "mouse") return;
      setHovered(true);
      setDismissed(false);
    }}
    onPointerLeave={() => setHovered(false)}
    onBlur={event => {
      if (event.currentTarget.contains(event.relatedTarget)) return;
      setFocused(false);
      setPinned(false);
    }}
    onKeyDown={event => {
      if (event.key !== "Escape" || !open) return;
      event.stopPropagation();
      setPinned(false);
      setDismissed(true);
    }}>
    <button type="button" className="progress-trigger" aria-label={label}
      aria-controls={id} aria-expanded={open}
      onFocus={event => {
        if (event.target.matches(":focus-visible")) {
          setFocused(true);
          setDismissed(false);
        }
      }}
      onClick={() => {
        setPinned(!pinned);
        setDismissed(pinned);
      }}>{label}</button>
    <span className="progress-panel" id={id} role="region" aria-label={label} hidden={!open} tabIndex={open ? 0 : -1}>
      {groups.map(({ level, last, description }) => <span className="progress-row" key={level}>
        <strong>Lv.{level}{level !== last && `〜${last}`}</strong><span>{description}</span>
      </span>)}
    </span>
  </span>;
}

export function EffectDetails({ tips }) {
  if (!tips?.length) return null;
  const grouped = new Map();
  for (const tip of tips) {
    if (!grouped.has(tip.id)) grouped.set(tip.id, []);
    grouped.get(tip.id).push(tip);
  }
  return <div className="effect-detail"><h3>効果の説明</h3>
    <dl>{[...grouped.entries()].map(([id, rows]) => {
      const { name, description } = rows.at(-1);
      return <div key={id}>
        <dt>{name} {new Set(rows.map(row => row.description)).size > 1 &&
          <ProgressPopover label={name + "の段階別効果"} rows={rows} />}</dt>
        <dd>{description}</dd>
      </div>;
    })}</dl>
  </div>;
}
