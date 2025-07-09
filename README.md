# Positioning (Fabric/Quilt Mod) – Minecraft 1.21.5

![Positioning Logo](https://raw.githubusercontent.com/NewDoge23/Positioning_1.21.5/main/icon.png)

## 🚩 ¿Qué es Positioning?

**Positioning** es un mod para servidores Minecraft que **divide el mundo en dos territorios** (Norte y Sur) usando el eje Z, ideal para servidores PvP o de equipos. Cada jugador pertenece a un grupo y sólo puede explorar “su lado” del mapa, con una **zona neutral** en el centro y sanciones para los que cruzan la frontera enemiga.

- 🔷 **Zona Norte (Z+)**
- 🔶 **Zona Sur (Z-)**
- ⚪ **Zona Neutral:** 50 bloques para cada lado del eje Z (entre -50 y +50)

---

## ⚙️ ¿Cómo funciona?

- **Al ingresar por primera vez**, cada jugador elige a qué grupo quiere pertenecer mediante un pop-up nativo (sin comandos ni admins).
- El grupo queda **guardado automáticamente** y no puede cambiarse salvo que se borre el archivo de datos.
- **Cada grupo tiene permitido explorar sólo su hemisferio** del eje Z:
  - Norte: solo Z ≥ 0
  - Sur: solo Z ≤ 0
- **Zona neutral:** ambos grupos pueden permanecer sin sanción en Z entre -50 y +50.
- **Advertencias y sanciones:**
  - Si entrás a la franja enemiga, primero recibís una advertencia.
  - Si seguís avanzando, se activa una **cuenta regresiva visual** (tipo boss bar) de 60 segundos.
  - Si llegás al límite final (Z -50 para norte, Z +50 para sur), ¡morís instantáneamente!
- **La lógica del mod solo funciona en el Overworld**. En Nether y End, no hay restricciones.

---

## 🛠️ Instalación

1. **Descargá el JAR del mod** (release o compilación manual).
2. Instálalo en la carpeta `mods` de tu servidor Minecraft 1.21.5 (Fabric API requerido).
3. **(Opcional)** También podés instalarlo en el cliente para ver la barra visual (recomendado para feedback visual).

---

## 🧩 Dependencias

- [Fabric API](https://modrinth.com/mod/fabric-api) (última versión compatible 1.21.5)
- Java 21+

---

## 📂 Guardado de datos

- Las elecciones de grupo se guardan en un archivo `player_groups.json` en la raíz del mundo (ej: `./world/player_groups.json`).
- Si borrás ese archivo, los jugadores deberán elegir grupo nuevamente.

---

## 👾 Código y contribución

El código es abierto y está pensado para fácil extensión o forks.
¿Querés mejorar la UI, agregar mecánicas o soporte para otro tipo de equipos?  
¡Forkeá, abrí un Issue o mandá tu PR!

---

## 🚦 Roadmap / Ideas futuras

- Animaciones y efectos visuales avanzados para la barra
- Sistema de “eventos de invasión” y logs de PvP
- Compatibilidad con mods de economía o ciudades
- Configuración avanzada por archivo

---

## 📜 Licencia

[MIT](LICENSE)  
Hecho con ❤️ por [NewDoge23](https://github.com/NewDoge23)

---

## 📝 Ejemplo de uso

1. Al ingresar, el jugador ve el menú para elegir Norte/Sur.
2. Explora el mundo según su grupo, con feedback visual si cruza límites.
3. Si invade territorio enemigo… ¡más le vale volver rápido!

---

*¡Que la frontera esté siempre de tu lado!*
