# SWave

Modo celebración para las compras de la tienda. Cuando alguien compra un paquete, se
abre una ventana de celebración: los jugadores que escriben **GG** en el chat reciben una
recompensa —una sola vez— y su GG aparece con un color propio, formando un arcoíris que
resalta la compra en todo el servidor.

Requiere **SiriusCore**. Sin él, el servidor no carga este plugin.

---

## El flujo

1. Un jugador compra un paquete en la tienda.
2. La tienda ejecuta `/swave start <comprador> <paquete>` por consola.
3. Se anuncia la compra y arranca la ventana (30 s por defecto).
4. Cada jugador escribe `GG` → recibe su recompensa y su GG sale coloreado.
5. Al terminar la ventana, se cierra la celebración.

Escribir GG una segunda vez no da nada: la recompensa es una por persona y por
celebración.

---

## Integración con la tienda (MineStore, Tebex, Craftingstore…)

Ninguna de estas plataformas necesita una API para esto: todas ejecutan **comandos por
consola** al confirmarse una compra. Ahí está la integración.

En **MineStore**, en el paquete que quieras celebrar, añade a los comandos de compra:

```
swave start {username} Nombre del Paquete
```

`{username}` es el marcador de MineStore para el comprador (en Tebex es `{name}`,
revisa el de tu plataforma). El resto del texto es el nombre del paquete que saldrá en
el anuncio.

> Ponlo como comando **del servidor / consola**, no como comando que se ejecuta en nombre
> del jugador: `swave start` requiere el permiso `swave.admin`, que la consola siempre tiene.

Un administrador también puede lanzarla a mano con el mismo comando.

---

## Comandos

| Comando | Qué hace | Permiso |
|---|---|---|
| `/swave start <comprador> [paquete...]` | Inicia (o extiende) una celebración. | `swave.admin` |
| `/swave stop` | Corta la celebración actual. | `swave.admin` |
| `/swave reload` | Recarga la configuración. | `swave.admin` |

`swave.claim` (por defecto todos) decide quién puede reclamar escribiendo GG.

---

## Pensado para servidores llenos

El chat de Bukkit llega en un hilo aparte del principal, y en un servidor con cientos de
jugadores un modo así podría convertirse en una fuente de lag. SWave lo evita:

- **Cuando no hay celebración** —el 99 % del tiempo— el listener sale en la primera línea.
  No hace nada mientras no toca.
- **La reclamación es sin candados.** Se apoya en estructuras atómicas, así que quinientos
  GG en el mismo instante no se bloquean entre sí. Probado con 500 hilos simultáneos:
  exactamente una recompensa por jugador, ni una de más.
- **Nada que toque la API de Bukkit corre fuera del hilo principal.** Repartir recompensas,
  anunciar y arrancar o parar se despachan al hilo del servidor a través del core; el hilo
  del chat solo lee estado y calcula colores, que es trabajo puro.
- **Tope de recompensas opcional** (`wave.max-rewards`) para acotar el gasto en oleadas
  muy grandes.

---

## Configuración

Todo se ajusta en `config.yml`: duración de la ventana, palabras que cuentan como GG,
formato y velocidad del arcoíris del chat, comandos de recompensa (`%player%`) y todos
los mensajes. Tras editarlo, `/swave reload`.
