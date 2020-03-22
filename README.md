# YoureDangerous

A Spigot / PaperMC plugin that allows players to lock their own chests, making other players
unable to open or destroy them.

*Only tested on PaperMC 1.15*

## Installation
Just download the [latest jar file](https://github.com/mattiamari/chestlock/releases),
and copy it to your ```plugins``` directory.

## Usage
Left click on any chest with a wooden stick in hand to lock and unlock the chest.
You can allow more players to open the chest by writing their nicknames on a sign, one on its own line.
The sign must be attached to the front of the chest.

Double chests are supported and work the same way. They also allow two signs to be placed on them.

Players written on signs will be able to open the chests but not destroy them, unless the chest is unlocked.

Explosions, caused by either chest owners, other players or other entities (like creepers)
DO NOT affect the chest nor the sign(s) placed on them.
