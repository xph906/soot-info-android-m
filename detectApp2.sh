#!/bin/bash

if [ "$#" -ne 1 ]; then
	echo "usage: ./detectApp.sh filepath"
	exit 1
fi

platformpath=/space/xpan/android/android-sdk-linux/platforms
filepath=$1
filename=`basename "$filepath"`

#java -Xmx16g -cp .:soot-trunk.jar:soot-infoflow.jar:soot-infoflow-android.jar:slf5j-api-1.7.5.jar:slf4j-simple-1.7.5.jar:axml-2.0.jar soot.jimple.infoflow.android.TestApps.Test  "$filepath" "$platformpath" --SYSTIMEOUT 100 --nostatic --aplength 1 --aliasflowins --nocallbacks --layoutmode none --noarraysize > logs/"$filename".txt
java -Xmx16g -cp .:soot-trunk.jar:soot-infoflow.jar:soot-infoflow-android.jar:slf5j-api-1.7.5.jar:slf4j-simple-1.7.5.jar:axml-2.0.jar soot.jimple.infoflow.android.TestApps.Test  "$filepath" "$platformpath" --noarraysize --aplength 1  
