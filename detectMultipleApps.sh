#!/bin/bash

if [ "$#" -ne 1 ]; then
	echo "usage: ./detectMultipleApps.sh appPathList.txt"
	exit 1
fi

appPathList="$1"

while read -r line
do
	filepath="$line"
	echo $filepath
	./detectApp.sh $filepath
done < "$appPathList"
