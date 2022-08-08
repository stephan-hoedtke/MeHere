


# MeHere - Todo

## Visibility, Accessibility
- Switch map between vector and satellite views
- Change size of letters (of street names)
- Change size of buttons (smaller)
- Show zoom level on zoom in / zoom out buttons


## Program features
Manage a list of journey 
-date
- start
- view
- delete

-Manage a list of point of interests
- date
- journey
- type
- description
- geo position
- city/street/area

Add point of interest with description and geo position and type (defining the icon) by clicking into the map.
Types: 
- start: starting position
- track: tracking position
- sight: position and description

Show altitude

Background process to track journey

Signalling when a certain point is reached

Compass features: 
- Show the current direction like google maps does 
- Rotate map automatically
- Rotate map just on button press and keep that rotation

## Progamming
Database 
- journey
- point of interests (related to a journey)
- journey tracked position (every view minutes)

API usage:
- komoot API to find current city / street / area...

Offline mode:
- Loading map tiles for a certain area in advance.

MapsForge
- use mapseForge tiles for better readability:
- https://osmdroid.github.io/osmdroid/Mapsforge.html
- https://stackoverflow.com/questions/10883653/streetnames-openstreetmaps-more-readable-on-android

## Bugs
Rotation: 
- Rotation with fling

Lint-Warnings
- View.OnTouchListener -> onTouch lambda should call View#performClick when a click is detected
- MenuProvider (deprecated: onOptionsMenu ...)
- EarthFragment: if (marker == null) ...