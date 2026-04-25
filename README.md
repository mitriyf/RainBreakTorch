# 🌧️ **RainBreakTorch** [![CodeFactor](https://www.codefactor.io/repository/github/mitriyf/rainbreaktorch/badge)](https://www.codefactor.io/repository/github/mitriyf/rainbreaktorch)
## 🔥 Extinguish the torches that are in the rain 
This plugin adds the mechanic of breaking torches in the rain for your players. 
- $ Versions 1.7.1-26.1+ are supported.
- $ Has been tested on versions: 1.7.10, 1.8.8, 1.12.2, 1.16.5, 1.19.4, 1.21, 26.1
- $ Attention! The plugin works partially fine with WorldEdit. Don't replace blocks with air if there are torches at that height. To fix errors related to this, you should delete these chunks in the plugin folder.
- $ If you recreate the world often, you should also delete the world folder in the plugin to avoid errors with torches.
- $ Due to some optimizations in the plugin, you will not be able to use some torches as safe blocks.
- $ If you already have a world ready and you need working torches in loaded chunks for that world, then you can use this command: /rainbreaktorch update all WorldName
- $ You can ask other questions in the resource discussion.
# 🕯️ Place the torches!
Put a torch in the rain, and you'll see it collapse!

![2026-04-02 23-58-26](https://github.com/user-attachments/assets/4648f6ce-4336-4da1-9796-4dac7abd485a)
## 🛠️ Supported:
### 🔮 Support HEX, MiniMessage (1.18+)
### 🌍 World and biome settings.

![2026-04-03 00-22-24](https://github.com/user-attachments/assets/99c30d63-e588-414e-ada5-46087b6e8181)
### ⚛ Block placement settings, such as speed, physics, and developer settings.

![2026-04-03 00-24-40](https://github.com/user-attachments/assets/62a1990a-e967-466a-986d-3d951bec0801)
### 🏮 Support for any torches (choose for yourself)! For example, a torch, a lever, a lamp, a redstone, what else?

![2026-04-03 00-17-40](https://github.com/user-attachments/assets/67ade2fd-69aa-4f7a-bf4a-aa4a64381016)
### 🛡️ You can customize your safe blocks, except for the torches.

![2026-04-03 00-19-52](https://github.com/user-attachments/assets/ba7ac986-b1b4-4cae-9cc0-61a0f3f6b036)
### 🔎 Checks:
- The plugin will automatically detect your server version so that it starts working correctly with your project.
- Torches have many checks that ensure the plugin works properly.
- Check the redstone torches you need.
- Set the type of protection blocks immediately: isSolid or IsOccluding (default: IsOccluding).
- You can configure the following safe block lists: otherlist, customlist, blacklist, multilist (default: otherlist).
  - otherlist - Blocks from checkType and blocks that are listed here. (work in only list and checktype)
  - customlist - Only the blocks from the list. (work in only list)
  - blacklist - Blocks from checkType, but only those that should be prohibited are listed. (work in only list and checktype)
  - multilist - Blocks from checkType, but the list section is responsible for adding new items, and you need to add a second blacklist section where blocks from checkType will be blocked. (list and blacklist)
- Check out the newly generated chunks right away! This feature is necessary for worlds that have no borders (default: enabled)!

## ♾️ Functions:
### ⌨️Command (/rainbreaktorch):
Get a list of commands using /rainbreaktorch help
- /rainbreaktorch reload - Reload the plugin configuration.
- /rainbreaktorch status - Get the plugin status.
- /rainbreaktorch loot - Get help with the loot subcommand.
- /rainbreaktorch update cancel - Canceling a previous task.
- /rainbreaktorch update speed Number - Set the chunk loading speed in the worlds. Attention! It is not recommended to set it to more than 8.
- /rainbreaktorch update active WorldName - Get all the torches from the active chunks in the world.
- /rainbreaktorch update all WorldName - Get all the torches from the the world.
  Warning! Do this if you have 1. loaded all the chunks and you need behavior for all the torches in the world, OR 2. you have already installed the plugin on a server with loaded chunks and player torches.
### Permission:
- rainbreaktorch.use - Allow the use of the command.
### 🚀Launch:
- Some security features that protect the server from lag due to this plugin. If you don't get drops from torches, you should increase the msLimit (onlyDeveloper section) in the plugin configuration.
- Partial physics, another protective function.
- ObjectRemove clears the chunk from memory. It is not recommended to change it. 

^^^ The following settings should be changed if your server is not working properly with the plugin. It is not recommended to change it without the developer's assistance.
### ⚙️Config:
- Send actions to players using messages (HEX, MiniMessage support from 1.18+).
- Settings for noperm, help.
### 🎁Loot:
- Loot checks and in case of errors, errors will be sent to the console.
- Add loot via the /rainbreaktorch loot
- You can add initial loot for torches or specific loot for a specific torch.
- You can add it manually (loot.yml) or via a command.
### 🔐Storage:
- Storing all torches in the plugin folder: RainBreakTorch/worlds/WorldName/chunkFiles...

## 📝 Configurations:
View them by navigating through the files using the following path: src\main\resources

# You can consider the rest of the possibilities when using the plugin.
