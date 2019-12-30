#!/bin/bash

for i in $( seq 0 30 ); do
  echo "stdout=${i}" >&1
  sleep 0.125
  echo "stderr=${i}" >&2
  sleep 0.125
done