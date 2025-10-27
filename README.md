# WorldFreeze

WorldFreeze is a simple, server-side Minecraft Fabric mod that allows administrators to "freeze" specific worlds or dimensions. When a world is frozen, players are prevented from interacting with it (breaking/placing blocks, interacting with entities, etc.).

## Features

- Freeze and unfreeze specific worlds/dimensions.
- Prevents block breaking and placement in frozen worlds.
- Prevents entity interaction (attacking, using) in frozen worlds.
- Simple command-based management.
- Permissions to ensure only administrators can control the freeze state.

## Usage

All commands are managed through the main `/worldfreeze` command. You need a permission level of 4 (admin) to use these commands.

- `/worldfreeze list`
  - Lists all currently frozen worlds.

- `/worldfreeze add [dimension]`
  - Freezes the specified world/dimension.
  - If no dimension is provided, it freezes the world you are currently in.

- `/worldfreeze remove [dimension]`
  - Unfreezes the specified world/dimension.
  - If no dimension is provided, it unfreezes the world you are currently in.

## Installation

1.  Ensure you have the [Fabric Loader](https://fabricmc.net/use/) installed.
2.  This mod requires the [Fabric API](https://modrinth.com/mod/fabric-api).
3.  Download the latest release of WorldFreeze from the releases page.
4.  Place the downloaded `.jar` file into your `mods` folder.
5.  Start the server.

## Building from Source

1.  Clone the repository:
    ```sh
    git clone https://github.com/thojo0/worldfreeze.git
    ```
2.  Navigate into the project directory:
    ```sh
    cd worldfreeze
    ```
3.  Build the mod using Gradle:
    ```sh
    ./gradlew build
    ```
4.  The compiled `.jar` file can be found in the `build/libs/` directory.

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.
