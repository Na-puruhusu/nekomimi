# Nekomimi

装備できる猫耳としっぽを追加する Minecraft Forge mod です。装備するとキャラクターの見た目が変わります。

- Minecraft 1.20.1 / Forge 47.4.28
- 依存mod なし（Forge 単体で動作）

## アイテム

色は 白・黒・オレンジ・灰色 の4種類です。

| アイテム | 装備スロット | ID |
| --- | --- | --- |
| 猫耳 | 頭 | `nekomimi:nekomimi_<color>` |
| 猫のしっぽ | 脚 | `nekomimi:neko_tail_<color>` |

`<color>` は `white` / `black` / `orange` / `gray` です。クリエイティブタブ「猫耳としっぽ」から入手できます。

- 防御力・耐久値はなく、見た目専用です（エンチャント不可）
- しっぽは装備中ゆらゆら揺れます
- 手持ちのヘルメット・レギンスとは同じスロットを使うため併用できません

## レシピ

羊毛は色ごとに対応する色のものを使います（例: 白い猫耳 = 白い羊毛）。`W` = 羊毛、`L` = 革。

```
猫耳                しっぽ
W _ W               _ _ W
W L W               _ W _
                    L W _
```

## ビルド

Java 17 が必要です。

```
./gradlew build
```

成果物は `build/libs/nekomimi-1.0.0.jar` です。GitHub Actions でも push のたびにビルドされ、jar が artifact として保存されます。

開発用クライアントは `./gradlew runClient` で起動できます。

## アセットの再生成

テクスチャ・アイテムモデル・レシピ・言語ファイルは `tools/generate_assets.py`（Pillow が必要）で生成しています。色の追加や見た目の調整はこのスクリプトと `NekoColor` を編集してください。

```
python3 tools/generate_assets.py
```

## ライセンス

MIT
