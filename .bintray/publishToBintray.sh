#!/bin/bash
echo Publishing binary artifacts to bintray.

if [ "$TRAVIS_BRANCH" == "master" ]
then
    BINTRAY_TARGET=Release
else
    BINTRAY_TARGET=Snapshots
fi

for f in ./target/perfmon4j-project*.zip
do
	echo curl -T $f -ddeuchert:$BINTRAY_API_KEY https://api.bintray.com/content/fss-development/Perfmon4j/$BINTRAY_TARGET/$(basename "$f")
	echo curl -XPOST -ddeuchert:$BINTRAY_API_KEY https://api.bintray.com/content/fss-development/Perfmon4j/$BINTRAY_TARGET/$(basename "$f")/publish
done
