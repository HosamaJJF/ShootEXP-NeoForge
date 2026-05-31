# ShootEXP

这是一个能让玩家之间通过进行深入交流而变得更加了解彼此，我觉得它是一个交友模组。

当一个玩家在另一个玩家（或生物）附近反复蹲下（潜行）时，达到一定次数后，就会让被蹲的玩家/生物"射出一滩粘稠的经验"，这些经验可以被捡起来食用。

## 版本区别

| 分支 | Minecraft 版本 | NeoForge 版本 |
|------|---------------|--------------|
| `main` | 26.1 | 26.1.0.1-beta |
| `mc/1.21.5` | 1.21.5 | 21.5.0-beta |
| `mc/1.21.1` | 1.21.1 | 21.1.1 |

## 使用方法

将对应版本的 `.jar` 文件放入服务器的 `mods/` 文件夹，重启服务器即可。

客户端无需安装此模组即可进入服务器，但如果想看到自定义材质的 EXP 物品，需要在客户端也安装并搭配资源包使用。

## 命令与权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/shootexp help` | 无 | 获取帮助 |
| `/shootexp status [玩家]` | 查看自己无需权限；查看他人需 OP 2 级 | 查看射精状态 |
| `/shootexp item <所有者> <赠予者> <数量>` | OP 2 级 | 获取一个经验物品 |
| `/shootexp restore all <玩家>` | OP 2 级 | 完全恢复 |
| `/shootexp restore times <玩家> <次数>` | OP 2 级 | 恢复施法次数 |
| `/shootexp restore stock <玩家> <数量>` | OP 2 级 | 恢复经验存量 |
| `/shootexp set <玩家> <施法次数> <存量>` | OP 2 级 | 设置玩家数据 |
| `/shootexp reload` | OP 2 级 | 重载配置文件 |

## 模组配置

如果觉得默认配置就很好的话，可以跳过此部分。配置文件位于 `config/shootexp.json`，首次启动后会自动生成。

### 术语约定

为了避免混淆，先声明几个概念：

- **施法**：指玩家反复蹲下的过程
- **攻击**：指玩家蹲下一次的行为
- **攻击者**：蹲下的那个玩家
- **防守者**：被蹲的那个玩家/生物
- **所有者**：射出经验的人（即攻击者）
- **赠予者**：被蹲后产生经验的人（即防守者）

### 语言

目前仅有 `zh_CN` 和 `en_US` 两种语言。（发出了白嫖更多语言翻译的声音）

```json
"lang": "zh_CN"
```

### 最大经验存量

一个玩家身上所储存的粘稠经验并不是无限的。

```json
"maxStock": 1000
```

### 射出经验函数

玩家施法需要一定的攻击次数才能射出经验，使用 exp4j 库自定义公式，公式中可以使用的变量有 `SHOOT`（已射出次数）、`STOCK`（当前存量）、`MAXSTOCK`（最大存量）：

```json
"requiredAttackTimes": "1.618^SHOOT + 10",
"shootAmount": "STOCK / 2"
```

其中 `requiredAttackTimes` 决定了需要攻击多少次才能射出经验——随着射出次数增多，所需攻击次数会指数级增长（黄金比例 1.618）。`shootAmount` 决定了每次射出多少经验。

### 可施法实体类型

原本只支持玩家，但由于有一位玩家表示他很爱他家的旺财，想要跟它进行更深入的交流，于是就支持了实体类型。另外有一些 xp 系统比较特殊的玩家可能会想要对着不是实体的东西施法，以后可能也会支持。

```json
"entityTypes": ["Player", "PathfinderMob"]
```

可用的实体类型为 Minecraft 实体类名，如 `Player`、`PathfinderMob`、`Animal`、`Monster` 等。

### 经验类型

目前支持 `VANILLA`、`SKILLAPI`、`MMOCORE` 三种经验系统，适配 RPG 服务器需求。

```json
"expType": "VANILLA"
```

### 攻击设置

纵使你的鞭再长也会有够不着的地方。

```json
"attack": {
    "distance": 2.0,
    "timeout": 100
}
```

- `distance`：最远攻击距离（格）
- `timeout`：施法超时时间（tick，100 = 5 秒），超时后施法进度重置

### 恢复设置

可以分别设置施法次数和经验存量的自动恢复速度。

```json
"restore": {
    "shoot": {
        "period": 6000,
        "amount": 1
    },
    "stock": {
        "period": 6000,
        "amount": 200
    }
}
```

- `period`：恢复间隔（tick，6000 = 5 分钟）
- `amount`：每次恢复的量

### 自定义模型 ID

原版骨粉的贴图看起来其实不那么让人有食欲，因此在 1.14+ 你可以使用自定义模型数据来为粘稠经验提供不一样的贴图。

```json
"customModelData": {
    "enable": false,
    "value": 0
}
```

### 自定义音效

本模组很温柔的，一点也不暴力。

```json
"sound": {
    "attack": "entity.parrot.imitate.slime",
    "shoot": "block.slime_block.step",
    "shootNoExp": "entity.llama.eat",
    "eat": "entity.generic.drink"
}
```

- `attack`：蹲下攻击时播放的音效（默认：鹦鹉学史莱姆，很可爱不是吗）
- `shoot`：成功射出经验时播放的音效
- `shootNoExp`：没有经验可射时播放的音效
- `eat`：食用经验时播放的音效

### 私密消息

默认情况下，攻击、射出、食用等消息会广播给全服玩家。如果你比较害羞，可以开启私密消息模式，只有相关人员才能看到。

```json
"privateMessage": false
```

## 致谢

本项目 fork 自 [MoeArea/ShootEXP](https://github.com/MoeArea/ShootEXP)，从 Bukkit 插件移植为 NeoForge 模组。感谢原作者 [R_Josef](https://github.com/R-Josef) 的开源精神。

## 许可证

MIT License
