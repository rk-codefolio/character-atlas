# オープンソースゲームの分析事例 · The Battle for Wesnoth

公開ソースを参照できる追加事例として、ターン制ゲームのユニット定義から「誰が回復や解毒を持つか」を調べました。公開用図鑑で使う検索設計を説明するための分析で、図鑑を起動すると自作の架空データが登録されます。

## 調査条件

- **対象**。The Battle for Wesnoth 1.18.8。タグが指すコミットを `9f54f6a4f65b0d942a70436372e7cf28c4ba14d0` に固定した。 [公式リポジトリの対象コミット](https://github.com/wesnoth/wesnoth/tree/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0)
- **範囲**。標準ユニット4体と、それらが参照する能力マクロ、戦闘中の毒・回復の分岐。ソースを読む静的確認。
- **役割分担**。私は公開ソースを使った再現例を載せる方針を決めた。AIが対象コミットを特定し、定義の参照先、設定値、実行時の条件を追い、結果を表に整理した。

## 調べ方

1. ユニットファイルの `[abilities]` から能力マクロ名を確認する。攻撃に付く `[specials]` も照合し、検索タグを付ける対象を区別する。
2. `abilities.cfg` でマクロを追う。回復量の `value` と、毒に対する作用の `poison` を別々に読む。
3. `heal.cpp` で `poison` がどの分岐で使われるかを確認する。能力を持つことと、盤面条件を満たして効果が発動することを分けて記録する。

## 4体の照合結果

| ユニット | 参照した能力 | HP回復 | 毒への作用 |
| --- | --- | --- | --- |
| Elvish Shaman | `ABILITY_HEALS` | 隣接する味方に4 | `slowed`。毒ダメージを抑える。[ユニット定義](https://github.com/wesnoth/wesnoth/blob/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0/data/core/units/elves/Shaman.cfg#L21) |
| Elvish Druid | `ABILITY_CURES` | 隣接する味方に8 | `cured` の分岐で解毒する能力を持つ。[ユニット定義](https://github.com/wesnoth/wesnoth/blob/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0/data/core/units/elves/Druid.cfg#L21) |
| White Mage | `ABILITY_CURES` | 隣接する味方に8 | Druidと同じ解毒能力を参照する。[ユニット定義](https://github.com/wesnoth/wesnoth/blob/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0/data/core/units/humans/Mage_White.cfg#L26) |
| Elvish Captain | `ABILITY_LEADERSHIP` | この能力に回復の定義なし | この能力に毒への作用なし。[ユニット定義](https://github.com/wesnoth/wesnoth/blob/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0/data/core/units/elves/Captain.cfg#L29) |

`ABILITY_CURES` は、解毒の定義とHP回復8の定義を組み合わせます。一方、Shamanが持つ攻撃特殊効果の `WEAPON_SPECIAL_SLOW` は攻撃側の能力です。回復側の `poison=slowed` と似た名前なので、キー名や表示文の単語だけで一つの効果として扱うと誤分類します。 [能力マクロの定義](https://github.com/wesnoth/wesnoth/blob/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0/data/core/macros/abilities.cfg#L4) · [WMLの能力仕様](https://wiki.wesnoth.org/AbilitiesWML)

戦闘処理では、対象が毒状態かどうかで、通常の能力による回復量の計算と毒の処理が分かれます。さらに回復役の所属側によって、解毒能力の定義を持っていても毒ダメージの抑制に留まる分岐があります。そのため、検索結果の「解毒を持つ」は能力の根拠を表し、そのターンに実際に解毒できるかは状況で変わります。 [毒の処理](https://github.com/wesnoth/wesnoth/blob/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0/src/actions/heal.cpp#L84) · [回復処理](https://github.com/wesnoth/wesnoth/blob/9f54f6a4f65b0d942a70436372e7cf28c4ba14d0/src/actions/heal.cpp#L290)

## 検索設計とのつながり

この4体の範囲では、「回復」の明示一致はShaman・Druid・White Mage、「解毒」の明示一致はDruid・White Mageになります。「毒対策」という広い分類を別に設けるなら、抑制と解毒の両方を関連づけられます。検索で「回復と解毒」をAND指定したときに抑制だけを解毒として扱わないため、効果IDと検索用の分類、根拠をそれぞれ保持します。公開用の架空データでは同じ考え方を「護りと回復」のAND／ORで試せます。 [検索の実装](../catalog-core/src/main/java/dev/characteratlas/application/CharacterCatalog.java) · [操作例](../archipelago-app/README.md#試し方)

## 照合の再現手順

```sh
revision=9f54f6a4f65b0d942a70436372e7cf28c4ba14d0
source_url="https://raw.githubusercontent.com/wesnoth/wesnoth/${revision}"
for file in \
  data/core/units/elves/Shaman.cfg \
  data/core/units/elves/Druid.cfg \
  data/core/units/humans/Mage_White.cfg \
  data/core/units/elves/Captain.cfg
do
  printf '\n%s\n' "$file"
  curl -fsSL "$source_url/$file" | grep -nE 'id=|ABILITY_|WEAPON_SPECIAL_SLOW'
done
curl -fsSL "$source_url/data/core/macros/abilities.cfg" |
  grep -nE 'ABILITY_(HEALS|EXTRA_HEAL|UNPOISON|CURES|LEADERSHIP)|poison=(slowed|cured)'
curl -fsSL "$source_url/src/actions/heal.cpp" |
  grep -nE 'poison_status|poison_progress|calculate_healing|cure_side != side'
```

公式プロジェクトはコード・WML等のデータと、画像・音声についてライセンス方針を分けています。この分析記録には、調査した定義の識別子・確認結果・元ファイルへのリンクを載せています。対象は上記4体と標準の処理経路です。全ユニット・アドオン・個別シナリオの挙動や、ゲーム内での再現は今回の確認範囲外です。 [公式のライセンス方針](https://wiki.wesnoth.org/Wesnoth%3ACopyrights)
