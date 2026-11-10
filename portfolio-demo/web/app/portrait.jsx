const palettes = {
  風: ["#143943", "#85c6b6", "#dce8c5"],
  潮: ["#19304b", "#56a8c5", "#c4e0e4"],
  灯: ["#4d2c30", "#df885f", "#fae0a5"],
  岩: ["#363c3c", "#aba486", "#ede1c1"],
  星: ["#292a48", "#9689cf", "#f2dfb1"],
};

function PortraitArt({ seed, palette }) {
  const [night, coat, light] = palette;
  const offset = seed % 35 - 17;
  const face = ["#c78c6f", "#a76f57", "#dfae84", "#885341"][seed % 4];
  const angle = seed % 19 - 9;
  return <>
    <rect width="160" height="190" fill={night} />
    <circle cx={110 - offset} cy="54" r={25 + seed % 13} fill={light} opacity=".27" />
    <path d="M0 116 Q40 89 88 112 T160 103 M0 127 Q43 106 83 125 T160 118 M0 145 Q45 122 92 141 T160 132"
      stroke={light} fill="none" opacity=".23" />
    <g transform={`translate(${offset / 3} 0) rotate(${angle} 80 96)`}>
      <path d="M19 190 Q23 141 59 127 L101 127 Q136 141 142 190Z" fill={coat} />
      <path d="M58 126 L80 153 L102 126 L112 190 H49Z" fill={night} opacity=".62" />
      <path d="M66 110 H94 L98 132 L80 146 L62 132Z" fill={face} />
      <path d="M51 86 Q52 58 80 60 Q110 60 109 89 L106 111 Q82 139 56 112Z" fill={face} />
      <path d={seed % 2 ? "M44 88 Q43 38 84 45 Q115 50 119 90 L96 76 L55 91Z" : "M46 91 L54 48 L103 54 L120 86 L102 95 L91 74 L55 94Z"}
        fill={night} />
      <path d="M54 75 Q79 65 104 74 M69 99h4 M91 99h4 M76 117q6 4 11 0"
        stroke={light} strokeWidth="2" strokeLinecap="round" fill="none" opacity=".8" />
      <path d="M25 182 L57 145 M105 145 L137 182" stroke={light} opacity=".62" />
      <path d="M80 154l7 11-7 11-7-11z" fill={light} />
    </g>
  </>;
}

export default function Portrait({ character, detail = false }) {
  const palette = palettes[character.attribute];
  const seed = Array.from(character.id).reduce((value, letter) => (value * 31 + letter.codePointAt(0)) >>> 0, 7);
  return <span className={"portrait" + (detail ? " portrait-detail" : "")}>
    <svg viewBox="0 0 160 190" role="img" aria-label={palette ? character.name + "の架空肖像" : "画像なし"}>
      {palette ? <PortraitArt seed={seed} palette={palette} /> : <>
        <rect width="160" height="190" fill="#26363a" /><text x="80" y="98" textAnchor="middle" fill="#dce8c5">画像なし</text>
      </>}
    </svg>
  </span>;
}
