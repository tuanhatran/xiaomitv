# Xiaomi Mi Box S 2nd gen (MiTV-AFKR0, jaws)

Google TV on Android 11 (SDK 30), 2 GB RAM. The debloat list for this box is in [debloat.txt](debloat.txt).

## Home screen

The custom launcher owns Home after the stock launcher and setupwraith are disabled. Clock, weather, air quality, system stats and the Netflix/YouTube/BrowseHere/France TV row are all shown on the launcher's own screen.

![Home screen with Netflix row](../tv-frost-home.png.png)

## App row

The second row holds VLC, Xiaomi TV+, YouTube Kids and YouTube Music. Scrolling down shows the rest of the apps.

![App row with VLC and Xiaomi TV+](../tv-fix-1.png)

![Scrolled view](../tv-snap-4.png)

## Resource use

Figures from the launcher's System card on the screenshots above:

| Resource | Value |
|---|---|
| Memory | about 1.3 GB of 1.9 GB used |
| Storage | 3.3 GB of 4.3 GB used |
| CPU | 12% to 76% while on the home screen |

## Apply

```
TV_HOST=<box ip> tools/disable.sh examples/xiaomi-mi-box-s-2nd-gen/debloat.txt
```

To undo:

```
TV_HOST=<box ip> tools/restore.sh examples/xiaomi-mi-box-s-2nd-gen/debloat.txt com.google.android.apps.tv.launcherx
```
