<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/Loader-NeoForge-orange?style=for-the-badge" alt="NeoForge">
  <img src="https://img.shields.io/badge/Requires-Create-blue?style=for-the-badge" alt="Requires Create">
  <img src="https://img.shields.io/badge/Built%20for-Create%20Simulated-purple?style=for-the-badge" alt="Create Simulated">
  </br>
  <a href="https://modrinth.com/mod/create-linear-motion-simulated">
    <img src="https://img.shields.io/modrinth/dt/create-linear-motion-simulated?style=for-the-badge&logo=modrinth&label=Modrinth%20Downloads" alt="Modrinth Downloads">
  </a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/create-linear-motion-simulated">
    <img src="https://img.shields.io/curseforge/dt/create-linear-motion-simulated?style=for-the-badge&logo=curseforge&label=CurseForge%20Downloads" alt="CurseForge Downloads">
  </a>
</p>

# Create: Linear Motion Simulated

**Create: Linear Motion Simulated** is an addon for **Create** and **Create: Simulated** that introduces a new linear motion component: the **Pneumatic Cylinder**.

The goal of this mod is to bring a compact, Create-style piston system designed around rotational power, redstone control, multiblock length, and simulated contraption movement.

> Build longer. Push farther. Move structures with Create-style linear motion.

---

## Pneumatic Cylinder

The Pneumatic Cylinder is a directional multiblock component that converts Create rotational power into linear extension.

A cylinder can be extended by placing multiple cylinder blocks in a straight line. The longer the cylinder body is, the farther its piston head can travel.

### Main behavior

- Forms a **1x1xN multiblock** cylinder body.
- Can face in any direction.
- Receives Create rotational power from the rear side.
- Activates with redstone.
- Extends and retracts a piston head.
- Maximum extension distance depends on the cylinder length.
- Designed to interact with simulated moving structures.

---

## How it works

The cylinder has two important sides:

### Rear side

The rear side is where rotational power is accepted.

For small 1x1 cylinders, the input shaft is placed on the back of the multiblock.

### Front side

The front side is the piston head side.

When activated, the cylinder assembles its moving head and extends it forward. Blocks attached to the head can be moved as part of a simulated structure.

---

## Usage overview

1. Place a Pneumatic Cylinder block.
2. Extend it by placing more cylinder blocks in the same line.
3. Power the rear side with Create rotational force.
4. Apply a redstone signal.
5. The piston head assembles and extends.
6. Remove the signal to retract.

The extension speed scales with rotational speed.

---

## Create integration

Create: Linear Motion Simulated is designed to feel like a natural extension of Create’s kinetic system.

The Pneumatic Cylinder uses rotational input and redstone control, making it suitable for compact mechanical builds, moving assemblies, doors, platforms, machines, and experimental simulated contraptions.

---

## Ponder support

The mod includes in-game Ponder tutorials to explain:

- how to place the Pneumatic Cylinder,
- how to extend the multiblock body,
- where to provide rotational input,
- how redstone activation works,
- and how the piston head behaves.

Hold the Ponder key on the Pneumatic Cylinder item to view the tutorial in-game.

---

## Dependencies

This mod requires:

- **Minecraft 1.21.1**
- **NeoForge**
- **[Sable](https://modrinth.com/mod/sable)**
- **[Create](https://modrinth.com/mod/create)**
- **Create: Simulated / [Create Aeronautics bundle](https://modrinth.com/mod/create-aeronautics)**  

Make sure all required dependencies match the version listed for the mod release.

---

## Current content

- Pneumatic Cylinder block
- In-game Ponder tutorials

More linear motion components may be added in the future.

---

## Planned content

- A Hydraulic Cylinder block : works the same as the Pneumatic cylinder block but with an external controller (not binded to the same sub level) with length controle using redstone power strengh.
- A springloaded piston Block : A variant of the Hydraulic Cylinder block that would be in a uncompressed state when placed and can be compressed with rotation (and a lot of Stress Unit) and released in an instant (because it's a spring).

---

## Notes

This project is still evolving. Some behavior may change as Create: Simulated, Sable, and the physics systems around them continue to develop.

If you encounter issues, please report them with:

- Minecraft version
- NeoForge version
- Create version
- Create: Simulated / Aeronautics version
- Sable version
- crash log or latest.log
- steps to reproduce the issue

---

## Credits

Code and project by **CookieG77**.

Textures are the exclusive property of **[jimmy421x67](https://modrinth.com/user/jimmy421x67)**.

This mod depends on and is inspired by the Create ecosystem, including Create, Create: Simulated, Create: Aeronautics, and Sable.

---

## License

This project is distributed under a source-available non-commercial license. (more info [here](https://github.com/CookieG77/Create-LinearMotionSimulated/blob/main/LICENSE.md))

Textures and visual assets credited to **jimmy421x67** may not be extracted, reused, modified, or redistributed separately without permission.
