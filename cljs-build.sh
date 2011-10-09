#!/bin/sh

cljs-watch src/weekly_hn/weekly_hn.cljs '{:output-to "resources/public/weekly_hn.js" :optimizations :advanced}'
