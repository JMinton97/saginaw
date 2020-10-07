# _saginaw_: Route Mapping for Long-Distance Cycle Touring
A desktop route-finding and visualising program developed for an undergraduate final year project in Computer Science at the University of Birmingham. Developed between October 2018 and April 2019.

## Project overview:
- Utilised OpenStreetMap data to create a graph representation of all cycleable routes within Great Britain. Millions of graph vertices and edges were stored in a serialisable format that could be loaded into memory at run-time
- Implemented a number of shortest-route-finding algorithms to find shortest routes across this graph. Techniques ranged from simple Dijkstra, to bi-directional Dijkstra, to goal-directed Dijkstra utilising pre-computed 'landmark' vertices, to hierarchical techniques utilising a 'core' graph achieved by contraction of the full graph, and combinations of these. Parallel processing was also employed to run different parts of the search concurrently.
- Additionally, the on-screen map was rendered entirely from scratch from the same OpenStreetMap data, generating thousands of PNG map tiles at various map scales which are stitched together during runtime.
- The best performing route-finding algorithm -- parallel, bi-directional, goal-directed search utilising a contracted graph 'core' -- took 23ms to find routes on average, rising to only 30ms for locations over 250km apart. This was 98.4% faster than simple Dijkstra searching.
- This level of performance allowed routes to not only be calculated and displayed nearly instantly, but also manipulated in 'real-time'. A 'route dragging' mechanism was implemented, where the user could click and drag on any part of the route to create a new waypoint along that segment, with the updated route being displayed 'live'.

![Creating a route](saginaw/res/gifs/clip1.gif?raw=true)

![Viewing a route](saginaw/res/gifs/clip2.gif?raw=true)

![Zooming out](saginaw/res/gifs/clip3.gif?raw=true)

Saginaw [_sag-uh-naw_]: a town in east-central Michigan on an arm of Lake Huron. Referenced in the 1968 Paul Simon song _America_; "it took me four days to hitch-hike from Saginaw..."

![Project cover image](saginaw/res/icon/splash.png?raw=true)
