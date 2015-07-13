#!/bin/bash

if [ "$TRAVIS_BRANCH" == "master" ]
then
    BINTRAY_TARGET=Release
else
    BINTRAY_TARGET=Snapshots
fi

for fullpath in ./target/perfmon4j-project*.zip
do
	filename=$(basename "$fullpath")
	version=$(echo $filename | grep -oE "([0-9]+?\.[0-9]+?\.[0-9]+?)")
	publishName=https://api.bintray.com/content/fss-development/Perfmon4j/${BINTRAY_TARGET}/${version}/${filename}
	echo Publishing $publishName to bintray.	
	curl -T $fullpath -uddeuchert:$BINTRAY_API_KEY ${publishName}?override=1\&publish=1
done

