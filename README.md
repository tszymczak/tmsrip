# tmsrip
tmsrip is a program that downloads satellite imagery from any TMS server. It's useful for saving satellite imagery (or OSM map tiles) for offline usage. Note that this utility sometimes requests large numbers of files. Therefore, please take care to use it responsibly and follow all limits that webmasters put on your usage of imagery services.

## Usage
```
tmsrip [options]
```

Options:

```-z LEVEL (Required)```

Minimum tile zoom level

```-Z LEVEL (Required)```

Maximum tile zoom level
    
```-b BBOX (Required)```

Bounding box to download the data
    
```-o OUTDIR (Required)```

The directory where the downloaded tiles will be saved.
    
```-t THREADS```

How many threads to use when downloading. In other words, how many tiles can be downloaded at the same time. More threads should generally allow faster downloads, up to a reasonable limit.

```-l LIMIT```

Do not download more than LIMIT tiles. -1 means no limit.

```-w```

Do not overwrite existing files.
    
BBox:
	Normal OSM Bounding box.
